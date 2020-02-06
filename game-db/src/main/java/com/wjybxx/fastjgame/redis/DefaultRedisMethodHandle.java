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

import com.wjybxx.fastjgame.async.DefaultMethodListenable;
import com.wjybxx.fastjgame.async.MethodListenable;
import com.wjybxx.fastjgame.concurrent.FutureResult;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutionException;

/**
 * 请使用{@link RedisMethodHandleFactory}创建对象。
 * 不要手动创建该对象，以免未来产生变化时不便修改。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/9
 * github - https://github.com/hl845740757
 */
public class DefaultRedisMethodHandle<V> implements RedisMethodHandle<V> {

    private final RedisCommand<V> command;

    DefaultRedisMethodHandle(RedisCommand<V> command) {
        this.command = command;
    }


    @Override
    public void execute(@Nonnull RedisClient redisClient) {
        redisClient.execute(command);
    }

    @Override
    public void executeAndFlush(@Nonnull RedisClient redisClient) {
        redisClient.executeAndFlush(command);
    }

    @Override
    public MethodListenable<FutureResult<V>, V> call(@Nonnull RedisClient redisClient) {
        final DefaultMethodListenable<FutureResult<V>, V> listener = new DefaultMethodListenable<>();
        redisClient.call(command).addListener(listener);
        return listener;
    }

    @Override
    public MethodListenable<FutureResult<V>, V> callAndFlush(@Nonnull RedisClient redisClient) {
        final DefaultMethodListenable<FutureResult<V>, V> listener = new DefaultMethodListenable<>();
        redisClient.callAndFlush(command).addListener(listener);
        return listener;
    }

    @Override
    public V syncCall(@Nonnull RedisClient redisClient) throws ExecutionException {
        return redisClient.syncCall(command);
    }
}
