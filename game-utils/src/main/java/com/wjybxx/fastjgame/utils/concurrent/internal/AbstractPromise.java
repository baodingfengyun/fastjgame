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

package com.wjybxx.fastjgame.utils.concurrent.internal;

import com.wjybxx.fastjgame.utils.ThreadUtils;
import com.wjybxx.fastjgame.utils.TimeUtils;
import com.wjybxx.fastjgame.utils.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.utils.concurrent.Promise;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.wjybxx.fastjgame.utils.ThreadUtils.checkInterrupted;

/**
 * promise的模板实现，主要负责<b>阻塞式获取结果</b>和<b>阻塞式等待</b>。
 * 仍然不负责监听器管理。
 * <p>
 * 它通过<b>对象的监视器锁</b>实现等待通知机制。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/6
 */
public abstract class AbstractPromise<V> extends PromiseBase<V> implements ListenableFuture<V>, Promise<V> {

    /**
     * 1毫秒多少纳秒
     */
    private static final int NANO_PER_MILLISECOND = (int) TimeUtils.NANO_PER_MILLISECOND;

    /**
     * 在当前对象上阻塞等待的线程数，决定是否需要调用{@link #notifyAll()}。
     * 减少notifyAll调用。（因为鼓励使用回调，而不是阻塞方式的获取结果，因此NotifyAll的几率是很小的）
     */
    @GuardedBy("this")
    private int waiters = 0;

    public AbstractPromise() {

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
    protected abstract void checkDeadlock();

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
    public boolean await(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
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
    public boolean awaitUninterruptibly(long timeout, @Nonnull TimeUnit unit) {
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

    private void notifyAllWaiters() {
        if (waiters > 0) {
            notifyAll();
        }
    }

    /**
     * 检查是否需要通知监听器
     * 注意：该方法运行在锁的保护下，最好不要直接进行通知，如果需要通知，请在{@link #notifyAllListeners()}中进行通知。
     * --
     * 并发组件将锁分布在多个类中是极其危险的，一般不推荐这样。
     */
    protected abstract boolean needNotifyListeners();

    /**
     * 通知所有的监听器
     * 注意：此时尚未获取锁，如果需要获取锁，可自行获取，但应尽量避免长时间占有锁。
     */
    protected abstract void notifyAllListeners();
}
