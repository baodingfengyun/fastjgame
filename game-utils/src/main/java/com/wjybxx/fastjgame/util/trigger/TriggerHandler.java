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
 * 该接口用于将{@link Trigger}中的部分数据与接口分离，方便管理。
 * handler可以通过操作{@link Trigger}实现生命周期管理
 */
public interface TriggerHandler {

    /**
     * 更新自己的状态。
     * 注意：
     * 1. 该方法仅在{@link Trigger#isRemoved()}为false的情况下被调用
     * 2. 该方法的设计目的仅仅是为了更新自己的状态，而不是为了检测事件。
     *
     * @param curTimeMillis 当前系统时间
     */
    default void update(Trigger trigger, long curTimeMillis) {

    }

    /**
     * 尝试触发对应的事件。
     * 注意：该方法仅仅在{@link Trigger#isActive()}为true的情况下被调用。
     *
     * @param curTimeMillis 当前系统时间
     * @return 如果成功触发事件，则返回true，否则返回false
     */
    boolean tryTrigger(Trigger trigger, long curTimeMillis);

}
