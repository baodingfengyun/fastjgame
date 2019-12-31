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
import java.util.concurrent.Executor;
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

    public NettyFutureAdapter(Future<V> future) {
        this.executor = null;
        this.future = future;
    }

    /**
     * @param executor 异步执行的默认executor
     */
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
    public ListenableFuture<V> await() throws InterruptedException {
        future.await();
        return this;
    }

    @Override
    public ListenableFuture<V> awaitUninterruptibly() {
        future.awaitUninterruptibly();
        return this;
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
    public ListenableFuture<V> addListener(@Nonnull FutureListener<? super V> listener) {
        addListener0(listener, executor);
        return this;
    }

    @Override
    public ListenableFuture<V> addListener(@Nonnull FutureListener<? super V> listener, @Nonnull EventLoop bindExecutor) {
        addListener0(listener, bindExecutor);
        return this;
    }

    private void addListener0(@Nonnull FutureListener<? super V> listener, @Nullable Executor bindExecutor) {
        if (null == bindExecutor) {
            future.addListener(future1 -> listener.onComplete(this));
        } else {
            future.addListener(future1 -> notifyAsync(listener, bindExecutor));
        }
    }

    private void notifyAsync(FutureListener<? super V> listener, Executor bindExecutor) {
        bindExecutor.execute(() -> {
            try {
                listener.onComplete(this);
            } catch (Exception e) {
                ConcurrentUtils.rethrow(e);
            }
        });
    }

    @Override
    public ListenableFuture<V> removeListener(@Nonnull FutureListener<? super V> listener) {
        // 需要ConcurrentMap保存映射才能删除，比较麻烦，先不支持
        throw new UnsupportedOperationException();
    }
}
