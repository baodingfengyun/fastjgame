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
import org.jctools.queues.MpscArrayQueue;
import org.jctools.queues.SpscLinkedQueue;

/**
 * 看{@link SpscLinkedQueue#isEmpty()}源码的时候总觉得有bug，写了个测试，哎，真的有bug
 * 测试单消费者队列的isEmpty约束
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/5/16
 */
public class SCMessagePassingQueueTest {

    private static volatile boolean stop = false;

    public static void main(String[] args) throws InterruptedException {
        MessagePassingQueue<String> messageQueue = new MpscArrayQueue<>(8);

        new Producer(messageQueue).start();
        new Producer(messageQueue).start();

        new Consumer(messageQueue).start();

        try {
            Thread.sleep(5 * 1000);
        } finally {
            stop = true;
        }
    }

    private static class Producer extends Thread {

        final MessagePassingQueue<String> messageQueue;

        long sequence = 0;

        Producer(MessagePassingQueue<String> messageQueue) {
            this.messageQueue = messageQueue;
        }

        @Override
        public void run() {
            try {
                while (!stop) {
                    messageQueue.offer(Long.toString(sequence++));
                    Thread.sleep(1);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static class Consumer extends Thread {

        final MessagePassingQueue<String> messageQueue;

        private Consumer(MessagePassingQueue<String> messageQueue) {
            this.messageQueue = messageQueue;
        }

        @Override
        public void run() {
            while (!stop) {
                messageQueue.poll();

                if (!messageQueue.isEmpty()) {
                    final String e = messageQueue.poll();
                    if (null == e) {
                        throw new Error("MessageQueue.isEmpty() is false, messageQueue.poll() return null! Queue " + messageQueue.getClass().getSimpleName());
                    }
                }
            }
        }

    }
}
