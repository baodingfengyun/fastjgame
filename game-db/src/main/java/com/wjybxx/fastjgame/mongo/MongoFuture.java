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

package com.wjybxx.fastjgame.mongo;

import com.wjybxx.fastjgame.concurrent.FutureListener;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;

import javax.annotation.Nonnull;

/**
 * Mongodb操作关联的future。
 * <p>
 * Q: 为什么提供该接口？
 * A:
 * 1. 可以屏蔽底层的真实实现，底层可能是同步驱动，也可能是异步驱动，但可以给用户一个统一的接口。
 * mongodb的异步驱动，总是需要将回调作为参数传入，由于数据库操作api极多，回调参数对api的污染就很明显。
 * 现在mongodb的异步驱动基本废弃了，引入了新的响应流api - (mongodb-driver-reactivestreams)。
 * 2. 数据库操作很可能需要同步阻塞式调用，只使用回调的话无法进行支持。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/15
 * github - https://github.com/hl845740757
 */
public interface MongoFuture<V> extends ListenableFuture<V> {

    /**
     * {@inheritDoc}
     * 监听器默认执行在发起请求时的用户线程。
     */
    @Override
    void addListener(@Nonnull FutureListener<? super V> listener);

}
