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

package com.wjybxx.fastjgame.concurrenttest;

import com.wjybxx.fastjgame.util.TestUtil;
import com.wjybxx.fastjgame.utils.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.utils.concurrent.disruptor.DisruptorEventLoop;
import com.wjybxx.fastjgame.utils.concurrent.disruptor.YieldWaitStrategyFactory;
import com.wjybxx.fastjgame.utils.concurrent.unbounded.TaskQueueFactory;
import com.wjybxx.fastjgame.utils.concurrent.unbounded.TemplateEventLoop;
import org.jctools.queues.MpscUnboundedArrayQueue;
import org.jctools.queues.MpscUnboundedXaddArrayQueue;
import org.junit.jupiter.api.Test;

/**
 * 测试1s吞吐量
 * <p>
 * Disruptor:
 * com.wjybxx.fastjgame.utils.concurrent.disruptor.DisruptorEventLoop, producerNum 2, count 17789527
 * com.wjybxx.fastjgame.utils.concurrent.disruptor.DisruptorEventLoop, producerNum 4, count 10767447
 * com.wjybxx.fastjgame.utils.concurrent.disruptor.DisruptorEventLoop, producerNum 8, count 9611073
 * </p>
 * Template(MpscUnboundedXaddArrayQueue):
 * com.wjybxx.fastjgame.utils.concurrent.unbounded.TemplateEventLoop, producerNum 2, count 40382012
 * com.wjybxx.fastjgame.utils.concurrent.unbounded.TemplateEventLoop, producerNum 4, count 50268831
 * com.wjybxx.fastjgame.utils.concurrent.unbounded.TemplateEventLoop, producerNum 8, count 30447050
 * </p>
 * Template(MpscUnboundedArrayQueue):
 * com.wjybxx.fastjgame.utils.concurrent.unbounded.TemplateEventLoop, producerNum 2, count 19304646
 * com.wjybxx.fastjgame.utils.concurrent.unbounded.TemplateEventLoop, producerNum 4, count 12144394
 * com.wjybxx.fastjgame.utils.concurrent.unbounded.TemplateEventLoop, producerNum 8, count 10351433
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/6/2
 */
public class EventLoopThroughputTest {

    private static EventLoop newDisruptorEventLoop() {
        return new DisruptorEventLoop(null, new DefaultThreadFactory("DISRUPTOR"),
                RejectedExecutionHandlers.discard(),
                new YieldWaitStrategyFactory(),
                1024 * 1024, 1024);
    }

    private static EventLoop newTemplateEventLoop(TaskQueueFactory factory) {
        return new TemplateEventLoop(null, new DefaultThreadFactory("TEMPLATE"),
                RejectedExecutionHandlers.discard(),
                new com.wjybxx.fastjgame.utils.concurrent.unbounded.YieldWaitStrategyFactory(),
                factory, 1024);
    }

    @Test
    void testDisruptor() {
        testOnce(2, newDisruptorEventLoop());
        testOnce(4, newDisruptorEventLoop());
        testOnce(8, newDisruptorEventLoop());
    }

    @Test
    void testTemplate1() {
        TaskQueueFactory factory = () -> new MpscUnboundedXaddArrayQueue<>(8192, 4);
        testOnce(2, newTemplateEventLoop(factory));
        testOnce(4, newTemplateEventLoop(factory));
        testOnce(8, newTemplateEventLoop(factory));
    }

    @Test
    void testTemplate2() {
        TaskQueueFactory factory = () -> new MpscUnboundedArrayQueue<>(1024 * 1024);
        testOnce(2, newTemplateEventLoop(factory));
        testOnce(4, newTemplateEventLoop(factory));
        testOnce(8, newTemplateEventLoop(factory));
    }

    private static void testOnce(int producerNum, EventLoop eventLoop) {
        try {
            final Thread[] producers = new Thread[producerNum];
            for (int index = 0; index < producerNum; index++) {
                producers[index] = new Producer(eventLoop);
            }
            TestUtil.startAndJoin(producers, eventLoop, 1000);
            System.out.println(eventLoop.getClass().getName() + ", producerNum " + producerNum + ", count " + ThroughputTask.count);
        } finally {
            ThroughputTask.count = 0;
        }
    }

    private static class Producer extends Thread {

        final EventLoop eventLoop;

        private Producer(EventLoop eventLoop) {
            this.eventLoop = eventLoop;
        }

        @Override
        public void run() {
            final Runnable runnable = ThroughputTask.INSTANCE;
            while (!eventLoop.isShuttingDown()) {
                eventLoop.execute(runnable);
            }
        }
    }


    private static class ThroughputTask implements Runnable {

        private static final Runnable INSTANCE = new ThroughputTask();

        private static long count;

        @Override
        public void run() {
            count++;
        }
    }

}
