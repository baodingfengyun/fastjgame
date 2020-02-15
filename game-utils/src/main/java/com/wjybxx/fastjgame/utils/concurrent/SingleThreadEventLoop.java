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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * 事件循环的模板实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14
 * github - https://github.com/hl845740757
 */
public abstract class SingleThreadEventLoop extends AbstractEventLoop {

    private static final Logger logger = LoggerFactory.getLogger(SingleThreadEventLoop.class);
    /**
     * 缓存队列的大小，不宜过大，但也不能过小。
     * 过大容易造成内存浪费，过小对于性能无太大意义。
     */
    private static final int CACHE_QUEUE_CAPACITY = 256;

    /**
     * 用于友好的唤醒当前线程的任务
     */
    protected static final Runnable WAKE_UP_TASK = () -> {
    };

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
     * 任务队列
     */
    private final BlockingQueue<Runnable> taskQueue;
    /**
     * 缓存队列，用于批量的将{@link #taskQueue}中的任务拉取到本地线程下，减少锁竞争，
     */
    private final ArrayDeque<Runnable> cacheQueue = new ArrayDeque<>(CACHE_QUEUE_CAPACITY);
    /**
     * 任务被拒绝时的处理策略
     */
    private final RejectedExecutionHandler rejectedExecutionHandler;

    /**
     * 线程终止future
     */
    private final Promise<?> terminationFuture = new DefaultPromise<>(GlobalEventLoop.INSTANCE);

    /**
     * @param parent                   EventLoop所属的容器，nullable
     * @param threadFactory            线程工厂，创建的线程不要直接启动，建议调用
     *                                 {@link Thread#setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler)}设置异常处理器
     * @param rejectedExecutionHandler 拒绝任务的策略
     */
    protected SingleThreadEventLoop(@Nullable EventLoopGroup parent,
                                    @Nonnull ThreadFactory threadFactory,
                                    @Nonnull RejectedExecutionHandler rejectedExecutionHandler) {
        super(parent);
        this.thread = Objects.requireNonNull(threadFactory.newThread(new Worker()), "newThread");
        // 记录异常退出日志
        UncaughtExceptionHandlers.logIfAbsent(thread, logger);

        this.rejectedExecutionHandler = rejectedExecutionHandler;
        this.taskQueue = newTaskQueue();
    }

    /**
     * 我自己测试了好多遍，{@link LinkedBlockingQueue} 和 {@link ConcurrentLinkedQueue}在
     * EventLoop架构下基本没啥差别。
     * (多生产者单消费者)
     *
     * @return queue
     */
    protected BlockingQueue<Runnable> newTaskQueue() {
        return new LinkedBlockingQueue<>();
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
        return terminationFuture.await(timeout, unit);
    }

    @Override
    public final ListenableFuture<?> terminationFuture() {
        return terminationFuture;
    }

    @Override
    public final void shutdown() {
        for (; ; ) {
            // 为何要存为临时变量？表示我们是基于特定的状态执行代码，compareAndSet才有意义
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
        } else if (oldState == ST_STARTED) {
            if (!inEventLoop()) {
                // 不确定是活跃状态
                wakeUp();
            }
            // else 当前是活跃状态(自己调用，当然是活跃状态)
        }
        // else 其它状态下不应该阻塞，不需要唤醒
    }

    /**
     * 用于其它线程友好的唤醒EventLoop线程，默认实现是向taskQueue中填充一个任务。
     * 如果填充任务不能唤醒线程，则子类需要复写该方法
     * <p>
     * Q: 为什么默认实现是向taskQueue中插入一个任务，而不是中断线程{@link #interruptThread()} ?
     * A: 我先不说这里能不能唤醒线程这个问题。
     * 中断最致命的一点是：向目标线程发出中断请求以后，你并不知道目标线程接收到中断信号的时候正在做什么！！！
     * 因此它并不是一种唤醒/停止目标线程的最佳方式，它可能导致一些需要原子执行的操作失败，也可能导致其它的问题。
     * 因此最好是对症下药，默认实现里认为线程可能阻塞在taskQueue上，因此我们尝试压入一个任务以尝试唤醒它。
     */
    protected void wakeUp() {
        assert !inEventLoop();
        taskQueue.offer(WAKE_UP_TASK);
    }

    /**
     * 中断当前运行的线程，用于唤醒线程。
     * 如果子类阻塞在taskQueue之外的的地方，但是可以通过中断唤醒线程时，那么可以选择中断。
     * <p>
     * Interrupt the current running {@link Thread}.
     */
    protected final void interruptThread() {
        assert !inEventLoop();
        thread.interrupt();
    }

    @Override
    public final void execute(@Nonnull Runnable task) {
        if (addTask(task) && !inEventLoop()) {
            // 其它线程添加任务成功后，需要确保executor已启动，自己添加任务的话，自然已经启动过了
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

    protected final void reject(@Nonnull Runnable task) {
        rejectedExecutionHandler.rejected(task, this);
    }

    /**
     * 确保线程已启动。
     * <p>
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
     * 阻塞式地从阻塞队列中获取一个任务。
     *
     * @return 如果executor被唤醒或被中断，则返回null
     */
    @Nullable
    protected final Runnable takeTask() {
        assert inEventLoop();
        try {
            return taskQueue.take();
        } catch (InterruptedException ignore) {
            // 被中断唤醒 wake up
            return null;
        }
    }

    /**
     * 阻塞式地从阻塞队列中获取一个任务。
     *
     * @param timeoutMs 超时时间 - 毫秒
     * @return 如果executor被唤醒或被中断或实时间到，则返回null
     */
    @Nullable
    protected final Runnable takeTask(long timeoutMs) {
        assert inEventLoop();
        try {
            return taskQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignore) {
            // 被中断唤醒 wake up
            return null;
        }
    }

    /**
     * 从任务队列中尝试获取一个有效任务
     *
     * @return 如果没有可执行任务则返回null
     * @see Queue#poll()
     */
    @Nullable
    protected final Runnable pollTask() {
        assert inEventLoop();
        return taskQueue.poll();
    }

    /**
     * @see Queue#isEmpty()
     */
    protected final boolean hasTasks() {
        assert inEventLoop();
        return !taskQueue.isEmpty();
    }

    /**
     * 运行任务队列中当前所有的任务。
     *
     * @return 至少有一个任务执行时返回true。
     */
    protected final boolean runAllTasks() {
        return runTasksBatch(-1);
    }

    /**
     * 尝试批量运行任务队列中的任务。
     *
     * @param taskBatchSize 执行的最大任务数，小于等于0表示不限制，设定限制数可避免执行任务耗费太多时间。
     * @return 至少有一个任务执行时返回true。
     */
    protected final boolean runTasksBatch(final int taskBatchSize) {
        // 不能出现负数
        long runTaskNum = 0;
        while (!isShutdown()) {
            final int maxDrainTasks = taskBatchSize <= 0 ? CACHE_QUEUE_CAPACITY : (int) (taskBatchSize - runTaskNum);
            if (maxDrainTasks <= 0) {
                break;
            }

            // drainTo - 批量拉取可执行任务(可减少竞争)
            final int size = taskQueue.drainTo(cacheQueue, maxDrainTasks);
            if (size <= 0) {
                break;
            }

            for (int index = 0; index < size; index++) {
                safeExecute(cacheQueue.pollFirst());
            }

            runTaskNum += size;
        }
        return runTaskNum > 0;
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
     * 子类自己决定如何实现事件循环。
     *
     * @apiNote 子类实现应该是一个死循环方法，并在适当的时候调用{@link #confirmShutdown()}确认是否需要退出循环。
     * 子类可以有更多的判断，但是至少需要调用{@link #confirmShutdown()}确定是否需要退出。
     * <p>
     * 警告：如果子类未捕获异常则会导致线程退出。
     */
    protected abstract void loop();

    /**
     * 确认是否需要立即退出事件循环，即是否可以立即退出{@link #loop()}方法。
     * <p>
     * Confirm that the shutdown if the instance should be done now!
     *
     * @apiNote 如果返回true，应该立即退出。
     */
    protected final boolean confirmShutdown() {
        // 用于EventLoop确认自己是否应该退出，不应该由外部线程调用
        assert inEventLoop();
        // 它放前面是因为更可能出现
        if (!isShuttingDown()) {
            return false;
        }
        // 它只在关闭阶段出现
        if (isShutdown()) {
            taskQueue.clear();
            return true;
        }
        // shuttingDown状态下，已不会接收新的任务，执行完当前所有未执行的任务就可以退出了。
        runAllTasks();

        // 切换至SHUTDOWN状态，准备执行最后的清理动作
        advanceRunState(ST_SHUTDOWN);

        // 由于shutdownNow，可能任务没有完全执行完毕，需要进行清理
        taskQueue.clear();
        return true;
    }

    /**
     * 在退出事件循环之前的清理动作。
     */
    protected void clean() throws Exception {

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
                    // 非正常退出下也尝试执行完所有的任务 - 当然这也不是很安全
                    // Run all remaining tasks and shutdown hooks.
                    for (; ; ) {
                        if (confirmShutdown()) {
                            break;
                        }
                    }
                } finally {
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
    }
}
