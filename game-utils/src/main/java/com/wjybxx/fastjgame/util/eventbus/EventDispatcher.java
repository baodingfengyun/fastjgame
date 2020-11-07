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

package com.wjybxx.fastjgame.util.eventbus;

import javax.annotation.Nonnull;

/**
 * 事件分发器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/27
 * github - https://github.com/hl845740757
 */
public interface EventDispatcher {

    /**
     * 发布一个事件。
     *
     * @param event 要发布的事件
     * @apiNote 实现要求：
     * 1. {@link GenericEvent}参数为通配符的监听者，能监听到该类型的所有事件。
     * 2. {@link GenericEvent}参数为具体类型的监听者，能监听该特定类型的事件。
     * 3. 非{@link GenericEvent}事件的监听者，根据事件的类型精确分发。
     */
    void post(@Nonnull Object event);

}
