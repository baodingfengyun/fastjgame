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

/**
 * 固定时间间隔的timer对应的handle。
 * {@link TimerSystem#newFixedDelay(long, long, TimerTask)}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/14
 * github - https://github.com/hl845740757
 */
class FixedDelayHandleImp extends AbstractTimerHandle implements FixedDelayHandle {

    /**
     * 初始延迟
     */
    private final long initialDelay;
    /**
     * 循环时的延迟
     */
    private long delay;

    /**
     * 上次执行时间
     */
    private long lastExecuteTimeMs;

    FixedDelayHandleImp(DefaultTimerSystem timerSystem, TimerTask timerTask,
                        long initialDelay, long delay) {
        super(timerSystem, timerTask);
        this.initialDelay = initialDelay;
        this.delay = delay;
    }

    @Override
    public long initialDelay() {
        return initialDelay;
    }

    @Override
    public long delay() {
        return delay;
    }

    @Override
    public boolean setDelay(long delay) {
        if (setDelayLazy(delay)) {
            timerSystem().adjust(this);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public final boolean setDelayLazy(long delay) {
        ensureDelay(delay);
        if (isClosed()) {
            return false;
        } else {
            this.delay = delay;
            return true;
        }
    }

    @Override
    protected final void init() {
        setNextExecuteTimeMs(getCreateTimeMs() + initialDelay);
    }

    @Override
    protected final void afterExecuteOnce(long curTimeMs) {
        // 上次执行时间为真实时间
        lastExecuteTimeMs = curTimeMs;
        // 下次执行时间为上次执行时间 + 延迟
        setNextExecuteTimeMs(lastExecuteTimeMs + delay);
    }

    /**
     * 更新下一次的执行时间
     */
    protected void adjustNextExecuteTime() {
        if (lastExecuteTimeMs > 0) {
            setNextExecuteTimeMs(lastExecuteTimeMs + delay);
        } else {
            setNextExecuteTimeMs(getCreateTimeMs() + initialDelay);
        }
    }

    static void ensureDelay(long delay) {
        if (delay <= 0) {
            throw new IllegalArgumentException("delay " + delay);
        }
    }

    @Override
    public String toString() {
        return "FixedDelayHandleImp{" +
                "initialDelay=" + initialDelay +
                ", delay=" + delay +
                ", lastExecuteTimeMs=" + lastExecuteTimeMs +
                '}';
    }
}