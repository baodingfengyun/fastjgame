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

import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.concurrent.ListenableFuture;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletionException;

/**
 * redisService的默认实现，它是一个本地service，其回调默认环境为用户所在线程{@link #appEventLoop}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/12
 * github - https://github.com/hl845740757
 */
public class DefaultRedisClient implements RedisClient {

    private final RedisEventLoop redisEventLoop;
    private final EventLoop appEventLoop;

    public DefaultRedisClient(RedisEventLoop redisEventLoop, EventLoop appEventLoop) {
        this.redisEventLoop = redisEventLoop;
        this.appEventLoop = appEventLoop;
    }

    @Override
    public void execute(@Nonnull RedisCommand<?> command) {
        redisEventLoop.execute(command, false);
    }

    @Override
    public void executeAndFlush(@Nonnull RedisCommand<?> command) {
        redisEventLoop.execute(command, true);
    }

    @Override
    public <V> ListenableFuture<V> call(@Nonnull RedisCommand<V> command) {
        return redisEventLoop.call(command, false, appEventLoop);
    }

    @Override
    public <V> ListenableFuture<V> callAndFlush(@Nonnull RedisCommand<V> command) {
        return redisEventLoop.call(command, true, appEventLoop);
    }

    @Override
    public <V> V syncCall(@Nonnull RedisCommand<V> command) throws CompletionException {
        return redisEventLoop.syncCall(command);
    }

}
