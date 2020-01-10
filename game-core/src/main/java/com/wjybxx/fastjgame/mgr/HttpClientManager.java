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

package com.wjybxx.fastjgame.mgr;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.concurrent.DefaultEventLoopGroup;
import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.net.http.HttpClientProxy;
import com.wjybxx.fastjgame.net.http.HttpFuture;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * <p>
 * 它不是线程安全的，需要的world绑定该管理器即可。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/30
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class HttpClientManager {

    private final GameConfigMgr gameConfigMgr;
    private final GameEventLoopMgr gameEventLoopMgr;
    private HttpClientProxy httpClientProxy;

    @Inject
    public HttpClientManager(GameConfigMgr gameConfigMgr, GameEventLoopMgr gameEventLoopMgr) {
        this.gameConfigMgr = gameConfigMgr;
        this.gameEventLoopMgr = gameEventLoopMgr;
    }

    public void start() {
        final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(gameConfigMgr.getHttpConnectTimeout()))
                .executor(new DefaultEventLoopGroup(gameConfigMgr.getHttpWorkerThreadNum(), new DefaultThreadFactory("HTTP-WORKER", true), RejectedExecutionHandlers.abort()))
                .build();
        httpClientProxy = new HttpClientProxy(httpClient, gameEventLoopMgr.getEventLoop(), gameConfigMgr.getHttpRequestTimeout());
    }

    /**
     * 如果你需要更多的灵活性 - 你可以通过httpClient来操作
     */
    public HttpClient getHttpClient() {
        if (null == httpClientProxy) {
            throw new IllegalStateException();
        }
        return httpClientProxy.getHttpClient();
    }

    /**
     * @param builder             http请求内容，之所以使用builder而不是构建完成的request是为了检查是否设置了超时时间。
     * @param responseBodyHandler 响应解析器
     * @param <T>                 响应内容的类型
     * @return 响应的内容
     * @throws IOException              if an I/O error occurs when sending or receiving
     * @throws InterruptedException     if the operation is interrupted
     * @throws IllegalArgumentException if timeout is empty
     */
    public <T> HttpResponse<T> send(HttpRequest.Builder builder, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException {
        return httpClientProxy.send(builder, responseBodyHandler);
    }

    /**
     * @param builder             http请求内容，之所以使用builder而不是构建完成的request是为了检查是否设置了超时时间。
     * @param responseBodyHandler 响应解析器
     * @param <T>                 响应内容的类型
     * @return Future - 注意：该future回调的执行就在游戏逻辑线程。
     * @throws IllegalArgumentException if timeout is empty
     */
    public <T> HttpFuture<HttpResponse<T>> sendAsync(HttpRequest.Builder builder, HttpResponse.BodyHandler<T> responseBodyHandler) {
        return httpClientProxy.sendAsync(builder, responseBodyHandler);
    }

    /**
     * @param request             http请求内容
     * @param responseBodyHandler 响应解析器
     * @param <T>                 响应内容的类型
     * @return 响应的内容
     * @throws IOException              if an I/O error occurs when sending or receiving
     * @throws InterruptedException     if the operation is interrupted
     * @throws IllegalArgumentException if timeout is empty
     */
    public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException {
        return httpClientProxy.send(request, responseBodyHandler);
    }

    /**
     * @param request             http请求内容
     * @param responseBodyHandler 响应解析器
     * @param <T>                 响应内容的类型
     * @return Future - 注意：该future回调的执行就在游戏逻辑线程
     * @throws IllegalArgumentException if timeout is empty
     */
    public <T> HttpFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
        return httpClientProxy.sendAsync(request, responseBodyHandler);
    }
}
