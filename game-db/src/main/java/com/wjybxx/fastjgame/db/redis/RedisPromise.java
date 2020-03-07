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

package com.wjybxx.fastjgame.db.redis;

import com.wjybxx.fastjgame.utils.concurrent.FailedFutureListener;
import com.wjybxx.fastjgame.utils.concurrent.FutureListener;
import com.wjybxx.fastjgame.utils.concurrent.Promise;
import com.wjybxx.fastjgame.utils.concurrent.SucceededFutureListener;

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;

/**
 * redis操作对应的promise
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/15
 * github - https://github.com/hl845740757
 */
public interface RedisPromise<V> extends Promise<V>,RedisFuture<V> {

    // 仅用于语法支持
    @Override
    RedisPromise<V> onComplete(@Nonnull FutureListener<? super V> listener);

    @Override
    RedisPromise<V> onComplete(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor);

    @Override
    RedisPromise<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener);

    @Override
    RedisPromise<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener, @Nonnull Executor bindExecutor);

    @Override
    RedisPromise<V> onFailure(@Nonnull FailedFutureListener<? super V> listener);

    @Override
    RedisPromise<V> onFailure(@Nonnull FailedFutureListener<? super V> listener, @Nonnull Executor bindExecutor);

    @Override
    RedisPromise<V> await() throws InterruptedException;

    @Override
    RedisPromise<V> awaitUninterruptibly();
}
