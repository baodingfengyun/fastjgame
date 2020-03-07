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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * {@link EventLoop}的抽象实现。这里负责一些简单的方法实现。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14
 * github - https://github.com/hl845740757
 */
public abstract class AbstractEventLoop extends AbstractExecutorService implements EventLoop {

    private static final Logger logger = LoggerFactory.getLogger(AbstractEventLoop.class);

    /**
     * 父节点的引用。
     */
    private final EventLoopGroup parent;
    /**
     * 封装一个只包含自己的集合。方便实现迭代查询等等。
     */
    private final Collection<EventLoop> selfCollection = Collections.singleton(this);

    protected AbstractEventLoop(@Nullable EventLoopGroup parent) {
        this.parent = parent;
    }

    @Nullable
    @Override
    public EventLoopGroup parent() {
        return parent;
    }

    @Nonnull
    @Override
    public EventLoop next() {
        return this;
    }

    @Nonnull
    @Override
    public EventLoop select(int key) {
        return this;
    }

    @Override
    public int numChildren() {
        return 1;
    }

    // -------------------------------------- promise --------------------------------------

    @Nonnull
    @Override
    public <V> Promise<V> newPromise() {
        return new DefaultPromise<V>(this);
    }

    @Nonnull
    @Override
    public final <V> ListenableFuture<V> newSucceededFuture(V result) {
        return new SucceededFuture<V>(this, result);
    }

    @Nonnull
    @Override
    public final <V> ListenableFuture<V> newFailedFuture(@Nonnull Throwable cause) {
        return new FailedFuture<V>(this, cause);
    }

    // --------------------------------------- 任务提交 ----------------------------------------
    // region 重写 AbstractExecutorService中的部分方法,返回特定的Future类型
    @Nonnull
    @Override
    public final ListenableFuture<?> submit(@Nonnull Runnable task) {
        return (ListenableFuture<?>) super.submit(task);
    }

    @Nonnull
    @Override
    public final <T> ListenableFuture<T> submit(@Nonnull Runnable task, T result) {
        return (ListenableFuture<T>) super.submit(task, result);
    }

    @Nonnull
    @Override
    public final <T> ListenableFuture<T> submit(@Nonnull Callable<T> task) {
        return (ListenableFuture<T>) super.submit(task);
    }

    // 重要，重写newTaskFor方法，返回具体的future类型
    @Override
    protected final <T> RunnableFuture<T> newTaskFor(@Nonnull Runnable runnable, T value) {
        return new ListenableFutureTask<>(this, Executors.callable(runnable, value));
    }

    @Override
    protected final <T> RunnableFuture<T> newTaskFor(@Nonnull Callable<T> callable) {
        return new ListenableFutureTask<>(this, callable);
    }
    // endregion

    // -------------------------------------- invoke阻塞调用检测 --------------------------------------
    @Nonnull
    @Override
    public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        throwIfInEventLoop("invokeAny");
        return super.invokeAny(tasks);
    }

    @Nonnull
    @Override
    public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        throwIfInEventLoop("invokeAny");
        return super.invokeAny(tasks, timeout, unit);
    }

    @Nonnull
    @Override
    public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        throwIfInEventLoop("invokeAll");
        return super.invokeAll(tasks);
    }

    @Nonnull
    @Override
    public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks,
                                         long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        throwIfInEventLoop("invokeAll");
        return super.invokeAll(tasks, timeout, unit);
    }

    private void throwIfInEventLoop(String method) {
        if (inEventLoop()) {
            throw new RejectedExecutionException("Calling " + method + " from within the EventLoop is not allowed");
        }
    }

    // ---------------------------------------- 迭代 ---------------------------------------

    @Nonnull
    @Override
    public final Iterator<EventLoop> iterator() {
        return selfCollection.iterator();
    }

    @Override
    public final void forEach(Consumer<? super EventLoop> action) {
        selfCollection.forEach(action);
    }

    @Override
    public final Spliterator<EventLoop> spliterator() {
        return selfCollection.spliterator();
    }

    /**
     * 安全的运行任务，避免线程退出
     * Try to execute the given {@link Runnable} and just log if it throws a {@link Throwable}.
     */
    protected static void safeExecute(Runnable task) {
        try {
            task.run();
        } catch (Throwable t) {
            if (t instanceof VirtualMachineError) {
                logger.error("A task raised an exception. Task: {}", task, t);
            } else {
                logger.warn("A task raised an exception. Task: {}", task, t);
            }
        }
    }

}