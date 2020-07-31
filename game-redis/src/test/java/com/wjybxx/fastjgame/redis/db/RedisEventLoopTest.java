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

package com.wjybxx.fastjgame.redis.db;

import com.wjybxx.fastjgame.utils.concurrent.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.utils.concurrent.unbounded.SleepWaitStrategyFactory;
import com.wjybxx.fastjgame.utils.concurrent.unbounded.TemplateEventLoop;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * redis事件循环示例
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/12
 * github - https://github.com/hl845740757
 */
public class RedisEventLoopTest {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        final RedisEventLoop redisEventLoop = newRedisEventLoop();
        final EventLoop appEventLoop = newAppEventLoop(redisEventLoop);
        try {
            // submit一个任务，阻塞到线程启动成功
            redisEventLoop.submit(ConcurrentUtils.NO_OP_TASK).get();
            appEventLoop.submit(ConcurrentUtils.NO_OP_TASK).get();

            appEventLoop.terminationFuture().awaitUninterruptibly();
        } finally {
            appEventLoop.shutdown();
            redisEventLoop.shutdown();
        }
    }

    private static EventLoop newAppEventLoop(RedisEventLoop redisEventLoop) {
        return new ClientEventLoop(redisEventLoop);
    }

    private static RedisEventLoop newRedisEventLoop() {
        final JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxIdle(1);
        config.setMaxTotal(5);

        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);

        config.setBlockWhenExhausted(true);
        config.setMaxWaitMillis(10);

        // 测试时不使用哨兵
        final JedisPool jedisPool = new JedisPool(config, "localhost", 6379);
        final RedisEventLoop redisEventLoop = new RedisEventLoop(null, new DefaultThreadFactory("RedisEventLoop"), RejectedExecutionHandlers.abort(), jedisPool);
        redisEventLoop.terminationFuture().addListener(future -> jedisPool.close());
        return redisEventLoop;
    }

    private static class ClientEventLoop extends TemplateEventLoop {

        private static final int MAX_LOOP_TIMES = 100_0000;

        private final RedisEventLoop redisEventLoop;
        private RedisClient redisClient;

        ClientEventLoop(RedisEventLoop redisEventLoop) {
            super(null, new DefaultThreadFactory("REDIS-CLIENT"), RejectedExecutionHandlers.log(), new SleepWaitStrategyFactory());
            this.redisEventLoop = redisEventLoop;
        }

        @Override
        protected void init() throws Exception {
            redisClient = new DefaultRedisClient(redisEventLoop, this);

            final long startTimeMS = System.currentTimeMillis();

            for (int loopTimes = 0; loopTimes < MAX_LOOP_TIMES; loopTimes++) {
                sendRedisCommands(loopTimes);
            }

            // 监听前面的redis命令完成
            redisClient.call(pipeline -> pipeline.hset("test-monitor", "monitor", "1"), Function.identity())
                    .addListener(future -> onAllCommandsFinish(startTimeMS));
        }

        private void onAllCommandsFinish(long startTimeMS) {
            System.out.println("execute " + (2 * MAX_LOOP_TIMES) + " commands, cost time ms " + (System.currentTimeMillis() - startTimeMS));
            shutdown();
        }

        private void sendRedisCommands(int loop) {
            redisClient.call(pipeline -> pipeline.hset("test-name", String.valueOf(loop), String.valueOf(loop)), Function.identity())
                    .addListener(f -> System.out.println(f.getNow()));

            redisClient.call(pipeline -> pipeline.hget("test-name", String.valueOf(loop)), Function.identity())
                    .addListener(f -> System.out.println(f.getNow()));
        }

        @Override
        protected void clean() throws Exception {

        }
    }
}
