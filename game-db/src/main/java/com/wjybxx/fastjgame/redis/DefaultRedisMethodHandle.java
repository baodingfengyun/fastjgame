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

import com.wjybxx.fastjgame.async.AbstractMethodHandle;
import com.wjybxx.fastjgame.concurrent.FutureResult;
import com.wjybxx.fastjgame.concurrent.GenericFailureFutureResultListener;
import com.wjybxx.fastjgame.concurrent.GenericFutureResultListener;
import com.wjybxx.fastjgame.concurrent.GenericSuccessFutureResultListener;

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
public class DefaultRedisMethodHandle<V> extends AbstractMethodHandle<RedisServiceHandle, FutureResult<V>, V>
        implements RedisMethodHandle<V> {

    private final RedisCommand<V> command;

    DefaultRedisMethodHandle(RedisCommand<V> command) {
        this.command = command;
    }

    @Override
    public RedisMethodHandle<V> onSuccess(GenericSuccessFutureResultListener<FutureResult<V>, V> listener) {
        super.onSuccess(listener);
        return this;
    }

    @Override
    public RedisMethodHandle<V> onFailure(GenericFailureFutureResultListener<FutureResult<V>, V> listener) {
        super.onFailure(listener);
        return this;
    }

    @Override
    public RedisMethodHandle<V> onComplete(GenericFutureResultListener<FutureResult<V>, V> listener) {
        super.onComplete(listener);
        return this;
    }

    @Override
    public void execute(@Nonnull RedisServiceHandle redisServiceHandle) {
        redisServiceHandle.execute(command);
    }

    @Override
    public void executeAndFlush(@Nonnull RedisServiceHandle redisServiceHandle) {
        redisServiceHandle.executeAndFlush(command);
    }

    @Override
    public void call(@Nonnull RedisServiceHandle redisServiceHandle) {
        final GenericFutureResultListener<FutureResult<V>, V> listener = detachListener();
        if (listener == null) {
            redisServiceHandle.execute(command);
        } else {
            redisServiceHandle.call(command).addListener(listener);
        }
    }

    @Override
    public void callAndFlush(@Nonnull RedisServiceHandle redisServiceHandle) {
        final GenericFutureResultListener<FutureResult<V>, V> listener = detachListener();
        if (listener == null) {
            redisServiceHandle.executeAndFlush(command);
        } else {
            redisServiceHandle.callAndFlush(command).addListener(listener);
        }
    }

    @Override
    public V syncCall(@Nonnull RedisServiceHandle redisServiceHandle) throws ExecutionException {
        return redisServiceHandle.syncCall(command);
    }
}
