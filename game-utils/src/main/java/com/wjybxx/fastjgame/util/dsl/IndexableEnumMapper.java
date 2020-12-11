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

package com.wjybxx.fastjgame.util.dsl;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 枚举值映射
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/4 15:08
 * github - https://github.com/hl845740757
 */
public interface IndexableEnumMapper<T extends IndexableEnum> {

    /**
     * 通过数字找到对应的枚举
     *
     * @param number 枚举的唯一编号
     * @return T 如果不存在，则返回null，而不是抛出异常
     */
    @Nullable
    T forNumber(int number);

    /**
     * 获取映射的所有枚举实例。
     */
    List<T> values();

    /**
     * 通过数字找到对应的枚举
     *
     * @param number 枚举的唯一编号
     * @return T number对应的枚举
     * @throws IllegalArgumentException 如果number对应的枚举不存在，则抛出异常
     */
    @Nullable
    default T checkedForName(int number) {
        final T result = forNumber(number);
        if (null == result) {
            throw new IllegalArgumentException("number: " + number);
        }
        return result;
    }
}
