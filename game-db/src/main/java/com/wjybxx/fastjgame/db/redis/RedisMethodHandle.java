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

import com.wjybxx.fastjgame.utils.async.FlushableMethodHandle;
import com.wjybxx.fastjgame.utils.concurrent.NonBlockingFuture;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletionException;

/**
 * redis方法句柄，执行主体为{@link RedisClient}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/9
 * github - https://github.com/hl845740757
 */
public interface RedisMethodHandle<V> extends FlushableMethodHandle<RedisClient, V> {

    @Override
    void execute(@Nonnull RedisClient client);

    @Override
    void executeAndFlush(@Nonnull RedisClient client);

    @Override
    NonBlockingFuture<V> call(@Nonnull RedisClient client);

    @Override
    NonBlockingFuture<V> callAndFlush(@Nonnull RedisClient client);

    @Override
    V syncCall(@Nonnull RedisClient client) throws CompletionException;

}
