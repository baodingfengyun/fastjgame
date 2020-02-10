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

package com.wjybxx.fastjgame.mgr;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.redis.RedisClient;
import com.wjybxx.fastjgame.redis.RedisCommand;
import com.wjybxx.fastjgame.redis.RedisFuture;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.concurrent.ExecutionException;

/**
 * redis管理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/12
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class RedisClientMgr implements RedisClient {

    private final RedisEventLoopMgr redisEventLoopMgr;
    private final GameEventLoopMgr gameEventLoopMgr;
    private RedisClient redisClient;

    @Inject
    public RedisClientMgr(RedisEventLoopMgr redisEventLoopMgr, GameEventLoopMgr gameEventLoopMgr) {
        this.redisEventLoopMgr = redisEventLoopMgr;
        this.gameEventLoopMgr = gameEventLoopMgr;
    }

    /**
     * 在使用其它方法之前，必须先构建服务
     * 在构造的时候无法确保gameEventLoop存在，所以在这里初始化
     */
    public void createClient() {
        if (redisClient != null) {
            throw new IllegalStateException();
        }
        redisClient = redisEventLoopMgr.newRedisClient(gameEventLoopMgr.getEventLoop());
    }

    @Override
    public void execute(@Nonnull RedisCommand<?> command) {
        redisClient.execute(command);
    }

    @Override
    public void executeAndFlush(@Nonnull RedisCommand<?> command) {
        redisClient.executeAndFlush(command);
    }

    @Override
    public <V> RedisFuture<V> call(@Nonnull RedisCommand<V> command) {
        return redisClient.call(command);
    }

    @Override
    public <V> RedisFuture<V> callAndFlush(@Nonnull RedisCommand<V> command) {
        return redisClient.callAndFlush(command);
    }

    @Override
    public <V> V syncCall(@Nonnull RedisCommand<V> command) throws ExecutionException {
        return redisClient.syncCall(command);
    }

}
