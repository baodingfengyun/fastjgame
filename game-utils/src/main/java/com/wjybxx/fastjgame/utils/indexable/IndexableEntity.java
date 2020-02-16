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

package com.wjybxx.fastjgame.utils.indexable;

import javax.annotation.Nonnull;

/**
 * 可索引的实体。
 * <p>
 * Q: 什么是可索引的实体？
 * A: 举个例子，策划的配置表格，我们总是根据某些列的值或某些行的值去确定一条配置，如：物品表，我们一般通过物品id去获取配置。
 * 这样的实体我们就称之为可索引的实体。
 * <p>
 * Q: 那么该接口的作用？
 * A: 既然是可索引的实体，那么我们在服务器之间传输以及持久化过程中，只需要存储其索引信息就可以了。
 * <p>
 * Q: 我们不是可以在实体里面只存储索引，不存储完整对象的吗，为什么还要设计该对象？
 * A: 我们要尽可能的<b>减少对业务实体的限制</b>！业务实体可能需要引用完整的实体对象，以方便业务逻辑，或减少大量查询等，
 * 不能因为要序列化或要持久化就要求业务实体必须是简单的bean。
 * <p>
 * 注意：如果想要序列化或持久化，请确保索引是可以序列化或持久化的
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/16
 * github - https://github.com/hl845740757
 */
public interface IndexableEntity<T> {

    /**
     * 获取实体的索引对象
     */
    @Nonnull
    T getIndex();

}
