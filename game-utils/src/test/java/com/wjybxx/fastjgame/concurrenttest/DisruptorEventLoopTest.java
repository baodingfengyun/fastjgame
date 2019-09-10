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

package com.wjybxx.fastjgame.concurrenttest;

import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.concurrent.disruptor.DisruptorEventLoop;
import com.wjybxx.fastjgame.concurrent.disruptor.DisruptorEventLoopGroup;
import com.wjybxx.fastjgame.concurrent.disruptor.Event;
import com.wjybxx.fastjgame.concurrent.disruptor.EventHandler;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/27
 * github - https://github.com/hl845740757
 */
public class DisruptorEventLoopTest {

    public static void main(String[] args) {
        DisruptorEventLoopGroup eventLoopGroup = new DisruptorEventLoopGroup(new DefaultThreadFactory("Disruptor-Thread"),
                RejectedExecutionHandlers.abort(),
                Collections.singletonList(new DisruptorEventLoopGroup.BuildContext(new EventHandlerImp())));

        DisruptorEventLoop eventLoop = eventLoopGroup.next();

        eventLoop.publishEvent(1, new StringEventParam("hello world!"));

        eventLoop.terminationFuture().awaitUninterruptibly(5, TimeUnit.SECONDS);

        eventLoop.shutdown();

        eventLoop.terminationFuture().awaitUninterruptibly();
    }

    private static class EventHandlerImp implements EventHandler {

        @Override
        public void startUp(DisruptorEventLoop eventLoop) {
            System.out.println("startUp");
        }

        @Override
        public void onEvent(Event event) throws Exception {
            System.out.println("onEvent, EventType = " + event.getType() + ", eventParam = " + event.getParam());
        }

        @Override
        public void onWaitEvent() {
            System.out.println("onWaitEvent");
        }

        @Override
        public void shutdown() {
            System.out.println("shutdown");
        }
    }

    private static class StringEventParam {

        private final String param;

        private StringEventParam(String param) {
            this.param = param;
        }

        @Override
        public String toString() {
            return "StringEventParam{" +
                    "param='" + param + '\'' +
                    '}';
        }
    }
}
