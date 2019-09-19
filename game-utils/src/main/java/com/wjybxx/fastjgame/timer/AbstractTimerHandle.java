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

import javax.annotation.Nullable;
import java.util.Comparator;

/**
 * 抽象的TimerHandle实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/14
 * github - https://github.com/hl845740757
 */
abstract class AbstractTimerHandle implements TimerHandle {

    /**
     * 执行时间越小越靠前，执行时间相同的，timerId越小越靠前(越先添加timerId越小)
     */
    static final Comparator<AbstractTimerHandle> timerComparator = Comparator.comparingLong(AbstractTimerHandle::getNextExecuteTimeMs)
            .thenComparingLong(AbstractTimerHandle::getTimerId);

    /**
     * 绑定的timer系统
     */
    private final DefaultTimerSystem timerSystem;
    /**
     * 该handle关联的timerTask
     */
    private final TimerTask timerTask;
    /**
     * 定时器id，先添加的必定更小...
     */
    private final long timerId;
    /**
     * timer的创建时间
     */
    private final long createTimeMs;

    /**
     * 上下文/附加属性
     */
    private Object attachment;

    /**
     * 下次的执行时间。
     * 该属性是为了避免堆结构被破坏。
     */
    private long nextExecuteTimeMs;
    /**
     * 是否已终止
     */
    private boolean terminated = false;

    AbstractTimerHandle(DefaultTimerSystem timerSystem, TimerTask timerTask) {
        this.timerSystem = timerSystem;
        this.timerTask = timerTask;
        this.timerId = timerSystem.nextTimerId();
        this.createTimeMs = timerSystem.getSystemMillTime();
    }

    @Override
    public final <T> T attach(@Nullable Object newData) {
        @SuppressWarnings("unchecked")
        T pre = (T) attachment;
        this.attachment = newData;
        return pre;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public final <T> T attachment() {
        return (T) attachment;
    }

    @SuppressWarnings("unchecked")
    public void run() throws Exception {
        timerTask.run(this);
    }

    @Override
    public long executeDelay() {
        if (terminated) {
            return -1;
        }
        return Math.max(0, nextExecuteTimeMs - timerSystem.getSystemMillTime());
    }

    @Override
    public final void cancel() {
        if (!terminated) {
            terminated = true;
            timerSystem.remove(this);
        }
    }

    @Override
    public boolean isTerminated() {
        return terminated;
    }

    DefaultTimerSystem timerSystem() {
        return timerSystem;
    }

    long getCreateTimeMs() {
        return createTimeMs;
    }

    final long getTimerId() {
        return timerId;
    }

    final void setNextExecuteTimeMs(long nextExecuteTimeMs) {
        this.nextExecuteTimeMs = nextExecuteTimeMs;
    }

    final long getNextExecuteTimeMs() {
        return nextExecuteTimeMs;
    }

    final void setTerminated() {
        this.terminated = true;
    }

    /**
     * timer创建时进行初始化。
     */
    protected abstract void init();

    /**
     * 任务执行一次之后，更新状态
     *
     * @param curTimeMs 当前系统时间
     */
    protected abstract void afterExecuteOnce(long curTimeMs);

    /**
     * 调整下一次的执行时间
     */
    protected abstract void adjustNextExecuteTime();
}
