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

package com.wjybxx.fastjgame.util.trigger;

/**
 * 触发器.
 * 触发器的作用是定时检测，并在满足条件的情况下执行一些逻辑或抛出一些事件。
 * <p>
 * 它和{@link java.util.TimerTask}最大的区别：它需要定时更新。
 */
public final class Trigger {

    private final int triggerId;
    private final long timeCreateMillis;
    private final TriggerHandler handler;

    private boolean removed = false;
    private boolean active = true;

    private Object extInfo;

    Trigger(int triggerId, long timeCreateMillis, TriggerHandler handler) {
        this.triggerId = triggerId;
        this.timeCreateMillis = timeCreateMillis;
        this.handler = handler;
    }

    /**
     * 触发器在其{@link TriggerSystem}中的id
     */
    public final int getTriggerId() {
        return triggerId;
    }

    public long getTimeCreateMillis() {
        return timeCreateMillis;
    }

    public TriggerHandler getHandler() {
        return handler;
    }

    /**
     * 是否是活动状态，如果返回为true，则会调用{@link TriggerHandler#tryTrigger(Trigger, long)}。
     */
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * 是否已经被移除，如果该返回true，则既不响应事件，也不被更新，并会在合适的时候被移除。
     */
    public boolean isRemoved() {
        return removed;
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }

    /**
     * @return trigger的扩展数据
     */
    public Object getExtInfo() {
        return extInfo;
    }

    public void setExtInfo(Object extInfo) {
        this.extInfo = extInfo;
    }
}