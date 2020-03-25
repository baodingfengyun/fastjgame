/*
 * Copyright 2019 wjybxx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wjybxx.fastjgame.utils.concurrent.disruptor;

import com.lmax.disruptor.AlertException;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.WaitStrategy;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * 该策略下，当没有事件可消费时，使用sleep进行等待，直到有新的事件到来。<b>
 * 该策略在等待时，对CPU资源很友好，延迟不稳定。<b>
 * 适用于那些延迟和吞吐量不那么重要的场景，eg: 日志、网络。
 * <b>该策略是默认的策略</b>
 * <p>
 * 注意：每睡眠一次，会调用一次{@link DisruptorEventLoop#loopOnce()}。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/28
 * github - https://github.com/hl845740757
 */
public class SleepWaitStrategyFactory implements WaitStrategyFactory {

    public static final int DEFAULT_RETRIES = 200;
    public static final long DEFAULT_SLEEP_NS = 100;

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

    @Nonnull
    @Override
    public WaitStrategy newWaitStrategy(DisruptorEventLoop eventLoop) {
        return new SleepWaitStrategy(eventLoop, retries, sleepTimeNs);
    }

    /**
     * 该策略使用<b>自旋 + yield + sleep</b>等待生产者生产数据。
     * 特征：延迟不稳定，但是CPU使用率较低。如果CPU资源紧张，使用该策略是不错的策略。
     */
    private static class SleepWaitStrategy implements WaitStrategy {

        private final DisruptorEventLoop eventLoop;
        private final int retries;
        private final long sleepTimeNs;

        SleepWaitStrategy(DisruptorEventLoop eventLoop, int retries, long sleepTimeNs) {
            this.eventLoop = eventLoop;
            this.retries = retries;
            this.sleepTimeNs = sleepTimeNs;
        }

        @Override
        public long waitFor(final long sequence, Sequence cursor, final Sequence dependentSequence,
                            final SequenceBarrier barrier) throws AlertException {
            long availableSequence;
            int counter = retries;

            // dependentSequence 该项目组织架构中，其实只是生产者的sequence，也就是cursor
            while ((availableSequence = dependentSequence.get()) < sequence) {
                counter = applyWaitMethod(barrier, counter);

                // 每睡眠一次，执行一次循环
                if (counter <= 0) {
                    eventLoop.safeLoopOnce();
                }
            }
            return availableSequence;
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

        @Override
        public void signalAllWhenBlocking() {
            // 消费者并不会在等待时阻塞，因此什么也不做
        }
    }
}
