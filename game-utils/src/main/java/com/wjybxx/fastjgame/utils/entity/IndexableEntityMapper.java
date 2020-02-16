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

package com.wjybxx.fastjgame.utils.entity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 可索引实体映射。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/16
 * github - https://github.com/hl845740757
 */
public interface IndexableEntityMapper<T, R> {

    /**
     * 通过实体的索引获取实体对象。
     *
     * @param index 实体的索引对象
     * @return 实体对象，如果不存在则返回null
     */
    @Nullable
    R forIndex(@Nonnull T index);

}
