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
import com.wjybxx.fastjgame.concurrent.EventLoopGroup;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandler;
import com.wjybxx.fastjgame.concurrent.SingleThreadEventLoop;
import com.wjybxx.fastjgame.concurrent.disruptor.DisruptorEventLoop;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolAbstract;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.util.LinkedList;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.LockSupport;

/**
 * redis事件循环
 * <p>
 * Q: 为什么要把redis操作单独放在一个线程？
 * A: 由于{@link Pipeline}缓冲区满或显式调用{@link Pipeline#sync()}会引发阻塞，具体阻塞多久是无法估计的。
 * 我们不能让应用线程陷入阻塞。
 * <p>
 * Q: 它为什么不继承{@link DisruptorEventLoop}？
 * A: 它属于中间件，应用层与它之间是双向交互，使用{@link DisruptorEventLoop}可能导致死锁。
 * <p>
 * 对于大多数游戏而言，单个redis线程应该够用了，不过也很容易扩展为线程池模式(连接池)。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/12
 * github - https://github.com/hl845740757
 */
public class RedisEventLoop extends SingleThreadEventLoop {

    private static final Logger logger = LoggerFactory.getLogger(RedisEventLoop.class);
    private static final int BATCH_TASK_SIZE = 8 * 1024;

    private final JedisPoolAbstract jedisPool;

    private Jedis jedis;
    private Pipeline pipeline;
    private final LinkedList<JedisPipelineTask<?>> waitResponseTasks = new LinkedList<>();

    public RedisEventLoop(@Nullable EventLoopGroup parent,
                          @Nonnull ThreadFactory threadFactory,
                          @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                          @Nonnull JedisPoolAbstract jedisPool) {
        super(parent, threadFactory, rejectedExecutionHandler);
        this.jedisPool = jedisPool;
    }

    @Override
    protected void init() throws Exception {
        super.init();
        resetJedis();
        resetPipeline();
    }

    @Override
    protected void clean() throws Exception {
        // close会进行同步
        closeQuietly(pipeline);
        closeQuietly(jedis);
    }

    private void resetJedis() {
        closeQuietly(jedis);
        try {
            jedis = jedisPool.getResource();
        } catch (JedisException e) {
            logger.warn("get jedis caught exception", e);
        }
    }

    private void resetPipeline() {
        try {
            pipeline = jedis.pipelined();
        } catch (JedisException e) {
            logger.warn("get pipeline caught exception", e);
        }
    }

    private static void closeQuietly(Closeable resource) {
        if (null != resource) {
            try {
                resource.close();
            } catch (Throwable ignore) {

            }
        }
    }

    @Override
    protected void loop() {
        while (true) {
            try {
                if (confirmShutdown()) {
                    break;
                }

                runTasksBatch(BATCH_TASK_SIZE);

                sync();

                // 降低cpu利用率
                LockSupport.parkNanos(100);
            } catch (Throwable e) {
                // 避免错误的退出循环
                logger.warn("loop caught exception", e);
            }
        }
    }

    private void sync() {
        if (waitResponseTasks.isEmpty()) {
            return;
        }

        try {
            pipeline.sync();
        } catch (JedisConnectionException exception) {
            logger.warn("pipeline sync caught exception", exception);
            resetJedis();
        } finally {
            JedisPipelineTask<?> task;
            while ((task = waitResponseTasks.pollFirst()) != null) {
                ConcurrentUtils.safeExecute(task.appEventLoop, newCallbackTaskSafely(task));
            }
            resetPipeline();
        }
    }

    private <T> JedisCallbackTask<T> newCallbackTaskSafely(JedisPipelineTask<T> task) {
        if (task.exception != null) {
            // 压入管道时就失败了
            return new JedisCallbackTask<>(task.redisResponse, null, task.exception);
        }
        try {
            final T response = task.realResponse.get();
            return new JedisCallbackTask<>(task.redisResponse, response, null);
        } catch (JedisException exception) {
            return new JedisCallbackTask<>(task.redisResponse, null, exception);
        }
    }

    <T> RedisResponse<T> enqueue(EventLoop appEventLoop, RedisPipelineCommand<T> pipelineCmd) {
        final DefaultRedisResponse<T> redisResponse = new DefaultRedisResponse<>();
        execute(new JedisPipelineTask<>(appEventLoop, pipelineCmd, redisResponse));
        return redisResponse;
    }

    private class JedisPipelineTask<T> implements Runnable {
        final EventLoop appEventLoop;
        final RedisPipelineCommand<T> pipelineCmd;
        final DefaultRedisResponse<T> redisResponse;
        Response<T> realResponse;
        JedisException exception;

        JedisPipelineTask(EventLoop appEventLoop, RedisPipelineCommand<T> pipelineCmd, DefaultRedisResponse<T> redisResponse) {
            this.appEventLoop = appEventLoop;
            this.redisResponse = redisResponse;
            this.pipelineCmd = pipelineCmd;
        }

        @Override
        public void run() {
            waitResponseTasks.add(this);
            try {
                realResponse = pipelineCmd.execute(pipeline);
            } catch (JedisConnectionException e) {
                logger.warn("execute command caught exception", e);
                exception = e;
                resetJedis();
                resetPipeline();
            } catch (JedisException e) {
                exception = e;
            }
        }
    }

    private static class JedisCallbackTask<T> implements Runnable {

        final DefaultRedisResponse<T> redisResponse;
        final T response;
        final JedisException exception;

        JedisCallbackTask(DefaultRedisResponse<T> redisResponse, T response, JedisException exception) {
            this.redisResponse = redisResponse;
            this.response = response;
            this.exception = exception;
        }

        @Override
        public void run() {
            redisResponse.onComplete(response, exception);
        }
    }

}
