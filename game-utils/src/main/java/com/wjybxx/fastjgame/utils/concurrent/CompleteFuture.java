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

package com.wjybxx.fastjgame.utils.concurrent;


import javax.annotation.Nonnull;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * {@link AbstractListenableFuture}的一个实现，表示它关联的操作早已完成。
 * 任何添加到上面的监听器将立即被通知。
 *
 * @param <V> the type of value
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14
 * github - https://github.com/hl845740757
 */
public abstract class CompleteFuture<V> extends AbstractListenableFuture<V> {

    /**
     * 默认的监听器执行环境
     */
    private final EventLoop defaultExecutor;

    /**
     * @param defaultExecutor 该future用于通知的线程, Listener的执行环境。
     */
    protected CompleteFuture(@Nonnull EventLoop defaultExecutor) {
        this.defaultExecutor = defaultExecutor;
    }

    @Override
    public final boolean isDone() {
        return true;
    }

    @Override
    public final boolean isCancelled() {
        return false;
    }

    @Override
    public final boolean isCancellable() {
        return false;
    }

    @Override
    public final boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    // 什么时候应该检查中断，个人觉得这里的操作都已完成，不会造成阻塞(不会执行耗时操作)，因此不检查中断

    @Override
    public final boolean await(long timeout, @Nonnull TimeUnit unit) {
        return true;
    }

    @Override
    public final boolean awaitUninterruptibly(long timeout, @Nonnull TimeUnit unit) {
        return true;
    }

    // 流式语法支持(允许重写)
    @Override
    public ListenableFuture<V> await() {
        return this;
    }

    @Override
    public ListenableFuture<V> awaitUninterruptibly() {
        return this;
    }

    // -------------------------------------------------- 监听器管理 ---------------------------------------------

    @Override
    public EventLoop defaultExecutor() {
        return defaultExecutor;
    }

    private void notifyListenerNow(@Nonnull FutureListener<? super V> listener, @Nonnull Executor executor) {
        DefaultPromise.notifyListenerNowSafely(this, new FutureListenerEntry<>(listener, executor));
    }

    @Override
    public ListenableFuture<V> onComplete(@Nonnull FutureListener<? super V> listener) {
        notifyListenerNow(listener, defaultExecutor);
        return this;
    }

    @Override
    public ListenableFuture<V> onComplete(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        notifyListenerNow(listener, bindExecutor);
        return this;
    }

    @Override
    public ListenableFuture<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener) {
        notifyListenerNow(listener, defaultExecutor);
        return this;
    }

    @Override
    public ListenableFuture<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        notifyListenerNow(listener, bindExecutor);
        return this;
    }

    @Override
    public ListenableFuture<V> onFailure(@Nonnull FailedFutureListener<? super V> listener) {
        notifyListenerNow(listener, defaultExecutor);
        return this;
    }

    @Override
    public ListenableFuture<V> onFailure(@Nonnull FailedFutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        notifyListenerNow(listener, bindExecutor);
        return this;
    }

}
