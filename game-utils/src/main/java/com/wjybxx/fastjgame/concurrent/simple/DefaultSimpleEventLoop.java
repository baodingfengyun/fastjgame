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

package com.wjybxx.fastjgame.concurrent.simple;

import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandler;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.concurrent.SingleThreadEventLoop;
import com.wjybxx.fastjgame.utils.CheckUtils;
import com.wjybxx.fastjgame.utils.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * 基于{@link SingleThreadEventLoop}的{@link SimpleEventLoop}实现，它用于无界任务队列。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/2
 * github - https://github.com/hl845740757
 */
public class DefaultSimpleEventLoop extends SingleThreadEventLoop implements SimpleEventLoop {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSimpleEventLoop.class);

    private final int taskBatchSize;
    private final long sleepTimeNs;
    private final EventLoopHandler eventLoopHandler;

    private DefaultSimpleEventLoop(@Nullable SimpleEventLoopGroup parent,
                                   @Nonnull ThreadFactory threadFactory,
                                   @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                                   int taskBatchSize, long sleepTimeNs,
                                   @Nonnull EventLoopHandler eventLoopHandler) {
        super(parent, threadFactory, rejectedExecutionHandler);
        this.taskBatchSize = taskBatchSize;
        this.sleepTimeNs = sleepTimeNs;
        this.eventLoopHandler = Objects.requireNonNull(eventLoopHandler);
    }

    @Nonnull
    @Override
    public SimpleEventLoop next() {
        return this;
    }

    @Nonnull
    @Override
    public SimpleEventLoop select(int key) {
        return this;
    }

    @Nullable
    @Override
    public SimpleEventLoopGroup parent() {
        return (SimpleEventLoopGroup) super.parent();
    }

    @Override
    protected void init() throws Exception {
        eventLoopHandler.init(this);
    }

    @Override
    protected void clean() throws Exception {
        eventLoopHandler.clean();
    }

    @Override
    protected void loop() {
        while (true) {
            try {
                runTasksBatch(taskBatchSize);

                eventLoopHandler.loopOnce();

                if (confirmShutdown()) {
                    break;
                }

                LockSupport.parkNanos(sleepTimeNs);
            } catch (Throwable cause) {
                try {
                    eventLoopHandler.onLoopOnceExceptionCaught(cause);
                } catch (Throwable unexpected) {
                    cause.addSuppressed(unexpected);
                    logger.warn("eventLoopHandler.onLoopOnceExceptionCaught caught exception", cause);
                }
            }
        }
    }

    @Override
    protected void wakeUp() {
        eventLoopHandler.wakeUpEventLoop(this, this::interruptThread);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private SimpleEventLoopGroup parent;
        private ThreadFactory threadFactory = new DefaultThreadFactory("DefaultSimpleEventLoop");
        private RejectedExecutionHandler rejectedExecutionHandler = RejectedExecutionHandlers.abort();

        private int taskBatchSize = 1024;
        private long sleepTimeNs = TimeUtils.NANO_PER_MILLISECOND;

        private EventLoopHandler eventLoopHandler;

        private Builder() {
        }

        public Builder setParent(@Nullable SimpleEventLoopGroup parent) {
            this.parent = parent;
            return this;
        }

        public Builder setThreadFactory(@Nonnull ThreadFactory threadFactory) {
            this.threadFactory = Objects.requireNonNull(threadFactory, "threadFactory");
            return this;
        }

        public Builder setRejectedExecutionHandler(@Nonnull RejectedExecutionHandler rejectedExecutionHandler) {
            this.rejectedExecutionHandler = Objects.requireNonNull(rejectedExecutionHandler, "rejectedExecutionHandler");
            return this;
        }

        public Builder setTaskBatchSize(int taskBatchSize) {
            this.taskBatchSize = CheckUtils.requirePositive(taskBatchSize, "taskBatchSize");
            return this;
        }

        public Builder setSleepTime(int sleepTime, @Nonnull TimeUnit timeUnit) {
            this.sleepTimeNs = timeUnit.toNanos(sleepTime);
            return this;
        }

        public Builder setEventLoopHandler(@Nonnull EventLoopHandler eventLoopHandler) {
            this.eventLoopHandler = Objects.requireNonNull(eventLoopHandler, "eventLoopHandler");
            return this;
        }

        public SimpleEventLoop build() {
            return new DefaultSimpleEventLoop(parent,
                    threadFactory, rejectedExecutionHandler,
                    taskBatchSize,
                    sleepTimeNs,
                    eventLoopHandler);
        }
    }
}
