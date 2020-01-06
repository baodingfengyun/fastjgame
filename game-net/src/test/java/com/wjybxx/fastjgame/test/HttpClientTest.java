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

package com.wjybxx.fastjgame.test;

import com.wjybxx.fastjgame.concurrent.DefaultEventLoop;
import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.net.http.HttpClientProxy;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 测试jdk11的HttpClient
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/30
 * github - https://github.com/hl845740757
 */
public class HttpClientTest {

    public static void main(String[] args) throws IOException, InterruptedException {
        final DefaultEventLoop appEventLoop = new DefaultEventLoop(null, new DefaultThreadFactory("APP"), RejectedExecutionHandlers.abort());
        final HttpClientProxy httpClientProxy = newHttpClientProxy(appEventLoop);

        try {
            syncGet(httpClientProxy);

            asycGet(httpClientProxy);

            appEventLoop.awaitTermination(1, TimeUnit.MINUTES);
        } finally {
            appEventLoop.shutdown();
        }
    }

    private static HttpClientProxy newHttpClientProxy(final EventLoop appEventLoop) {
        final HttpClient client = HttpClient.newBuilder()
                .executor(new DefaultEventLoop(null, new DefaultThreadFactory("HTTP-WORKER", true), RejectedExecutionHandlers.abort()))
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        return new HttpClientProxy(client, appEventLoop, 15);
    }

    private static void syncGet(HttpClientProxy httpClientProxy) throws IOException, InterruptedException {
        final HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("https://www.baidu.com/"));

        final HttpResponse<String> response = httpClientProxy.send(builder, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
    }

    private static void asycGet(HttpClientProxy httpClientProxy) {
        final HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("https://www.baidu.com/"));

        httpClientProxy.sendAsync(builder, HttpResponse.BodyHandlers.ofString())
                .addListener(future -> {
                    System.out.println("Thread: " + Thread.currentThread());
                    final HttpResponse<String> response = future.getIfSuccess();
                    if (null != response) {
                        System.out.println(response.body());
                    }
                    httpClientProxy.getAppEventLoop().shutdown();
                });
    }
}
