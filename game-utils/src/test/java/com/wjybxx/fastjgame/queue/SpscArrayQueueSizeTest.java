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
import org.jctools.queues.SpscArrayQueue;

import java.util.concurrent.locks.LockSupport;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/5/23
 */
public class SpscArrayQueueSizeTest {

    private static volatile boolean stop = false;

    public static void main(String[] args) throws InterruptedException {
        MessagePassingQueue<String> messageQueue = new SpscArrayQueue<>(8);

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
            while (!stop) {
                messageQueue.offer(Long.toString(sequence++));
                int size = messageQueue.size();
                int capacity = messageQueue.capacity();
                if (size > capacity) {
                    throw new IllegalStateException("size : " + size + ", capacity : " + capacity);
                }
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
                LockSupport.parkNanos(1);
            }
        }

    }
}
