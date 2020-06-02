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
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.concurrent.RejectedExecutionHandler;
import com.wjybxx.fastjgame.utils.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.utils.misc.LongHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.RejectedExecutionException;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/6/2
 */
public abstract class EventLoopSanityTest {

    abstract EventLoop newEventLoop(RejectedExecutionHandler rejectedExecutionHandler);

    @Test
    void testFifo() {
        // 必须使用abort策略，否则生产者无法感知失败
        final EventLoop eventLoop = newEventLoop(RejectedExecutionHandlers.abort());
        final LongHolder fail = new LongHolder();
        final LongHolder lastSequence = new LongHolder();

        final Thread producer = new FIFOProducer(eventLoop, fail, lastSequence);

        TestUtil.startAndJoin(producer, eventLoop, 1000);

        Assertions.assertEquals(0, fail.get(), "Observed out of order");
    }

    private static class FIFOProducer extends Thread {

        private final EventLoop eventLoop;
        private final LongHolder fail;
        private final LongHolder lastSequence;

        private FIFOProducer(EventLoop eventLoop, LongHolder fail, LongHolder lastSequence) {
            this.eventLoop = eventLoop;
            this.fail = fail;
            this.lastSequence = lastSequence;
        }

        @Override
        public void run() {
            long sequence = 0;
            lastSequence.set(-1);

            while (!eventLoop.isShutdown()) {
                try {
                    eventLoop.execute(new FIFOTask(sequence, fail, lastSequence));
                    sequence++;
                } catch (RejectedExecutionException ignore) {
                    TestUtil.sleepQuietly(1);
                }
            }
        }
    }

    private static class FIFOTask implements Runnable {

        long sequence;
        LongHolder fail;
        LongHolder lastSequence;

        FIFOTask(long sequence, LongHolder fail, LongHolder lastSequence) {
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

}
