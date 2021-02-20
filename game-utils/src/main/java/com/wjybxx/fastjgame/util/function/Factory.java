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

package com.wjybxx.fastjgame.util.function;

import javax.annotation.Nonnull;

/**
 * 通用工厂
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/27
 * github - https://github.com/hl845740757
 */
@FunctionalInterface
public interface Factory<T> {

    /**
     * 创建一个新的实例
     *
     * @return newInstance
     */
    @Nonnull
    T newInstance();

}