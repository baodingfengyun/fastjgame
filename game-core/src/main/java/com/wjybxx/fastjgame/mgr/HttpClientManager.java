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
import com.wjybxx.fastjgame.net.http.DefaultTimeoutHttpClient;
import com.wjybxx.fastjgame.net.http.HttpFuture;
import com.wjybxx.fastjgame.net.http.TimeoutHttpClient;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HttpClient管理器，将回调绑定到world线程。
 * 它不是线程安全的，需要的world绑定该管理器即可。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/30
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class HttpClientManager implements TimeoutHttpClient {

    private final GameConfigMgr gameConfigMgr;
    private final GameEventLoopMgr gameEventLoopMgr;
    private DefaultTimeoutHttpClient delegated;

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
        this.delegated = new DefaultTimeoutHttpClient(httpClient, gameEventLoopMgr.getEventLoop(), gameConfigMgr.getHttpRequestTimeout());
    }

    /**
     * 如果你需要更多的灵活性 - 你可以通过httpClient来操作
     */
    public HttpClient getHttpClient() {
        if (null == delegated) {
            throw new IllegalStateException();
        }
        return delegated.getHttpClient();
    }

    @Override
    public <T> HttpResponse<T> send(HttpRequest.Builder builder, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException {
        return delegated.send(builder, responseBodyHandler);
    }

    @Override
    public <T> HttpFuture<HttpResponse<T>> sendAsync(HttpRequest.Builder builder, HttpResponse.BodyHandler<T> responseBodyHandler) {
        return delegated.sendAsync(builder, responseBodyHandler);
    }

    @Override
    public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException {
        return delegated.send(request, responseBodyHandler);
    }

    @Override
    public <T> HttpFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
        return delegated.sendAsync(request, responseBodyHandler);
    }
}
