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
import javax.annotation.Nullable;

/**
 * 事件处理器注册表。
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
     * @param <T>        事件的类型
     * @param eventType  关注的事件类型
     * @param customData 自定义数据
     * @param handler    事件处理器
     * @return 注册成功则返回true
     */
    <T> boolean register(@Nonnull Class<T> eventType, @Nullable String customData, @Nonnull EventHandler<? super T> handler);

    /**
     * 注册一个泛型事件的观察者。
     * 正常情况下，该方法由生成的代码调用。当然也可以手动注册一些事件，即不使用注解处理器。
     *
     * @param parentType 父事件类型
     * @param childKey   子事件key
     * @param customData 自定义数据
     * @param handler    事件处理器
     * @return 注册成功则返回true
     */
    <T extends DynamicChildEvent> boolean register(@Nonnull Class<T> parentType, @Nonnull Object childKey, @Nullable String customData, @Nonnull EventHandler<? super T> handler);

    <T> void deregister(@Nonnull Class<T> eventType, @Nonnull EventHandler<? super T> handler);

    <T extends DynamicChildEvent> void deregister(@Nonnull Class<T> parentType, @Nonnull Object childKey, @Nonnull EventHandler<? super T> handler);

    /**
     * 释放所有的资源，因为{@link #register(Class, String, EventHandler)} 会捕获太多对象，当不再使用{@link EventHandlerRegistry}时，
     * 手动的释放，避免因为registry对象存在导致内存泄漏。
     */
    void release();

}