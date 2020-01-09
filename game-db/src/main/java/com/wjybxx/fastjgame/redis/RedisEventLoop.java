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

import com.wjybxx.fastjgame.concurrent.*;
import com.wjybxx.fastjgame.concurrent.adapter.FutureListenerAdapter;
import com.wjybxx.fastjgame.concurrent.disruptor.DisruptorEventLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolAbstract;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.exceptions.JedisConnectionException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.concurrent.ExecutionException;
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
 * pipeline的缺陷：由于多个指令是批量执行的，因此不是原子的。
 * 当{@link #pipelineSync()}出现异常时：可能部分成功，部分失败，部分未执行。
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

    private static final int TASK_BATCH_SIZE = 1024;

    /**
     * 它决定了最多多少个命令执行一次{@link #pipelineSync()}。
     * 不宜太大，太大时，一旦出现异常，破坏太大。
     * 不宜太小，太小时，无法充分利用网络。
     */
    private static final int MAX_WAIT_RESPONSE_TASK = 512;
    private final ArrayDeque<JedisPipelineTask<?>> waitResponseTasks = new ArrayDeque<>(MAX_WAIT_RESPONSE_TASK);

    private final JedisPoolAbstract jedisPool;
    /**
     * 我们使用null表示没有连接可用的状态，在出现异常时，将关闭连接，并将该属性置为null
     */
    private Jedis jedis;
    private Pipeline pipeline;

    public RedisEventLoop(@Nullable RedisEventLoopGroup parent,
                          @Nonnull ThreadFactory threadFactory,
                          @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                          @Nonnull JedisPoolAbstract jedisPool) {
        super(parent, threadFactory, rejectedExecutionHandler);
        this.jedisPool = jedisPool;
    }

    @Nullable
    @Override
    public RedisEventLoopGroup parent() {
        return (RedisEventLoopGroup) super.parent();
    }

    @Nonnull
    @Override
    public RedisEventLoop next() {
        return this;
    }

    @Nonnull
    @Override
    public RedisEventLoop select(int key) {
        return this;
    }

    @Override
    protected void init() {
        connectSafely();
    }

    @Override
    protected void clean() {
        try {
            pipelineSync();
        } finally {
            closeConnection();
        }
    }

    @Override
    protected void loop() {
        while (true) {
            try {
                runTasksBatch(TASK_BATCH_SIZE);

                pipelineSync();

                checkConnection();

                if (confirmShutdown()) {
                    break;
                }

                // 等待以降低cpu利用率
                sleepQuietly(1);
            } catch (Throwable t) {
                logger.warn("loop caught exception", t);
            }
        }
    }

    /**
     * 刷新管道，为未获得结果的redis请求生成结果
     */
    private void pipelineSync() {
        if (waitResponseTasks.isEmpty()) {
            return;
        }

        try {
            if (pipeline != null) {
                pipeline.sync();
            }
        } catch (Throwable t) {
            logger.warn("pipeline.sync caught exception", t);

            closeConnection();
        } finally {
            generateResponses();
        }
    }

    /**
     * 检查连接是否可用
     */
    private void checkConnection() {
        if (pipeline == null) {
            // 当前连接不可用，尝试建立连接
            // 不在执行管道命令的时候进行连接，是为了避免处理任务过慢，导致任务大量堆积，从而产生过高的内存占用
            connectSafely();
        }
    }

    /**
     * 建立连接(从连接池获取连接)
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
     * 关闭连接
     */
    private void closeConnection() {
        // pipeline在关闭时会调用sync
        closeQuietly(pipeline);
        closeQuietly(jedis);

        pipeline = null;
        jedis = null;
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

    /**
     * 执行一个redis命令
     */
    public <V> void execute(RedisCommand<V> command) {
        // TODO 无promise实现
        final RedisPromise<V> redisPromise = newRedisPromise(this);
        execute(new JedisPipelineTask<>(command, redisPromise));
    }

    /**
     * 执行一个管道命令，并在完成之后，在指定线程中执行回调逻辑
     */
    public <V> void call(RedisCommand<V> command, GenericFutureResultListener<FutureResult<V>> listener, EventLoop appEventLoop) {
        // TODO 无promise实现
        final RedisPromise<V> redisPromise = newRedisPromise(appEventLoop);
        redisPromise.addListener(new FutureListenerAdapter<>(listener));
        execute(new JedisPipelineTask<>(command, redisPromise));
    }

    /**
     * 执行一个管道命令，并阻塞到执行完成。
     */
    public <V> V syncCall(RedisCommand<V> command, EventLoop appEventLoop) throws ExecutionException {
        final RedisPromise<V> redisPromise = newRedisPromise(appEventLoop);
        execute(new JedisPipelineTask<>(command, redisPromise));
        return redisPromise.join();
    }

    /**
     * 创建一个用于等待当前所有命令执行完毕的future。
     * 创建的future会在这之前的命令执行完毕之后得到通知。
     */
    RedisFuture<?> newWaitFuture(EventLoop appEventLoop) {
        final RedisPromise<?> redisPromise = newRedisPromise(appEventLoop);
        execute(new SyncTask(redisPromise));
        return redisPromise;
    }

    private <T> RedisPromise<T> newRedisPromise(EventLoop appEventLoop) {
        return new DefaultRedisPromise<>(this, appEventLoop);
    }

    /**
     * 普通的管道任务
     *
     * @param <V>
     */
    private class JedisPipelineTask<V> implements Runnable {
        private final RedisCommand<V> pipelineCmd;
        private final RedisPromise<V> redisPromise;

        private Response<V> dependency;
        private Throwable cause;

        JedisPipelineTask(RedisCommand<V> pipelineCmd, RedisPromise<V> redisPromise) {
            this.pipelineCmd = pipelineCmd;
            this.redisPromise = redisPromise;
        }

        @Override
        public void run() {
            waitResponseTasks.add(this);

            if (null == pipeline) {
                // 连接不可用，快速失败
                cause = RedisConnectionException.INSTANCE;
                checkSync();
                return;
            }

            try {
                dependency = pipelineCmd.execute(pipeline);
                checkSync();
            } catch (Throwable t) {
                cause = t;

                handleException(t);
            }
        }

        /**
         * 检查是否需要调用sync
         */
        private void checkSync() {
            if (waitResponseTasks.size() > MAX_WAIT_RESPONSE_TASK) {
                logger.warn("unexpected waitResponseTasks.size {}", waitResponseTasks.size());
            }

            if (waitResponseTasks.size() >= MAX_WAIT_RESPONSE_TASK) {
                pipelineSync();
            }
        }

        /**
         * 处理执行管道命令中出现的异常
         */
        private void handleException(Throwable t) {
            if (t instanceof JedisConnectionException) {
                // 关闭当前连接
                closeConnection();

                // 等同于调用sync
                generateResponses();

                // 尝试一次恢复连接
                connectSafely();
            } else {
                checkSync();
            }
        }
    }

    /**
     * 刷新管道的任务
     */
    private class SyncTask implements Runnable {

        private final RedisPromise<?> redisPromise;

        SyncTask(RedisPromise<?> redisPromise) {
            this.redisPromise = redisPromise;
        }

        @Override
        public void run() {
            try {
                pipelineSync();
            } finally {
                redisPromise.trySuccess(null);
            }
        }
    }
}
