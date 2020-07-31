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
import com.wjybxx.fastjgame.utils.concurrent.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.concurrent.GlobalEventLoop;
import com.wjybxx.fastjgame.utils.misc.LongHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Arrays;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.wjybxx.fastjgame.util.TestUtil.TEST_TIMEOUT;

/**
 * {@link GlobalEventLoop}健壮性测试
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/6/2
 */
public class EventLoopSanityTestGlobal {

    /**
     * 对于每一个生产者而言，都应该满足先入先出
     */
    @Timeout(TEST_TIMEOUT)
    @Test
    void testFifo() {
        final int producerNum = 4;

        final AtomicBoolean stop = new AtomicBoolean();
        final LongHolder fail = new LongHolder();
        final long[] lastSequences = new long[producerNum];

        final FIFOProducer[] producers = new FIFOProducer[producerNum];
        for (int index = 0; index < producerNum; index++) {
            producers[index] = new FIFOProducer(stop, index, fail, lastSequences);
        }

        TestUtil.startAndJoin(Arrays.asList(producers), stop, 1000);

        Assertions.assertEquals(0, fail.get(), "Observed out of order");
    }

    private static class FIFOProducer extends Thread {

        final AtomicBoolean stop;
        final int index;
        final LongHolder fail;
        final long[] lastSequences;

        private FIFOProducer(AtomicBoolean stop, int index, LongHolder fail, long[] lastSequences) {
            this.stop = stop;
            this.index = index;
            this.fail = fail;
            this.lastSequences = lastSequences;
        }

        @Override
        public void run() {
            final EventLoop eventLoop = GlobalEventLoop.INSTANCE;
            long sequence = 0;
            lastSequences[index] = -1;

            while (!stop.get()) {
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
        Assertions.assertThrows(UnsupportedOperationException.class, GlobalEventLoop.INSTANCE::shutdown);
    }

    @Test
    void testShutdownNow() {
        Assertions.assertThrows(UnsupportedOperationException.class, GlobalEventLoop.INSTANCE::shutdownNow);
    }

    @Test
    void testAutoShutdown() {
        GlobalEventLoop.INSTANCE.execute(ConcurrentUtils.NO_OP_TASK);
        Assertions.assertTrue(GlobalEventLoop.INSTANCE.isThreadAlive(), "GlobalEventLoop thread is not live");
        TestUtil.sleepQuietly(GlobalEventLoop.INSTANCE.getQuietPeriodMs() + 1000);
        Assertions.assertFalse(GlobalEventLoop.INSTANCE.isThreadAlive(), "GlobalEventLoop thread is alive");
    }

}
