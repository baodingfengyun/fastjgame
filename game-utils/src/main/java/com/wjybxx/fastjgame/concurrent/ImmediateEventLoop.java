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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * 当前线程立即执行提交的任务的EventLoop
 * (参考自netty)
 * <p>
 * 提交的任务的线程立即执行它所提交的任务。{@link #execute(Runnable)}是可重入的，新提交的任务将会压入队列直到前面的任务完成执行。
 * <p>
 * {@link #execute(Runnable)} 抛出的所有异常都将被吞没并记录一个日志。目的是确保所有压入队列的任务都有机会执行。
 * </p>
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/15
 */
public class ImmediateEventLoop extends AbstractEventLoop {

    private static final Logger logger = LoggerFactory.getLogger(ImmediateEventLoop.class);
    public static final ImmediateEventLoop INSTANCE = new ImmediateEventLoop();

    /**
     * 延迟执行的任务队列。
     * 1 .必须是ThreadLocal的，因为使用{@link ImmediateEventLoop}的时候，并不是创建一个对象，而是立即捕获当前线程进行执行。
     * 因此要保证数据的隔离，必须是ThreadLocal的。
     * 2. 使用队列可以避免堆栈溢出，避免一直往下执行。
     * <p>
     * A Runnable will be queued if we are executing a Runnable. This is to prevent a {@link StackOverflowError}.
     */
    private static final ThreadLocal<Queue<Runnable>> DELAYED_RUNNABLES = ThreadLocal.withInitial(LinkedList::new);

    /**
     * 运行状态。提交第一个任务的时候进入运行状态，执行完本批次任务后进入非运行状态。
     * 必须是ThreadLocal，理由同上。
     * Set to {@code true} if we are executing a runnable.
     */
    private static final ThreadLocal<Boolean> RUNNING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * 因为它并没有创建新线程，而是捕获了当前线程，将当前线程伪装成为一个EventExecutor，因此也不支持关闭。
     */
    private final ListenableFuture<?> terminationFuture = new FailedFuture<>(GlobalEventLoop.INSTANCE, new UnsupportedOperationException());

    private ImmediateEventLoop() {
        super(null);
    }

    @Override
    public boolean inEventLoop() {
        // 每个线程都返回true，但是使用的数据都是隔离的。
        // 必须返回true，提交的任务才能被当前线程立即执行。
        return true;
    }

    @Override
    public ListenableFuture<?> terminationFuture() {
        return terminationFuture;
    }

    @Override
    @Deprecated
    public void shutdown() {

    }

    @Nonnull
    @Override
    public List<Runnable> shutdownNow() {
        return Collections.emptyList();
    }

    @Override
    public boolean isShuttingDown() {
        return false;
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) {
        return false;
    }

    @Override
    public void execute(@Nonnull Runnable command) {
        if (!RUNNING.get()) {
            // 如果提交任务的时候，当前EventExecutor处于非活动状态，那么需要先标记为运行状态，使得当前任务执行期间提交的新任务进入队列
            RUNNING.set(true);
            // 立即执行提交的任务
            try {
                command.run();
            } catch (Throwable cause) {
                logger.info("Throwable caught while executing Runnable {}", command, cause);
            } finally {
                Queue<Runnable> delayedRunnables = DELAYED_RUNNABLES.get();
                Runnable runnable;
                // 检查该线程(任务)提交的所有任务，直到所有任务执行完毕
                while ((runnable = delayedRunnables.poll()) != null) {
                    try {
                        runnable.run();
                    } catch (Throwable cause) {
                        // 必须捕获异常，使得所有任务都可以被执行
                        logger.info("Throwable caught while executing Runnable {}", runnable, cause);
                    }
                }
                // 当前所有任务都运行结束了
                RUNNING.set(false);
            }
        } else {
            // 如果该线程提交任务的时候，有任务正在执行，则将任务压入队列，什么情况下会出现呢？
            // 当正在执行的任务会提交新的任务时就会产生。
            DELAYED_RUNNABLES.get().add(command);
        }
    }

    @Nonnull
    @Override
    public <V> Promise<V> newPromise() {
        return new ImmediatePromise<>(this);
    }

    private static class ImmediatePromise<V> extends DefaultPromise<V> {

        private ImmediatePromise(EventLoop executor) {
            super(executor);
        }

        @Override
        protected void checkDeadlock() {
            // 为何不检查死锁？ 因为检查死锁一定会抛出BlockingOperateException
            // 因为检查死锁过程中，获取到的Executor就是ImmediateEventExecutor，inEventLoop始终返回true。
        }
    }
}
