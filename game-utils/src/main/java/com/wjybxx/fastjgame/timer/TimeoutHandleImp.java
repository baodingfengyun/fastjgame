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
 * 只执行一次的Timer的handle。
 * {@link TimerSystem#newTimeout(long, TimerTask)}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/14
 * github - https://github.com/hl845740757
 */
class TimeoutHandleImp extends AbstractTimerHandle implements TimeoutHandle {

    private long timeout;

    TimeoutHandleImp(DefaultTimerSystem timerSystem, TimerTask timerTask,
                     long timeout) {
        super(timerSystem, timerTask);
        this.timeout = timeout;
    }

    @Override
    public long timeout() {
        return timeout;
    }

    @Override
    public boolean setTimeout(long timeout) {
        if (isTerminated()) {
            return false;
        }
        this.timeout = timeout;
        timerSystem().adjust(this);
        return true;
    }

    @Override
    protected final void init() {
        setNextExecuteTimeMs(getCreateTimeMs() + timeout);
    }

    @Override
    protected final void afterExecuteOnce(long curTimeMs) {
        // 执行一次之后就结束了。
        setTerminated();
    }

    protected void adjustNextExecuteTime() {
        setNextExecuteTimeMs(getCreateTimeMs() + timeout);
    }

}
