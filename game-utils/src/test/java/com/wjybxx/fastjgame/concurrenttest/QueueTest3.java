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

/**
 * 一个不靠谱的队列性能测试
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/5/31
 */
public class QueueTest3 {

    private static final int TASK_NUM = 1024 * 1024;

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

        final Thread[] producers = producers(threadNum, () -> {
            if (queue instanceof MessagePassingQueue) {
                @SuppressWarnings("unchecked") MessagePassingQueue<String> q = (MessagePassingQueue<String>) queue;
                for (int i = 0; i < TASK_NUM; ) {
                    if (q.relaxedOffer("")) {
                        i++;
                    }
                }
            } else {
                for (int i = 0; i < TASK_NUM; ) {
                    if (queue.offer("")) {
                        i++;
                    }
                }
            }
        });

        final Thread consumer = new Thread(() -> {
            int remainTask = threadNum * TASK_NUM;

            if (queue instanceof MessagePassingQueue) {
                MessagePassingQueue<?> q = (MessagePassingQueue<?>) queue;
                do {
                    if (q.relaxedPoll() != null) {
                        remainTask--;
                    }
                } while (remainTask > 0);
            } else {
                do {
                    if (queue.poll() != null) {
                        remainTask--;
                    }
                } while (remainTask > 0);
            }
        });


        final long startTime = System.currentTimeMillis();
        startAndJoin(producers, consumer);
        final long endTime = System.currentTimeMillis();
        System.out.println(name + " producer " + threadNum + " consumer 1" + " cost timeMs " + (endTime - startTime));
    }

    private static void startAndJoin(Thread[] producers, Thread consumer) {
        Arrays.stream(producers)
                .forEach(Thread::start);
        consumer.start();

        Arrays.stream(producers)
                .forEach(QueueTest3::joinQuietly);
        joinQuietly(consumer);
    }

    private static void joinQuietly(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException ignore) {

        }
    }

    private static Thread[] producers(int threadNum, Runnable runnable) {
        final Thread[] threads = new Thread[threadNum];
        for (int index = 0; index < threadNum; index++) {
            threads[index] = new Thread(runnable);
        }
        return threads;
    }

}
