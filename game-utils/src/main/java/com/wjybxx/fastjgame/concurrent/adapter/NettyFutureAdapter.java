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

package com.wjybxx.fastjgame.concurrent.adapter;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.FutureListener;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import io.netty.util.concurrent.Future;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 对Netty的{@link Future}进行适配 - 主要是适配监听器。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/1
 * github - https://github.com/hl845740757
 */
public final class NettyFutureAdapter<V> implements ListenableFuture<V> {

    private final EventLoop executor;
    private final Future<V> future;

    public NettyFutureAdapter(@Nonnull EventLoop executor, Future<V> future) {
        this.executor = executor;
        this.future = future;
    }

    @Override
    public boolean isSuccess() {
        return future.isSuccess();
    }

    @Override
    public boolean isCancellable() {
        return future.isCancellable();
    }

    @Nullable
    @Override
    public Throwable cause() {
        return future.cause();
    }

    @Override
    public void await() throws InterruptedException {
        future.await();
    }

    @Override
    public void awaitUninterruptibly() {
        future.awaitUninterruptibly();
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return future.await(timeout, unit);
    }

    @Override
    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
        return future.awaitUninterruptibly(timeout, unit);
    }

    @Override
    public V getNow() {
        return future.getNow();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    @Override
    public boolean isDone() {
        return future.isDone();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return future.get();
    }

    @Override
    public V get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }

    // ---------------------------------------------------------------------------------------------

    @Override
    public void addListener(@Nonnull FutureListener<? super V> listener) {
        future.addListener(new FutureListenerAdapter<>(this, listener, executor));
    }

    @Override
    public void addListener(@Nonnull FutureListener<? super V> listener, @Nonnull EventLoop bindExecutor) {
        future.addListener(new FutureListenerAdapter<>(this, listener, bindExecutor));
    }

    @Override
    public boolean removeListener(@Nonnull FutureListener<? super V> listener) {
        // 需要ConcurrentMap保存映射才能删除，比较麻烦，先不支持
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeListener(@Nonnull FutureListener<? super V> listener, @Nonnull EventLoop bindExecutor) {
        throw new UnsupportedOperationException();
    }

    /**
     * 转发到指定线程下执行回调逻辑
     *
     * @param <V>
     */
    private static class FutureListenerAdapter<V> implements io.netty.util.concurrent.FutureListener<V> {

        private final ListenableFuture<V> listenableFuture;
        private final FutureListener<? super V> futureListener;
        private final EventLoop bindExecutor;

        private FutureListenerAdapter(ListenableFuture<V> listenableFuture, FutureListener<? super V> futureListener, EventLoop bindExecutor) {
            this.listenableFuture = listenableFuture;
            this.futureListener = futureListener;
            this.bindExecutor = bindExecutor;
        }

        @Override
        public void operationComplete(Future<V> future) throws Exception {
            bindExecutor.execute(() -> {
                try {
                    futureListener.onComplete(listenableFuture);
                } catch (Exception e) {
                    ConcurrentUtils.rethrow(e);
                }
            });
        }
    }
}
