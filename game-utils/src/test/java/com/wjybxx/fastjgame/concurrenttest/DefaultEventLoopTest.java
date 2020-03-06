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

package com.wjybxx.fastjgame.concurrenttest;

import com.wjybxx.fastjgame.utils.concurrent.*;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/23
 * github - https://github.com/hl845740757
 */
public class DefaultEventLoopTest {

    private static DefaultEventLoopGroup eventLoopGroup = new DefaultEventLoopGroup(2, new DefaultThreadFactory("test"), RejectedExecutionHandlers.abort());

    public static void main(String[] args) {

        final ListenableFuture<String> future = eventLoopGroup.submit(() -> {
            System.out.println("before task1 return.");
            Thread.sleep(200);
            return "-hello world.";
        });

        final ListenableFuture<String> future2 = eventLoopGroup.submit(() -> {
            System.out.println("before task2  return.");
            Thread.sleep(200);
            return "-java";
        });

        System.out.println("before await");
        future.awaitUninterruptibly();
        future2.awaitUninterruptibly();

        System.out.println(future.getNow());
        System.out.println(future2.getNow());

        final ListenableFuture<String> future3 = eventLoopGroup.submit(() -> {
            Thread.sleep(500);
            return "500";
        });

        future3.onComplete((listenableFuture) -> {
            System.out.println("1 onComplete, thread " + Thread.currentThread().getName() + ",result = " + listenableFuture.getNow());
        }, GlobalEventLoop.INSTANCE);

        future3.onComplete((listenableFuture) -> {
            System.out.println("2 onComplete, thread " + Thread.currentThread().getName() + ",result = " + listenableFuture.getNow());
        });

        future3.onComplete((listenableFuture) -> {
            System.out.println("3 onComplete, thread " + Thread.currentThread().getName() + ",result = " + listenableFuture.getNow());
            // 已完成的时候添加监听
            future3.onComplete(listenableFuture2 -> {
                System.out.println("4 onComplete, thread " + Thread.currentThread().getName() + ",result = " + listenableFuture2.getNow());
            }, GlobalEventLoop.INSTANCE);
        });


        eventLoopGroup.shutdown();
    }
}
