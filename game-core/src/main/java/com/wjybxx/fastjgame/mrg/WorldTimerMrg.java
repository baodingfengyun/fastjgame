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

package com.wjybxx.fastjgame.mrg;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.annotation.WorldSingleton;
import com.wjybxx.fastjgame.trigger.*;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * World级别的全局定时器管理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 22:13
 * github - https://github.com/hl845740757
 */
@WorldSingleton
@NotThreadSafe
public class WorldTimerMrg implements TimerSystem {

    private final TimerSystem timerSystem;

    @Inject
    public WorldTimerMrg(WorldTimeMrg worldTimeMrg) {
        timerSystem = new DefaultTimerSystem(worldTimeMrg);
    }

    @Override
    public void tick() {
        timerSystem.tick();
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public void close() {
        timerSystem.close();
    }

    @Override
    @Nonnull
    public TimeoutHandle newTimeout(long timeout, @Nonnull TimerTask<TimeoutHandle> task) {
        return timerSystem.newTimeout(timeout, task);
    }

    @Override
    @Nonnull
    public FixedDelayHandle newFixedDelay(long initialDelay, long delay, @Nonnull TimerTask<FixedDelayHandle> timerTask) {
        return timerSystem.newFixedDelay(initialDelay, delay, timerTask);
    }

    @Override
    @Nonnull
    public FixedDelayHandle newFixedDelay(long delay, @Nonnull TimerTask<FixedDelayHandle> timerTask) {
        return timerSystem.newFixedDelay(delay, timerTask);
    }

    @Override
    @Nonnull
    public FixedRateHandle newFixRate(long initialDelay, long period, @Nonnull TimerTask<FixedRateHandle> timerTask) {
        return timerSystem.newFixRate(initialDelay, period, timerTask);
    }

    @Override
    @Nonnull
    public FixedRateHandle newFixRate(long period, @Nonnull TimerTask<FixedRateHandle> timerTask) {
        return timerSystem.newFixRate(period, timerTask);
    }

}
