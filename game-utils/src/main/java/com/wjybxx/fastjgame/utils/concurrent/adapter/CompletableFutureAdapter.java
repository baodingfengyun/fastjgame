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

package com.wjybxx.fastjgame.utils.concurrent.adapter;

import com.wjybxx.fastjgame.utils.ThreadUtils;
import com.wjybxx.fastjgame.utils.concurrent.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.*;

/**
 * JDK{@link CompletableFuture}的适配器。
 * <p>
 * 其实在最开始构建并发组件的时候，我就想过是选择{@link BlockingFuture}还是JDK的{@link CompletableFuture}，
 * 扫一遍{@link CompletableFuture}，它的api实在是太多了，理解和使用成本都太高，不适合暴露给逻辑程序员使用，而对其进行封装的成本更高，
 * 且游戏内一般并不需要特别多的功能，所以最终选择了{@link BlockingFuture}。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/30
 * github - https://github.com/hl845740757
 */
public class CompletableFutureAdapter<V> extends AbstractBlockingFuture<V> {

    private final EventLoop executor;
    private final CompletableFuture<V> future;

    /**
     * @param executor 异步执行的默认executor
     */
    public CompletableFutureAdapter(EventLoop executor, CompletableFuture<V> future) {
        this.executor = executor;
        this.future = future;
    }

    @Override
    public final boolean isDone() {
        return future.isDone();
    }

    @Override
    public final boolean isSuccess() {
        return !future.isCompletedExceptionally();
    }

    @Override
    public final boolean isCancelled() {
        return future.isCancelled();
    }

    @Override
    public final boolean isCancellable() {
        return true;
    }

    @Override
    public final V get() throws InterruptedException, ExecutionException {
        return future.get();
    }

    @Override
    public final V get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }

    @Override
    public final V join() throws CompletionException {
        return future.join();
    }

    @Nullable
    @Override
    public final V getNow() {
        return future.getNow(null);
    }

    @Nullable
    @Override
    public final Throwable cause() {
        try {
            future.getNow(null);
            return null;
        } catch (CompletionException e) {
            return e.getCause();
        } catch (Throwable e) {
            return e;
        }
    }

    @Override
    public final boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    @Override
    public BlockingFuture<V> await() throws InterruptedException {
        try {
            future.get();
        } catch (InterruptedException e) {
            throw e;
        } catch (Throwable ignore) {

        }
        return this;
    }

    @Override
    public BlockingFuture<V> awaitUninterruptibly() {
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
            ThreadUtils.recoveryInterrupted(true);
        } catch (Throwable ignore) {
        }
        return false;
    }

    // -------------------------------------------------- 监听器管理 ---------------------------------------------
    @Override
    public final EventLoop defaultExecutor() {
        return executor;
    }

    private void addListener(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        // 不要内联该对象 - lambda表达式捕获的对象不一样
        final FutureListenerEntry<? super V> listenerEntry = new FutureListenerEntry<>(listener, bindExecutor);
        future.thenRun(() -> FutureUtils.notifyListenerNowSafely(this, listenerEntry));
    }

    @Override
    public BlockingFuture<V> onComplete(@Nonnull FutureListener<? super V> listener) {
        addListener(listener, executor);
        return this;
    }

    @Override
    public BlockingFuture<V> onComplete(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        addListener(listener, bindExecutor);
        return this;
    }

    @Override
    public BlockingFuture<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener) {
        addListener(listener, executor);
        return this;
    }

    @Override
    public BlockingFuture<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        addListener(listener, bindExecutor);
        return this;
    }

    @Override
    public BlockingFuture<V> onFailure(@Nonnull FailedFutureListener<? super V> listener) {
        addListener(listener, executor);
        return this;
    }

    @Override
    public BlockingFuture<V> onFailure(@Nonnull FailedFutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        addListener(listener, bindExecutor);
        return this;
    }
}
