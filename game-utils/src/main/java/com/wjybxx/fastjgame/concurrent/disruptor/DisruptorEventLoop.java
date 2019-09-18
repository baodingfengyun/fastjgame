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

package com.wjybxx.fastjgame.concurrent.disruptor;

import com.lmax.disruptor.*;
import com.wjybxx.fastjgame.concurrent.*;
import com.wjybxx.fastjgame.timer.TimerSystem;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * 基于Disruptor的事件循环。
 *
 * <p>
 * Q: {@link DisruptorEventLoop}比起{@link SingleThreadEventLoop}，优势在哪？<br>
 * A: {@link DisruptorEventLoop}对资源的利用率远胜{@link SingleThreadEventLoop}，性能也要高出不少。
 * <p>
 * Q: 那缺陷在哪呢？
 * A: 1. 最大的缺陷就是它只能是有界的队列。<br>
 * 2. {@link DisruptorEventLoop}涉及很多额外的知识，涉及到Disruptor框架，而且我为了解决它存在的一些问题，又选择了一些自定义的实现，
 * 导致其阅读难度较大，不像{@link SingleThreadEventLoop}那么清晰易懂。<br>
 * 3. 子类不能控制循环逻辑，无法自己决定什么时候调用{@link #loopOnce()}。<br>
 *
 * <p>
 * Q: 哪些事件循环适合使用{@link DisruptorEventLoop} ?<br>
 * A: 如果你的服务处于终端节点，且需要极低的延迟和极高的吞吐量的时候。{@link DisruptorEventLoop}最好不要用在中间节点，超过负载可能死锁！
 *
 * <p>
 * 警告：由于{@link EventLoop}都是单线程的，如果两个{@link EventLoop}都使用有界队列，如果互相通信，如果超过负载，则可能死锁！
 * - eg: 网络层尝试向应用层提交任务，应用层尝试向网络层提交任务。
 * <b>
 * 而{@link DisruptorEventLoop}使用的就是有界队列。
 * </b>
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/24
 * github - https://github.com/hl845740757
 */
public class DisruptorEventLoop extends AbstractEventLoop {

    private static final Logger logger = LoggerFactory.getLogger(DisruptorEventLoop.class);

    /**
     * 执行{@link #loopOnce()}的间隔，该值越小{@link #loopOnce()}调用频率越高。
     */
    static final int LOOP_ONCE_INTERVAL = 8192;

    /**
     * 默认ringBuffer大小 - 大一点可以减少降低阻塞概率
     * 1024 * 1024 个对象大概 16M
     */
    public static final int DEFAULT_RING_BUFFER_SIZE = SystemUtils.getProperties().getAsInt("DisruptorEventLoop.ringBufferSize", 1024 * 1024);

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
     * 事件队列
     */
    private final RingBuffer<RunnableEvent> ringBuffer;

    /**
     * 线程状态
     */
    private final AtomicInteger stateHolder = new AtomicInteger(ST_NOT_STARTED);

    /**
     * 线程终止future
     */
    private final Promise<?> terminationFuture = new DefaultPromise(GlobalEventLoop.INSTANCE);
    /**
     * 任务拒绝策略
     */
    private final RejectedExecutionHandler rejectedExecutionHandler;

    /**
     * @param parent                   容器节点
     * @param threadFactory            线程工厂
     * @param rejectedExecutionHandler 拒绝策略
     */
    public DisruptorEventLoop(@Nullable EventLoopGroup parent,
                              @Nonnull ThreadFactory threadFactory,
                              @Nonnull RejectedExecutionHandler rejectedExecutionHandler) {
        this(parent, threadFactory, rejectedExecutionHandler, DEFAULT_RING_BUFFER_SIZE, DisruptorWaitStrategyType.SLEEP);
    }

    /**
     * @param parent                   容器节点
     * @param threadFactory            线程工厂
     * @param rejectedExecutionHandler 拒绝策略
     * @param ringBufferSize           环形缓冲区大小
     */
    public DisruptorEventLoop(@Nullable EventLoopGroup parent,
                              @Nonnull ThreadFactory threadFactory,
                              @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                              int ringBufferSize) {
        this(parent, threadFactory, rejectedExecutionHandler, ringBufferSize, DisruptorWaitStrategyType.SLEEP);
    }


    /**
     * @param parent                   容器节点
     * @param threadFactory            线程工厂
     * @param rejectedExecutionHandler 拒绝策略
     * @param waitStrategyType         等待策略
     */
    public DisruptorEventLoop(@Nullable EventLoopGroup parent,
                              @Nonnull ThreadFactory threadFactory,
                              @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                              @Nonnull DisruptorWaitStrategyType waitStrategyType) {
        this(parent, threadFactory, rejectedExecutionHandler, DEFAULT_RING_BUFFER_SIZE, waitStrategyType);
    }

    /**
     * @param parent                   容器节点
     * @param threadFactory            线程工厂
     * @param rejectedExecutionHandler 拒绝策略
     * @param ringBufferSize           环形缓冲区大小
     * @param waitStrategyType         等待策略
     */
    public DisruptorEventLoop(@Nullable EventLoopGroup parent,
                              @Nonnull ThreadFactory threadFactory,
                              @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                              int ringBufferSize,
                              @Nonnull DisruptorWaitStrategyType waitStrategyType) {

        super(parent);
        this.rejectedExecutionHandler = rejectedExecutionHandler;
        this.ringBuffer = RingBuffer.createMultiProducer(RunnableEvent::new,
                ringBufferSize,
                DisruptorWaitStrategyFactory.newWaitStrategy(this, waitStrategyType));

        // 它不依赖于其它消费者，只依赖生产者的sequence
        worker = new Worker(ringBuffer.newBarrier());
        // worker的sequence添加为网关sequence，生产者们会监听到该sequence
        ringBuffer.addGatingSequences(worker.sequence);

        // 保存线程对象
        this.thread = threadFactory.newThread(worker);
    }

    @Override
    public boolean inEventLoop() {
        return thread == Thread.currentThread();
    }

    @Override
    public boolean isShuttingDown() {
        return isShuttingDown0(stateHolder.get());
    }

    private static boolean isShuttingDown0(int state) {
        return state >= ST_SHUTTING_DOWN;
    }

    @Override
    public boolean isShutdown() {
        return isShutdown0(stateHolder.get());
    }

    private static boolean isShutdown0(int state) {
        return state >= ST_SHUTDOWN;
    }

    @Override
    public boolean isTerminated() {
        return stateHolder.get() == ST_TERMINATED;
    }

    @Override
    public ListenableFuture<?> terminationFuture() {
        return terminationFuture;
    }

    @Override
    public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        return terminationFuture.await(timeout, unit);
    }

    @Override
    public void shutdown() {
        for (; ; ) {
            int oldState = stateHolder.get();
            if (isShuttingDown0(oldState)) {
                return;
            }
            if (stateHolder.compareAndSet(oldState, ST_SHUTTING_DOWN)) {
                // 确保线程能够关闭
                ensureThreadTerminable(oldState);
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
            // 中断当前线程，即使inEventLoop也需要中断，否则可能丢失信号，在waitFor处无法停止
            worker.sequenceBarrier.alert();
        }
    }

    /**
     * Q: 如何保证算法的安全性的？
     * A: 前提：{@link Worker#cleanRingBuffer()}与其它生产者调用{@link RingBuffer#publish(long)}发布事件之间是互斥的！
     * 1. 如果sequence是在{@link Worker#cleanRingBuffer()}之后申请的sequence，那么一定能检测到{@link #isShuttingDown() true}，
     * 因此不会发布任务。
     * 2. 如果是在{@link Worker#cleanRingBuffer()}之前申请的sequence，那么{@link Worker#cleanRingBuffer()}一定能清理掉它！
     */
    @Override
    public void execute(@Nonnull Runnable task) {
        if (inEventLoop()) {
            // 防止死锁 - 因为是单消费者模型，自己发布事件时，如果没有足够空间，会导致死锁。
            // 线程内部，请使用TimerSystem延迟执行

            // Q: 为什么不使用 tryNext()？
            // A: 会使得使用的人懵逼的，怎么一会儿能用，一会儿不能用。

            // Q: 这里为什么不直接添加到timerSystem？
            // A: 因为时序问题，会让让用户以为execute之间有时序保证，而实际上是没有的。

            throw new BlockingOperationException("");
        } else {
            // 消费者线程已开始关闭，则直接拒绝
            if (isShuttingDown()) {
                rejectedExecutionHandler.rejected(task, this);
                return;
            }
            // 这里之所以可以安全的调用 next()，是因为我实现了自己的事件处理器(Worker)，否则next()是可能死锁的！
            final long sequence = ringBuffer.next(1);
            try {
                if (isShuttingDown()) {
                    // 申请sequence是一个过程，判断如果申请完毕之后开始关闭，发布一个无操作的Task，避免Null
                    ringBuffer.get(sequence).setTask(ConcurrentUtils.NO_OP_TASK);
                    // 拒绝任务
                    rejectedExecutionHandler.rejected(task, this);
                } else {
                    // 发布真正的任务
                    ringBuffer.get(sequence).setTask(task);
                    // 确保线程已启动
                    ensureThreadStarted();
                }
            } finally {
                ringBuffer.publish(sequence);
            }
        }
    }

    /**
     * 确保线程已启动
     */
    private void ensureThreadStarted() {
        int oldState = stateHolder.get();
        if (oldState == ST_NOT_STARTED) {
            if (stateHolder.compareAndSet(ST_NOT_STARTED, ST_STARTED)) {
                thread.start();
            }
        }
    }

    // ----------------------------- 生命周期begin ------------------------------

    /**
     * 将运行状态转换为给定目标，或者至少保留给定状态。
     * 参考自{@code ThreadPoolExecutor#advanceRunState}
     *
     * @param targetState 期望的目标状态
     */
    private void advanceRunState(int targetState) {
        for (; ; ) {
            int oldState = stateHolder.get();
            if (oldState >= targetState || stateHolder.compareAndSet(oldState, targetState))
                break;
        }
    }

    /**
     * 事件循环线程启动时的初始化操作。
     *
     * @apiNote 抛出任何异常都将导致线程终止
     */
    protected void init() throws Exception {

    }

    /**
     * 执行一次循环。
     *
     * @apiNote 注意由于调用时机并不确定，子类实现需要自己控制真实的帧间隔，可以使用{@link TimerSystem}控制。
     */
    protected void loopOnce() throws Exception {

    }

    /**
     * 线程退出前的清理动作
     */
    protected void clean() throws Exception {

    }

    /**
     * 如果使用超时等待策略，返回超时对应的纳秒数，默认1毫秒对应的纳秒数。
     *
     * @return nanoTime
     */
    protected long timeoutInNano() {
        return TimeUnit.MILLISECONDS.toNanos(1);
    }

    /**
     * 安全的执行一次循环。
     * 注意：该方法不是给子类的API
     */
    final void safeLoopOnce() {
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

    // ------------------------------ 生命周期end --------------------------------

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
                if (ConcurrentUtils.isInterrupted(e)) {
                    logger.info("thread exit due to interrupted!", e);
                } else {
                    logger.error("thread exit due to exception!", e);
                }
            } finally {
                // 如果是非正常退出，需要切换到正在关闭状态 - 告知其它线程，已经开始关闭
                advanceRunState(ST_SHUTTING_DOWN);
                try {
                    // 清理ringBuffer中的数据
                    cleanRingBuffer();
                } finally {
                    // 退出前进行必要的清理，释放系统资源
                    try {
                        clean();
                    } catch (Exception e) {
                        logger.error("thread clean caught exception!", e);
                    } finally {
                        // 设置为终止状态
                        stateHolder.set(ST_TERMINATED);
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
                    availableSequence = sequenceBarrier.waitFor(nextSequence);
                    // 处理所有可消费的事件
                    while (nextSequence <= availableSequence) {
                        safeExecute(ringBuffer.get(nextSequence).detachTask());
                        nextSequence++;
                    }

                    // 标记这批事件已处理
                    sequence.set(availableSequence);

                    if ((LOOP_ONCE_INTERVAL & availableSequence) == 0) {
                        // 处理完一批事件，执行一次循环
                        safeLoopOnce();
                    }
                } catch (AlertException | InterruptedException e) {
                    // 请求了关闭 BatchEventProcessor并没有响应中断请求，会导致中断信号丢失。
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

        private void cleanRingBuffer() {
            // 删除自己：这是解决生产者死锁问题的关键，生产者一定能从next中醒来
            ringBuffer.removeGatingSequence(sequence);
            // 清理数据，有最大尝试次数，是为了避免长时间无法关闭
            long startNanoTime = System.nanoTime();
            for (int triTimes = 0; triTimes < 100_0000; triTimes++) {
                try {
                    // 申请整个空间，因此与真正的生产者之间是互斥的！这是关键
                    final long finalSequence = ringBuffer.tryNext(ringBuffer.getBufferSize());
                    final long initialSequence = finalSequence - (ringBuffer.getBufferSize() - 1);
                    try {
                        for (long sequence = initialSequence; sequence <= finalSequence; sequence++) {
                            // 丢弃数据，避免内存泄漏
                            ringBuffer.get(sequence).disCard();
                        }
                        break;
                    } finally {
                        ringBuffer.publish(initialSequence, finalSequence);
                        logger.info("cleanRingBuffer success! tryTimes = {}, cost nanoTime = {}", triTimes, System.nanoTime() - startNanoTime);
                    }
                } catch (InsufficientCapacityException ignore) {
                    // 等待真实的生产者发布数据
                    LockSupport.parkNanos(100);
                }
            }
            // 标记为已进入最终清理阶段
            advanceRunState(ST_SHUTDOWN);
        }
    }

}
