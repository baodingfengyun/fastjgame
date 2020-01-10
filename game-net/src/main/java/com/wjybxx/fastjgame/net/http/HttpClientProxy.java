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

package com.wjybxx.fastjgame.net.http;

import com.wjybxx.fastjgame.concurrent.EventLoop;

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
 * A: jdk11的{@link java.net.http.HttpClient}是reactor模式，1个selector线程 + N工作者线程。线程需求少，吞吐量更好。
 * 而OkHttp3/Apache HttpClient会创建大量的线程。
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

    /**
     * @param httpClient         注意：{@link HttpClient}并没有close方法，其自动关闭依赖于守护线程，因此制定executor时请注意。
     * @param appEventLoop       异步http请求回调的默认执行环境
     * @param httpRequestTimeout http请求默认超时时间
     */
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
     * @param builder             http请求内容，之所以使用builder而不是构建完成的request是为了统一设置超时时间。
     * @param responseBodyHandler 响应解析器
     * @param <T>                 响应内容的类型
     * @return 响应的内容
     * @throws IOException          if an I/O error occurs when sending or receiving
     * @throws InterruptedException if the operation is interrupted
     */
    public <T> HttpResponse<T> send(HttpRequest.Builder builder, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException {
        return httpClient.send(setTimeoutAndBuild(builder), responseBodyHandler);
    }

    /**
     * @param builder             http请求内容，之所以使用builder而不是构建完成的request是为了统一设置超时时间。
     * @param responseBodyHandler 响应解析器
     * @param <T>                 响应内容的类型
     * @return Future - 注意：该future回调的执行环境为{@link #appEventLoop}
     */
    public <T> HttpFuture<HttpResponse<T>> sendAsync(HttpRequest.Builder builder, HttpResponse.BodyHandler<T> responseBodyHandler) {
        return new DefaultHttpFuture<>(appEventLoop, httpClient.sendAsync(setTimeoutAndBuild(builder), responseBodyHandler));
    }

    /**
     * 添加超时时间
     *
     * @param builder http请求内容的builder
     * @return httpRequest
     */
    private HttpRequest setTimeoutAndBuild(HttpRequest.Builder builder) {
        return builder.timeout(httpRequestTimeout).build();
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
        ensureTimeoutPresent(request);
        return httpClient.send(request, responseBodyHandler);
    }

    /**
     * @param request             http请求内容
     * @param responseBodyHandler 响应解析器
     * @param <T>                 响应内容的类型
     * @return Future - 注意：该future回调的执行环境为{@link #appEventLoop}
     * @throws IllegalArgumentException if timeout is empty
     */
    public <T> HttpFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
        ensureTimeoutPresent(request);
        return new DefaultHttpFuture<>(appEventLoop, httpClient.sendAsync(request, responseBodyHandler));
    }

    /**
     * 确保超时时间存在
     *
     * @param request http请求内容
     */
    private void ensureTimeoutPresent(HttpRequest request) {
        if (request.timeout().isEmpty()) {
            throw new IllegalArgumentException("request timeout is empty");
        }
    }
}
