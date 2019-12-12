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

package com.wjybxx.fastjgame.example;

import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.concurrent.SingleThreadEventLoop;
import com.wjybxx.fastjgame.misc.IntHolder;
import com.wjybxx.fastjgame.redis.DefaultRedisPipeline;
import com.wjybxx.fastjgame.redis.RedisEventLoop;
import com.wjybxx.fastjgame.redis.RedisPipeline;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.TimeUtils;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;

import java.util.Collections;
import java.util.Set;

/**
 * redis事件循环示例
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/12
 * github - https://github.com/hl845740757
 */
public class RedisEventLoopExample {

    public static void main(String[] args) {
        final RedisEventLoop redisEventLoop = newRedisEventLoop();
        redisEventLoop.execute(ConcurrentUtils.NO_OP_TASK);

        final EventLoop appEventLoop = newAppEventLoop(redisEventLoop);
        appEventLoop.execute(ConcurrentUtils.NO_OP_TASK);

        try {
            Thread.sleep(TimeUtils.MIN);
        } catch (InterruptedException ignore) {

        }

        redisEventLoop.shutdown();
        appEventLoop.shutdown();
    }

    private static EventLoop newAppEventLoop(RedisEventLoop redisEventLoop) {
        return new ClientEventLoop(redisEventLoop);
    }

    private static RedisEventLoop newRedisEventLoop() {
        final JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxIdle(1);
        config.setMaxTotal(5);
        config.setBlockWhenExhausted(false);

        final HostAndPort sentinel = new HostAndPort("localhost", 16379);
        Set<String> sentinels = Collections.singleton(sentinel.toString());

        final JedisSentinelPool jedisSentinelPool = new JedisSentinelPool("mymaster", sentinels, config);
        return new RedisEventLoop(null, new DefaultThreadFactory("REDIS"),
                RejectedExecutionHandlers.abort(),
                jedisSentinelPool);
    }

    private static class ClientEventLoop extends SingleThreadEventLoop {

        private final RedisEventLoop redisEventLoop;
        private RedisPipeline redisPipeline;

        public ClientEventLoop(RedisEventLoop redisEventLoop) {
            super(null, new DefaultThreadFactory("REDIS-CLIENT"), RejectedExecutionHandlers.log());
            this.redisEventLoop = redisEventLoop;
        }

        @Override
        protected void init() throws Exception {
            redisPipeline = new DefaultRedisPipeline(redisEventLoop, this);
        }

        @Override
        protected void loop() {
            final IntHolder countdown = new IntHolder(0);
            final int MAX_LOOP_TIMES = 100_0000;
            int loop = 0;
            final long startTimeMS = System.currentTimeMillis();
            while (true) {

                runAllTasks();

                if (loop++ >= MAX_LOOP_TIMES) {
                    // 不再继续发送redis请求
                    if (countdown.get() == 0) {
                        shutdown();
                        break;
                    } else {
                        continue;
                    }
                }

                sendRedisCommands(countdown, loop);
            }

            System.out.println("execute " + (4 * MAX_LOOP_TIMES) + " commands, cost time ms " + (System.currentTimeMillis() - startTimeMS));
        }

        private void sendRedisCommands(IntHolder countdown, int loop) {
            countdown.getAndAdd(4);

            redisPipeline.hset("name", String.valueOf(loop), String.valueOf(loop))
                    .addCallback(response -> {
                        countdown.decAndGet();
                        System.out.println(response.get());
                    });

            redisPipeline.hget("name", String.valueOf(loop))
                    .addCallback(response -> {
                        countdown.decAndGet();
                        System.out.println(response.get());
                    });

            redisPipeline.zadd("rank", loop, String.valueOf(loop))
                    .addCallback(response -> {
                        countdown.decAndGet();
                        System.out.println(response.get());
                    });

            redisPipeline.zrank("rank", String.valueOf(loop))
                    .addCallback(response -> {
                        countdown.decAndGet();
                        System.out.println(response.get());
                    });
        }

        @Override
        protected void clean() throws Exception {

        }
    }
}
