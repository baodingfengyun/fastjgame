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
 * 事件处理器注册表
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/27
 * github - https://github.com/hl845740757
 */
public interface EventHandlerRegistry {

    /**
     * 注册一个事件的观察者。
     * 注意：由于没有Event接口，因此请手动保证Context类型的一致性，否则可能抛出类型转换错误。
     * 正常情况下，该方法由生成的代码调用。当然也可以手动注册一些事件，即不使用注解处理器。
     *
     * @param eventType 关注的事件类型
     * @param handler   事件处理器
     * @param <T>       上下文类型
     * @param <E>       事件的类型
     */
    <T, E> void register(@Nonnull Class<E> eventType, @Nonnull EventHandler<T, ? super E> handler);

    /**
     * 注册一个事件的观察者。
     * 使用该方法注册时，表示该事件总是没有额外的上下文，或处理器并不关心上下文。
     *
     * @param eventType 关注的事件类型
     * @param handler   事件处理器
     * @param <E>       事件的类型
     */
    default <E> void register(@Nonnull Class<E> eventType, @Nonnull SimpleEventHandler<? super E> handler) {
        register(eventType, (EventHandler<Object, ? super E>) handler);
    }

    /**
     * 释放所有的资源，因为{@link #register(Class, EventHandler)} 会捕获太多对象，当不再使用{@link EventHandlerRegistry}时，
     * 手动的释放，避免因为registry对象存在导致内存泄漏。
     */
    void release();

}
