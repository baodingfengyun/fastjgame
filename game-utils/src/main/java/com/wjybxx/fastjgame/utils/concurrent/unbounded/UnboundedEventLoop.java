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

package com.wjybxx.fastjgame.utils.concurrent.unbounded;


import com.lmax.disruptor.RingBuffer;
import com.wjybxx.fastjgame.utils.concurrent.*;
import com.wjybxx.fastjgame.utils.concurrent.disruptor.DisruptorEventLoop;
import com.wjybxx.fastjgame.utils.concurrent.unbounded.WaitStrategyFactory.WaitStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * 事件循环的模板实现。
 * 它是无界事件循环的超类，如果期望使用有界队列，请使用{@link DisruptorEventLoop}。
 * 等待策略实现与{@link DisruptorEventLoop}的等待策略是一致的。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14
 * github - https://github.com/hl845740757
 */
public class UnboundedEventLoop extends AbstractEventLoop {

    private static final Logger logger = LoggerFactory.getLogger(UnboundedEventLoop.class);

    /**
     * 批量拉取(执行)任务数 - 该值越小{@link #loopOnce()}执行越频繁，响应关闭请求越快。
     */
    private static final int DEFAULT_BATCH_EVENT_SIZE = 1024;

    // 线程的状态
    /**
     * 初始状态，未启动状态
     */
    private static final int ST_NOT_STARTED = 1;
    /**
     * 已启动状态，运行状态
     */
    private static final int ST_STARTED = 2;
    /**
     * 正在关闭状态，正在尝试执行最后的任务
     */
    private static final int ST_SHUTTING_DOWN = 3;
    /**
     * 已关闭状态，正在进行最后的清理
     */
    private static final int ST_SHUTDOWN = 4;
    /**
     * 终止状态(二阶段终止模式 - 已关闭状态下进行最后的清理，然后进入终止状态)
     */
    private static final int ST_TERMINATED = 5;

    /**
     * 持有的线程
     */
    private final Thread thread;
    /**
     * 线程的生命周期标识。
     * 未和netty一样使用{@link AtomicIntegerFieldUpdater}，需要更多的理解成本，对于不熟悉的人来说容易用错。
     * 首先保证正确性，易分析。
     */
    private final AtomicInteger stateHolder = new AtomicInteger(ST_NOT_STARTED);
    /**
     * 任务队列。
     * Q: 为什么选择{@link ConcurrentLinkedQueue}？
     * A: 关于任务队列，对{@link RingBuffer} {@link ConcurrentLinkedQueue} {@link LinkedBlockingQueue}也对比了很多次，测试结果大致如下：
     * 1. {@link RingBuffer}的性能始终是最好的，但是它是有界的。
     * 2. 在高竞争下，{@link LinkedBlockingQueue}优于{@link ConcurrentLinkedQueue}，但是差距不算大（可能竞争还不够激烈）。
     * 3. 在低竞争下，{@link ConcurrentLinkedQueue}表现更好，{@link LinkedBlockingQueue}在低竞争下吞吐量实在不能看。
     * 考虑到高竞争的情况较少，最终选择了{@link ConcurrentLinkedQueue}。
     */
    private final ConcurrentLinkedQueue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();

    /**
     * 批量执行任务的大小
     */
    private final int taskBatchSize;
    /**
     * 当没有可执行任务时的等待策略
     */
    private final WaitStrategy waitStrategy;

    /**
     * 任务被拒绝时的处理策略
     */
    private final RejectedExecutionHandler rejectedExecutionHandler;

    /**
     * 线程终止future
     */
    private final BlockingPromise<?> terminationFuture = new DefaultBlockingPromise<>(GlobalEventLoop.INSTANCE);

    protected UnboundedEventLoop(@Nullable EventLoopGroup parent,
                                 @Nonnull ThreadFactory threadFactory,
                                 @Nonnull RejectedExecutionHandler rejectedExecutionHandler) {
        this(parent, threadFactory, rejectedExecutionHandler, DEFAULT_BATCH_EVENT_SIZE, new SleepWaitStrategyFactory());
    }

    protected UnboundedEventLoop(@Nullable EventLoopGroup parent,
                                 @Nonnull ThreadFactory threadFactory,
                                 @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                                 int taskBatchSize) {
        this(parent, threadFactory, rejectedExecutionHandler, taskBatchSize, new SleepWaitStrategyFactory());
    }

    protected UnboundedEventLoop(@Nullable EventLoopGroup parent,
                                 @Nonnull ThreadFactory threadFactory,
                                 @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                                 @Nonnull WaitStrategyFactory waitStrategyFactory) {
        this(parent, threadFactory, rejectedExecutionHandler, DEFAULT_BATCH_EVENT_SIZE, waitStrategyFactory);
    }

    /**
     * @param parent                   EventLoop所属的容器，nullable
     * @param threadFactory            线程工厂，创建的线程不要直接启动，建议调用
     *                                 {@link Thread#setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler)}设置异常处理器
     * @param rejectedExecutionHandler 拒绝任务的策略
     * @param taskBatchSize            批量执行任务数，设定合理的任务数可避免执行任务耗费太多时间。
     * @param waitStrategyFactory      没有任务时的等待策略
     */
    protected UnboundedEventLoop(@Nullable EventLoopGroup parent,
                                 @Nonnull ThreadFactory threadFactory,
                                 @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                                 int taskBatchSize,
                                 @Nonnull WaitStrategyFactory waitStrategyFactory) {
        super(parent);

        if (taskBatchSize <= 0) {
            throw new IllegalArgumentException("taskBatchSize expected: > 0");
        }

        this.thread = Objects.requireNonNull(threadFactory.newThread(new Worker()), "newThread");
        // 记录异常退出日志
        UncaughtExceptionHandlers.logIfAbsent(thread, logger);

        this.waitStrategy = waitStrategyFactory.newInstance();
        this.taskBatchSize = taskBatchSize;
        this.rejectedExecutionHandler = rejectedExecutionHandler;
    }

    @Override
    public final boolean inEventLoop() {
        return thread == Thread.currentThread();
    }
    // ------------------------------------------------ 线程生命周期 -------------------------------------

    /**
     * 确保运行状态至少已到指定状态。
     * 参考自{@code ThreadPoolExecutor#advanceRunState}
     *
     * @param targetState 期望的目标状态， {@link #ST_SHUTTING_DOWN} 或者 {@link #ST_SHUTDOWN}
     */
    private void advanceRunState(int targetState) {
        for (; ; ) {
            int oldState = stateHolder.get();
            if (oldState >= targetState || stateHolder.compareAndSet(oldState, targetState))
                break;
        }
    }

    @Override
    public final boolean isShuttingDown() {
        return isShuttingDown0(stateHolder.get());
    }

    private static boolean isShuttingDown0(int state) {
        return state >= ST_SHUTTING_DOWN;
    }

    @Override
    public final boolean isShutdown() {
        return isShutdown0(stateHolder.get());
    }

    private static boolean isShutdown0(int state) {
        return state >= ST_SHUTDOWN;
    }

    @Override
    public final boolean isTerminated() {
        return stateHolder.get() == ST_TERMINATED;
    }

    @Override
    public final boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        return terminationFuture().await(timeout, unit);
    }

    @Override
    public final BlockingFuture<?> terminationFuture() {
        return terminationFuture;
    }

    @Override
    public final void shutdown() {
        for (; ; ) {
            int oldState = stateHolder.get();
            if (isShuttingDown0(oldState)) {
                return;
            }

            if (stateHolder.compareAndSet(oldState, ST_SHUTTING_DOWN)) {
                // 确保线程可进入终止状态
                ensureThreadTerminable(oldState);
                return;
            }
        }
    }

    @Nonnull
    @Override
    public final List<Runnable> shutdownNow() {
        for (; ; ) {
            int oldState = stateHolder.get();
            if (isShutdown0(oldState)) {
                return Collections.emptyList();
            }

            if (stateHolder.compareAndSet(oldState, ST_SHUTDOWN)) {
                // 确保线程可进入终止状态 - 这里不能操作TaskQueue中的数据，不能打破[多生产者单消费者]的架构
                ensureThreadTerminable(oldState);
                return Collections.emptyList();
            }
        }
    }

    /**
     * 确保线程可终止。
     * - terminable
     *
     * @param oldState 切换到shutdown之前的状态
     */
    private void ensureThreadTerminable(int oldState) {
        if (oldState == ST_NOT_STARTED) {
            stateHolder.set(ST_TERMINATED);
            terminationFuture.setSuccess(null);
        } else {
            if (!inEventLoop()) {
                // 可能阻塞在等待任务处
                waitStrategy.signalAllWhenBlocking();

                wakeUpForShutdown();
            }
            // else 当前是活跃状态(自己调用，当然是活跃状态)
        }
    }

    /**
     * 如果子类可能阻塞在其它地方，那么应该重写该方法以唤醒线程
     */
    protected void wakeUpForShutdown() {

    }

    /**
     * 中断当前运行的线程，用于唤醒线程。
     * 如果子类阻塞在taskQueue之外的的地方，但是可以通过中断唤醒线程时，那么可以选择中断。
     * <p>
     * Interrupt the current running {@link Thread}.
     */
    protected final void interruptThread() {
        thread.interrupt();
    }

    @Override
    public final void execute(@Nonnull Runnable task) {
        if (addTask(task) && !inEventLoop()) {
            waitStrategy.signalAllWhenBlocking();
            ensureStarted();
        }
    }

    /**
     * 尝试添加一个任务到任务队列
     *
     * @param task 期望运行的任务
     * @return 添加任务是否成功
     */
    private boolean addTask(@Nonnull Runnable task) {
        // 1. 在检测到未关闭的状态下尝试压入队列
        if (!isShuttingDown() && taskQueue.offer(task)) {
            // 2. 压入队列是一个过程！在压入队列的过程中，executor的状态可能改变，因此必须再次校验 - 以判断线程是否在任务压入队列之后已经开始关闭了
            // remove失败表示executor已经处理了该任务
            if (isShuttingDown() && taskQueue.remove(task)) {
                reject(task);
                return false;
            }
            return true;
        } else {
            // executor已关闭 或 压入队列失败，拒绝
            reject(task);
            return false;
        }
    }

    private void reject(@Nonnull Runnable task) {
        rejectedExecutionHandler.rejected(task, this);
    }

    /**
     * 确保线程已启动。
     * 外部线程提交任务后需要保证线程已启动。
     */
    private void ensureStarted() {
        int state = stateHolder.get();
        if (state == ST_NOT_STARTED) {
            if (stateHolder.compareAndSet(ST_NOT_STARTED, ST_STARTED)) {
                thread.start();
            }
        }
    }

    /**
     * 这里提醒一下，对于{@link ConcurrentLinkedQueue}，一定不要用size方法，size方法会遍历所有元素，性能极差。
     */
    final boolean isTaskQueueEmpty() {
        assert inEventLoop();
        return taskQueue.isEmpty();
    }

    /**
     * 如果EventLoop已经开始关闭，则抛出{@link ShuttingDownException}
     */
    final void checkShuttingDown() throws ShuttingDownException {
        if (isShuttingDown()) {
            throw ShuttingDownException.INSTANCE;
        }
    }

    // --------------------------------------- 线程管理 ----------------------------------------

    /**
     * 在开启事件循环之前的初始化动作
     *
     * @apiNote 初始化方法抛出任何异常都将导致线程退出
     */
    protected void init() throws Exception {

    }

    /**
     * 执行一次循环（刷帧）。
     * 调用时机分两种：
     * 1. 每执行一批任务，会执行一次循环。
     * 2. 等待任务期间，每等待一段“时间”会执行一次循环。
     *
     * @apiNote 注意由于调用时机并不确定，子类实现需要自己控制真实的帧间隔。
     */
    protected void loopOnce() throws Exception {

    }

    /**
     * 在退出事件循环之前的清理动作。
     */
    protected void clean() throws Exception {

    }

    /**
     * 安全的执行一次循环。
     * 注意：该方法不是给子类的API。用于{@link WaitStrategy}的api
     */
    final void safeLoopOnce() {
        assert inEventLoop();
        try {
            loopOnce();
        } catch (Throwable t) {
            if (t instanceof VirtualMachineError) {
                logger.error("loopOnce caught exception", t);
            } else {
                logger.warn("loopOnce caught exception", t);
            }
        }
    }

    /**
     * 工作者线程
     * <p>
     * 两阶段终止模式 --- 在终止前进行清理操作，安全的关闭线程不是一件容易的事情。
     */
    private class Worker implements Runnable {

        @Override
        public void run() {
            try {
                init();

                loop();
            } catch (Throwable e) {
                logger.error("thread exit due to exception!", e);
            } finally {
                // 如果是非正常退出，需要切换到正在关闭状态 - 告知其它线程正在关闭
                advanceRunState(ST_SHUTTING_DOWN);

                try {
                    // 清理任务队列中的数据
                    cleanTaskQueue();
                } finally {
                    // 标记为已进入最终清理阶段
                    advanceRunState(ST_SHUTDOWN);

                    // 退出前进行必要的清理，释放系统资源
                    try {
                        clean();
                    } catch (Throwable e) {
                        logger.error("thread clean caught exception!", e);
                    } finally {
                        // 设置为终止状态
                        stateHolder.set(ST_TERMINATED);
                        terminationFuture.setSuccess(null);
                    }
                }
            }
        }

        private void loop() {
            while (true) {
                try {
                    waitStrategy.waitFor(UnboundedEventLoop.this);

                    runTasksBatch();

                    safeLoopOnce();
                } catch (ShuttingDownException | InterruptedException e) {
                    // 检测到EventLoop关闭或被中断
                    if (isShuttingDown()) {
                        break;
                    }
                } catch (TimeoutException e) {
                    // 等待超时，执行一次循环
                    safeLoopOnce();
                } catch (Throwable e) {
                    // 不好的等待策略实现
                    logger.error("bad waitStrategy imp", e);
                    // 检测退出
                    if (isShuttingDown()) {
                        break;
                    }
                }
            }
        }

        private void runTasksBatch() {
            Runnable task;
            for (int countDown = taskBatchSize; countDown > 0; countDown--) {
                task = taskQueue.poll();

                if (task == null) {
                    break;
                }

                safeExecute(task);
            }
        }

        private void cleanTaskQueue() {
            Runnable task;
            while (!isShutdown()) {
                task = taskQueue.poll();

                if (task == null) {
                    break;
                }

                safeExecute(task);
            }

            taskQueue.clear();
        }
    }

}
