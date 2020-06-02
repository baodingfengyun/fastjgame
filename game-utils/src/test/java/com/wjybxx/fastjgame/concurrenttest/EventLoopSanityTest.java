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
import org.junit.jupiter.api.Test;

import java.util.concurrent.RejectedExecutionException;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/6/2
 */
public abstract class EventLoopSanityTest {

    private static final ThreadLocal<Long> fifoLastSequence = new ThreadLocal<>();

    abstract EventLoop newEventLoop(RejectedExecutionHandler rejectedExecutionHandler);

    @Test
    void testFifo() {
        final EventLoop eventLoop = newEventLoop(RejectedExecutionHandlers.abort());
        final Thread producer = new FIFOProducer(eventLoop);
        TestUtil.startAndJoin(producer, eventLoop, 1000);
    }

    private static class FIFOProducer extends Thread {

        private final EventLoop eventLoop;

        private FIFOProducer(EventLoop eventLoop) {
            this.eventLoop = eventLoop;
        }

        @Override
        public void run() {
            long sequence = 0;
            while (!eventLoop.isShutdown()) {
                try {
                    eventLoop.execute(new FIFOTask(sequence));
                    sequence++;
                } catch (RejectedExecutionException ignore) {
                    TestUtil.sleepQuietly(1);
                }
            }
        }
    }

    private static class FIFOTask implements Runnable {

        long sequence;

        FIFOTask(long sequence) {
            this.sequence = sequence;
        }

        @Override
        public void run() {
            final Long lastSequence = fifoLastSequence.get();
            if (lastSequence == null) {
                fifoLastSequence.set(sequence);
                return;
            }

            if (sequence != lastSequence + 1) {
                throw new IllegalStateException("sequence " + sequence + ", lastSequence " + lastSequence);
            }

            fifoLastSequence.set(sequence);
        }
    }

}
