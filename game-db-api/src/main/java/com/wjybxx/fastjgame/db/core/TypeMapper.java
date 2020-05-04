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

package com.wjybxx.fastjgame.db.core;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * 类型映射器。
 * 一个类型{@link Class}的名字和唯一标识应尽量是稳定的，否则在持久化和序列化方面会面临问题。
 * <p>
 * 子类实现必须是不可变的，方便安全的共享，避免产生不必要的竞争。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/4/20
 */
@Immutable
public interface TypeMapper {

    /**
     * 通过类型获取标识符
     */
    @Nullable
    TypeIdentifier ofType(Class<?> type);

    /**
     * 通过类型的数字id找到类型信息
     */
    @Nullable
    TypeIdentifier ofNumber(long number);

    /**
     * 通过类型的名字找到类型信息
     */
    TypeIdentifier ofName(String name);

}
