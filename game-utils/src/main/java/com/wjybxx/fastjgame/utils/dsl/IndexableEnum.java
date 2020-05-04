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

package com.wjybxx.fastjgame.utils.dsl;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * 可索引的枚举。
 * 枚举是特殊的值对象，我们要求项目中的所有可序列化的枚举必须实现该接口。
 * <p>
 * Q: 名字的由来？
 * A: 如果一个值对象的索引为数字，我们可以认为它就是枚举。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/4 13:35
 * github - https://github.com/hl845740757
 */
@Immutable
public interface IndexableEnum extends IndexableValue<Integer> {

    /**
     * 获取枚举值的数字。
     * 注意：持久化会使用该数字，因此该值必须是稳定的，不建议使用{@link Enum#ordinal()}。
     */
    int getNumber();

    /**
     * @deprecated use {@link #getNumber()} instead
     */
    @Nonnull
    @Deprecated
    @Override
    default Integer getIndex() {
        return getNumber();
    }

}
