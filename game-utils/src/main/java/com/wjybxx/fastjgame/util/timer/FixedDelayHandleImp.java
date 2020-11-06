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
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/14
 * github - https://github.com/hl845740757
 */
class FixedDelayHandleImp extends AbstractPeriodicTimerHandle implements FixedDelayHandle {

    FixedDelayHandleImp(DefaultTimerSystem timerSystem, TimerTask timerTask,
                        long initialDelay, long period) {
        super(timerSystem, timerTask, initialDelay, period);
    }

    @Override
    protected void afterExecuteOnceHook(long curTimeMs) {
        // 上次执行时间为真实时间
        lastExecuteTimeMs = curTimeMs;
        // 下次执行时间为上次执行时间 + 延迟
        setNextExecuteTimeMs(lastExecuteTimeMs + period());
    }

    /**
     * 更新下一次的执行时间
     */
    @Override
    protected final void adjustNextExecuteTime() {
        if (executedTimes() > 0) {
            setNextExecuteTimeMs(lastExecuteTimeMs + period());
        } else {
            setNextExecuteTimeMs(getCreateTimeMs() + initialDelay());
        }
    }

    @Override
    public String toString() {
        return "FixedDelayHandleImp{} " + super.toString();
    }
}