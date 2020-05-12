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

import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.MathUtils;
import com.wjybxx.fastjgame.utils.TimeUtils;
import com.wjybxx.fastjgame.utils.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.utils.concurrent.disruptor.DisruptorEventLoop;
import com.wjybxx.fastjgame.utils.concurrent.unbounded.UnboundedEventLoop;
import com.wjybxx.fastjgame.utils.concurrent.unbounded.YieldWaitStrategyFactory;

import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

/**
 * 一个简单的不靠谱测试
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/5/11
 */
public class QueueTest {

    public static void main(String[] args) throws InterruptedException {
        final int consumerNum = 8;
        final int maxLoop = 10 * 10000;

        final EventLoop consumer =
//                newUnboundEventLoop();
                newDisruptorEventLoop(consumerNum * maxLoop);
        // 先启动
        consumer.execute(ConcurrentUtils.NO_OP_TASK);

        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch stopLatch = new CountDownLatch(consumerNum);

        IntStream.range(0, consumerNum)
                .mapToObj(i -> new Worker(consumer, maxLoop, startLatch, stopLatch))
                .forEach(Thread::start);

        startLatch.countDown();

        final long startTimeNano = System.nanoTime();
        stopLatch.await();
        consumer.shutdown();
        consumer.terminationFuture().awaitUninterruptibly();

        final long costTimeNano = System.nanoTime() - startTimeNano;
        final long costTimeMs = costTimeNano / TimeUtils.NANO_PER_MILLISECOND;
        System.out.println("costTimeMs: " + costTimeMs);
    }

    private static UnboundedEventLoop newUnboundEventLoop() {
        return new UnboundedEventLoop(null, new DefaultThreadFactory("CONSUMER"),
                RejectedExecutionHandlers.abort(), new YieldWaitStrategyFactory());
    }

    private static DisruptorEventLoop newDisruptorEventLoop(int minQueueSize) {
        return new DisruptorEventLoop(null, new DefaultThreadFactory("CONSUMER"),
                RejectedExecutionHandlers.abort(),
                MathUtils.roundToPowerOfTwo(minQueueSize), 8192,
                new com.wjybxx.fastjgame.utils.concurrent.disruptor.YieldWaitStrategyFactory());
    }

    private static class Worker extends Thread {

        private final EventLoop consumer;
        private final int maxLoop;
        private final CountDownLatch startLatch;
        private final CountDownLatch stopLatch;

        private Worker(EventLoop consumer, int maxLoop, CountDownLatch startLatch, CountDownLatch stopLatch) {
            this.consumer = consumer;
            this.maxLoop = maxLoop;
            this.startLatch = startLatch;
            this.stopLatch = stopLatch;
        }

        @Override
        public void run() {
            try {
                startLatch.await();

                final int max = maxLoop;
                for (int index = 0; index < max; index++) {
                    consumer.execute(ConcurrentUtils.NO_OP_TASK);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                stopLatch.countDown();
            }
        }
    }
}
