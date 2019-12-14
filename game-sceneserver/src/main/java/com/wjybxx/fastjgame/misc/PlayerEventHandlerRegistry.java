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

package com.wjybxx.fastjgame.misc;

import javax.annotation.Nonnull;

/**
 * 玩家事件处理器注册表
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/25
 * github - https://github.com/hl845740757
 */
public interface PlayerEventHandlerRegistry {

    /**
     * 注册一个事件对应的处理函数
     *
     * @param clazz   事件类型
     * @param handler 事件对应的处理函数
     * @param <T>     事件类型
     */
    <T> void register(@Nonnull Class<T> clazz, @Nonnull PlayerEventHandler<T> handler);

    /**
     * 释放所有的资源，因为{@link #register(Class, PlayerEventHandler)}会捕获太多对象，
     * 当不再使用{@link PlayerEventHandlerRegistry}时，进行手动释放，避免因为registry对象存在导致内存泄漏。
     */
    void release();
}
