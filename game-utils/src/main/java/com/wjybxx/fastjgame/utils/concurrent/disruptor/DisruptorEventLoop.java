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

package com.wjybxx.fastjgame.utils.concurrent.disruptor;

import com.lmax.disruptor.*;
import com.wjybxx.fastjgame.utils.concurrent.*;
import com.wjybxx.fastjgame.utils.concurrent.unbounded.UnboundedEventLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * 基于Disruptor的事件循环。
 * 它是有界事件循环的超类，如果期望使用无界队列，请使用{@link UnboundedEventLoop}。
 * 等待策略与{@link UnboundedEventLoop}是一致的。
 *
 * <p>
 * Q: {@link DisruptorEventLoop}比起{@link UnboundedEventLoop}，优势在哪？<br>
 * A: 1. {@link DisruptorEventLoop}采用的是无锁队列，性能高于{@link UnboundedEventLoop}。
 * 2. {@link DisruptorEventLoop}对资源的利用率远胜{@link UnboundedEventLoop}。
 * <p>
 * Q: 那缺陷在哪呢？
 * A: 最大的缺陷就是它只能是有界的队列。由于{@link EventLoop}都是单线程的，如果某个EventLoop使用有界队列，且有阻塞式操作，则可能导致死锁或较长时间阻塞。
 *
 * <p>
 * Q: 哪些事件循环适合使用{@link DisruptorEventLoop} ?<br>
 * A: 如果你的服务处于终端节点，且需要极低的延迟和极高的吞吐量的时候。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/24
 * github - https://github.com/hl845740757
 */
public class DisruptorEventLoop extends AbstractEventLoop {

    private static final Logger logger = LoggerFactory.getLogger(DisruptorEventLoop.class);

    /**
     * 默认ringBuffer大小 - 大一点可以减少降低阻塞概率
     * 64 * 1024 个{@link RunnableEvent}对象大概1M。
     * 如果提交的任务较大，那么内存占用可能较大，用户请根据实际情况调整。
     */
    private static final int DEFAULT_RING_BUFFER_SIZE = 64 * 1024;
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
     * 运行状态
     */
    private static final int ST_STARTED = 2;
    /**
     * 正在关闭状态
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
     * 工作线程
     */
    private final Thread thread;
    /**
     * 真正执行逻辑的对象
     */
    private final Worker worker;
    /**
     * 线程状态
     */
    private volatile int state = ST_NOT_STARTED;

    /**
     * 事件队列
     */
    private final RingBuffer<RunnableEvent> ringBuffer;
    /**
     * 批量执行任务的大小
     */
    private final int taskBatchSize;

    /**
     * 任务拒绝策略
     */
    private final RejectedExecutionHandler rejectedExecutionHandler;

    /**
     * 线程终止future
     */
    private final Promise<?> terminationFuture = FutureUtils.newPromise();

    /**
     * @param parent                   容器节点
     * @param threadFactory            线程工厂
     * @param rejectedExecutionHandler 拒绝策略
     */
    public DisruptorEventLoop(@Nullable EventLoopGroup parent,
                              @Nonnull ThreadFactory threadFactory,
                              @Nonnull RejectedExecutionHandler rejectedExecutionHandler) {
        this(parent, threadFactory, rejectedExecutionHandler,
                DEFAULT_RING_BUFFER_SIZE, DEFAULT_BATCH_EVENT_SIZE,
                new SleepWaitStrategyFactory());
    }

    /**
     * @param parent                   容器节点
     * @param threadFactory            线程工厂
     * @param rejectedExecutionHandler 拒绝策略
     * @param ringBufferSize           环形缓冲区大小
     * @param taskBatchSize            批量拉取(执行)任务数(小于等于0则不限制)
     */
    public DisruptorEventLoop(@Nullable EventLoopGroup parent,
                              @Nonnull ThreadFactory threadFactory,
                              @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                              int ringBufferSize, int taskBatchSize) {
        this(parent, threadFactory, rejectedExecutionHandler,
                ringBufferSize, taskBatchSize,
                new SleepWaitStrategyFactory());
    }

    /**
     * @param parent                   容器节点
     * @param threadFactory            线程工厂
     * @param rejectedExecutionHandler 拒绝策略
     * @param waitStrategyFactory      等待策略工厂
     */
    public DisruptorEventLoop(@Nullable EventLoopGroup parent,
                              @Nonnull ThreadFactory threadFactory,
                              @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                              @Nonnull WaitStrategyFactory waitStrategyFactory) {
        this(parent, threadFactory, rejectedExecutionHandler,
                DEFAULT_RING_BUFFER_SIZE, DEFAULT_BATCH_EVENT_SIZE,
                waitStrategyFactory);
    }

    /**
     * @param parent                   容器节点
     * @param threadFactory            线程工厂
     * @param rejectedExecutionHandler 拒绝策略
     * @param ringBufferSize           环形缓冲区大小
     * @param taskBatchSize            批量拉取(执行)任务数(小于等于0则不限制)
     * @param waitStrategyFactory      等待策略工厂
     */
    public DisruptorEventLoop(@Nullable EventLoopGroup parent,
                              @Nonnull ThreadFactory threadFactory,
                              @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                              int ringBufferSize, int taskBatchSize,
                              @Nonnull WaitStrategyFactory waitStrategyFactory) {

        super(parent);
        this.rejectedExecutionHandler = rejectedExecutionHandler;
        this.ringBuffer = RingBuffer.createMultiProducer(RunnableEvent::new,
                ringBufferSize,
                waitStrategyFactory.newWaitStrategy(this));
        this.taskBatchSize = taskBatchSize;

        // 它不依赖于其它消费者，只依赖生产者的sequence
        worker = new Worker(ringBuffer.newBarrier());
        // 添加worker的sequence为网关sequence，生产者们会监听到该sequence
        ringBuffer.addGatingSequences(worker.sequence);

        // 保存线程对象
        this.thread = Objects.requireNonNull(threadFactory.newThread(worker), "newThread");
        UncaughtExceptionHandlers.logIfAbsent(thread, logger);
    }

    @Override
    public final boolean inEventLoop() {
        return thread == Thread.currentThread();
    }

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
        int expectedState = state;
        for (; ; ) {
            if (isShutdown0(expectedState)) {
                // 已被其它线程关闭
                return Collections.emptyList();
            }

            int realState = compareAndExchangeState(expectedState, ST_SHUTDOWN);
            if (expectedState == realState) {
                // CAS成功，当前线程负责了关闭 - 这里不能操作ringBuffer中的数据，不能打破[多生产者单消费者]的架构
                ensureThreadTerminable(expectedState);
                return Collections.emptyList();
            }
            // retry
            expectedState = realState;
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
            // TODO 是否需要启动线程，进行更彻底的清理？
            state = ST_TERMINATED;
            terminationFuture.setSuccess(null);
        } else {
            // 等待策略的是根据该信号判断EventLoop是否已开始关闭的，因此即使inEventLoop也需要中断，否则可能丢失信号，在waitFor处无法停止
            worker.sequenceBarrier.alert();

            // 唤醒线程 - 如果线程可能阻塞在其它地方
            if (!inEventLoop()) {
                wakeUpForShutdown();
            }
        }
    }

    /**
     * 如果子类可能阻塞在其它地方，那么应该重写该方法以唤醒线程
     */
    protected void wakeUpForShutdown() {

    }

    /**
     * 中断消费者线程。
     * 通常用于唤醒线程，如果线程需要通过中断唤醒。
     */
    protected final void interruptThread() {
        thread.interrupt();
    }

    @Override
    public final void execute(@Nonnull Runnable task) {
        // 这里不先判断{@code isShuttingDown()}，在申请sequence之后判断是否拒绝任务，可以减小整体开销
        try {
            tryPublish(task, ringBuffer.tryNext(1));
        } catch (InsufficientCapacityException ignore) {
            rejectedExecutionHandler.rejected(task, this);
        }
    }

    /**
     * Q: 如何保证算法的安全性的？
     * A: 我们只需要保证申请到的sequence是有效的，且发布任务在{@link Worker#cleanRingBuffer()}之前即可。
     * 三个关键时序：
     * 1. {@link #isShuttingDown()}为true一定在{@link Worker#removeFromGatingSequence()}之前。
     * 2. {@link Worker#removeFromGatingSequence()}在{@link Worker#cleanRingBuffer()}之前。
     * 3. {@link Worker#cleanRingBuffer()}必须等待在这之前申请到的sequence发布。
     * <p>
     * 如果sequence是在{@link Worker#removeFromGatingSequence()}之前申请到的，那么该sequence就是有效的（它考虑了EventLoop的消费进度）。
     * 如果sequence是在{@link #isShuttingDown()}为true之前申请到的，那么一定在{@link Worker#removeFromGatingSequence()}之前，也就是一定是有效的！
     * 因此：申请到sequence之后，如果{@link #isShuttingDown()}为false，那么sequence一定是有效的。如果{@link #isShuttingDown()}为true，则可能有效，也可能无效。
     * <p>
     * 根据上文，如果生产者持有有效的sequence，那么一定在{@link Worker#removeFromGatingSequence()}之前，也就一定在{@link Worker#cleanRingBuffer()}之前，
     * 因此{@link Worker#cleanRingBuffer()}必须等待生产者发布该sequence，也就保证了发布任务一定在{@link Worker#cleanRingBuffer()}之前。
     */
    private void tryPublish(@Nonnull Runnable task, long sequence) {
        if (isShuttingDown()) {
            // 先发布sequence，避免拒绝逻辑可能产生的阻塞
            ringBuffer.publish(sequence);

            rejectedExecutionHandler.rejected(task, this);
        } else {
            try {
                // 发布任务
                ringBuffer.get(sequence).setTask(task);
            } finally {
                ringBuffer.publish(sequence);

                // 确保线程已启动
                // 如果sequence >= ringBufferSize 表明消费者一定已启动
                if (sequence < ringBuffer.getBufferSize() && !inEventLoop()) {
                    ensureThreadStarted();
                }
            }
        }
    }

    /**
     * 确保线程已启动
     */
    private void ensureThreadStarted() {
        if (state == ST_NOT_STARTED
                && STATE.compareAndSet(this, ST_NOT_STARTED, ST_STARTED)) {
            thread.start();
        }
    }

    // --------------------------------------- 线程管理 ----------------------------------------

    /**
     * 事件循环线程启动时的初始化操作。
     *
     * @apiNote 抛出任何异常都将导致线程终止
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
     * 线程退出前的清理动作
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
     * 将运行状态转换为给定目标，或者至少保留给定状态。
     * 参考自{@code ThreadPoolExecutor#advanceRunState}
     *
     * @param targetState 期望的目标状态
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
     * 实现{@link RingBuffer}的消费者，实现基本和{@link BatchEventProcessor}一致。
     * 但解决了两个问题：
     * 1. 生产者调用{@link RingBuffer#next()}时，如果消费者已关闭，则会死锁！为避免死锁不得不使用{@link RingBuffer#tryNext()}，但是那样的代码并不友好。
     * 2. 内存泄漏问题，使用{@link BatchEventProcessor}在关闭时无法清理{@link RingBuffer}中的数据。
     */
    private class Worker implements Runnable {

        private final Sequence sequence = new Sequence(Sequencer.INITIAL_CURSOR_VALUE);
        private final SequenceBarrier sequenceBarrier;

        private Worker(SequenceBarrier sequenceBarrier) {
            this.sequenceBarrier = sequenceBarrier;
        }

        @Override
        public void run() {
            try {
                init();

                loop();
            } catch (Throwable e) {
                logger.error("thread exit due to exception!", e);
            } finally {
                // 如果是非正常退出，需要切换到正在关闭状态 - 告知其它线程，已经开始关闭
                advanceRunState(ST_SHUTTING_DOWN);

                try {
                    // 从网关sequence中删除自己
                    removeFromGatingSequence();

                    // 清理ringBuffer中的数据
                    cleanRingBuffer();
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

        private void loop() throws Exception {
            long availableSequence;
            long nextSequence = sequence.get() + 1L;

            while (true) {
                try {
                    // 等待生产者生产数据
                    availableSequence = waitFor(nextSequence);

                    if (nextSequence <= availableSequence) {

                        // 处理所有可消费的事件
                        while (nextSequence <= availableSequence) {
                            safeExecute(ringBuffer.get(nextSequence).detachTask());
                            nextSequence++;
                        }

                        // 标记这批事件已处理
                        sequence.set(availableSequence);
                    }

                    // 执行一次循环
                    safeLoopOnce();
                } catch (AlertException | InterruptedException e) {
                    // 请求了关闭
                    // BatchEventProcessor实现中并没有处理中断异常
                    if (isShuttingDown()) {
                        break;
                    }
                } catch (TimeoutException e) {
                    // 等待超时，执行一次循环
                    safeLoopOnce();
                } catch (Throwable e) {
                    // 不好的等待策略实现
                    // 这是和BatchEventProcessor不一样的地方，这里并不会更新sequence，不会导致数据丢失问题！
                    logger.error("bad waitStrategy imp", e);
                    // 检测退出
                    if (isShuttingDown()) {
                        break;
                    }
                }
            }
        }

        private long waitFor(long nextSequence) throws AlertException, InterruptedException, TimeoutException {
            if (taskBatchSize < 1) {
                return sequenceBarrier.waitFor(nextSequence);
            } else {
                return Math.min(nextSequence + taskBatchSize - 1, sequenceBarrier.waitFor(nextSequence));
            }
        }

        /**
         * 这是解决死锁问题的关键，如果不从gatingSequence中移除，则{@link RingBuffer#next(int)} 方法可能死锁。
         */
        private void removeFromGatingSequence() {
            ringBuffer.removeGatingSequence(sequence);
        }

        private void cleanRingBuffer() {
            final long startTimeMillis = System.currentTimeMillis();
            // 申请整个空间，因此与真正的生产者之间是互斥的！这是关键
            final long finalSequence = ringBuffer.next(ringBuffer.getBufferSize());
            final long initialSequence = finalSequence - (ringBuffer.getBufferSize() - 1);
            try {
                // Q: 为什么可以继续消费
                // A: 保证了生产者不会覆盖未消费的数据 - shutdown处的处理是必须的
                long nextSequence = sequence.get() + 1;
                final long endSequence = sequence.get() + ringBuffer.getBufferSize();
                for (; nextSequence <= endSequence; nextSequence++) {
                    final Runnable task = ringBuffer.get(nextSequence).detachTask();
                    // Q: 这里可能为null吗？
                    // A: 这里可能为null - 因为是多生产者模式，关闭前发布的数据可能是不连续的
                    // 如果已进入shutdown阶段，则直接丢弃任务，而不是执行
                    if (null != task && !isShutdown()) {
                        safeExecute(task);
                    }
                }
            } finally {
                ringBuffer.publish(initialSequence, finalSequence);
                logger.info("cleanRingBuffer success! cost timeMillis = {}", System.currentTimeMillis() - startTimeMillis);
            }
        }
    }

    private static final class RunnableEvent {

        private Runnable task;

        RunnableEvent() {

        }

        Runnable detachTask() {
            Runnable r = task;
            task = null;
            return r;
        }

        void setTask(@Nonnull Runnable task) {
            this.task = task;
        }

    }

    private static final VarHandle STATE;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            STATE = l.findVarHandle(DisruptorEventLoop.class, "state", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
