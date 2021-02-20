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

package com.wjybxx.fastjgame.net.serialization;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * 类型映射器。
 * 注意：
 * 1. 必须保证同一个类在所有机器上的映射结果是相同的，这意味着你应该基于名字映射，而不能直接使用class对象的hash值。
 * 2. 一个类型{@link Class}的名字和唯一标识应尽量是稳定的，即同一个类的映射值在不同版本之间是相同的。
 * 3. 子类必须是线程安全的，建议优先实现为不可变对象。不可变对象可以安全的共享，且没有额外的竞争。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/4/20
 */
@ThreadSafe
public interface TypeIdMapper {

    /**
     * 通过类型获取数字标识符
     */
    @Nullable
    TypeId ofType(Class<?> type);

    /**
     * 通过数字id找到类型信息
     */
    @Nullable
    Class<?> ofId(TypeId typeId);

}
