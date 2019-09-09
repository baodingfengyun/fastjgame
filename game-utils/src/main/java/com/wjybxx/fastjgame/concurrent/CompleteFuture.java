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

package com.wjybxx.fastjgame.concurrent;


import javax.annotation.Nonnull;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * {@link AbstractListenableFuture}的一个实现，表示它关联的操作早已完成。
 * 任何添加到上面的监听器将立即收到通知。
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
    private final EventLoop executor;

    /**
     * @param executor 该future用于通知的线程,Listener的执行环境。
     */
    protected CompleteFuture(@Nonnull EventLoop executor) {
        this.executor = executor;
    }

    /**
     * 返回该 {@link CompleteFuture} 关联的用于通知的默认{@link Executor}。
     */
    @Nonnull
    protected EventLoop executor() {
        return executor;
    }

    @Override
    public void addListener(@Nonnull FutureListener<? super V> listener) {
        notifyListener(listener, executor());
    }

    @Override
    public void addListener(@Nonnull FutureListener<? super V> listener, @Nonnull EventLoop bindExecutor) {
        // notify
        notifyListener(listener, bindExecutor);
    }

    /**
     * 通知一个监听器。
     */
    private void notifyListener(@Nonnull FutureListener<? super V> listener, @Nonnull EventLoop executor) {
        DefaultPromise.notifyListenerSafely(this, listener, executor);
    }

    @Override
    public boolean removeListener(@Nonnull FutureListener<? super V> listener) {
        // NOOP (因为并没有真正添加，因此也不需要移除)
        return false;
    }

    @Override
    public boolean removeListener(@Nonnull FutureListener<? super V> listener, @Nonnull EventLoop bindExecutor) {
        // NOOP (因为并没有真正添加，因此也不需要移除)
        return false;
    }

    // 什么时候应该检查中断，不是简单的事，个人觉得这里的操作都已完成，不会造成阻塞(不会执行耗时操作)，因此不需要检查中断

    @Override
    public void await() {

    }

    @Override
    public void awaitUninterruptibly() {

    }

    @Override
    public boolean await(long timeout, TimeUnit unit) {
        return true;
    }

    @Override
    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
        return true;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isCancellable() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @param mayInterruptIfRunning this value has no effect in this implementation.
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }
}
