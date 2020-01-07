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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * JDK{@link CompletableFuture}的适配器。
 * <p>
 * 其实在最开始构建并发组件的时候，我就想过是选择{@link ListenableFuture}还是JDK的{@link CompletableFuture}，
 * 扫一遍{@link CompletableFuture}，它的api实在是太多了，理解和使用成本都太高，不适合暴露给逻辑程序员使用，而对其进行封装的成本更高，
 * 且游戏内一般并不需要特别多的功能，所以最终选择了{@link ListenableFuture}。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/30
 * github - https://github.com/hl845740757
 */
public class CompletableFutureAdapter<V> implements ListenableFuture<V> {

    private final EventLoop executor;
    private final CompletableFuture<V> future;

    public CompletableFutureAdapter(CompletableFuture<V> future) {
        this.executor = null;
        this.future = future;
    }

    /**
     * @param executor 异步执行的默认executor
     */
    public CompletableFutureAdapter(EventLoop executor, CompletableFuture<V> future) {
        this.executor = executor;
        this.future = future;
    }

    @Override
    public boolean isDone() {
        return future.isDone();
    }

    @Override
    public boolean isSuccess() {
        return !future.isCompletedExceptionally();
    }

    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    @Override
    public boolean isCancellable() {
        return true;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return future.get();
    }

    @Override
    public V get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }

    @Nullable
    @Override
    public V getNow() {
        // jdk的getNow和自实现的getNow有区别
        try {
            return future.getNow(null);
        } catch (Throwable cause) {
            return null;
        }
    }

    @Nullable
    @Override
    public Throwable cause() {
        try {
            future.getNow(null);
            return null;
        } catch (Throwable cause) {
            return cause;
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    @Override
    public ListenableFuture<V> await() throws InterruptedException {
        try {
            future.get();
        } catch (InterruptedException e) {
            throw e;
        } catch (Throwable ignore) {

        }
        return this;
    }

    @Override
    public ListenableFuture<V> awaitUninterruptibly() {
        try {
            future.join();
        } catch (Throwable ignore) {

        }
        return this;
    }

    @Override
    public boolean await(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        try {
            future.get(timeout, unit);
            return true;
        } catch (InterruptedException e) {
            throw e;
        } catch (Throwable ignore) {
        }
        return false;
    }

    @Override
    public boolean awaitUninterruptibly(long timeout, @Nonnull TimeUnit unit) {
        try {
            // JDK不支持限时不中断的方式，暂时先不做额外处理
            future.get(timeout, unit);
            return true;
        } catch (InterruptedException e) {
            ConcurrentUtils.recoveryInterrupted(true);
        } catch (Throwable ignore) {
        }
        return false;
    }

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

    private void addListener0(@Nonnull FutureListener<? super V> listener, @Nullable EventLoop bindExecutor) {
        if (null == bindExecutor) {
            future.thenRun(() -> notify(listener));
        } else {
            future.thenRunAsync(() -> notify(listener), bindExecutor);
        }
    }

    private void notify(@Nonnull FutureListener<? super V> listener) {
        try {
            listener.onComplete(this);
        } catch (Exception e) {
            ConcurrentUtils.rethrow(e);
        }
    }

    @Override
    public ListenableFuture<V> removeListener(@Nonnull FutureListener<? super V> listener) {
        throw new UnsupportedOperationException();
    }
}
