/*
 *  Copyright 2019 wjybxx
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to iBn writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.wjybxx.fastjgame.misc;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.concurrent.adapter.CompletableFutureAdapter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

/**
 * HttpClient代理。
 * 新版本httpClient管理器 - 使用JDK11自带的{@link java.net.http.HttpClient}。
 * <p>
 * Q: 为什么值得改动？
 * A: 1. jdk11的{@link java.net.http.HttpClient}是异步非阻塞的，一个IO线程负责所有http请求。而OkHttp3/Apache HttpClient是线程池。
 * 2. 其异步接口更加优秀。
 * <p>
 * Q: 为什么要弄个中间对象出来？
 * - 统一管理超时时间。
 * - 统一消息转发。
 * - 方便测试。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/30
 * github - https://github.com/hl845740757
 */
public class HttpClientProxy {

    private final HttpClient httpClient;
    private final EventLoop appEventLoop;
    private final Duration httpRequestTimeout;

    public HttpClientProxy(@Nonnull HttpClient httpClient, @Nonnull EventLoop appEventLoop, long httpRequestTimeout) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.appEventLoop = Objects.requireNonNull(appEventLoop);
        this.httpRequestTimeout = Duration.ofSeconds(httpRequestTimeout);
    }

    /**
     * 如果你需要更多的灵活性 - 你可以通过httpClient来操作
     */
    public HttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * 获取绑定的用户线程
     */
    public EventLoop getAppEventLoop() {
        return appEventLoop;
    }

    /**
     * @param builder             http请求内容，之所以使用builder而不是构建完成的request是为了检查是否设置了超时时间。
     * @param responseBodyHandler 响应解析器
     * @param <T>                 响应内容的类型
     * @return 响应的内容
     * @throws IOException          if an I/O error occurs when sending or receiving
     * @throws InterruptedException if the operation is interrupted
     */
    public <T> HttpResponse<T> send(HttpRequest.Builder builder, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException {
        return httpClient.send(appendTimeout(builder), responseBodyHandler);
    }

    /**
     * @param builder             http请求内容，之所以使用builder而不是构建完成的request是为了检查是否设置了超时时间。
     * @param responseBodyHandler 响应解析器
     * @param <T>                 响应内容的类型
     * @return Future - 注意：该future的执行关键就在游戏逻辑线程。
     */
    public <T> ListenableFuture<HttpResponse<T>> sendAsync(HttpRequest.Builder builder, HttpResponse.BodyHandler<T> responseBodyHandler) {
        return new CompletableFutureAdapter<>(appEventLoop, httpClient.sendAsync(appendTimeout(builder), responseBodyHandler));
    }

    /**
     * 添加超时时间
     *
     * @param builder http请求内容的builder
     * @return httpRequest
     */
    private HttpRequest appendTimeout(HttpRequest.Builder builder) {
        return builder.timeout(httpRequestTimeout)
                .build();
    }
}
