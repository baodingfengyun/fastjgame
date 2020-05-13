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
import com.wjybxx.fastjgame.utils.concurrent.unbounded.TemplateEventLoop;
import com.wjybxx.fastjgame.utils.concurrent.unbounded.YieldWaitStrategyFactory;

import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

/**
 * 一个简单的不靠谱测试(测试的时候修改了{@link DisruptorEventLoop}和{@link TemplateEventLoop}的一些实现)
 * 以消费者Pool消费100W个任务为基础，数据如下(单位:毫秒):
 * MpscArrayQueue: 105-110 非常稳定
 * Disruptor:  极致110  但更多落在130~140
 * ConcurrentLinkedQueue 极致130 但更多落在140-150
 * MpscLinkedQueue：极致45~50 但更多落在300+  这个差别太大，还没较仔细的研究，猜测是JVM干了什么特殊的优化导致的....(Debug模式表现稳定，极致性能，但是Run模式下不稳定)
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/5/11
 */
public class QueueTest {

    private static volatile boolean stop = false;

    public static void main(String[] args) throws InterruptedException {
        final int producerNum = 8;

        final EventLoop consumer =
                newUnboundEventLoop();
//                newDisruptorEventLoop(100_0000 / producerNum);

        // 先启动
        consumer.execute(ConcurrentUtils.NO_OP_TASK);

        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch stopLatch = new CountDownLatch(producerNum);

        IntStream.range(0, producerNum)
                .mapToObj(i -> new Producer(consumer, startLatch, stopLatch))
                .forEach(Thread::start);

        startLatch.countDown();

        final long startTimeNano = System.nanoTime();
        consumer.terminationFuture().awaitUninterruptibly();

        final long costTimeNano = System.nanoTime() - startTimeNano;
        final long costTimeMs = costTimeNano / TimeUtils.NANO_PER_MILLISECOND;
        System.out.println("costTimeMs: " + costTimeMs);

        stop = true;
        stopLatch.await();
    }

    private static TemplateEventLoop newUnboundEventLoop() {
        return new TemplateEventLoop(null, new DefaultThreadFactory("CONSUMER"),
                RejectedExecutionHandlers.discard(),
                new YieldWaitStrategyFactory());
    }

    private static DisruptorEventLoop newDisruptorEventLoop(int minQueueSize) {
        return new DisruptorEventLoop(null, new DefaultThreadFactory("CONSUMER"),
                RejectedExecutionHandlers.discard(),
                MathUtils.roundToPowerOfTwo(minQueueSize), 8192,
                new com.wjybxx.fastjgame.utils.concurrent.disruptor.YieldWaitStrategyFactory());
    }

    private static class Producer extends Thread {

        private final EventLoop consumer;
        private final CountDownLatch startLatch;
        private final CountDownLatch stopLatch;

        private Producer(EventLoop consumer, CountDownLatch startLatch, CountDownLatch stopLatch) {
            this.consumer = consumer;
            this.startLatch = startLatch;
            this.stopLatch = stopLatch;
        }

        @Override
        public void run() {
            try {
                startLatch.await();

                do {
                    for (int index = 0; index < 8192; index++) {
                        consumer.execute(ConcurrentUtils.NO_OP_TASK);
                    }
                } while (!stop);

            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                stopLatch.countDown();
            }
        }
    }
}
