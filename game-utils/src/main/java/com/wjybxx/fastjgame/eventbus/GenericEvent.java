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

package com.wjybxx.fastjgame.eventbus;

import javax.annotation.Nonnull;

/**
 * 泛型事件。
 * Q: 该设计为了解决什么问题？
 * A: 有时候我们期望能将既有类作为分发过程中的键，因为无法修改或<b>不应该</b>修改这些类。
 * 那么有两种解决方案： 1. 以context + event 组合的方式抛出 2. 封装为新对象的方式抛出。
 * 在上一版本中，我忽略了泛型特性，因此认为第二种方案的成本太高，因此选择了第一种方案。
 * 但是第一种方案过于灵活，无法在编译器校验context的类型，较为危险，且双参数不利于修改和扩展。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/19
 * github - https://github.com/hl845740757
 */
public interface GenericEvent<T> {

    /**
     * 获取事件包含的子事件.
     * 该api主要用于事件分发过程中，实现类应该定义针对业务的更具表达力的方法。
     */
    @Nonnull
    T child();
}
