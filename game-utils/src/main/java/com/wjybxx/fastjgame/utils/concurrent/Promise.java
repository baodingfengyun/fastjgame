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

import javax.annotation.Nonnull;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * promise用于为关联的future赋值结果。
 * 我们希望业务的执行者专注于计算，暴露最少的api。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/6
 */
public interface Promise<V> extends FluentFuture<V> {

    /**
     * 将future标记为成功完成。
     * <p>
     * 如果该future对应的操作早已完成(失败或成功)，将抛出一个{@link IllegalStateException}.
     */
    void setSuccess(V result);

    /**
     * 尝试将future标记为成功完成。
     *
     * @return 当且仅当成功将future标记为成功完成时返回true，如果future对应的操作已完成(成功或失败)，则返回false，并什么都不改变。
     */
    boolean trySuccess(V result);

    /**
     * 将future标记为失败完成。
     * <p>
     * 如果future对应的操作早已完成（成功或失败），则抛出一个{@link IllegalStateException}.
     */
    void setFailure(@Nonnull Throwable cause);

    /**
     * 尝试将future标记为失败完成。
     *
     * @return 当前仅当成功将future标记为失败完成时返回true，如果future对应的操作已完成（成功或失败），则返回false，并什么也不改变。
     */
    boolean tryFailure(@Nonnull Throwable cause);

    /**
     * 将future标记为不可取消状态，它表示计算已经开始，不可以被取消。
     *
     * @return 当且仅当任务已经被取消时返回false。
     */
    boolean setUncancellable();

    // -------------------------------------------------- 语法支持 ---------------------------------------------

    @Override
    Promise<V> await() throws InterruptedException;

    @Override
    Promise<V> awaitUninterruptibly();

    @Override
    <U> Promise<U> thenCompose(@Nonnull Function<? super V, ? extends ListenableFuture<U>> fn);

    @Override
    <U> Promise<U> thenCompose(@Nonnull Callable<? extends ListenableFuture<U>> fn);

    @Override
    Promise<Void> thenRun(@Nonnull Runnable action);

    @Override
    <U> Promise<U> thenCall(@Nonnull Callable<U> fn);

    @Override
    Promise<Void> thenAccept(@Nonnull Consumer<? super V> action);

    @Override
    <U> Promise<U> thenApply(@Nonnull Function<? super V, ? extends U> fn);

    @Override
    <X extends Throwable>
    Promise<V> catching(@Nonnull Class<X> exceptionType, @Nonnull Function<? super X, ? extends V> fallback);

    @Override
    <U> Promise<U> thenHandle(@Nonnull BiFunction<? super V, ? super Throwable, ? extends U> fn);

    @Override
    Promise<V> whenComplete(@Nonnull BiConsumer<? super V, ? super Throwable> action);

    @Override
    Promise<V> whenExceptionally(@Nonnull Consumer<? super Throwable> action);

    @Override
    <U> Promise<U> thenComposeAsync(@Nonnull Function<? super V, ? extends ListenableFuture<U>> fn, Executor executor);

    @Override
    <U> Promise<U> thenComposeAsync(@Nonnull Callable<? extends ListenableFuture<U>> fn, Executor executor);

    @Override
    Promise<Void> thenRunAsync(@Nonnull Runnable action, Executor executor);

    @Override
    <U> Promise<U> thenCallAsync(@Nonnull Callable<U> fn, Executor executor);

    @Override
    Promise<Void> thenAcceptAsync(@Nonnull Consumer<? super V> action, Executor executor);

    @Override
    <U> Promise<U> thenApplyAsync(@Nonnull Function<? super V, ? extends U> fn, Executor executor);

    @Override
    <X extends Throwable>
    Promise<V> catchingAsync(@Nonnull Class<X> exceptionType, @Nonnull Function<? super X, ? extends V> fallback, Executor executor);

    @Override
    <U> Promise<U> thenHandleAsync(@Nonnull BiFunction<? super V, ? super Throwable, ? extends U> fn, Executor executor);

    @Override
    Promise<V> whenCompleteAsync(@Nonnull BiConsumer<? super V, ? super Throwable> action, Executor executor);

    @Override
    Promise<V> whenExceptionallyAsync(@Nonnull Consumer<? super Throwable> action, Executor executor);

    @Override
    Promise<V> addListener(FutureListener<? super V> listener);

    @Override
    Promise<V> addListener(FutureListener<? super V> listener, Executor executor);

    @Override
    Promise<V> addListener(BiConsumer<? super V, ? super Throwable> action);

    @Override
    Promise<V> addListener(BiConsumer<? super V, ? super Throwable> action, Executor executor);

    @Override
    Promise<V> addFailedListener(Consumer<? super Throwable> action);

    @Override
    Promise<V> addFailedListener(Consumer<? super Throwable> action, Executor executor);
}
