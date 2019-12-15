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

package com.wjybxx.fastjgame.redis;

import com.wjybxx.fastjgame.concurrent.FutureListener;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;

import javax.annotation.Nonnull;

/**
 * Redis异步操作获取结果的句柄。
 * Q: 为什么改为future了？
 * A: 数据库操作很可能需要同步阻塞式调用，只使用回调的话无法进行支持。
 * (redis也是数据库啊，很可能需要阻塞式调用)
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/12
 * github - https://github.com/hl845740757
 */
public interface RedisFuture<V> extends ListenableFuture<V> {

    /**
     * {@inheritDoc}
     * 回调默认执行在发起请求的用户线程。
     */
    @Override
    void addListener(@Nonnull FutureListener<? super V> listener);

}
