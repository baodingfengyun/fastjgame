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

package com.wjybxx.fastjgame.utils.concurrent;

import java.util.concurrent.CompletableFuture;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/4/26
 */
public class JdkFutureAdapters {

    public static <V> Promise<V> delegateFuture(CompletableFuture<V> completableFuture) {
        final Promise<V> promise = new JdkPromise<>(completableFuture);
        completableFuture.whenComplete(new FutureUtils.UniRelay<>(promise));
        return promise;
    }

    private static class JdkPromise<V> extends DefaultPromise<V> {

        final CompletableFuture<V> completableFuture;

        private JdkPromise(CompletableFuture<V> completableFuture) {
            this.completableFuture = completableFuture;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return completableFuture.cancel(mayInterruptIfRunning) && super.cancel(mayInterruptIfRunning);
        }
    }
}
