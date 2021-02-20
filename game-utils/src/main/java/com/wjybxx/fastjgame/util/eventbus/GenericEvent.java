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
 * 静态泛型事件。
 * <p>
 * Q: 何为静态泛型事件？
 * A: 其子类型为{@link Class}。这使得我们能够在编译期获得其子键的值（泛型参数上获得），从而生成辅助代码。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/19
 * github - https://github.com/hl845740757
 */
public interface GenericEvent<T> extends DynamicChildEvent {

    /**
     * 子键为{@link Class}类型，即包含的内容的类型
     */
    @Nonnull
    Class<T> childKey();

}