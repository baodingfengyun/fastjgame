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

package com.wjybxx.fastjgame.queue;

import org.jctools.queues.MessagePassingQueue;
import org.jctools.queues.MpmcUnboundedXaddArrayQueue;

import java.util.concurrent.locks.LockSupport;

/**
 * MpmcUnboundedXaddArrayQueue peek乱序测试用例
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/6/2
 */
public class MpmcUnboundedXaddArrayQueuePeekTest {

    private static final int chunkSize = 16;
    private static final int poolSize = 4;
    private static final int capacity = chunkSize * poolSize;

    private static volatile boolean stop = false;

    public static void main(String[] args) throws InterruptedException {
        MessagePassingQueue<Long> messageQueue = new MpmcUnboundedXaddArrayQueue<>(chunkSize, poolSize);
        new Producer(messageQueue, 600).start();
        new Consumer(messageQueue, 1000).start();
        new Peeker(messageQueue).start();

        try {
            Thread.sleep(10 * 1000);
        } finally {
            stop = true;
        }
    }

    private static class Producer extends Thread {

        final MessagePassingQueue<Long> messageQueue;
        final long sleepNanos;

        long sequence = 0;

        Producer(MessagePassingQueue<Long> messageQueue, long sleepNanos) {
            this.messageQueue = messageQueue;
            this.sleepNanos = sleepNanos;
        }

        @Override
        public void run() {
            while (!stop) {
                if (messageQueue.size() >= capacity - chunkSize) {
                    LockSupport.parkNanos(sleepNanos);
                    continue;
                }

                if (messageQueue.offer(sequence)) {
                    sequence++;
                }
            }
        }
    }

    private static class Consumer extends Thread {

        final MessagePassingQueue<Long> messageQueue;
        final long sleepNanos;

        private Consumer(MessagePassingQueue<Long> messageQueue, long sleepNanos) {
            this.messageQueue = messageQueue;
            this.sleepNanos = sleepNanos;
        }

        @Override
        public void run() {
            while (!stop) {
                messageQueue.poll();

                if (messageQueue.size() < chunkSize) {
                    LockSupport.parkNanos(sleepNanos);
                }
            }
        }

    }

    private static class Peeker extends Thread {

        final MessagePassingQueue<Long> messageQueue;
        long lastPeekedSequence;

        private Peeker(MessagePassingQueue<Long> messageQueue) {
            this.messageQueue = messageQueue;
            setPriority(MIN_PRIORITY);
        }

        @Override
        public void run() {
            while (!stop) {
                final Long peekedSequence = messageQueue.peek();
                if (peekedSequence == null) {
                    continue;
                }

                if (peekedSequence < lastPeekedSequence) {
                    String msg = String.format("peekedSequence %s, lastPeekedSequence %s", peekedSequence, lastPeekedSequence);
                    throw new IllegalStateException(msg);
                }

                lastPeekedSequence = peekedSequence;
            }
        }
    }
}
