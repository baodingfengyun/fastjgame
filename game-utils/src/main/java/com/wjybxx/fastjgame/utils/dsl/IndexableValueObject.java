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

/**
 * 可索引的值对象。
 * <p>
 * Q: 什么是可索引的值对象？
 * A: 值对象一般没有唯一标识，但存在某些类型的值对象，它们有类似唯一标识的索引的，通过索引我们获取获取到对应的值对象，这样的值对象我们就称之为可索引的值对象。。
 * 举个例子，策划的配置表格，每一行都是一个值对象，我们总是根据某些列的值去确定一条配置，如：物品表，我们一般通过物品id去获取配置。
 * <p>
 * Q: 那么该接口的作用？
 * A: 既然是可索引的值对象，那么我们在服务器之间传输以及持久化过程中，只需要存储其索引信息就可以了。
 * <p>
 * Q: 我们不是可以在值对象里面只存储索引，不存储完整对象的吗，为什么还要设计该对象？
 * A: 我们要尽可能的减少对业务逻辑的限制，业务逻辑可能需要引用完整的值对象对象，以方便业务逻辑，如：减少查询。
 * 注意：如果想要序列化或持久化，请确保索引是可以序列化或持久化的
 * <p>
 * 注意：实现类必须满足值对象的要求。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/16
 * github - https://github.com/hl845740757
 */
public interface IndexableValueObject<T> extends ValueObject {

    /**
     * 获取值对象的索引对象
     */
    @Nonnull
    T getIndex();

}
