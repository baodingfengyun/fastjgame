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
     * 正常情况下，该方法由生成的代码调用。当然也可以手动注册一些事件，即不使用注解处理器。
     *
     * @param eventType 关注的事件类型
     * @param handler   事件处理器
     * @param <T>       事件的类型
     */
    <T> void register(@Nonnull Class<T> eventType, @Nonnull EventHandler<? super T> handler);

    /**
     * 注册一个泛型事件的观察者。
     * 正常情况下，该方法由生成的代码调用。当然也可以手动注册一些事件，即不使用注解处理器。
     *
     * @param genericType 泛型事件类型
     * @param childType   泛型事件的子事件类型
     * @param handler     事件处理器
     * @param <T>         泛型事件类型
     */
    <T extends GenericEvent<?>> void register(@Nonnull Class<T> genericType, @Nonnull Class<?> childType, @Nonnull EventHandler<? super T> handler);

    /**
     * 判断是否存在对应的事件处理器。
     * 对于{@link GenericEvent}，建议只在必要的时候测试，因为可能会产生额外的对象。
     *
     * @return 如果存在对应的事件处理器，则返回true，否则返回false
     */
    boolean hasHandler(@Nonnull Object event);

    /**
     * 释放所有的资源，因为{@link #register(Class, EventHandler)} 会捕获太多对象，当不再使用{@link EventHandlerRegistry}时，
     * 手动的释放，避免因为registry对象存在导致内存泄漏。
     */
    void release();

}