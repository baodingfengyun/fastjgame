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

package com.wjybxx.fastjgame.utils.timer;

import com.wjybxx.fastjgame.utils.ThreadUtils;
import com.wjybxx.fastjgame.utils.exception.InfiniteLoopException;
import com.wjybxx.fastjgame.utils.timeprovider.TimeProvider;
import com.wjybxx.fastjgame.utils.timeprovider.TimeProviders;
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
     * 默认空间大小，使用JDK默认大小
     */
    private static final int DEFAULT_INITIAL_CAPACITY = 11;
    /**
     * 默认的时间提供器（它是线程安全的，不可以使用非线程安全的作为静态变量）
     */
    private static final TimeProvider DEFAULT_TIME_PROVIDER = TimeProviders.realtimeProvider();

    /**
     * timer队列
     */
    private final PriorityQueue<AbstractTimerHandle> timerQueue;
    /**
     * 用于获取当前时间
     */
    private final TimeProvider timeProvider;

    /**
     * 用于分配timerId。
     * 如果是静态的将存在线程安全问题(或使用AtomicLong) - 不想产生不必要的竞争，因此每个timerSystem一个。
     */
    private long timerIdSequencer = 0;
    /**
     * 是否已关闭
     */
    private boolean closed = false;

    /**
     * 正在执行回调的timer
     */
    private AbstractTimerHandle runningTimer = null;
    /**
     * 当前帧的时间戳
     */
    private long curTickTimeMillis;
    /**
     * 当前帧数（int足够）
     */
    private int curTickFrame = 0;

    public DefaultTimerSystem() {
        this(DEFAULT_TIME_PROVIDER, DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * @see DefaultTimerSystem#DefaultTimerSystem(TimeProvider, int)
     */
    public DefaultTimerSystem(int initCapacity) {
        this(DEFAULT_TIME_PROVIDER, initCapacity);
    }

    /**
     * @see DefaultTimerSystem#DefaultTimerSystem(TimeProvider, int)
     */
    public DefaultTimerSystem(TimeProvider timeProvider) {
        this(timeProvider, DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * @param timeProvider 时间提供函数
     * @param initCapacity 初始timer空间，当你能预见timer的空间大小时，指定空间大小能提高性能和空间利用率
     */
    public DefaultTimerSystem(TimeProvider timeProvider, int initCapacity) {
        this.timeProvider = timeProvider;
        timerQueue = new PriorityQueue<>(initCapacity, AbstractTimerHandle.timerComparator);
    }

    @Nonnull
    @Override
    public TimeoutHandle newTimeout(long timeout, @Nonnull TimerTask task) {
        TimeoutHandleImp timeoutHandleImp = new TimeoutHandleImp(this, task, timeout);
        return tryAddTimerAndInit(timeoutHandleImp);
    }

    @Nonnull
    @Override
    public FixedDelayHandle newFixedDelay(long initialDelay, long delay, @Nonnull TimerTask task) {
        FixedDelayHandleImp.ensureDelay(delay);
        FixedDelayHandleImp fixedDelayHandleImp = new FixedDelayHandleImp(this, task, initialDelay, delay);
        return tryAddTimerAndInit(fixedDelayHandleImp);
    }

    @Nonnull
    @Override
    public FixedRateHandle newFixRate(long initialDelay, long period, @Nonnull TimerTask task) {
        FixedRateHandleImp.ensurePeriod(period);
        FixedRateHandleImp fixedRateHandleImp = new FixedRateHandleImp(this, task, initialDelay, period);
        return tryAddTimerAndInit(fixedRateHandleImp);
    }

    /**
     * 将timer压入队列，并进行适当的初始化。
     */
    private <T extends AbstractTimerHandle> T tryAddTimerAndInit(T timerHandle) {
        if (closed) {
            // timer系统已关闭，不压入队列
            timerHandle.setClosed();
        } else {
            // 先初始化，才能获得首次执行时间
            timerHandle.init();
            timerQueue.add(timerHandle);
            checkInterruptTick(timerHandle);
        }
        return timerHandle;
    }

    private <T extends AbstractTimerHandle> void checkInterruptTick(T timerHandle) {
        if (runningTimer == null) {
            return;
        }

        if (curTickTimeMillis < timerHandle.getNextExecuteTimeMs()) {
            return;
        }

        // tick过程中创建了一个要立即执行的timer，那么tick到这个timer的时候，强制中断，进入下一帧的时候继续。
        timerHandle.setNextExecuteFrameThreshold(curTickFrame + 1);
        logger.warn("Added a timer for immediate execution, tick will by interrupted, caller info:\n" + ThreadUtils.getCallerInfo(4));
    }

    @Override
    public void tick() {
        if (closed) {
            return;
        }

        if (runningTimer != null) {
            throw new InfiniteLoopException("may caused by timer call tick, runningTimer " + runningTimer);
        }

        // 由于该timerSystem是基于缓存时间戳tick的，因此需要缓存该时间戳用于别处判断
        final long curTimeMillis = timeProvider.curTimeMillis();

        curTickTimeMillis = curTimeMillis;
        curTickFrame++;

        try {
            tickTimer(curTimeMillis);
        } finally {
            runningTimer = null;
        }
    }

    /**
     * 检查周期性执行的timer
     */
    private void tickTimer(final long curTimeMillis) {
        AbstractTimerHandle timerHandle;
        while ((timerHandle = timerQueue.peek()) != null) {
            // 优先级最高的timer不需要执行，那么后面的也不需要执行
            if (curTimeMillis < timerHandle.getNextExecuteTimeMs()) {
                return;
            }

            // timer对帧数有要求（避免无限循环）
            if (curTickFrame < timerHandle.getNextExecuteFrameThreshold()) {
                return;
            }

            // 先弹出队列，并记录正在执行
            runningTimer = timerQueue.poll();

            do {
                callbackSafely(timerHandle, curTimeMillis);
                // 可能由于延迟导致需要执行多次(可以避免在当前轮反复压入弹出)，也可能在执行回调之后被取消了。
            } while (!timerHandle.isClosed() && curTimeMillis >= timerHandle.getNextExecuteTimeMs());

            if (!timerHandle.isClosed()) {
                // 如果未取消的话，压入队列稍后执行
                timerQueue.offer(timerHandle);
            }
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
        } catch (final Throwable cause) {
            try {
                timerHandle.getExceptionHandler().onExceptionCaught(timerHandle, cause);
            } catch (final Throwable unExpectedException) {
                // 判断rethrow
                if (unExpectedException != cause) {
                    cause.addSuppressed(unExpectedException);
                }
                logger.warn("onExceptionCaught caught exception!", cause);
                // 在处理异常时再出现异常，一定会关闭timer
                timerHandle.setClosed();
            }
        }

        if (!timerHandle.isClosed()) {
            timerHandle.afterExecuteOnce(curTimeMillis);
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
            handle.setClosed();
        }
    }

    /**
     * 删除一个已关闭的timer
     */
    void removeClosedTimer(AbstractTimerHandle timerHandle) {
        if (timerHandle != runningTimer) {
            timerQueue.remove(timerHandle);
        }
    }

    /**
     * 调整handle在timerSystem中的优先级
     * (这不是个频繁的操作，暂时先删除再插入，先不做优化)
     *
     * @param <T>         定时器句柄类型
     * @param timerHandle 定时器句柄
     */
    <T extends AbstractTimerHandle> void adjust(T timerHandle) {
        if (runningTimer == timerHandle) {
            // 正在执行的时候调整间隔
            timerHandle.adjustNextExecuteTime();
        } else {
            // 其它时候调整间隔
            timerQueue.remove(timerHandle);
            timerHandle.adjustNextExecuteTime();
            timerQueue.add(timerHandle);
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