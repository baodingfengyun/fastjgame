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

import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.ThreadUtils;
import com.wjybxx.fastjgame.utils.TimeUtils;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import java.util.concurrent.*;

import static com.wjybxx.fastjgame.utils.ThreadUtils.checkInterrupted;

/**
 * 默认的{@link BlockingPromise}实现。
 *
 * <h3>监听器性能</h3>
 * 使用合适的{@link #defaultExecutor}将有助于减小通知监听器的开销。
 * {@link #defaultExecutor}应该为使用监听器最多的线程。
 *
 * <p>
 * 建议使用 {@link EventLoop#newBlockingPromise()}代替构造方法。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14
 * github - https://github.com/hl845740757
 */
public class DefaultBlockingPromise<V> extends AbstractPromise<V> implements BlockingPromise<V> {

    /**
     * 1毫秒多少纳秒
     */
    private static final int NANO_PER_MILLISECOND = (int) TimeUtils.NANO_PER_MILLISECOND;

    /**
     * 该future上注册的监听器们。
     * 我们使用Null表示一个监听器也没有的状态，每次通知时都会置为null。
     */
    @GuardedBy("this")
    private Object listenerEntries = null;
    /**
     * 当前是否有线程正在通知监听器们。我们必须阻止并发的通知 和 保持监听器的先入先出顺序(先添加的先被通知)。
     */
    @GuardedBy("this")
    private boolean notifyingListeners = false;

    /**
     * 在当前对象上阻塞等待的线程数，决定是否需要调用{@link #notifyAll()}。
     * 减少notifyAll调用。（因为鼓励使用回调，而不是阻塞方式的获取结果，因此NotifyAll的几率是很小的）
     */
    @GuardedBy("this")
    private int waiters = 0;

    /**
     * 默认的通知线程，只有在该线程下才可以进行派发。
     * 一来可以避免并发通知，二来可以减少通知开销。
     * 该{@link EventLoop}最好是使用监听器最多的线程。
     */
    private final EventLoop defaultExecutor;
    /**
     * {@link #defaultExecutor}是否是工作者线程
     */
    private final boolean isWorkingExecutor;

    public DefaultBlockingPromise(@Nonnull EventLoop defaultExecutor) {
        this(defaultExecutor, true);
    }

    public DefaultBlockingPromise(@Nonnull EventLoop defaultExecutor, boolean isWorkingExecutor) {
        this.defaultExecutor = defaultExecutor;
        this.isWorkingExecutor = isWorkingExecutor;
    }

    // --------------------------------------------- 阻塞式获取结果 -----------------------------------

    @Override
    public final V get() throws InterruptedException, ExecutionException {
        final Object result = resultHolder.get();
        if (isDone0(result)) {
            return reportGet(result);
        }

        await();

        return reportGet(resultHolder.get());
    }

    /**
     * {@link BlockingFuture#get()} {@link BlockingFuture#get(long, TimeUnit)}
     */
    @SuppressWarnings("unchecked")
    private static <T> T reportGet(Object r) throws ExecutionException {
        if (r == SUCCESS) {
            return null;
        }

        if (r instanceof CauseHolder) {
            return FutureUtils.rethrowGet(((CauseHolder) r).cause);
        }

        return (T) r;
    }

    @Override
    public final V get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        final Object result = resultHolder.get();
        if (isDone0(result)) {
            return reportGet(result);
        }

        if (await(timeout, unit)) {
            return reportGet(resultHolder.get());
        }

        throw new TimeoutException();
    }

    @Override
    public final V join() throws CompletionException {
        final Object result = resultHolder.get();
        if (isDone0(result)) {
            return reportJoin(result);
        }

        awaitUninterruptibly();

        return reportJoin(resultHolder.get());
    }

    // --------------------------------------------- 阻塞式等待 -----------------------------------

    @Override
    public BlockingPromise<V> await() throws InterruptedException {
        // 先检查一次是否已完成，减小锁竞争，同时在完成的情况下，等待不会死锁。
        if (isDone()) {
            return this;
        }
        // 检查死锁可能
        checkDeadlock();

        // 检查中断 --- 在执行一个耗时操作之前检查中断是有必要的
        checkInterrupted();

        synchronized (this) {
            while (!isDone()) {
                incWaiters();
                try {
                    this.wait();
                } finally {
                    decWaiters();
                }
            }
        }
        return this;
    }

    /**
     * 检查死锁可能。
     */
    private void checkDeadlock() {
        if (isWorkingExecutor) {
            EventLoopUtils.checkDeadLock(defaultExecutor);
        }
    }

    /**
     * 等待线程数+1
     */
    private void incWaiters() {
        waiters++;
    }

    /**
     * 等待线程数-1
     */
    private void decWaiters() {
        waiters++;
    }

    @Override
    public BlockingPromise<V> awaitUninterruptibly() {
        // 先检查一次是否已完成，减小锁竞争，同时在完成的情况下，等待不会死锁。
        if (isDone()) {
            return this;
        }
        // 检查死锁可能
        checkDeadlock();

        boolean interrupted = false;
        try {
            synchronized (this) {
                while (!isDone()) {
                    incWaiters();
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        interrupted = true;
                    } finally {
                        decWaiters();
                    }
                }
            }
        } finally {
            // 恢复中断
            ThreadUtils.recoveryInterrupted(interrupted);
        }
        return this;
    }

    @Override
    public final boolean await(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        // 小于等于0，则不阻塞
        if (timeout <= 0) {
            return isDone();
        }
        // 先检查一次是否已完成，减小锁竞争，同时在完成的情况下，等待不会死锁。
        if (isDone()) {
            return true;
        }
        // 等待之前检查死锁可能
        checkDeadlock();

        // 即将等待之前检查中断标记（在耗时操作开始前检查中断是有必要的 -- 要养成习惯）
        checkInterrupted();
        final long endTime = System.nanoTime() + unit.toNanos(timeout);
        synchronized (this) {
            while (!isDone()) {
                // 获取锁需要时间，因此应该在获取锁之后计算剩余时间
                final long remainNano = endTime - System.nanoTime();
                if (remainNano <= 0) {
                    return false;
                }
                incWaiters();
                try {
                    this.wait(remainNano / NANO_PER_MILLISECOND, (int) (remainNano % NANO_PER_MILLISECOND));
                } finally {
                    decWaiters();
                }
            }
            return true;
        }
    }

    @Override
    public final boolean awaitUninterruptibly(long timeout, @Nonnull TimeUnit unit) {
        // 小于等于0，则不阻塞
        if (timeout <= 0) {
            return isDone();
        }
        // 先检查一次是否已完成，减小锁竞争，同时在完成的情况下，等待不会死锁。
        if (isDone()) {
            return true;
        }
        // 检查死锁可能
        checkDeadlock();

        boolean interrupted = false;
        final long endTime = System.nanoTime() + unit.toNanos(timeout);
        try {
            synchronized (this) {
                while (!isDone()) {
                    // 获取锁需要时间，因此应该在获取锁之后计算剩余时间
                    final long remainNano = endTime - System.nanoTime();
                    if (remainNano <= 0) {
                        return false;
                    }
                    incWaiters();
                    try {
                        this.wait(remainNano / NANO_PER_MILLISECOND, (int) (remainNano % NANO_PER_MILLISECOND));
                    } catch (InterruptedException e) {
                        interrupted = true;
                    } finally {
                        decWaiters();
                    }
                }
                return true;
            }
        } finally {
            // 恢复中断状态
            ThreadUtils.recoveryInterrupted(interrupted);
        }
    }

    // --------------------------------------------- 完成时处理 -----------------------------------

    @Override
    protected final void postComplete() {
        final boolean needNotifyListeners;

        synchronized (this) {
            notifyAllWaiters();

            needNotifyListeners = needNotifyListeners();
        }

        if (needNotifyListeners) {
            notifyAllListeners();
        }
    }

    /**
     * 唤醒在该future上等待的线程。
     */
    private void notifyAllWaiters() {
        if (waiters > 0) {
            notifyAll();
        }
    }

    private boolean needNotifyListeners() {
        return listenerEntries != null && !notifyingListeners;
    }

    private void notifyAllListeners() {
        if (defaultExecutor.inEventLoop()) {
            notifyAllListenersNow();
        } else {
            ConcurrentUtils.safeExecute(defaultExecutor, this::notifyAllListenersNow);
        }
    }

    /**
     * Q: 为什么要在{@link #defaultExecutor}下再通知监听器？？？<br>
     * A: 基于这样一种假设：我们认为{@link #defaultExecutor}下执行的监听器是最多的，
     * 那么在{@link #defaultExecutor}下，多数监听器可以直接执行，这样可以有最少的任务数。
     */
    private void notifyAllListenersNow() {
        assert defaultExecutor.inEventLoop();

        // 用于拉取最新的监听器，避免长时间的占有锁
        Object listenerEntries;
        synchronized (this) {
            // 有线程正在进行通知 或当前 没有监听器，则不需要当前线程进行通知
            if (notifyingListeners || null == this.listenerEntries) {
                return;
            }
            // 标记为正在通知(避免并发通知，当前正在通知的线程，会通知所有的监听器)
            notifyingListeners = true;
            listenerEntries = this.listenerEntries;
            this.listenerEntries = null;
        }

        for (; ; ) {
            // 通知当前批次的监听器(此时不需要获得锁) -- 但是这里不能抛出异常，否则可能死锁 -- notifyingListeners无法恢复
            FutureUtils.notifyAllListenerNowSafely(this, listenerEntries);

            // 通知完当前批次后，检查是否有新的监听器加入
            synchronized (this) {
                if (null == this.listenerEntries) {
                    // 通知完毕
                    this.notifyingListeners = false;
                    break;
                }
                // 有新的监听器加入，拉取最新的监听器，继续通知 -- 可以保证被通知的顺序
                listenerEntries = this.listenerEntries;
                this.listenerEntries = null;
            }
        }
    }

    // ------------------------------------------------- 监听器管理 ------------------------------------------
    @Override
    public EventLoop defaultExecutor() {
        return defaultExecutor;
    }

    @Override
    public BlockingPromise<V> addListener(@Nonnull FutureListener<? super V> listener) {
        addListener0(listener, defaultExecutor);
        return this;
    }

    @Override
    public BlockingPromise<V> addListener(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        addListener0(listener, bindExecutor);
        return this;
    }

    private void addListener0(@Nonnull FutureListener<? super V> listener, @Nonnull Executor executor) {
        // 不管是否已完成，先加入等待通知集合
        synchronized (this) {
            listenerEntries = FutureUtils.aggregateListenerEntry(listenerEntries, new FutureListenerEntry<>(listener, executor));
        }

        // 必须检查完成状态，如果已进入完成状态，通知刚刚加入监听器们（否则可能丢失通知）
        // 因为状态改变 和 锁没有关系
        if (isDone()) {
            notifyAllListeners();
        }
    }

}
