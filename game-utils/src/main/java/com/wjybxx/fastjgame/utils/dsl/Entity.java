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

/**
 * 实体，与DDD中的实体概念保持一致。
 * <p>
 * 一个实体拥有唯一的标识{@link #id()}，如果实体具有相同的类型和标识，则认为是相等的。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/4/19
 */
@SuppressWarnings("unused")
public interface Entity {

    /**
     * 实体的唯一标识
     */
    Object id();

}