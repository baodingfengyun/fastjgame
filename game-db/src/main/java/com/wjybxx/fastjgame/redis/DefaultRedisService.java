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

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.FutureResult;
import com.wjybxx.fastjgame.concurrent.GenericFutureResultListener;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutionException;

/**
 * redisService的默认实现，它是一个本地service，其回调默认环境为用户所在线程{@link #appEventLoop}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/12
 * github - https://github.com/hl845740757
 */
public class DefaultRedisService implements RedisService {

    private final RedisEventLoop redisEventLoop;
    private final EventLoop appEventLoop;

    public DefaultRedisService(RedisEventLoop redisEventLoop, EventLoop appEventLoop) {
        this.redisEventLoop = redisEventLoop;
        this.appEventLoop = appEventLoop;
    }

    @Override
    public void execute(@Nonnull RedisCommand<?> command) {
        redisEventLoop.execute(command);
    }

    @Override
    public void executeAndFlush(@Nonnull RedisCommand<?> command) {

    }

    @Override
    public <V> void call(@Nonnull RedisCommand<V> command, GenericFutureResultListener<FutureResult<V>> listener) {
        redisEventLoop.call(command, listener, appEventLoop);
    }

    @Override
    public <V> void callAndFlush(@Nonnull RedisCommand<V> command, GenericFutureResultListener<FutureResult<V>> listener) {

    }

    @Override
    public <V> V syncCall(@Nonnull RedisCommand<V> command) throws ExecutionException {
        return redisEventLoop.syncCall(command, appEventLoop);
    }

    @Override
    public RedisFuture<?> newWaitFuture() {
        return redisEventLoop.newWaitFuture(appEventLoop);
    }
}
