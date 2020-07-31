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

package com.wjybxx.fastjgame.concurrenttest;

import com.wjybxx.fastjgame.util.concurrent.*;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.concurrent.TimeUnit;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/15
 */
public class FutureCombinerTest {

    public static void main(String[] args) {
        // 测试可以使用ImmediateEventLoop.INSTANCE，其它时候不要使用
        final DefaultEventLoop eventLoopA = new DefaultEventLoop(null, new DefaultThreadFactory("AAAAA"), RejectedExecutionHandlers.abort());
        final DefaultEventLoop eventLoopB = new DefaultEventLoop(null, new DefaultThreadFactory("BBBBBB"), RejectedExecutionHandlers.abort());

        final FluentFuture<String> aFuture = eventLoopA.submit(() -> "success");
        aFuture.addListener(future -> {
            System.out.println("CallbackA, Thread : " + Thread.currentThread().getName());
            System.out.println("CallbackA, result " + getResultAsStringSafely(future));
        });

        final FluentFuture<String> bFuture = eventLoopB.submit(() -> {
            throw new Exception("failure");
        });
        bFuture.addListener(future -> {
            System.out.println("CallbackB, Thread : " + Thread.currentThread().getName());
            System.out.println("CallbackB, result " + getResultAsStringSafely(future));
        });

        final DefaultEventLoop appEventLoop = new DefaultEventLoop(null, new DefaultThreadFactory("APP"), RejectedExecutionHandlers.abort());
        appEventLoop.submit(() -> {
            doCombine(aFuture, bFuture, appEventLoop);
        });

        try {
            appEventLoop.terminationFuture().awaitUninterruptibly(3, TimeUnit.SECONDS);
        } finally {
            eventLoopA.shutdown();
            eventLoopB.shutdown();
            appEventLoop.shutdown();
        }
    }

    private static void doCombine(FluentFuture<String> aFuture, FluentFuture<String> bFuture, DefaultEventLoop appEventLoop) {
        new FutureCombiner()
                .add(aFuture)
                .add(bFuture)
                .finish(FutureUtils.newPromise())
                .addListener(future -> {
                    System.out.println("Callback Combine, Thread : " + Thread.currentThread().getName());
                    System.out.println("Callback Combine, result " + getResultAsStringSafely(future));
                });
    }

    private static <V> String getResultAsStringSafely(ListenableFuture<V> future) {
        Throwable cause = future.cause();
        if (cause != null) {
            return ExceptionUtils.getStackTrace(cause);
        } else {
            return String.valueOf(future.getNow());
        }
    }
}
