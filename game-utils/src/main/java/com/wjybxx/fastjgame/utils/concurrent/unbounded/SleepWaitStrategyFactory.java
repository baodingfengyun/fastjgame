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
import java.util.concurrent.locks.LockSupport;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/24
 */
public class SleepWaitStrategyFactory implements WaitStrategyFactory {

    private static final int DEFAULT_RETRIES = com.wjybxx.fastjgame.utils.concurrent.disruptor.SleepWaitStrategyFactory.DEFAULT_RETRIES;
    private static final long DEFAULT_SLEEP_NS = com.wjybxx.fastjgame.utils.concurrent.disruptor.SleepWaitStrategyFactory.DEFAULT_SLEEP_NS;

    private final int retries;
    private final long sleepTimeNs;

    public SleepWaitStrategyFactory() {
        this(DEFAULT_RETRIES, DEFAULT_SLEEP_NS, TimeUnit.NANOSECONDS);
    }

    /**
     * @see #SleepWaitStrategyFactory(int, long, TimeUnit)
     */
    public SleepWaitStrategyFactory(long sleepTimeout, TimeUnit sleepTimeUnit) {
        this(DEFAULT_RETRIES, sleepTimeout, sleepTimeUnit);
    }

    /**
     * @param retries       自旋多少次后开始睡眠
     * @param sleepTimeout  睡眠时间
     * @param sleepTimeUnit 时间单位
     */
    public SleepWaitStrategyFactory(int retries, long sleepTimeout, TimeUnit sleepTimeUnit) {
        this.retries = retries;
        this.sleepTimeNs = sleepTimeUnit.toNanos(sleepTimeout);
    }

    @Override
    public WaitStrategy newInstance() {
        return new SleepWaitStrategy(retries, sleepTimeNs);
    }

    private static class SleepWaitStrategy implements WaitStrategy {

        private final int retries;
        private final long sleepTimeNs;

        SleepWaitStrategy(int retries, long sleepTimeNs) {
            this.retries = retries;
            this.sleepTimeNs = sleepTimeNs;
        }

        @Override
        public void waitFor(UnboundedEventLoop eventLoop) throws ShuttingDownException, TimeoutException, InterruptedException {
            int counter = retries;
            while (eventLoop.isTaskQueueEmpty()) {
                counter = applyWaitMethod(eventLoop, counter);

                // 每睡眠一次，执行一次循环
                if (counter <= 0) {
                    eventLoop.safeLoopOnce();
                }
            }
        }

        private int applyWaitMethod(final UnboundedEventLoop eventLoop, int counter) throws ShuttingDownException {
            // 检查中断/终止信号
            eventLoop.checkShuttingDown();

            if (counter > 100) {
                // 大于100时自旋
                --counter;
            } else if (counter > 0) {
                // 大于0时尝试让出Cpu
                --counter;
                Thread.yield();
            } else {
                // 等到最大次数了，睡眠等待
                LockSupport.parkNanos(sleepTimeNs);
            }
            return counter;
        }

        @Override
        public void signalAllWhenBlocking() {
            // 消费者并不会在等待时阻塞，因此什么也不做
        }
    }
}
