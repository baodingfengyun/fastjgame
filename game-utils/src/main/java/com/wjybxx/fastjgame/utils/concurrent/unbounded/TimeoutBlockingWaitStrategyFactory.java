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

package com.wjybxx.fastjgame.utils.concurrent.unbounded;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/23
 */
public class TimeoutBlockingWaitStrategyFactory implements WaitStrategyFactory {

    private static final int DEFAULT_TIMEOUT_MS = com.wjybxx.fastjgame.utils.concurrent.disruptor.TimeoutBlockingWaitStrategyFactory.DEFAULT_TIMEOUT_MS;

    private final int timeout;
    private final TimeUnit timeUnit;

    public TimeoutBlockingWaitStrategyFactory() {
        this(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    public TimeoutBlockingWaitStrategyFactory(int timeout, TimeUnit timeUnit) {
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    @Override
    public WaitStrategy newInstance() {
        return new TimeoutBlockingWaitStrategy(timeout, timeUnit);
    }

    private static class TimeoutBlockingWaitStrategy implements WaitStrategyFactory.WaitStrategy {

        private final Lock lock = new ReentrantLock();
        private final Condition processorNotifyCondition = lock.newCondition();
        /**
         * 是否需要通知，可以减少不必要是锁申请和通知
         */
        private final AtomicBoolean signalNeeded = new AtomicBoolean(false);
        private final long timeoutInNanos;

        TimeoutBlockingWaitStrategy(final long timeout, final TimeUnit units) {
            timeoutInNanos = units.toNanos(timeout);
        }

        @Override
        public void waitFor(UnboundedEventLoop eventLoop) throws ShuttingDownException, TimeoutException, InterruptedException {
            if (eventLoop.isTaskQueueEmpty()) {
                // 阻塞式等待生产者消费
                long nanos = timeoutInNanos;
                lock.lock();
                try {
                    while (eventLoop.isTaskQueueEmpty()) {
                        eventLoop.checkShuttingDown();

                        signalNeeded.set(true);
                        try {
                            nanos = processorNotifyCondition.awaitNanos(nanos);
                            // 正确性保证: 因为是单消费者模型，最多只有一个线程在锁上等待，因此，醒来是否将signalNeeded设置为false，都不会导致错误，只是可能会有额外开销。
                            // 如果总是设置为false，那么也会增加开销，因为它可能是被signal唤醒的，此时已经是false，
                            // 如果总是不设置，那么也会增加开销，因为它已经醒来了，但是其它线程还是会获取锁进行通知。
                            // 我们只对等待超时和被中断进行处理，其它情况下认为已经是false了(认为是被signal唤醒的)。
                            if (nanos <= 0) {
                                signalNeeded.set(false);
                                throw TimeoutException.INSTANCE;
                            }
                        } catch (InterruptedException e) {
                            signalNeeded.set(false);
                            throw e;
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }
        }

        @Override
        public void signalAllWhenBlocking() {
            if (signalNeeded.getAndSet(false)) {
                lock.lock();
                try {
                    // 这里调用signal和signalAll是一样的，因为最多只有一个线程(消费者线程)在等待
                    processorNotifyCondition.signalAll();
                } finally {
                    lock.unlock();
                }
            }
        }
    }
}
