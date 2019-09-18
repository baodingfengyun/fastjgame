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

package com.wjybxx.fastjgame.concurrent.disruptor;

import com.lmax.disruptor.*;
import com.lmax.disruptor.util.ThreadHints;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * {@link DisruptorEventLoop}等待策略工厂
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/16
 * github - https://github.com/hl845740757
 */
class DisruptorWaitStrategyFactory {

    private DisruptorWaitStrategyFactory() {

    }

    static WaitStrategy newWaitStrategy(@Nonnull DisruptorEventLoop disruptorEventLoop,
                                        @Nonnull DisruptorWaitStrategyType waitStrategyType) {
        switch (waitStrategyType) {
            case TIMEOUT:
                return new LiteTimeoutBlockingWaitStrategy(disruptorEventLoop.timeoutInNano(), TimeUnit.NANOSECONDS);
            case SLEEP:
                return new SleepWaitStrategy(disruptorEventLoop);
            case YIELD:
                return new YieldWaitStrategy(disruptorEventLoop);
            case BUSY_SPIN:
                return new BusySpinWaitStrategy(disruptorEventLoop);
        }
        throw new UnsupportedOperationException("" + waitStrategyType);
    }


    /**
     * 该策略使用<b>自旋 + yield + sleep</b>等待生产者生产数据。
     * 特征：延迟不稳定，但是CPU使用率较低。如果CPU资源紧张，使用该策略是不错的策略。
     */
    static class SleepWaitStrategy implements WaitStrategy {

        private static final int DEFAULT_RETRIES = 200;
        private static final long DEFAULT_SLEEP = 100;

        private final int retries;
        private final long sleepTimeNs;

        private final DisruptorEventLoop eventLoop;

        SleepWaitStrategy(DisruptorEventLoop eventLoop) {
            this.eventLoop = eventLoop;
            this.retries = DEFAULT_RETRIES;
            this.sleepTimeNs = DEFAULT_SLEEP;
        }

        @Override
        public long waitFor(final long sequence, Sequence cursor, final Sequence dependentSequence,
                            final SequenceBarrier barrier) throws AlertException {
            long availableSequence;
            int counter = retries;
            int waitTimes = 0;

            // dependentSequence 该项目组织架构中，其实只是生产者的sequence。
            while ((availableSequence = dependentSequence.get()) < sequence) {
                counter = applyWaitMethod(barrier, counter);
                // 每隔一段时间执行一次循环
                if (++waitTimes == DisruptorEventLoop.LOOP_ONCE_INTERVAL) {
                    waitTimes = 0;
                    eventLoop.safeLoopOnce();
                }
            }
            return availableSequence;
        }

        @Override
        public void signalAllWhenBlocking() {
            // 消费者并不会在等待时阻塞，因此什么也不做
        }

        private int applyWaitMethod(final SequenceBarrier barrier, int counter)
                throws AlertException {
            // 检查中断/终止信号
            barrier.checkAlert();

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
    }

    /**
     * {@link YieldWaitStrategy}使用<b>自旋 + yield</b>在屏障上等待生产者生产数据。
     * 特征：该策略有着延迟的较低，较高的吞吐量，以及较高的CPU使用率。 当CPU核心数足够时，建议使用该策略。
     * <p>
     * 注意：该策略将使用100%的cpu，但是当其它线程需要CPU资源时，比{@link BusySpinWaitStrategy}更容易让出CPU资源。
     */
    static class YieldWaitStrategy implements WaitStrategy {

        private static final int SPIN_TRIES = 100;

        private final DisruptorEventLoop eventLoop;

        YieldWaitStrategy(DisruptorEventLoop eventLoop) {
            this.eventLoop = eventLoop;
        }

        @Override
        public long waitFor(final long sequence, Sequence cursor, final Sequence dependentSequence,
                            final SequenceBarrier barrier) throws AlertException {
            long availableSequence;
            int counter = SPIN_TRIES;
            int waitTimes = 0;

            while ((availableSequence = dependentSequence.get()) < sequence) {
                counter = applyWaitMethod(barrier, counter);
                // 每隔一段时间执行一次循环
                if (++waitTimes == DisruptorEventLoop.LOOP_ONCE_INTERVAL) {
                    waitTimes = 0;
                    eventLoop.safeLoopOnce();
                }
            }

            return availableSequence;
        }

        @Override
        public void signalAllWhenBlocking() {
            // 没有消费者在这里阻塞，因此什么也不干
        }

        private int applyWaitMethod(final SequenceBarrier barrier, int counter)
                throws AlertException {
            // 检查中断、停止信号
            barrier.checkAlert();

            if (counter > 0) {
                --counter;
            } else {
                Thread.yield();
            }
            return counter;
        }
    }

    /**
     * 该策略使用<b>完全的自旋</b>等待生产者生产数据。
     * 特征：该策略有着极低的延迟，以及极高的吞吐量，以及极高的CPU使用率...
     * <p>
     * 此策略将使用CPU资源来避免可能引入延迟抖动的系统调用。当线程可以绑定到特定的CPU内核时，推荐使用它。
     * 注意：如果使用该策略，一定要保证有足够的CPU核心，且能接受它带来的CPU使用率。
     *
     * @see com.lmax.disruptor.BusySpinWaitStrategy
     */
    public static final class BusySpinWaitStrategy implements WaitStrategy {

        private final DisruptorEventLoop eventLoop;

        BusySpinWaitStrategy(DisruptorEventLoop eventLoop) {
            this.eventLoop = eventLoop;
        }

        @Override
        public long waitFor(final long sequence, Sequence cursor, final Sequence dependentSequence,
                            final SequenceBarrier barrier) throws AlertException, InterruptedException {

            long availableSequence;
            int waitTimes = 0;

            while ((availableSequence = dependentSequence.get()) < sequence) {
                barrier.checkAlert();
                ThreadHints.onSpinWait();

                if (++waitTimes == DisruptorEventLoop.LOOP_ONCE_INTERVAL) {
                    waitTimes = 0;
                    eventLoop.safeLoopOnce();
                }
            }

            return availableSequence;
        }

        @Override
        public void signalAllWhenBlocking() {
            // 没有消费者在这里阻塞，因此什么也不干
        }
    }
}
