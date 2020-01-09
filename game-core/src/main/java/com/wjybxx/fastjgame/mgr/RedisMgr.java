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
import com.wjybxx.fastjgame.concurrent.FutureResult;
import com.wjybxx.fastjgame.concurrent.GenericFutureResultListener;
import com.wjybxx.fastjgame.redis.RedisFuture;
import com.wjybxx.fastjgame.redis.RedisCommand;
import com.wjybxx.fastjgame.redis.RedisService;

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
public class RedisMgr implements RedisService {

    private final RedisEventLoopMgr redisEventLoopMgr;
    private final GameEventLoopMgr gameEventLoopMgr;
    private RedisService redisService;

    @Inject
    public RedisMgr(RedisEventLoopMgr redisEventLoopMgr, GameEventLoopMgr gameEventLoopMgr) {
        this.redisEventLoopMgr = redisEventLoopMgr;
        this.gameEventLoopMgr = gameEventLoopMgr;
    }

    /**
     * 在使用其它方法之前，必须先构建服务
     * 在构造的时候无法确保gameEventLoop存在，所以在这里初始化
     */
    public void initService() {
        if (redisService != null) {
            throw new IllegalStateException();
        }
        redisService = redisEventLoopMgr.newService(gameEventLoopMgr.getEventLoop());
    }

    @Override
    public void execute(@Nonnull RedisCommand<?> command) {
        redisService.execute(command);
    }

    @Override
    public void executeAndFlush(@Nonnull RedisCommand<?> command) {
        redisService.executeAndFlush(command);
    }

    @Override
    public <V> void call(@Nonnull RedisCommand<V> command, GenericFutureResultListener<FutureResult<V>> listener) {
        redisService.call(command, listener);
    }

    @Override
    public <V> void callAndFlush(@Nonnull RedisCommand<V> command, GenericFutureResultListener<FutureResult<V>> listener) {
        redisService.callAndFlush(command, listener);
    }

    @Override
    public <V> V syncCall(@Nonnull RedisCommand<V> command) throws ExecutionException {
        return redisService.syncCall(command);
    }

    @Override
    public RedisFuture<?> newWaitFuture() {
        return redisService.newWaitFuture();
    }
}
