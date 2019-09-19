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

/**
 * 抽象的固定频率的TimerHandle实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/14
 * github - https://github.com/hl845740757
 */
class FixRateHandleImp extends AbstractTimerHandle implements FixedRateHandle {

    /**
     * 第一次执行的延迟
     */
    private final long initialDelay;
    /**
     * 循环时的周期
     */
    private long period;
    /**
     * 上次执行时间
     */
    private long lastExecuteTimeMs;

    FixRateHandleImp(DefaultTimerSystem timerSystem, TimerTask timerTask,
                     long initialDelay, long period) {
        super(timerSystem, timerTask);
        this.initialDelay = initialDelay;
        this.period = period;
    }

    @Override
    public long initialDelay() {
        return initialDelay;
    }

    @Override
    public long period() {
        return period;
    }

    @Override
    public final boolean setPeriodLazy(long period) {
        ensurePeriod(period);
        if (isTerminated()) {
            return false;
        } else {
            this.period = period;
            return true;
        }
    }

    @Override
    public boolean setPeriod(long period) {
        if (setPeriodLazy(period)) {
            timerSystem().adjust(this);
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected final void init() {
        setNextExecuteTimeMs(getCreateTimeMs() + initialDelay);
    }

    @Override
    protected final void afterExecuteOnce(long curTimeMs) {
        // 上次执行时间非真实时间
        lastExecuteTimeMs = getNextExecuteTimeMs();
        // 下次执行时间为上次执行时间 + 周期
        setNextExecuteTimeMs(lastExecuteTimeMs + period);
    }

    protected void adjustNextExecuteTime() {
        if (lastExecuteTimeMs > 0) {
            setNextExecuteTimeMs(lastExecuteTimeMs + period);
        } else {
            setNextExecuteTimeMs(getCreateTimeMs() + initialDelay);
        }
    }

    static void ensurePeriod(long period) {
        if (period <= 0) {
            throw new IllegalArgumentException("period " + period);
        }
    }
}
