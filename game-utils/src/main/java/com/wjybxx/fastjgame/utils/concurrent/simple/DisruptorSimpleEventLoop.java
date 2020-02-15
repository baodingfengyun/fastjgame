/*
 * Copyright 2019 wjybxx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wjybxx.fastjgame.utils.concurrent.simple;

import com.wjybxx.fastjgame.utils.CheckUtils;
import com.wjybxx.fastjgame.utils.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.utils.concurrent.RejectedExecutionHandler;
import com.wjybxx.fastjgame.utils.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.utils.concurrent.disruptor.DisruptorEventLoop;
import com.wjybxx.fastjgame.utils.concurrent.disruptor.WaitStrategyFactory;
import com.wjybxx.fastjgame.utils.concurrent.disruptor.YieldWaitStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;

/**
 * 基于{@link DisruptorEventLoop}的{@link SimpleEventLoop}实现，它用于有界任务队列
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/2
 * github - https://github.com/hl845740757
 */
public class DisruptorSimpleEventLoop extends DisruptorEventLoop implements SimpleEventLoop {

    private static final Logger logger = LoggerFactory.getLogger(DisruptorSimpleEventLoop.class);

    private final EventLoopHandler eventLoopHandler;

    private DisruptorSimpleEventLoop(@Nullable SimpleEventLoopGroup parent,
                                     @Nonnull ThreadFactory threadFactory,
                                     @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                                     int ringBufferSize, int taskBatchSize,
                                     @Nonnull WaitStrategyFactory waitStrategyFactory,
                                     @Nonnull EventLoopHandler eventLoopHandler) {
        super(parent, threadFactory, rejectedExecutionHandler, ringBufferSize, taskBatchSize, waitStrategyFactory);
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
    protected void loopOnce() throws Exception {
        try {
            eventLoopHandler.loopOnce();
        } catch (Throwable cause) {
            try {
                eventLoopHandler.onLoopOnceExceptionCaught(cause);
            } catch (Throwable unexpected) {
                cause.addSuppressed(unexpected);
                logger.warn("eventLoopHandler.onLoopOnceExceptionCaught caught exception", cause);
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

        private int ringBufferSize = 64 * 1024;
        private int taskBatchSize = 1024;

        private WaitStrategyFactory waitStrategyFactory = new YieldWaitStrategyFactory();

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

        public Builder setRingBufferSize(int ringBufferSize) {
            this.ringBufferSize = ringBufferSize;
            return this;
        }

        public Builder setTaskBatchSize(int taskBatchSize) {
            this.taskBatchSize = CheckUtils.requirePositive(taskBatchSize, "taskBatchSize");
            return this;
        }

        public Builder setWaitStrategyFactory(WaitStrategyFactory waitStrategyFactory) {
            this.waitStrategyFactory = Objects.requireNonNull(waitStrategyFactory, "waitStrategyFactory");
            return this;
        }

        public Builder setEventLoopHandler(@Nonnull EventLoopHandler eventLoopHandler) {
            this.eventLoopHandler = Objects.requireNonNull(eventLoopHandler, "eventLoopHandler");
            return this;
        }

        public SimpleEventLoop build() {
            return new DisruptorSimpleEventLoop(parent,
                    threadFactory, rejectedExecutionHandler,
                    ringBufferSize, taskBatchSize,
                    waitStrategyFactory,
                    eventLoopHandler);
        }
    }
}
