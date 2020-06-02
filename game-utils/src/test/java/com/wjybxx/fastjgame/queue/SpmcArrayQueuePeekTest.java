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
import org.jctools.queues.SpmcArrayQueue;

/**
 * 看{@link SpmcArrayQueue}和{@link org.jctools.queues.MpmcArrayQueue}的源码的时候发现peek有bug，
 * 因此写测试用例看能否复现，在当前测试用例下几乎必现。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/5/21
 * github - https://github.com/hl845740757
 */
public class SpmcArrayQueuePeekTest {

    private static volatile boolean stop = false;

    public static void main(String[] args) throws InterruptedException {
        // Smaller capacity helps test
        MessagePassingQueue<Long> messageQueue = new SpmcArrayQueue<>(8);
        new Producer(messageQueue).start();
        new Consumer(messageQueue).start();
        new Peeker(messageQueue).start();

        try {
            Thread.sleep(10 * 1000);
        } finally {
            stop = true;
        }
    }

    private static class Producer extends Thread {

        final MessagePassingQueue<Long> messageQueue;

        long sequence = 0;

        Producer(MessagePassingQueue<Long> messageQueue) {
            this.messageQueue = messageQueue;
        }

        @Override
        public void run() {
            while (!stop) {
                if (messageQueue.offer(sequence)) {
                    sequence++;
                }
            }
        }
    }

    private static class Consumer extends Thread {

        final MessagePassingQueue<Long> messageQueue;

        private Consumer(MessagePassingQueue<Long> messageQueue) {
            this.messageQueue = messageQueue;
        }

        @Override
        public void run() {
            while (!stop) {
                messageQueue.poll();
                // wait producer fill
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private static class Peeker extends Thread {

        final MessagePassingQueue<Long> messageQueue;
        long lastPeekedSequence;

        private Peeker(MessagePassingQueue<Long> messageQueue) {
            this.messageQueue = messageQueue;
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