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

package com.wjybxx.fastjgame.util.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/6
 */
public class FutureUtils {

    private FutureUtils() {

    }

    private static final FluentFuture<?> EMPTY_FUTURE = newSucceedFuture(null);

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 创建一个{@link Promise}。
     * 用户提交一个任务，执行方持有Promise，用户方持Future，执行方通过Promise赋值，用户通过Future获取结果或监听。
     */
    public static <V> Promise<V> newPromise() {
        return new DefaultPromise<>();
    }

    /**
     * 创建一个{@link FluentFuture}，该future表示它关联的任务早已正常完成。因此{@link FluentFuture#isCompletedExceptionally()}总是返回false。
     * 所有添加到该future上的{@link FutureListener}都会立即被通知。并且该future上的所有阻塞方法会立即返回而不会阻塞。
     */
    public static <V> FluentFuture<V> newSucceedFuture(V result) {
        return new DefaultPromise<>(result);
    }

    /**
     * 创建一个{@link FluentFuture}，该future表示它关联的任务早已失败。因此{@link FluentFuture#isCompletedExceptionally()}总是返回true。
     * 所有添加到该future上的{@link FutureListener}都会立即被通知。并且该future上的所有阻塞方法会立即返回而不会阻塞。
     *
     * @param <V>   the type of value
     * @param cause 任务失败的原因
     * @return Future
     */
    public static <V> FluentFuture<V> newFailedFuture(Throwable cause) {
        return new DefaultPromise<>(cause);
    }

    /**
     * 返回结果为空的future，该future表示它关联的任务早已正常完成，但结果为null。
     */
    @SuppressWarnings("unchecked")
    public static <V> FluentFuture<V> emptyFuture() {
        return (FluentFuture<V>) EMPTY_FUTURE;
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 将future的结果传输到promise上
     */
    public static <V> void setFuture(Promise<? super V> promise, ListenableFuture<V> future) {
        final UniRelay<? super V> uniRelay = new UniRelay<>(promise);
        if (!future.acceptNow(uniRelay)) {
            future.addListener(uniRelay);
        }
    }

    private static class UniRelay<V> implements FutureListener<V>, BiConsumer<V, Throwable> {

        final Promise<V> promise;

        UniRelay(Promise<V> promise) {
            this.promise = promise;
        }

        @Override
        public void onComplete(ListenableFuture<V> future) throws Exception {
            future.acceptNow(this);
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

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * {@link java.util.concurrent.CompletableFuture}总是使用{@link CompletionException}包装异常，我们需要找到原始异常
     */
    public static Throwable unwrapCompletionException(Throwable t) {
        while (t instanceof CompletionException && t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }

    public static <V> Promise<V> fromCompletableFuture(CompletableFuture<V> completableFuture) {
        final Promise<V> promise = new JdkPromise<>(completableFuture);
        completableFuture.whenComplete(new UniRelay<>(promise));
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