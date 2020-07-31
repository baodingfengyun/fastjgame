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
import com.wjybxx.fastjgame.util.concurrent.ConcurrentUtils;
import com.wjybxx.fastjgame.util.concurrent.EventLoop;
import com.wjybxx.fastjgame.util.concurrent.RejectedExecutionHandler;
import com.wjybxx.fastjgame.util.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.util.misc.LongHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Arrays;
import java.util.concurrent.RejectedExecutionException;

import static com.wjybxx.fastjgame.util.TestUtil.TEST_TIMEOUT;

/**
 * EventLoop健壮性测试
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/6/2
 */
public abstract class EventLoopSanityTest {

    abstract EventLoop newEventLoop(RejectedExecutionHandler rejectedExecutionHandler);

    /**
     * 对于每一个生产者而言，都应该满足先入先出
     */
    @Timeout(TEST_TIMEOUT)
    @Test
    void testFifo() {
        final int producerNum = 4;

        // 必须使用abort策略，否则生产者无法感知失败
        final EventLoop eventLoop = newEventLoop(RejectedExecutionHandlers.abort());
        final LongHolder fail = new LongHolder();
        final long[] lastSequences = new long[producerNum];

        final FIFOProducer[] producers = new FIFOProducer[producerNum];
        for (int index = 0; index < producerNum; index++) {
            producers[index] = new FIFOProducer(eventLoop, index, fail, lastSequences);
        }

        TestUtil.startAndJoin(Arrays.asList(producers), eventLoop, 1000);

        Assertions.assertEquals(0, fail.get(), "Observed out of order");
    }

    private static class FIFOProducer extends Thread {

        final EventLoop eventLoop;
        final int index;
        final LongHolder fail;
        final long[] lastSequences;

        private FIFOProducer(EventLoop eventLoop, int index, LongHolder fail, long[] lastSequences) {
            this.eventLoop = eventLoop;
            this.index = index;
            this.fail = fail;
            this.lastSequences = lastSequences;
        }

        @Override
        public void run() {
            long sequence = 0;
            lastSequences[index] = -1;

            while (!eventLoop.isShuttingDown()) {
                try {
                    eventLoop.execute(new FIFOTask(fail, lastSequences, index, sequence));
                    sequence++;
                } catch (RejectedExecutionException ignore) {
                    TestUtil.sleepQuietly(1);
                }
            }
        }
    }

    private static class FIFOTask implements Runnable {

        LongHolder fail;
        long[] lastSequences;

        int index;
        long sequence;

        FIFOTask(LongHolder fail, long[] lastSequences, int index, long sequence) {
            this.index = index;
            this.sequence = sequence;
            this.fail = fail;
            this.lastSequences = lastSequences;
        }

        @Override
        public void run() {
            if (sequence != lastSequences[index] + 1) {
                fail.incAndGet();
            }
            lastSequences[index] = sequence;
        }
    }

    @Test
    void testShutdown() {
        final EventLoop eventLoop = newEventLoop(RejectedExecutionHandlers.abort());
        eventLoop.shutdown();
        Assertions.assertTrue(eventLoop.isShuttingDown(), "shutdown invoked, but state is not shuttingDone");
    }

    @Test
    void testShutdownNow() {
        final EventLoop eventLoop = newEventLoop(RejectedExecutionHandlers.abort());
        eventLoop.shutdownNow();
        Assertions.assertTrue(eventLoop.isShutdown(), "shutdownNow invoked, but state is not shutdown");
    }

    @Test
    void testTerminated() {
        final EventLoop eventLoop = newEventLoop(RejectedExecutionHandlers.abort());
        eventLoop.shutdownNow();
        eventLoop.terminationFuture().join();
        Assertions.assertTrue(eventLoop.isTerminated(), "join completed, but eventLoop is not terminated");
    }

    @Test
    void testReject() {
        final EventLoop eventLoop = newEventLoop(RejectedExecutionHandlers.abort());
        eventLoop.shutdown();
        Assertions.assertThrows(RejectedExecutionException.class, () -> eventLoop.execute(ConcurrentUtils.NO_OP_TASK),
                "shutdown invoked, but the task was not rejected");
    }

}
