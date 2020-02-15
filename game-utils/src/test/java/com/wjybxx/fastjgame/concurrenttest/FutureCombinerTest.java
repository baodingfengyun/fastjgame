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

import com.wjybxx.fastjgame.concurrent.FutureCombiner;
import com.wjybxx.fastjgame.concurrent.ImmediateEventLoop;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.concurrent.Promise;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/15
 */
public class FutureCombinerTest {

    public static void main(String[] args) {
        // 测试可以使用ImmediateEventLoop.INSTANCE，其它时候不要使用
        final Promise<String> aFuture = ImmediateEventLoop.INSTANCE.newPromise();

        aFuture.addListener(future -> {
            System.out.println("CallbackA, Thread : " + Thread.currentThread().getName());
            System.out.println("a result " + getResultAsStringSafely(future));
        });

        final Promise<String> bFuture = ImmediateEventLoop.INSTANCE.newPromise();
        bFuture.addListener(future -> {
            System.out.println("CallbackB, Thread : " + Thread.currentThread().getName());
            System.out.println("b result " + getResultAsStringSafely(future));
        });

        new FutureCombiner(ImmediateEventLoop.INSTANCE)
                .add(aFuture)
                .add(bFuture)
                .finish(ImmediateEventLoop.INSTANCE.newPromise())
                .addListener(future -> {
                    System.out.println("Callback Combine, Thread : " + Thread.currentThread().getName());
                    System.out.println("result " + getResultAsStringSafely(future));
                });

        aFuture.trySuccess("success");
        bFuture.tryFailure(new Exception("failure"));
    }

    private static String getResultAsStringSafely(ListenableFuture<?> future) {
        if (future.isSuccess()) {
            return String.valueOf(future.getNow());
        } else {
            final Throwable cause = future.cause();
            assert null != cause;
            return ExceptionUtils.getStackTrace(cause);
        }
    }
}
