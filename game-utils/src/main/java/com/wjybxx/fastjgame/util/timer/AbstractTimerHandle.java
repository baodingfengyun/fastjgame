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

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Objects;

/**
 * TimerHandle的模板实现
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
     * 定时器id，先添加的必定更小...
     */
    private final long timerId;
    /**
     * timer的创建时间
     */
    private final long createTimeMs;

    /**
     * 该handle关联的timerTask
     */
    private TimerTask timerTask;
    /**
     * 出现异常时是否自动关闭timer
     */
    private boolean autoCloseOnExceptionCaught = true;
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
     * 下次执行的最小帧要求
     */
    private int nextExecuteFrameThreshold;

    AbstractTimerHandle(DefaultTimerSystem timerSystem, TimerTask timerTask) {
        this.timerSystem = Objects.requireNonNull(timerSystem, "timerSystem");
        this.timerTask = Objects.requireNonNull(timerTask, "timerTask");
        this.timerId = timerSystem.nextTimerId();
        this.createTimeMs = timerSystem.curTimeMillis();
    }

    @Override
    public final DefaultTimerSystem timerSystem() {
        return timerSystem;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public final <T> T attachment() {
        return (T) attachment;
    }

    @Override
    public final <T> T attach(@Nullable Object newData) {
        @SuppressWarnings("unchecked")
        T pre = (T) attachment;
        this.attachment = newData;
        return pre;
    }

    @Nullable
    @Override
    public <T> T detach() {
        @SuppressWarnings("unchecked")
        T pre = (T) attachment;
        this.attachment = null;
        return pre;
    }

    @Override
    public long nextDelay() {
        return Math.max(0, nextExecuteTimeMs - timerSystem.curTimeMillis());
    }

    @Override
    public void close() {
        if (timerTask != null) {
            // 避免用户无意识的持有handle导致内存泄漏
            timerTask = null;
            timerSystem.removeClosedTimer(this);
        }
    }

    @Override
    public boolean isClosed() {
        return timerTask == null;
    }

    @Override
    public boolean isAutoCloseOnExceptionCaught() {
        return autoCloseOnExceptionCaught;
    }

    @Override
    public void setAutoCloseOnExceptionCaught(boolean autoCloseOnExceptionCaught) {
        this.autoCloseOnExceptionCaught = autoCloseOnExceptionCaught;
    }

    final long getCreateTimeMs() {
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

    final int getNextExecuteFrameThreshold() {
        return nextExecuteFrameThreshold;
    }

    final void setNextExecuteFrameThreshold(int nextExecuteFrameThreshold) {
        this.nextExecuteFrameThreshold = nextExecuteFrameThreshold;
    }

    /**
     * 在执行任务之前，更新必要的状态
     */
    protected void beforeExecuteOnce() {

    }

    final void run() throws Exception {
        timerTask.run(this);
    }

    /**
     * 任务执行一次之后，更新必要的状态
     *
     * @param curTimeMs 当前系统时间
     */
    protected abstract void afterExecuteOnce(long curTimeMs);

    /**
     * 调整下一次的执行时间
     */
    protected abstract void adjustNextExecuteTime();

    /**
     * 关闭timer，但不从队列中中删除
     */
    final void closeWithoutRemove() {
        timerTask = null;
    }

}