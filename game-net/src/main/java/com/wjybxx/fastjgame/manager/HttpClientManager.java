/*
 * Copyright 2019 wjybxx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wjybxx.fastjgame.manager;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.net.http.HttpCallback;
import com.wjybxx.fastjgame.utils.CloseableUtils;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import okhttp3.*;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * http客户端控制器。
 * 该控制器负责请求和响应的线程安全。
 * OkHttp 4.x用kotlin重写了
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/28 19:56
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class HttpClientManager {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientManager.class);

    /**
     * 请注意查看{@link Dispatcher#executorService()}默认创建executorService的方式。
     */
    private OkHttpClient okHttpClient;

    @Inject
    public HttpClientManager() {

    }

    public void init(int httpRequestTimeout) {
        if (null != okHttpClient) {
            throw new IllegalStateException();
        }
        okHttpClient = new OkHttpClient.Builder()
                .callTimeout(httpRequestTimeout, TimeUnit.SECONDS)
                .build();
    }

    public void shutdown() {
        okHttpClient.dispatcher().cancelAll();
        okHttpClient.dispatcher().executorService().shutdown();
        logger.info("HttpClientManager shutdown success");
    }

    /**
     * 同步get请求
     */
    public byte[] syncGet(String url, Map<String, String> params) throws IOException {
        Request request = new Request.Builder().get().url(buildGetUrl(url, params)).build();
        return readBody(okHttpClient.newCall(request).execute());
    }

    /**
     * 异步get请求
     *
     * @param url          url
     * @param params       get参数
     * @param eventLoop    用户线程，可以实现线程安全
     * @param httpCallback 回调
     */
    public void asyncGet(String url, Map<String, String> params, EventLoop eventLoop, HttpCallback httpCallback) {
        Request request = new Request.Builder().get().url(buildGetUrl(url, params)).build();
        okHttpClient.newCall(request).enqueue(new DelegateEventCallBack(eventLoop, httpCallback));
    }

    /**
     * 同步post请求
     */
    public byte[] syncPost(String url, Map<String, String> params) throws IOException {
        Request request = new Request.Builder().url(checkUrl(url)).post(buildPostBody(params)).build();
        return readBody(okHttpClient.newCall(request).execute());
    }

    /**
     * 异步post请求
     *
     * @param url          url
     * @param params       post参数
     * @param eventLoop    用户线程，可以实现线程安全
     * @param httpCallback 回调
     */
    public void asyncPost(String url, Map<String, String> params, EventLoop eventLoop, HttpCallback httpCallback) {
        Request request = new Request.Builder().url(checkUrl(url)).post(buildPostBody(params)).build();
        okHttpClient.newCall(request).enqueue(new DelegateEventCallBack(eventLoop, httpCallback));
    }

    /**
     * 构建get请求的参数部分
     *
     * @param url    远程地址
     * @param params get请求参数
     * @return full request
     */
    private String buildGetUrl(String url, Map<String, String> params) {
        String safeUrl = checkUrl(url);
        StringBuilder builder = new StringBuilder(safeUrl);
        // 是否添加&符号
        boolean appendAnd = false;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (appendAnd) {
                builder.append("&");
            }
            builder.append(entry.getKey()).append("=").append(entry.getValue());
            appendAnd = true;
        }
        return builder.toString();
    }

    /**
     * 检查url格式，默认http协议
     *
     * @param url 待检查的url
     * @return 正确的url格式
     */
    private String checkUrl(final String url) {
        String safeUrl;
        if (url.startsWith("http://") || url.startsWith("https://")) {
            safeUrl = url;
        } else {
            safeUrl = "http://" + url;
        }
        // 末尾添加参数之前添加?
        if (safeUrl.charAt(safeUrl.length() - 1) != '?') {
            safeUrl = safeUrl + "?";
        }
        return safeUrl;
    }

    /**
     * 构建post请求的数据部分
     *
     * @param params post参数
     * @return body
     */
    private RequestBody buildPostBody(Map<String, String> params) {
        FormBody.Builder builder = new FormBody.Builder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    /**
     * 封装原生回调，以保证线程安全
     */
    private static class DelegateEventCallBack implements Callback {

        private final EventLoop eventLoop;
        private final HttpCallback httpCallback;

        private DelegateEventCallBack(EventLoop eventLoop, HttpCallback httpCallback) {
            this.eventLoop = eventLoop;
            this.httpCallback = httpCallback;
        }

        @Override
        public void onFailure(@Nonnull Call call, @Nonnull IOException cause) {
            // 提交到用户所在线程，以保证线程安全
            ConcurrentUtils.safeExecute(eventLoop, () -> {
                httpCallback.onFailure(cause);
            });
        }

        @Override
        public void onResponse(@Nonnull Call call, @Nonnull Response response) throws IOException {
            // 提交到用户所在线程，以保证线程安全
            ConcurrentUtils.safeExecute(eventLoop, () -> {
                httpCallback.onResponse(readBody(response));
            });
        }
    }

    /**
     * 读取okHttp响应内容
     *
     * @param response okHttp响应
     * @return bytes
     */
    private static byte[] readBody(final Response response) {
        final ResponseBody body = response.body();
        if (body == null) {
            return ArrayUtils.EMPTY_BYTE_ARRAY;
        }

        try {
            return body.bytes();
        } catch (Throwable e) {
            return ConcurrentUtils.rethrow(e);
        } finally {
            CloseableUtils.closeQuietly(body);
        }
    }
}
