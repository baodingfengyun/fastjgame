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

package com.wjybxx.fastjgame.net.http;

import com.wjybxx.fastjgame.utils.concurrent.FluentFuture;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * 所有请求都具有超时时间限制的HttpClient。
 * <NOTE>该client强制检查超时时间</NOTE>
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/2
 * github - https://github.com/hl845740757
 */
public interface TimeoutHttpClient {

    /**
     * @param builder             http请求内容，之所以使用builder而不是构建完成的request是为了统一设置超时时间。
     * @param responseBodyHandler 响应解析器
     * @param <T>                 响应内容的类型
     * @return future
     * @throws IOException          if an I/O error occurs when sending or receiving
     * @throws InterruptedException if the operation is interrupted
     */
    <T> HttpResponse<T> send(HttpRequest.Builder builder, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException;

    /**
     * @param builder             http请求内容，之所以使用builder而不是构建完成的request是为了统一设置超时时间。
     * @param responseBodyHandler 响应解析器
     * @param <T>                 响应内容的类型
     * @return future
     */
    <T> FluentFuture<HttpResponse<T>> sendAsync(HttpRequest.Builder builder, HttpResponse.BodyHandler<T> responseBodyHandler);

    /**
     * @param request             http请求内容
     * @param responseBodyHandler 响应解析器
     * @param <T>                 响应内容的类型
     * @return 响应的内容
     * @throws IOException              if an I/O error occurs when sending or receiving
     * @throws InterruptedException     if the operation is interrupted
     * @throws IllegalArgumentException if timeout does not set
     */
    <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException;

    /**
     * @param request             http请求内容
     * @param responseBodyHandler 响应解析器
     * @param <T>                 响应内容的类型
     * @throws IllegalArgumentException if timeout does not set
     */
    <T> FluentFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler);
}
