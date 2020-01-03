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
import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.redis.DefaultRedisPipeline;
import com.wjybxx.fastjgame.redis.RedisEventLoop;
import com.wjybxx.fastjgame.redis.RedisPipeline;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.DebugUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolAbstract;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * redis线程管理器。
 * 就多数游戏而言，redis线程开单个就足够了。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/12
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class RedisEventLoopMgr {

    private static final Logger logger = LoggerFactory.getLogger(RedisEventLoopMgr.class);

    private final RedisEventLoop redisEventLoop;
    private volatile boolean shutdown = false;

    @Inject
    public RedisEventLoopMgr(GameConfigMgr gameConfigMgr) {
        redisEventLoop = newRedisEventLoop(gameConfigMgr);
    }

    private static RedisEventLoop newRedisEventLoop(GameConfigMgr gameConfigMgr) {
        final JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxIdle(2);
        config.setMaxTotal(5);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        config.setBlockWhenExhausted(true);
        config.setMaxWaitMillis(10);

        final JedisPoolAbstract jedisPool = newJedisPool(gameConfigMgr, config);

        return new RedisEventLoop(null, new DefaultThreadFactory("RedisEventLoop"),
                RejectedExecutionHandlers.abort(),
                jedisPool);
    }

    @Nonnull
    private static JedisPoolAbstract newJedisPool(GameConfigMgr gameConfigMgr, JedisPoolConfig config) {
        if (DebugUtils.isDebugOpen()) {
            // debug模式从简
            return new JedisPool(config, "localhost", 6379);
        } else {
            // 使用哨兵机制达成高可用
            final Set<String> sentinels = new HashSet<>(Arrays.asList(gameConfigMgr.getRedisSentinelList().split(",")));
            final String password = StringUtils.isBlank(gameConfigMgr.getRedisPassword()) ? null : gameConfigMgr.getRedisPassword();
            return new JedisSentinelPool("mymaster", sentinels, config, password);
        }
    }

    /**
     * 启动redis线程
     */
    public void start() {
        redisEventLoop.terminationFuture().addListener(future -> {
            if (!shutdown) {
                logger.error("redisEventLoop shutdown by mistake");
            }
        });

        redisEventLoop.execute(ConcurrentUtils.NO_OP_TASK);
    }

    /**
     * 关闭redis线程
     */
    public void shutdown() {
        shutdown = true;
        redisEventLoop.shutdown();
        logger.info("RedisEventLoop shutdown success");
    }

    /**
     * 创建一个新的管道
     */
    public RedisPipeline newPipeline(@Nonnull final EventLoop appEventLoop) {
        return new DefaultRedisPipeline(redisEventLoop, Objects.requireNonNull(appEventLoop));
    }
}
