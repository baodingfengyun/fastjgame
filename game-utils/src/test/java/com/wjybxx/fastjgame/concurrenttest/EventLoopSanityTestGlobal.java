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
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.concurrent.GlobalEventLoop;
import com.wjybxx.fastjgame.utils.misc.LongHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.wjybxx.fastjgame.util.TestUtil.TEST_TIMEOUT;
import static com.wjybxx.fastjgame.util.TestUtil.sleepQuietly;

/**
 * {@link GlobalEventLoop}健壮性测试
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/6/2
 */
public class EventLoopSanityTestGlobal {

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

    /**
     * 测试单生产者下的先入先出
     */
    @Timeout(TEST_TIMEOUT)
    @Test
    void testSpFifo() {
        final AtomicBoolean stop = new AtomicBoolean();
        final LongHolder fail = new LongHolder();
        final LongHolder lastSequence = new LongHolder();
        final Thread producer = new SpFIFOProducer(stop, fail, lastSequence);

        TestUtil.startAndJoin(Collections.singletonList(producer), stop, 1000);

        Assertions.assertEquals(0, fail.get(), "Observed out of order");
    }

    private static class SpFIFOProducer extends Thread {

        final AtomicBoolean stop;
        final LongHolder fail;
        final LongHolder lastSequence;

        private SpFIFOProducer(AtomicBoolean stop, LongHolder fail, LongHolder lastSequence) {
            this.stop = stop;
            this.fail = fail;
            this.lastSequence = lastSequence;
        }

        @Override
        public void run() {
            final EventLoop eventLoop = GlobalEventLoop.INSTANCE;

            long sequence = 0;
            lastSequence.set(-1);

            while (!stop.get()) {
                try {
                    eventLoop.execute(new SpFIFOTask(sequence, fail, lastSequence));
                    sequence++;
                } catch (RejectedExecutionException ignore) {
                    sleepQuietly(1);
                }
            }
        }
    }

    private static class SpFIFOTask implements Runnable {

        long sequence;
        LongHolder fail;
        LongHolder lastSequence;

        SpFIFOTask(long sequence, LongHolder fail, LongHolder lastSequence) {
            this.sequence = sequence;
            this.fail = fail;
            this.lastSequence = lastSequence;
        }

        @Override
        public void run() {
            if (sequence != lastSequence.get() + 1) {
                fail.incAndGet();
            }
            lastSequence.set(sequence);
        }
    }

    /**
     * 测试多生产下各自的先入先出
     */
    @Timeout(TEST_TIMEOUT)
    @Test
    void testMpFifo() {
        final int producerNum = 4;

        // 必须使用abort策略，否则生产者无法感知失败
        final AtomicBoolean stop = new AtomicBoolean();
        final LongHolder fail = new LongHolder();
        final long[] lastSequences = new long[producerNum];

        final MpFIFOProducer[] producers = new MpFIFOProducer[producerNum];
        for (int index = 0; index < producerNum; index++) {
            producers[index] = new MpFIFOProducer(stop, index, fail, lastSequences);
        }

        TestUtil.startAndJoin(Arrays.asList(producers), stop, 1000);

        Assertions.assertEquals(0, fail.get(), "Observed out of order");
    }

    private static class MpFIFOProducer extends Thread {

        final AtomicBoolean stop;
        final int index;
        final LongHolder fail;
        final long[] lastSequences;

        private MpFIFOProducer(AtomicBoolean stop, int index, LongHolder fail, long[] lastSequences) {
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
                    eventLoop.execute(new MpFIFOTask(index, fail, lastSequences, sequence));
                    sequence++;
                } catch (RejectedExecutionException ignore) {
                    TestUtil.sleepQuietly(1);
                }
            }
        }
    }

    private static class MpFIFOTask implements Runnable {

        int index;
        LongHolder fail;
        long[] lastSequences;
        long sequence;

        MpFIFOTask(int index, LongHolder fail, long[] lastSequences, long sequence) {
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
}
