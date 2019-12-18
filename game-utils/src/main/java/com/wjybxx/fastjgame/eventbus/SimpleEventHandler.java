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

package com.wjybxx.fastjgame.eventbus;

import javax.annotation.Nonnull;

/**
 * 该事件处理器表示关心的事件总是没有额外的上下文，或处理器并不关心上下文。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/17
 * github - https://github.com/hl845740757
 */
@FunctionalInterface
public interface SimpleEventHandler<E> extends EventHandler<Object, E> {

    @Override
    default void onEvent(Object context, @Nonnull E event) throws Exception {
        onEvent(event);
    }

    void onEvent(@Nonnull E event);
}
