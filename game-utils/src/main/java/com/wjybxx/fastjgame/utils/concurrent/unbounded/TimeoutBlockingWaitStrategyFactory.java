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

    private static final int DEFAULT_TIMEOUT_MS = 1;

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

    static class TimeoutBlockingWaitStrategy implements WaitStrategyFactory.WaitStrategy {

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
        public void waitTask(UnboundedEventLoop eventLoop) throws ShuttingDownException, TimeoutException, InterruptedException {
            long nanos = timeoutInNanos;

            if (eventLoop.isTaskQueueEmpty()) {
                lock.lock();
                try {
                    while (eventLoop.isTaskQueueEmpty()) {
                        eventLoop.checkShuttingDown();

                        signalNeeded.getAndSet(true);

                        nanos = processorNotifyCondition.awaitNanos(nanos);

                        if (nanos <= 0) {
                            throw TimeoutException.INSTANCE;
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
