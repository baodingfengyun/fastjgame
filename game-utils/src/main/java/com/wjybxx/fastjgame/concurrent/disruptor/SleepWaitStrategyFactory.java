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
 * 注意：
 * 1. 每等待一段时间才会执行一次{@link DisruptorEventLoop#loopOnce()}
 * {@link SleepWaitStrategy}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/28
 * github - https://github.com/hl845740757
 */
public class SleepWaitStrategyFactory implements WaitStrategyFactory {

    private static final int DEFAULT_RETRIES = 200;
    private static final long DEFAULT_SLEEP_NS = 100;

    private final int retries;
    private final long sleepTimeNs;
    private final int waitTimesThreshold;

    public SleepWaitStrategyFactory() {
        this(DEFAULT_RETRIES, DEFAULT_SLEEP_NS, TimeUnit.NANOSECONDS, WaitStrategyFactory.DEFAULT_WAIT_TIMES_THRESHOLD);
    }

    /**
     * @param retries            尝试多少次空循环后开始睡眠
     * @param sleepTimeout       睡眠时间
     * @param sleepTimeUnit      时间单位
     * @param waitTimesThreshold 每等待多少次执行一次事件循环
     */
    public SleepWaitStrategyFactory(int retries, long sleepTimeout, TimeUnit sleepTimeUnit, int waitTimesThreshold) {
        this.retries = retries;
        this.sleepTimeNs = sleepTimeUnit.toNanos(sleepTimeout);
        this.waitTimesThreshold = waitTimesThreshold;
    }

    @Nonnull
    @Override
    public WaitStrategy newWaitStrategy(DisruptorEventLoop eventLoop) {
        return new SleepWaitStrategy(eventLoop, retries, sleepTimeNs, waitTimesThreshold);
    }

    /**
     * 该策略使用<b>自旋 + yield + sleep</b>等待生产者生产数据。
     * 特征：延迟不稳定，但是CPU使用率较低。如果CPU资源紧张，使用该策略是不错的策略。
     */
    static class SleepWaitStrategy implements WaitStrategy {

        private final DisruptorEventLoop eventLoop;
        private final int retries;
        private final long sleepTimeNs;
        private final int waitTimesThreshold;

        SleepWaitStrategy(DisruptorEventLoop eventLoop, int retries, long sleepTimeNs, int waitTimesThreshold) {
            this.eventLoop = eventLoop;
            this.retries = retries;
            this.sleepTimeNs = sleepTimeNs;
            this.waitTimesThreshold = waitTimesThreshold;
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
                if (++waitTimes == waitTimesThreshold) {
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
}
