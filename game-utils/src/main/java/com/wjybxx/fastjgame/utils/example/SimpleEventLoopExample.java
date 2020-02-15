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

package com.wjybxx.fastjgame.utils.example;

import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.TimeUtils;
import com.wjybxx.fastjgame.utils.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.concurrent.RejectedExecutionHandler;
import com.wjybxx.fastjgame.utils.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.utils.concurrent.disruptor.SleepWaitStrategyFactory;
import com.wjybxx.fastjgame.utils.concurrent.simple.*;

import javax.annotation.Nonnull;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * {@link SimpleEventLoop}的一个简单使用示例
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/3
 * github - https://github.com/hl845740757
 */
public class SimpleEventLoopExample {

    public static void main(String[] args) throws InterruptedException {
        final DefaultSimpleEventLoopGroup eventLoopGroup = new DefaultSimpleEventLoopGroup(2,
                new DefaultThreadFactory(""), RejectedExecutionHandlers.abort(),
                new SimpleEventLoopFactoryExample());

        eventLoopGroup.forEach(eventLoop -> eventLoop.execute(ConcurrentUtils.NO_OP_TASK));

        Thread.sleep(TimeUtils.MIN);
        eventLoopGroup.shutdown();
        eventLoopGroup.terminationFuture().awaitUninterruptibly();
    }

    private static class SimpleEventLoopFactoryExample implements SimpleEventLoopFactory {

        @Nonnull
        @Override
        public SimpleEventLoop newInstance(@Nonnull SimpleEventLoopGroup parent, @Nonnull ThreadFactory threadFactory, @Nonnull RejectedExecutionHandler rejectedExecutionHandler) {
            return DisruptorSimpleEventLoop.newBuilder()
                    .setParent(parent)
                    .setThreadFactory(threadFactory)
                    .setRejectedExecutionHandler(rejectedExecutionHandler)
                    .setEventLoopHandler(new EventLoopHandlerExample())
                    .setWaitStrategyFactory(new SleepWaitStrategyFactory(1, TimeUnit.MILLISECONDS))
                    .build();
        }
    }

    private static class EventLoopHandlerExample implements EventLoopHandler {

        private EventLoop eventLoop;

        @Override
        public void init(EventLoop eventLoop) throws Exception {
            this.eventLoop = eventLoop;
        }

        @Override
        public void loopOnce() throws Exception {
            System.out.println("LoopOnce : " + Thread.currentThread().getName() + ", time : " + System.currentTimeMillis());
        }

        @Override
        public void clean() throws Exception {

        }

        @Override
        public void wakeUpEventLoop(EventLoop eventLoop, ThreadInterrupter interrupter) {
            interrupter.interrupt();
        }

        @Override
        public void onLoopOnceExceptionCaught(Throwable cause) {
            if (cause instanceof OutOfMemoryError) {
                eventLoop.shutdownNow();
            } else {
                cause.printStackTrace();
            }
        }
    }
}
