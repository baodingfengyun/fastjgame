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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolAbstract;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.concurrent.ThreadFactory;

import static com.wjybxx.fastjgame.utils.CloseableUtils.closeQuietly;
import static com.wjybxx.fastjgame.utils.ConcurrentUtils.sleepQuietly;

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
    /**
     * 它决定了最多多少个命令执行一次{@link #sync()}。
     * 不宜太大，太大时，一旦出现异常，破坏太大。
     * 不宜太小，太小时，无法充分利用网络。
     */
    private static final int BATCH_TASK_SIZE = 512;
    private final ArrayDeque<JedisPipelineTask<?>> waitResponseTasks = new ArrayDeque<>(BATCH_TASK_SIZE);

    private final JedisPoolAbstract jedisPool;
    private Jedis jedis;
    private Pipeline pipeline;

    public RedisEventLoop(@Nullable EventLoopGroup parent,
                          @Nonnull ThreadFactory threadFactory,
                          @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                          @Nonnull JedisPoolAbstract jedisPool) {
        super(parent, threadFactory, rejectedExecutionHandler);
        this.jedisPool = jedisPool;
    }

    @Override
    protected void init() {
        connectSafely();
    }

    @Override
    protected void clean() {
        try {
            // pipeline关闭时会调用sync
            closeQuietly(pipeline);
            closeQuietly(jedis);
        } finally {
            generateResponses();
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

                // 等待以降低cpu利用率
                sleepQuietly(1);
            } catch (Throwable t) {
                // 避免错误的退出循环
                logger.warn("loop caught exception", t);
            }
        }
    }

    /**
     * 刷新管道，为未获得结果的redis请求生成结果
     */
    private void sync() {
        if (waitResponseTasks.isEmpty()) {
            return;
        }

        try {
            if (pipeline != null) {
                pipeline.sync();
            } else {
                // 当前连接不可用，尝试建立连接
                // 不在执行管道命令的时候进行连接，是为了避免处理任务过慢，导致任务大量堆积，从而产生过高的内存占用
                connectSafely();
            }
        } catch (Throwable t) {
            // pipeline的缺陷：由于多个指令是批量执行的，因此不是原子的。
            // 出现异常时：可能部分成功，部分失败，部分未执行。
            logger.warn("pipeline.sync caught exception", t);

            closeQuietly(pipeline);
            closeQuietly(jedis);

            jedis = null;
            pipeline = null;

            connectSafely();
        } finally {
            generateResponses();
        }
    }

    /**
     * 安全的建立连接 - 不抛出任何异常
     */
    private void connectSafely() {
        try {
            jedis = jedisPool.getResource();
            pipeline = jedis.pipelined();
        } catch (Throwable t) {
            logger.warn("jedisPool.getResource caught exception", t);
            // 防止频繁出现异常导致cpu资源利用率过高。
            // 获取连接失败时，在接下来的一段时间里大概率总是失败。
            sleepQuietly(1000);
        }
    }

    /**
     * 为未获得结果的redis请求生成结果
     */
    private void generateResponses() {
        JedisPipelineTask<?> task;
        while ((task = waitResponseTasks.pollFirst()) != null) {
            setData(task.redisPromise, task.dependency, task.cause);
        }
    }

    /**
     * 安全的为promise赋值
     */
    @SuppressWarnings("unchecked")
    private static void setData(RedisPromise redisPromise, Response dependency, Throwable cause) {
        if (cause != null) {
            redisPromise.tryFailure(cause);
            return;
        }
        try {
            redisPromise.trySuccess(dependency.get());
        } catch (Throwable t) {
            redisPromise.tryFailure(t);
        }
    }

    <T> RedisFuture<T> enqueue(EventLoop appEventLoop, RedisPipelineCommand<T> pipelineCmd) {
        final DefaultRedisPromise<T> redisPromise = new DefaultRedisPromise<>(this, appEventLoop);
        execute(new JedisPipelineTask<>(appEventLoop, pipelineCmd, redisPromise));
        return redisPromise;
    }

    private class JedisPipelineTask<T> implements Runnable {
        final EventLoop appEventLoop;
        final RedisPipelineCommand<T> pipelineCmd;
        final DefaultRedisPromise<T> redisPromise;

        Response<T> dependency;
        /**
         * 用于减少对{@link Response}的依赖 - 过多的依赖其内部实现不太安全。
         */
        Throwable cause;

        JedisPipelineTask(EventLoop appEventLoop, RedisPipelineCommand<T> pipelineCmd, DefaultRedisPromise<T> redisPromise) {
            this.appEventLoop = appEventLoop;
            this.pipelineCmd = pipelineCmd;
            this.redisPromise = redisPromise;
        }

        @Override
        public void run() {
            waitResponseTasks.add(this);

            if (null == pipeline) {
                // 连接不可用，快速失败
                cause = RedisConnectionException.INSTANCE;
                return;
            }

            try {
                dependency = pipelineCmd.execute(pipeline);
            } catch (Throwable t) {
                // 执行管道命令出现异常
                cause = t;
            }
        }
    }

}
