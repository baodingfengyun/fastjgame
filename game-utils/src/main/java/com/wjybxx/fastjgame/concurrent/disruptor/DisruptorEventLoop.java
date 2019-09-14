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

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.LifecycleAware;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.wjybxx.fastjgame.concurrent.*;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * 基于Disruptor的事件循环。
 * <p>
 * 可能阻塞生产者，因为消费者的速度可能跟不上。
 * <p>
 * 代码和{@link SingleThreadEventLoop}很像，但是又不完全一样，类似的代码写两遍还是有点难受。。。。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/24
 * github - https://github.com/hl845740757
 */
public class DisruptorEventLoop extends AbstractEventLoop {

    private static final Logger logger = LoggerFactory.getLogger(DisruptorEventLoop.class);

    /**
     * 默认ringBuffer大小
     */
    static final int DEFAULT_RING_BUFFER_SIZE = SystemUtils.getProperties().getAsInt("DisruptorEventLoop.ringBufferSize", 16 * 1024);

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
     * Disruptor线程
     */
    private volatile Thread thread;
    /**
     * disruptor
     */
    private final Disruptor<DisruptorEvent> disruptor;

    /**
     * 事件队列
     */
    private final RingBuffer<DisruptorEvent> ringBuffer;

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
     * @param ringBufferSize           事件缓冲区大小
     * @param rejectedExecutionHandler 拒绝策略
     */
    public DisruptorEventLoop(@Nullable EventLoopGroup parent,
                              @Nonnull ThreadFactory threadFactory,
                              int ringBufferSize,
                              @Nonnull RejectedExecutionHandler rejectedExecutionHandler) {
        super(parent);
        this.rejectedExecutionHandler = rejectedExecutionHandler;

        this.disruptor = new Disruptor<>(DisruptorEvent::new,
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
            for (int tryTimes = 1; !isShuttingDown(); tryTimes++) {
                try {
                    long sequence = ringBuffer.tryNext();
                    try {
                        DisruptorEvent event = ringBuffer.get(sequence);
                        event.setTask(task);
                        return;
                    } finally {
                        ringBuffer.publish(sequence);
                        // 确保线程已启动
                        ensureThreadStarted();
                    }
                } catch (InsufficientCapacityException ignore) {
                    // 最多睡眠1毫秒
                    int sleepTimes = (1 + ThreadLocalRandom.current().nextInt(Math.min(100, tryTimes))) * 10000;
                    LockSupport.parkNanos(sleepTimes);
                }
            }
            rejectedExecutionHandler.rejected(task, this);
        }
    }

    // ----------------------------- 生命周期begin ------------------------------

    /**
     * 将运行状态转换为给定目标，或者至少保留给定状态。
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

    /**
     * 确保线程已启动
     */
    private void ensureThreadStarted() {
        int state = stateHolder.get();
        if (state == ST_NOT_STARTED) {
            if (stateHolder.compareAndSet(ST_NOT_STARTED, ST_STARTED)) {
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
        if (!inEventLoop()) {
            // 非真正启动，earlyExit
            return;
        }
        try {
            init();
        } catch (Exception e) {
            ConcurrentUtils.rethrow(e);
        }
    }

    /**
     * 事件循环线程启动时的初始化操作。
     * @apiNote 抛出任何异常都将导致线程终止
     */
    protected void init() throws Exception{

    }

    /**
     * 执行一次循环
     */
    protected void loopOnce() {

    }

    /**
     * 线程退出前的清理动作
     */
    protected void clean() {

    }

    private void onShutdown() {
        if (!inEventLoop()) {
            // 非真正退出，earlyExit
            return;
        }
        // 告知其它线程已开始关闭 - 不要再提交任务
        advanceRunState(ST_SHUTTING_DOWN);

        // 退出前进行必要的清理，释放系统资源
        try {
            clean();
        } catch (Exception e) {
            logger.error("thread clean caught exception!", e);
        } finally {
            stateHolder.set(ST_TERMINATED);
            terminationFuture.setSuccess(null);
        }
    }

    private void onEvent(DisruptorEvent event, boolean endOfBatch) throws Exception {
        try {
            safeExecute(event.getTask());
        } finally {
            event.close();
            if (endOfBatch) {
                // 没执行一批事件，执行一次循环
                loopOnce();
            }
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
    private static class InnerEventHandler implements EventHandler<DisruptorEvent>, LifecycleAware {

        private final DisruptorEventLoop disruptorEventLoop;

        private InnerEventHandler(DisruptorEventLoop disruptorEventLoop) {
            this.disruptorEventLoop = disruptorEventLoop;
        }

        @Override
        public void onStart() {
            disruptorEventLoop.onStart();
        }

        @Override
        public void onEvent(DisruptorEvent event, long sequence, boolean endOfBatch) throws Exception {
            disruptorEventLoop.onEvent(event, endOfBatch);
        }

        @Override
        public void onShutdown() {
            disruptorEventLoop.onShutdown();
        }
    }

}
