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
 * 动态泛型事件 - 通{@link GenericEventType}限定
 * <p>
 * Q: 为什么称为动态泛型事件？
 * A: 我们通过变量{@link GenericEventType}来限定内部动态数据的类型，而不是类型{@link Class} - 即使用对象代替类型。
 * （这其实也是一种模式 - 类型对象模式）
 */
public interface DynamicGenericEvent<T> extends DynamicChildEvent {

    @Nonnull
    @Override
    GenericEventType<T> childKey();

}