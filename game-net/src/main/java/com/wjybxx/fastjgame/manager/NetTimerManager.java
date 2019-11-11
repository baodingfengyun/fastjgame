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

package com.wjybxx.fastjgame.manager;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.timer.*;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * 网络层全局定时器管理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 22:13
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class NetTimerManager {

    private final TimerSystem timerSystem;

    @Inject
    public NetTimerManager(NetTimeManager netTimeManager) {
        timerSystem = new DefaultTimerSystem(netTimeManager, 1023);
    }

    public void tick() {
        timerSystem.tick();
    }

    public void close() {
        timerSystem.close();
    }

    @Nonnull
    public TimeoutHandle newTimeout(long timeout, @Nonnull TimerTask task) {
        return timerSystem.newTimeout(timeout, task);
    }

    @Nonnull
    public TimeoutHandle nextTick(@Nonnull TimerTask task) {
        return timerSystem.nextTick(task);
    }

    @Nonnull
    public FixedDelayHandle newFixedDelay(long initialDelay, long delay, @Nonnull TimerTask task) {
        return timerSystem.newFixedDelay(initialDelay, delay, task);
    }

    @Nonnull
    public FixedDelayHandle newFixedDelay(long delay, @Nonnull TimerTask task) {
        return timerSystem.newFixedDelay(delay, task);
    }

    @Nonnull
    public FixedRateHandle newFixRate(long initialDelay, long period, @Nonnull TimerTask task) {
        return timerSystem.newFixRate(initialDelay, period, task);
    }

    @Nonnull
    public FixedRateHandle newFixRate(long period, @Nonnull TimerTask task) {
        return timerSystem.newFixRate(period, task);
    }

    public long curTimeMillis() {
        return timerSystem.curTimeMillis();
    }

    public int curTimeSeconds() {
        return timerSystem.curTimeSeconds();
    }
}
