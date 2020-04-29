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

import com.wjybxx.fastjgame.utils.concurrent.FutureUtils;
import com.wjybxx.fastjgame.utils.concurrent.GlobalEventLoop;
import com.wjybxx.fastjgame.utils.concurrent.Promise;

import java.util.concurrent.ExecutionException;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/4/27
 */
public class DefaultPromiseTest {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        final Promise<String> promise = FutureUtils.newPromise();
        promise.thenAccept(System.out::println)
                .whenExceptionally(System.out::println);

        promise.addListener(f -> {
            System.out.println("Thread : " + Thread.currentThread().getName() + ", value : " + f.getNow());
        }, GlobalEventLoop.INSTANCE);

        promise.trySuccess("success");

        FutureUtils.newSucceedFuture("hello world")
                .thenAccept(System.out::println)
                .getNow();

        FutureUtils.newFailedFuture(new RuntimeException("already failed"))
                .thenRun(() -> System.out.println("failed future run?"))
                .getNow();
    }

}
