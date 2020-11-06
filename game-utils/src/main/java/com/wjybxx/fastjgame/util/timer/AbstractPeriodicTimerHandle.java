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

/**
 * 周期性执行的timer的模板实现
 *
 * @author wjybxx
 * date - 2019/8/14
 * github - https://github.com/hl845740757
 */
abstract class AbstractPeriodicTimerHandle extends AbstractTimerHandle implements PeriodicTimerHandle {

    /**
     * 初始延迟
     */
    private final long initialDelay;
    /**
     * 循环时的延迟
     */
    private long period;
    /**
     * 已执行次数
     */
    private int executedTimes;

    /**
     * 上次执行时间
     */
    long lastExecuteTimeMs;

    AbstractPeriodicTimerHandle(DefaultTimerSystem timerSystem, TimerTask timerTask,
                                long initialDelay, long period) {
        super(timerSystem, timerTask);
        this.initialDelay = initialDelay;
        this.period = ensurePeriodGreaterThanZero(period);
    }

    static long ensurePeriodGreaterThanZero(long period) {
        if (period <= 0) {
            throw new IllegalArgumentException("period must be greater than 0");
        }
        return period;
    }

    @Override
    public final long initialDelay() {
        return initialDelay;
    }

    @Override
    public final long period() {
        return period;
    }

    @Override
    public final boolean setPeriod(long period) {
        if (setPeriodLazy(period)) {
            timerSystem().adjust(this);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public final boolean setPeriodLazy(long period) {
        if (isClosed()) {
            return false;
        } else {
            this.period = ensurePeriodGreaterThanZero(period);
            return true;
        }
    }

    @Override
    public final int executedTimes() {
        return executedTimes;
    }

    @Override
    protected final void afterExecuteOnce(long curTimeMs) {
        executedTimes++;
        afterExecuteOnceHook(curTimeMs);
    }

    /**
     * 任务执行一次之后，更新状态下次执行时间。
     * 实现类应该直接修改{@link #lastExecuteTimeMs}和调用{@link #setNextExecuteTimeMs(long)}修改下次执行时间。
     *
     * @param curTimeMs timerSystem当前tick的时间戳
     */
    protected abstract void afterExecuteOnceHook(long curTimeMs);

    /**
     * 实现类应该根据{@link #lastExecuteTimeMs}和{@link #period()} {@link #initialDelay()}计算下次执行时间
     */
    @Override
    protected abstract void adjustNextExecuteTime();

    @Override
    public String toString() {
        return "AbstractPeriodicTimerHandle{" +
                "initialDelay=" + initialDelay +
                ", period=" + period +
                ", executedTimes=" + executedTimes +
                ", lastExecuteTimeMs=" + lastExecuteTimeMs +
                "} ";
    }
}