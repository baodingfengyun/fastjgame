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
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandler;
import com.wjybxx.fastjgame.concurrent.*;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * 基于Disruptor的事件循环。
 * <p>
 * 警告：任意两个{@link DisruptorEventLoop}之间最好不要互相发消息/事件，可能造成死锁！
 * <p>
 * Q: {@link DisruptorEventLoop}比{@link SingleThreadEventLoop}强在哪？
 * A: 1. 有界队列{@link LinkedBlockingQueue}、{@link ArrayBlockingQueue}性能太差。
 * 2. {@link ConcurrentLinkedQueue} 是无界的，不能很好的管理资源。
 * 3. {@link RingBuffer}的内存利用的很好，且性能极好，我测了几次，比{@link ConcurrentLinkedQueue}大概高20%-25%。
 * <p>
 * Q: 那么有什么缺陷呢？<br>
 * A: 1. {@link RingBuffer}是有界的。由于{@link EventLoop}都是单线程的，如果两个{@link EventLoop}都使用有界队列，容易死锁！ - eg: 网络层尝试向应用层提交任务，应用层尝试向网络层提交任务。
 * 2. 提交任务失败时：如果限制重试次数，则可能丢掉任务，可能产生严重影响。如果不限制重试次数，则可能长时间阻塞，甚至死锁！
 * 2. 线程的生命周期不好控制！因为线程不是自己创建的，而是来源于{@link BatchEventProcessor}
 * 3. 线程的循环逻辑也不好控制。因为循环逻辑也是来源于{@link BatchEventProcessor}
 * 4. 内存泄漏，{@link Disruptor}关闭时无法很好清理{@link RingBuffer}剩余的事件。
 * 第一项和第四项其实是有点危险的。
 *
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
     * 1024 * 1024 个对象大概 16M
     */
    public static final int DEFAULT_RING_BUFFER_SIZE = SystemUtils.getProperties().getAsInt("DisruptorEventLoop.ringBufferSize", 1024 * 1024);
    /**
     * 提交任务最大尝试次数 - 避免死锁
     */
    public static final int MAX_TRY_TIMES = SystemUtils.getProperties().getAsInt("DisruptorEventLoop.maxTryTimes", 10 * 10000);

    // 线程的状态
    /**
     * 初始状态，未启动状态
     */
    private static final int ST_NOT_STARTED = 1;
    /**
     * 运行状态
     */
    private static final int ST_RUNNING = 2;
    /**
     * 已关闭状态，正在进行最后的清理
     */
    private static final int ST_SHUTDOWN = 3;
    /**
     * 终止状态(二阶段终止模式 - 已关闭状态下进行最后的清理，然后进入终止状态)
     */
    private static final int ST_TERMINATED = 4;

    /**
     * Disruptor线程 {@link BatchEventProcessor}所属的线程。
     */
    private volatile Thread thread;
    /**
     * disruptor
     */
    private final Disruptor<RunnableEvent> disruptor;

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

    public DisruptorEventLoop(@Nullable EventLoopGroup parent,
                              @Nonnull ThreadFactory threadFactory,
                              @Nonnull RejectedExecutionHandler rejectedExecutionHandler) {
        this(parent, threadFactory, rejectedExecutionHandler, DEFAULT_RING_BUFFER_SIZE);
    }

    /**
     * @param parent                   容器节点
     * @param threadFactory            线程工厂
     * @param rejectedExecutionHandler 拒绝策略
     * @param ringBufferSize           事件缓冲区大小
     */
    public DisruptorEventLoop(@Nullable EventLoopGroup parent,
                              @Nonnull ThreadFactory threadFactory,
                              @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                              int ringBufferSize) {
        super(parent);
        this.rejectedExecutionHandler = rejectedExecutionHandler;

        this.disruptor = new Disruptor<>(RunnableEvent::new,
                ringBufferSize,
                new InnerThreadFactory(threadFactory),
                ProducerType.MULTI,
                new DisruptorEventLoopWaitStrategy(this));

        disruptor.setDefaultExceptionHandler(new DisruptorExceptionHandler());

        // 加一层封装，避免EventLoop暴露Disruptor相关接口
        disruptor.handleEventsWith(new InnerEventHandler(this));

        this.ringBuffer = disruptor.getRingBuffer();
    }

    @Override
    public boolean inEventLoop() {
        return thread == Thread.currentThread();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated use {@link #isShutdown()}
     */
    @Deprecated
    @Override
    public boolean isShuttingDown() {
        return isShutdown();
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
            if (isShutdown0(oldState)) {
                return;
            }
            if (stateHolder.compareAndSet(oldState, ST_SHUTDOWN)) {
                // 确保线程能够关闭 - 这里不要判断inEventLoop，因为线程并不由自己控制 - 后期会修改实现
                ensureThreadTerminable(oldState);
            }
        }
    }

    @Override
    public void execute(@Nonnull Runnable task) {
        if (inEventLoop()) {
            // 防止死锁 - 因为是单消费者模型，自己发布事件时，如果没有足够空间，会导致死锁。
            // 线程内部，请使用TimerSystem延迟执行
            // Q: 这里为什么不直接添加到timerSystem？
            // A: 因为时序问题，会让让用户以为execute之间有时序保证，而实际上是没有的。
            throw new BlockingOperationException("");
        } else {
            // 直接调用next()的情况下，Disruptor内部是死循环，没有任何机制能保证安全的退出，即使Disruptor线程已退出。
            // 如果disruptor已关闭则可能死锁。
            int tryTimes = 1;
            for (; tryTimes <= MAX_TRY_TIMES && !isShutdown(); tryTimes++) {
                try {
                    long sequence = ringBuffer.tryNext();
                    try {
                        RunnableEvent event = ringBuffer.get(sequence);
                        event.setTask(task);
                        return;
                    } finally {
                        ringBuffer.publish(sequence);
                        // 确保线程已启动
                        ensureThreadStarted();
                    }
                } catch (InsufficientCapacityException ignore) {
                    // 最少睡眠100纳秒，最多睡眠1毫秒
                    long sleepTimes = (1 + ThreadLocalRandom.current().nextInt(Math.min(10000, tryTimes))) * 100;
                    LockSupport.parkNanos(sleepTimes);
                }
            }
            if (tryTimes >= MAX_TRY_TIMES) {
                logger.error("may cause dead lock, tryTimes {}", tryTimes);
            }
            rejectedExecutionHandler.rejected(task, this);
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
     * 确保线程已启动
     */
    private void ensureThreadStarted() {
        if (stateHolder.get() == ST_NOT_STARTED) {
            if (stateHolder.compareAndSet(ST_NOT_STARTED, ST_RUNNING)) {
                disruptor.start();
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
            terminationFuture.trySuccess(null);
        } else {
            // 中断当前线程
            disruptor.halt();
        }
    }

    private void onStart() {
        try {
            init();
        } catch (Throwable e) {
            stateHolder.set(ST_TERMINATED);
            terminationFuture.tryFailure(e);
            // 启动时抛出异常，无法走到 onShutdown
            ConcurrentUtils.rethrow(new RuntimeException(e));
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
     * 执行一次循环
     */
    protected void loopOnce() {

    }

    /**
     * 线程退出前的清理动作
     */
    protected void clean() throws Exception {

    }

    private void onShutdown() {
        // 告知其它线程已开始关闭 - 不要再提交任务
        advanceRunState(ST_SHUTDOWN);

        // 退出前进行必要的清理，释放系统资源
        try {
            clean();
        } catch (Throwable e) {
            logger.error("thread clean caught exception!", e);
        } finally {
            stateHolder.set(ST_TERMINATED);
            terminationFuture.setSuccess(null);
        }
    }

    private void onEvent(RunnableEvent event, boolean endOfBatch) throws Exception {
        safeExecute(event.detachTask());
        if (endOfBatch) {
            // 每执行一批事件，执行一次循环
            loopOnce();
        }
    }
    // ------------------------------ 生命周期end --------------------------------

    private class InnerThreadFactory implements ThreadFactory {

        private final ThreadFactory threadFactory;

        private InnerThreadFactory(ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
        }

        @Override
        public Thread newThread(@Nonnull Runnable r) {
            Thread thread = threadFactory.newThread(r);
            // 捕获运行线程
            DisruptorEventLoop.this.thread = thread;
            return thread;
        }
    }

    /**
     * EventHandler内部实现，避免{@link DisruptorEventLoop}对外暴露这些接口
     */
    private static class InnerEventHandler implements EventHandler<RunnableEvent>, LifecycleAware, TimeoutHandler {

        private final DisruptorEventLoop disruptorEventLoop;

        private InnerEventHandler(DisruptorEventLoop disruptorEventLoop) {
            this.disruptorEventLoop = disruptorEventLoop;
        }

        @Override
        public void onStart() {
            disruptorEventLoop.onStart();
        }

        @Override
        public void onEvent(RunnableEvent event, long sequence, boolean endOfBatch) throws Exception {
            disruptorEventLoop.onEvent(event, endOfBatch);
        }

        @Override
        public void onTimeout(long sequence) throws Exception {
            logger.warn("waitOnBarrier timeout, sequence {}", sequence);
        }

        @Override
        public void onShutdown() {
            disruptorEventLoop.onShutdown();
        }
    }

}
