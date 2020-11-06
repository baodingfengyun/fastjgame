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

package com.wjybxx.fastjgame.util.timer;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/14
 * github - https://github.com/hl845740757
 */
class FixedRateHandleImp extends AbstractPeriodicTimerHandle implements FixedRateHandle {

    FixedRateHandleImp(DefaultTimerSystem timerSystem, TimerTask timerTask,
                       long initialDelay, long period) {
        super(timerSystem, timerTask, initialDelay, period);
    }

    @Override
    protected void afterExecuteOnceHook(long curTimeMs) {
        // 上次执行时间为逻辑时间
        lastExecuteTimeMs = getNextExecuteTimeMs();
        // 下次执行时间为上次执行时间 + 周期
        setNextExecuteTimeMs(lastExecuteTimeMs + period());
    }

    protected void adjustNextExecuteTime() {
        if (lastExecuteTimeMs > 0) {
            setNextExecuteTimeMs(lastExecuteTimeMs + period());
        } else {
            setNextExecuteTimeMs(getCreateTimeMs() + initialDelay());
        }
    }

    @Override
    public String toString() {
        return "FixedRateHandleImp{} " + super.toString();
    }
}
