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

import org.jctools.queues.MessagePassingQueue;
import org.jctools.queues.MpscArrayQueue;
import org.jctools.queues.MpscLinkedQueue;
import org.jctools.queues.MpscUnboundedArrayQueue;

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.LockSupport;

/**
 * 又一个不靠谱的队列性能测试（1s吞吐量）
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/5/31
 */
public class QueueTest2 {

    private static final int TASK_NUM = 1024 * 1024;
    private static final int runTimeMs = 1000;

    private static volatile boolean stop = false;

    public static void main(String[] args) {
        // -1 给消费者
        int threadNum = Runtime.getRuntime().availableProcessors() - 1;
        testQueue(threadNum, new ConcurrentLinkedQueue<>());
        testQueue(threadNum, new LinkedBlockingQueue<>());
        testQueue(threadNum, new ArrayBlockingQueue<>(TASK_NUM));

        testQueue(threadNum, new MpscArrayQueue<>(TASK_NUM));
        testQueue(threadNum, new MpscLinkedQueue<>());
        testQueue(threadNum, new MpscUnboundedArrayQueue<>(8192));
    }

    private static void testQueue(final int threadNum, Queue<String> queue) {
        final String name = queue.getClass().getSimpleName();

        final Producer[] producers = producers(threadNum, queue);
        final Consumer consumer = new Consumer(queue);

        startAndJoin(producers, consumer, runTimeMs);

        final long producerOffer = Arrays.stream(producers).mapToLong(p -> p.sequence).sum();
        final long consumerPoll = consumer.sequence;

        System.out.println(name + " producer " + threadNum + " consumer 1" + " runTimeMs " + runTimeMs
                + " producerOffer " + producerOffer + " consumerPoll " + consumerPoll);
    }

    private static void startAndJoin(Thread[] producers, Thread consumer, long runTimeMs) {
        Arrays.stream(producers)
                .forEach(Thread::start);
        consumer.start();

        LockSupport.parkNanos(1000_000 * runTimeMs);

        stop = true;

        Arrays.stream(producers)
                .forEach(QueueTest2::joinQuietly);
        joinQuietly(consumer);

        // 用于下次测试
        stop = false;
    }

    private static void joinQuietly(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException ignore) {

        }
    }

    private static Producer[] producers(int threadNum, Queue<String> queue) {
        final Producer[] threads = new Producer[threadNum];
        for (int index = 0; index < threadNum; index++) {
            threads[index] = new Producer(queue);
        }
        return threads;
    }

    private static class Producer extends Thread {

        private long sequence = 0;
        private final Queue<String> queue;

        private Producer(Queue<String> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            if (queue instanceof MessagePassingQueue) {
                @SuppressWarnings("unchecked") MessagePassingQueue<String> q = (MessagePassingQueue<String>) queue;
                while (!stop) {
                    if (q.relaxedOffer("")) {
                        sequence++;
                    }
                }
            } else {
                while (!stop) {
                    if (queue.offer("")) {
                        sequence++;
                    }
                }
            }
        }
    }

    private static class Consumer extends Thread {

        private long sequence = 0;
        private final Queue<String> queue;

        private Consumer(Queue<String> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            if (queue instanceof MessagePassingQueue) {
                @SuppressWarnings("unchecked") MessagePassingQueue<String> q = (MessagePassingQueue<String>) queue;
                while (!stop) {
                    if (q.relaxedPoll() != null) {
                        sequence++;
                    }
                }
            } else {
                while (!stop) {
                    if (queue.poll() != null) {
                        sequence++;
                    }
                }
            }
        }
    }

}
