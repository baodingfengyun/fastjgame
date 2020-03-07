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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.wjybxx.fastjgame.utils.ThreadUtils.checkInterrupted;

/**
 * 默认的{@link Promise}实现。
 *
 * <h3>监听器性能</h3>
 * 使用合适的{@link #defaultExecutor}将有助于减小通知监听器的开销。
 * {@link #defaultExecutor}应该为使用监听器最多的线程。
 *
 * <p>
 * 建议使用 {@link EventLoop#newPromise()}代替构造方法。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14
 * github - https://github.com/hl845740757
 */
public class DefaultPromise<V> extends AbstractPromise<V> implements Promise<V> {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPromise.class);
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

    public DefaultPromise(@Nonnull EventLoop defaultExecutor) {
        this.defaultExecutor = defaultExecutor;
    }

    // --------------------------------------------- 阻塞式获取结果 -----------------------------------

    @Override
    public final V get() throws InterruptedException, CompletionException {
        final Object result = resultHolder.get();
        if (isDone0(result)) {
            return reportGet(result);
        }

        await();

        return reportGet(resultHolder.get());
    }

    @Override
    public final V get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, CompletionException, TimeoutException {
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
            return reportGet(result);
        }

        awaitUninterruptibly();

        return reportGet(resultHolder.get());
    }

    // --------------------------------------------- 阻塞式等待 -----------------------------------

    @Override
    public Promise<V> await() throws InterruptedException {
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
    protected void checkDeadlock() {
        // 基础实现认为默认的通知线程就是工作者线程
        EventLoopUtils.checkDeadLock(defaultExecutor);
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
    public Promise<V> awaitUninterruptibly() {
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

    /**
     * 检查是否需要通知监听器
     */
    private boolean needNotifyListeners() {
        return listenerEntries != null && !notifyingListeners;
    }

    protected final void notifyAllListeners() {
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
            notifyAllListenerNowSafely(this, listenerEntries);

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

    static <V> void notifyAllListenerNowSafely(@Nonnull final NFuture<V> future, @Nonnull final Object listenerEntries) {
        if (listenerEntries instanceof FutureListenerEntry) {
            notifyListenerNowSafely(future, (FutureListenerEntry) listenerEntries);
        } else {
            final FutureListenerEntries futureListenerEntries = (FutureListenerEntries) listenerEntries;
            final FutureListenerEntry<?>[] children = futureListenerEntries.getChildren();
            final int size = futureListenerEntries.getSize();
            for (int index = 0; index < size; index++) {
                notifyListenerNowSafely(future, children[index]);
            }
        }
    }

    /**
     * @param listenerEntry 监听器的信息
     */
    public static <V> void notifyListenerNowSafely(@Nonnull NFuture<V> future, @Nonnull FutureListenerEntry listenerEntry) {
        if (EventLoopUtils.inEventLoop(listenerEntry.executor)) {
            notifyListenerNowSafely(future, listenerEntry.listener);
        } else {
            final FutureListener listener = listenerEntry.listener;
            ConcurrentUtils.safeExecute(listenerEntry.executor, () -> notifyListenerNowSafely(future, listener));
        }
    }

    @SuppressWarnings({"unchecked"})
    private static <V> void notifyListenerNowSafely(@Nonnull NFuture<V> future, @Nonnull FutureListener listener) {
        try {
            listener.onComplete(future);
        } catch (Throwable e) {
            logger.warn("An exception was thrown by " + future.getClass().getName() + ".onComplete()", e);
        }
    }

    // ------------------------------------------------- 监听器管理 ------------------------------------------
    @Override
    public EventLoop defaultExecutor() {
        return defaultExecutor;
    }

    @Override
    public Promise<V> onComplete(@Nonnull FutureListener<? super V> listener) {
        addListener(listener, defaultExecutor);
        return this;
    }

    @Override
    public Promise<V> onComplete(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        addListener(listener, bindExecutor);
        return this;
    }

    private void addListener(@Nonnull FutureListener<? super V> listener, @Nonnull Executor executor) {
        // 不管是否已完成，先加入等待通知集合
        synchronized (this) {
            listenerEntries = aggregateListenerEntry(listenerEntries, new FutureListenerEntry<>(listener, executor));
        }

        // 必须检查完成状态，如果已进入完成状态，通知刚刚加入监听器们（否则可能丢失通知）
        // 因为状态改变 和 锁没有关系
        if (isDone()) {
            notifyAllListeners();
        }
    }

    /**
     * @param listenerEntries 当前的所有监听器们
     * @param listenerEntry   新增的监听器
     * @return 合并后的结果
     */
    @Nonnull
    static Object aggregateListenerEntry(@Nullable Object listenerEntries, @Nonnull FutureListenerEntry<?> listenerEntry) {
        if (listenerEntries == null) {
            return listenerEntry;
        }
        if (listenerEntries instanceof FutureListenerEntry) {
            return new FutureListenerEntries((FutureListenerEntry<?>) listenerEntries, listenerEntry);
        } else {
            ((FutureListenerEntries) listenerEntries).addChild(listenerEntry);
            return listenerEntries;
        }
    }

    @Override
    public Promise<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener) {
        addListener(listener, defaultExecutor);
        return this;
    }

    @Override
    public Promise<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        addListener(listener, bindExecutor);
        return this;
    }

    @Override
    public Promise<V> onFailure(@Nonnull FailedFutureListener<? super V> listener) {
        addListener(listener, defaultExecutor);
        return this;
    }

    @Override
    public Promise<V> onFailure(@Nonnull FailedFutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        addListener(listener, bindExecutor);
        return this;
    }
}
