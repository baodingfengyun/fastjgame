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

package com.wjybxx.fastjgame.timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * 定时器系统的默认实现。
 * 注意查看测试用例 {@code TimerSystemTest}的输出结果。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/7
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class DefaultTimerSystem implements TimerSystem {

    private static final Logger logger = LoggerFactory.getLogger(DefaultTimerSystem.class);
    /**
     * 无效的timerId
     */
    private static final int INVALID_TIMER_ID = -1;
    /**
     * 默认空间大小，使用JDK默认大小
     */
    private static final int DEFAULT_INITIAL_CAPACITY = 11;
    /**
     * 默认的时间提供器（它是线程安全的，不可以使用非线程安全的作为静态变量）
     */
    private static final SystemTimeProvider DEFAULT_TIME_PROVIDER = SystemTimeProviders.getRealtimeProvider();

    /**
     * timer队列
     */
    private final PriorityQueue<AbstractTimerHandle> timerQueue;
    /**
     * 用于获取当前时间
     */
    private final SystemTimeProvider timeProvider;
    /**
     * 用于分配timerId。
     * 如果是静态的将存在线程安全问题(或使用AtomicLong - 不想产生不必要的竞争，因此每个timerSystem一个)
     */
    private long timerIdSequencer = 0;
    /**
     * 是否已关闭
     */
    private boolean closed = false;
    /**
     * 正在执行回调的timer
     */
    private long runningTimerId = INVALID_TIMER_ID;

    public DefaultTimerSystem() {
        this(DEFAULT_TIME_PROVIDER, DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * @see DefaultTimerSystem#DefaultTimerSystem(SystemTimeProvider, int)
     */
    public DefaultTimerSystem(int initCapacity) {
        this(DEFAULT_TIME_PROVIDER, initCapacity);
    }

    /**
     * @see DefaultTimerSystem#DefaultTimerSystem(SystemTimeProvider, int)
     */
    public DefaultTimerSystem(SystemTimeProvider timeProvider) {
        this(timeProvider, DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * @param timeProvider 时间提供函数
     * @param initCapacity 初始timer空间，当你能预见timer的空间大小时，指定空间大小能提高性能和空间利用率
     */
    public DefaultTimerSystem(SystemTimeProvider timeProvider, int initCapacity) {
        this.timeProvider = timeProvider;
        timerQueue = new PriorityQueue<>(initCapacity, AbstractTimerHandle.timerComparator);
    }

    @Nonnull
    @Override
    public TimeoutHandle newTimeout(long timeout, @Nonnull TimerTask<TimeoutHandle> task) {
        TimeoutHandleImp timeoutHandleImp = new TimeoutHandleImp(this, task, timeout);
        return tryAddTimerAndInit(timeoutHandleImp);
    }

    @Nonnull
    @Override
    public FixedDelayHandle newFixedDelay(long initialDelay, long delay, @Nonnull TimerTask<FixedDelayHandle> task) {
        FixedDelayHandleImp.ensureDelay(delay);
        FixedDelayHandleImp fixedDelayHandleImp = new FixedDelayHandleImp(this, task, initialDelay, delay);
        return tryAddTimerAndInit(fixedDelayHandleImp);
    }

    @Nonnull
    @Override
    public FixedRateHandle newFixRate(long initialDelay, long period, @Nonnull TimerTask<FixedRateHandle> task) {
        FixRateHandleImp.ensurePeriod(period);
        FixRateHandleImp fixRateHandleImp = new FixRateHandleImp(this, task, initialDelay, period);
        return tryAddTimerAndInit(fixRateHandleImp);
    }

    /**
     * 将timer压入队列，并进行适当的初始化。
     */
    private <T extends AbstractTimerHandle> T tryAddTimerAndInit(T timerHandle) {
        if (closed) {
            // timer系统已关闭，不压入队列
            timerHandle.setTerminated();
        } else {
            // 先初始化，才能获得首次执行时间
            timerHandle.init();
            timerQueue.add(timerHandle);
        }
        return timerHandle;
    }

    @Override
    public void tick() {
        if (closed) {
            return;
        }
        tickTimer();
    }

    /**
     * 检查周期性执行的timer
     */
    private void tickTimer() {
        AbstractTimerHandle timerHandle;
        final long curTimeMillis = timeProvider.curTimeMillis();

        while ((timerHandle = timerQueue.peek()) != null) {
            // 优先级最高的timer不需要执行，那么后面的也不需要执行
            if (curTimeMillis < timerHandle.getNextExecuteTimeMs()) {
                return;
            }
            // 先弹出队列，并记录正在执行
            runningTimerId = timerQueue.poll().getTimerId();

            do {
                callbackSafely(timerHandle, curTimeMillis);
                // 可能由于延迟导致需要执行多次(可以避免在当前轮反复压入弹出)，也可能在执行回调之后被取消了。do while用的不甚习惯...
            } while (!timerHandle.isTerminated() && curTimeMillis >= timerHandle.getNextExecuteTimeMs());

            // 出现异常的timer会被取消，不再压入队列

            if (!timerHandle.isTerminated()) {
                // 如果未取消的话，压入队列稍后执行
                timerQueue.offer(timerHandle);
            }
            // 清除标记
            runningTimerId = INVALID_TIMER_ID;
        }
    }

    /**
     * 安全的执行timer的回调。
     *
     * @param timerHandle   timer
     * @param curTimeMillis 当前时间戳
     */
    private static void callbackSafely(AbstractTimerHandle timerHandle, long curTimeMillis) {
        try {
            timerHandle.run();
            timerHandle.afterExecuteOnce(curTimeMillis);
        } catch (Throwable e) {
            // 取消执行
            timerHandle.setTerminated();
            logger.warn("timer callback caught exception!", e);
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        closeQueue(timerQueue);
    }

    /**
     * 关闭一个队列的全部timer
     *
     * @param queue timer所在的队列
     */
    private static void closeQueue(Queue<AbstractTimerHandle> queue) {
        AbstractTimerHandle handle;
        while ((handle = queue.poll()) != null) {
            handle.setTerminated();
        }
    }

    /**
     * 删除一个timer
     */
    void remove(AbstractTimerHandle timerHandle) {
        // 此时已调用 isTerminated() 为 true， 因此不必再次赋值
        if (timerHandle.getTimerId() != runningTimerId) {
            timerQueue.remove(timerHandle);
        }
    }

    /**
     * 调整handle在timerSystem中的优先级(暂时先删除再插入，如果切换底层数据结构，那么可能会修改)
     *
     * @param <T>    定时器句柄类型
     * @param handle 定时器句柄
     */
    <T extends AbstractTimerHandle> void adjust(T handle) {
        if (runningTimerId == handle.getTimerId()) {
            // 正在执行的时候调整间隔
            handle.adjustNextExecuteTime();
        } else {
            // 其它时候调整间隔
            timerQueue.remove(handle);
            handle.adjustNextExecuteTime();
            timerQueue.add(handle);
        }
    }

    long nextTimerId() {
        return ++timerIdSequencer;
    }

    @Override
    public long curTimeMillis() {
        return timeProvider.curTimeMillis();
    }

    @Override
    public int curTimeSeconds() {
        return timeProvider.curTimeSeconds();
    }
}