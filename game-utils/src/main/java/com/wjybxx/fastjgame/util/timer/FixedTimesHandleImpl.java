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
 * date - 2020/11/6
 * github - https://github.com/hl845740757
 */
class FixedTimesHandleImpl extends FixedDelayHandleImp implements FixedTimesHandle {

    private int remainTimes;

    FixedTimesHandleImpl(DefaultTimerSystem timerSystem, TimerTask timerTask,
                         long initialDelay, long period,
                         int remainTimes) {
        super(timerSystem, timerTask, initialDelay, period);
        this.remainTimes = ensureRemainTimesGreaterThanZero(remainTimes);
    }

    static int ensureRemainTimesGreaterThanZero(int remainTimes) {
        if (remainTimes <= 0) {
            throw new IllegalArgumentException("remainTimes must be greater than 0");
        }
        return remainTimes;
    }

    @Override
    public int remainTimes() {
        return remainTimes;
    }

    @Override
    public boolean setRemainTimes(int remainTimes) {
        if (isClosed()) {
            return false;
        }
        this.remainTimes = ensureRemainTimesGreaterThanZero(remainTimes);
        return true;
    }

    @Override
    protected void afterExecuteOnceHook(long curTimeMs) {
        super.afterExecuteOnceHook(curTimeMs);
        remainTimes--;
        if (remainTimes <= 0) {
            // 这里是在执行的时候回调，timer当前不在队列中
            closeWithoutRemove();
        }
    }

    @Override
    public String toString() {
        return "FixedTimesHandleImpl{" +
                "remainTimes=" + remainTimes +
                "} " + super.toString();
    }
}