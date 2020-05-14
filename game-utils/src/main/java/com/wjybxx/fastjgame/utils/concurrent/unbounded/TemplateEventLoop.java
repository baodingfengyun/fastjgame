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
import org.jctools.queues.MessagePassingQueue;
import org.jctools.queues.MpscArrayQueue;
import org.jctools.queues.MpscLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * 事件循环的模板实现。
 * 它是无界事件循环的超类，如果期望使用有界队列，请使用{@link DisruptorEventLoop}或覆盖{@link #newTaskQueue()}方法创建有界队列。
 * 等待策略实现与{@link DisruptorEventLoop}的等待策略是一致的。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14
 * github - https://github.com/hl845740757
 */
public class TemplateEventLoop extends AbstractEventLoop {

    private static final Logger logger = LoggerFactory.getLogger(TemplateEventLoop.class);

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
     */
    private volatile int state = ST_NOT_STARTED;

    /**
     * 任务队列
     * <p>
     * {@link EventLoop}是多生产者单消费者模型，在该模型下，任务队列讲究极致性能的话，性能大致如下(CPU核心足够的情况下)：
     * 1. {@link MpscArrayQueue} - 性能极好，表现稳定，但我引入的{@link WrappedRunnable}申请了额外的资源。
     * 2. {@link RingBuffer} - 性能也极好，表现稳定，资源利用率好于{@link MpscArrayQueue}，暴露的API更底层，性能吃亏在多消费者模型上。
     * 3. {@link MpscLinkedQueue} - 暂时放这里，表现很不稳定(猜测：被JVM的某些优化去掉了性能提升)，性能好的时候遥遥领先，能比{@link MpscArrayQueue}高出一倍。
     * 而性能差的时候，甚至比{@link ConcurrentLinkedQueue}性能差。
     * 4. {@link ConcurrentLinkedQueue} - 性能较好，表现稳定，次于{@link RingBuffer}。
     * 5. {@link LinkedBlockingQueue} - 性能一般，表现稳定，高度竞争时性能高于{@link ConcurrentLinkedQueue}，但低竞争下吞吐量实在惨烈。
     * <p>
     * {@link MpscArrayQueue}{@link MpscLinkedQueue}最开始是在netty里看见的，但是由于是internal的，没能搞过来，后来发现是jctools的组件。
     * {@link MpscArrayQueue}性能比{@link RingBuffer}高一些，但资源利用率差一点。
     */
    private final MessagePassingQueue<Runnable> taskQueue;

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
    private final Promise<?> terminationFuture = FutureUtils.newPromise();

    public TemplateEventLoop(@Nullable EventLoopGroup parent,
                             @Nonnull ThreadFactory threadFactory,
                             @Nonnull RejectedExecutionHandler rejectedExecutionHandler) {
        this(parent, threadFactory, rejectedExecutionHandler, DEFAULT_BATCH_EVENT_SIZE, new SleepWaitStrategyFactory());
    }

    public TemplateEventLoop(@Nullable EventLoopGroup parent,
                             @Nonnull ThreadFactory threadFactory,
                             @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                             int taskBatchSize) {
        this(parent, threadFactory, rejectedExecutionHandler, taskBatchSize, new SleepWaitStrategyFactory());
    }

    public TemplateEventLoop(@Nullable EventLoopGroup parent,
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
    public TemplateEventLoop(@Nullable EventLoopGroup parent,
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

        this.taskQueue = newTaskQueue();
        this.waitStrategy = waitStrategyFactory.newInstance();
        this.taskBatchSize = taskBatchSize;
        this.rejectedExecutionHandler = rejectedExecutionHandler;
    }

    /**
     * 如果子类期望使用有界队列可以覆盖该方法
     */
    protected MessagePassingQueue<Runnable> newTaskQueue() {
        return new MpscLinkedQueue<>();
    }

    @Override
    public final boolean inEventLoop() {
        return thread == Thread.currentThread();
    }
    // ------------------------------------------------ 线程生命周期 -------------------------------------

    @Override
    public final boolean isShuttingDown() {
        return isShuttingDown0(state);
    }

    private static boolean isShuttingDown0(int state) {
        return state >= ST_SHUTTING_DOWN;
    }

    @Override
    public final boolean isShutdown() {
        return isShutdown0(state);
    }

    private static boolean isShutdown0(int state) {
        return state >= ST_SHUTDOWN;
    }

    @Override
    public final boolean isTerminated() {
        return state == ST_TERMINATED;
    }

    @Override
    public final boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        return terminationFuture().await(timeout, unit);
    }

    @Override
    public final FluentFuture<?> terminationFuture() {
        return terminationFuture;
    }

    private int compareAndExchangeState(int expectedState, int targetState) {
        return (int) STATE.compareAndExchange(this, expectedState, targetState);
    }

    @Override
    public final void shutdown() {
        int expectedState = state;
        for (; ; ) {
            if (isShuttingDown0(expectedState)) {
                // 已被其它线程关闭
                return;
            }

            int realState = compareAndExchangeState(expectedState, ST_SHUTTING_DOWN);
            if (realState == expectedState) {
                // CAS成功，当前线程负责了关闭
                ensureThreadTerminable(expectedState);
                return;
            }
            // retry
            expectedState = realState;
        }
    }

    @Nonnull
    @Override
    public final List<Runnable> shutdownNow() {
        shutdown();
        advanceRunState(ST_SHUTDOWN);
        // 这里不能操作taskQueue中的数据，不能打破[多生产者单消费者]的架构
        return Collections.emptyList();
    }

    /**
     * 确保线程可终止。
     * - terminable
     *
     * @param oldState 切换到shutdown之前的状态
     */
    private void ensureThreadTerminable(int oldState) {
        if (oldState == ST_NOT_STARTED) {
            state = ST_TERMINATED;
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
        WrappedRunnable r;
        // 1. 在检测到未关闭的状态下尝试压入队列
        if (!isShuttingDown() && taskQueue.relaxedOffer(r = new WrappedRunnable(task))) {
            // 2. 压入队列是一个过程！插入队列后，executor的状态可能已开始关闭，因此必须再次校验
            // 由于是多生产者单消费者模型，因此非消费者不能删除元素，因此只能置为null
            if (isShuttingDown() && TASK.compareAndSet(r, task, null)) {
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
        if (state == ST_NOT_STARTED
                && STATE.compareAndSet(this, ST_NOT_STARTED, ST_STARTED)) {
            thread.start();
        }
    }

    /**
     * 检查任务队列是否为空 - 主要用在等待策略中
     */
    public final boolean isTaskQueueEmpty() {
        assert inEventLoop();
        return taskQueue.isEmpty();
    }

    /**
     * 如果EventLoop已经开始关闭，则抛出{@link ShuttingDownException}
     */
    public final void checkShuttingDown() throws ShuttingDownException {
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
     * 确保运行状态至少已到指定状态。
     * 参考自{@code ThreadPoolExecutor#advanceRunState}
     *
     * @param targetState 期望的目标状态， {@link #ST_SHUTTING_DOWN} 或者 {@link #ST_SHUTDOWN}
     */
    private void advanceRunState(int targetState) {
        int expectedState = state;
        for (; ; ) {
            if (expectedState >= targetState) {
                return;
            }

            int realState = compareAndExchangeState(expectedState, targetState);
            if (realState >= targetState) {
                return;
            }

            // retry
            expectedState = realState;
        }
    }

    /**
     * 工作者线程
     * <p>
     * 两阶段终止模式 --- 在终止前进行清理操作，安全的关闭线程不是一件容易的事情。
     * <p>
     * 实现{@link org.jctools.queues.MessagePassingQueue.Consumer}接口用于避免lambda表达式。
     */
    private class Worker implements Runnable, MessagePassingQueue.Consumer<Runnable> {

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
                        state = ST_TERMINATED;
                        terminationFuture.setSuccess(null);
                    }
                }
            }
        }

        private void loop() {
            while (true) {
                try {
                    // 等待生产者生产数据
                    waitStrategy.waitFor(TemplateEventLoop.this);

                    // 批量消费可用数据 DisruptorEventLoop其实也批量拉取消费的
                    taskQueue.drain(this, taskBatchSize);

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

        @Override
        public void accept(Runnable runnable) {
            runnable.run();
        }

        private void cleanTaskQueue() {
            Runnable task;
            while (!isShutdown()) {
                // 这里需要使用poll
                task = taskQueue.poll();

                if (task == null) {
                    break;
                }

                task.run();
            }

            taskQueue.clear();
        }
    }

    /**
     * Q: 为什么提供该封装？
     * A: 由于{@link EventLoop}是单消费者模型，生产者只能插入不能删除。如果一个生产者在插入之后，想要删除，则违反了单消费者模型，会产生并发问题。
     * 提供一层代理，如果生产者在插入之后想要撤回，则将task置为null，这样不违背队列的单消费原则。
     * 但是这样也产生了额外的消耗，包括一次{@link VarHandle#getAndSet(Object...)}和一次{@link Runnable#run()}转发，会抵消一部分性能提升。
     * eg: 如果没有该机制的话，插入之后EventLoop开始关闭，我们将束手无策....
     */
    static class WrappedRunnable implements Runnable {

        /**
         * 非volatile，首次可见性由插入队列提供的可见性保证，后续可见性由{@link #TASK}的CAS操作提供可见性保证。
         */
        private Runnable task;

        WrappedRunnable(Runnable task) {
            this.task = task;
        }

        @Override
        public void run() {
            final Runnable r = (Runnable) TASK.getAndSet(this, null);
            // 关闭时的边界情况下，可能为null，因为生产者并不能删除任务，因此只能将task置为无效值
            if (r != null) {
                safeExecute(r);
            }
        }
    }

    private static final VarHandle STATE;
    private static final VarHandle TASK;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            STATE = l.findVarHandle(TemplateEventLoop.class, "state", int.class);
            TASK = l.findVarHandle(WrappedRunnable.class, "task", Runnable.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
