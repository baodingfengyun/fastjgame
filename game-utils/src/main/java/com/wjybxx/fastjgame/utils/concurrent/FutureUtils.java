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

import java.util.function.BiConsumer;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/6
 */
public class FutureUtils {

    private FutureUtils() {
    }

    public static <V> Promise<V> newPromise() {
        return new DefaultPromise<>();
    }

    public static <V> FluentFuture<V> newSucceedFuture(V result) {
        return new DefaultPromise<>(result);
    }

    public static <V> FluentFuture<V> newFailedFuture(Throwable cause) {
        return new DefaultPromise<>(cause);
    }

    /**
     * 将future的结果传输到promise上
     */
    public static <V> void setFuture(FluentFuture<V> future, Promise<? super V> promise) {
        final UniRelay<? super V> uniRelay = new UniRelay<>(promise);
        if (!future.acceptNow(uniRelay)) {
            future.addListener(uniRelay);
        }
    }

    static class UniRelay<V> implements BiConsumer<V, Throwable> {

        final Promise<V> promise;

        UniRelay(Promise<V> promise) {
            this.promise = promise;
        }

        @Override
        public void accept(V v, Throwable throwable) {
            if (throwable != null) {
                promise.tryFailure(throwable);
            } else {
                promise.trySuccess(v);
            }
        }

    }

}
