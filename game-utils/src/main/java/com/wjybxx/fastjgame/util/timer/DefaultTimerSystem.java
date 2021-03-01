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

package com.wjybxx.fastjgame.util.timer;

import com.wjybxx.fastjgame.util.ThreadUtils;
import com.wjybxx.fastjgame.util.misc.InfiniteLoopException;
import com.wjybxx.fastjgame.util.time.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Objects;
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
     * 用于获取当前时间
     */
    private final TimeProvider timeProvider;
    /**
     * timer队列
     */
    private PriorityQueue<AbstractTimerHandle> timerQueue;

    /**
     * 用于分配timerId。
     * 如果是静态的将存在线程安全问题(或使用AtomicLong) - 不想产生不必要的竞争，因此每个timerSystem一个。
     */
    private long timerIdSequencer = 0;

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
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider");
        timerQueue = new PriorityQueue<>(initCapacity, AbstractTimerHandle.timerComparator);
    }

    @Nonnull
    @Override
    public TimeoutHandle newTimeout(long timeout, @Nonnull TimerTask task) {
        final TimeoutHandleImp timeoutHandleImp = new TimeoutHandleImp(this, task, timeout);
        return tryAddTimerAndInit(timeoutHandleImp);
    }

    @Nonnull
    @Override
    public FixedDelayHandle newFixedDelay(long initialDelay, long delay, @Nonnull TimerTask task) {
        AbstractPeriodicTimerHandle.ensurePeriodGreaterThanZero(delay);
        final FixedDelayHandleImp fixedDelayHandleImp = new FixedDelayHandleImp(this, task, initialDelay, delay);
        return tryAddTimerAndInit(fixedDelayHandleImp);
    }

    @Nonnull
    @Override
    public FixedRateHandle newFixedRate(long initialDelay, long period, @Nonnull TimerTask task) {
        AbstractPeriodicTimerHandle.ensurePeriodGreaterThanZero(period);
        final FixedRateHandleImp fixedRateHandleImp = new FixedRateHandleImp(this, task, initialDelay, period);
        return tryAddTimerAndInit(fixedRateHandleImp);
    }

    /**
     * 将timer压入队列，并进行适当的初始化。
     */
    private <T extends AbstractTimerHandle> T tryAddTimerAndInit(T timerHandle) {
        if (isClosed()) {
            timerHandle.closeWithoutRemove();
            throw new IllegalStateException("closed");
        } else {
            timerHandle.adjustNextExecuteTime();
            timerQueue.add(timerHandle);
            checkInterruptTick(timerHandle);
            return timerHandle;
        }
    }

    private <T extends AbstractTimerHandle> void checkInterruptTick(T timerHandle) {
        if (runningTimer == null) {
            return;
        }

        if (timerHandle.getNextExecuteTimeMs() > curTickTimeMillis) {
            return;
        }

        // tick过程中创建了一个要立即执行的timer，那么tick到这个timer的时候，强制中断，进入下一帧的时候继续。
        // 如果插在既有要执行的timer的后面，那么理论上是没有破坏性的，因为它并没有影响当前帧要执行的timer
        // 而尝试插在既有要执行的timer的前面，则是个危险的操作，因为它影响了当前帧要执行的timer
        timerHandle.setNextExecuteFrameThreshold(curTickFrame + 1);

        if (timerHandle.getNextExecuteTimeMs() < curTickTimeMillis) {
            // 尝试插到既有要执行的timer的前面，记录调用方信息
            logger.error("Added a timer for immediate execution, tick will be interrupted, caller info:\n" +
                    ThreadUtils.getCallerInfo(DefaultTimerSystem::isOtherClass));
        }
    }

    private static boolean isOtherClass(StackWalker.StackFrame stackFrame) {
        // 需要处理超类或子类调用(如nextTick调用newTimeout)
        return !TimerSystem.class.isAssignableFrom(stackFrame.getDeclaringClass());
    }

    @Override
    public void tick() {
        if (isClosed()) {
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
            tickTimer(curTimeMillis, curTickFrame);
        } finally {
            runningTimer = null;
        }
    }

    /**
     * 检查周期性执行的timer
     */
    private void tickTimer(final long curTimeMillis, final int curFrame) {
        PriorityQueue<AbstractTimerHandle> timerQueue;
        AbstractTimerHandle timerHandle;

        while ((timerQueue = this.timerQueue) != null && (timerHandle = timerQueue.peek()) != null) {
            // 优先级最高的timer不需要执行，那么后面的也不需要执行
            if (curTimeMillis < timerHandle.getNextExecuteTimeMs()) {
                return;
            }

            // timer对帧数有要求（避免无限循环）
            if (curFrame < timerHandle.getNextExecuteFrameThreshold()) {
                return;
            }

            // 先弹出队列，并记录正在执行
            runningTimer = timerQueue.poll();

            // 执行一次回调
            callbackSafely(timerHandle, curTimeMillis);

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
        timerHandle.beforeExecuteOnce();
        try {
            timerHandle.run();
        } catch (Exception e) {
            if (timerHandle.isAutoCloseOnExceptionCaught()) {
                // 出现异常时关闭timer
                timerHandle.closeWithoutRemove();
            }
            logger.warn("timerHandle.run caught exception!", e);
        }

        if (!timerHandle.isClosed()) {
            timerHandle.afterExecuteOnce(curTimeMillis);
        }
    }

    @Override
    public boolean isClosed() {
        return timerQueue == null;
    }

    @Override
    public void close() {
        if (timerQueue == null) {
            return;
        }

        // 某个回调尝试关闭整个timer系统，不是个好的实践
        if (runningTimer != null) {
            runningTimer.closeWithoutRemove();
        }

        closeQueue(timerQueue);
        timerQueue = null;
    }

    /**
     * 关闭一个队列的全部timer
     *
     * @param queue timer所在的队列
     */
    private static void closeQueue(Queue<AbstractTimerHandle> queue) {
        AbstractTimerHandle handle;
        while ((handle = queue.poll()) != null) {
            handle.closeWithoutRemove();
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
            // 其它时候调整间隔(必须先删除才可以修改优先级)
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