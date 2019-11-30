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
import com.lmax.disruptor.util.ThreadHints;

import javax.annotation.Nonnull;

/**
 * 该策略使用自旋(空循环)来在barrier上等待，该策略通过占用CPU资源去比避免系统调用带来的延迟抖动。
 * 该策略在等待时，具有极低的延迟，极高的吞吐量，以及极高的CPU占用。
 * <p>
 * 由于会持续占用CPU资源，基本不会让出CPU资源，因此最好在线程能绑定到特定的CPU核心时使用。
 * 如果你要使用该等待策略，确保有足够的CPU资源，且你能接受它带来的CPU使用率。
 * <p>
 * 注意：
 * 1. 每自旋一定次数才会执行一次{@link DisruptorEventLoop#loopOnce()}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/28
 * github - https://github.com/hl845740757
 */
public class BusySpinWaitStrategyFactory implements WaitStrategyFactory {

    private final int waitTimesThreshold;

    public BusySpinWaitStrategyFactory() {
        this(WaitStrategyFactory.DEFAULT_WAIT_TIMES_THRESHOLD);
    }

    /**
     * @param waitTimesThreshold 每等待多少次执行一次事件循环
     */
    public BusySpinWaitStrategyFactory(int waitTimesThreshold) {
        this.waitTimesThreshold = waitTimesThreshold;
    }

    @Nonnull
    @Override
    public WaitStrategy newWaitStrategy(DisruptorEventLoop eventLoop) {
        return new BusySpinWaitStrategy(eventLoop, waitTimesThreshold);
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
    static final class BusySpinWaitStrategy implements WaitStrategy {

        private final DisruptorEventLoop eventLoop;
        private final int waitTimesThreshold;

        BusySpinWaitStrategy(DisruptorEventLoop eventLoop, int waitTimesThreshold) {
            this.eventLoop = eventLoop;
            this.waitTimesThreshold = waitTimesThreshold;
        }

        @Override
        public long waitFor(final long sequence, Sequence cursor, final Sequence dependentSequence,
                            final SequenceBarrier barrier) throws AlertException, InterruptedException {

            long availableSequence;
            int waitTimes = 0;

            while ((availableSequence = dependentSequence.get()) < sequence) {
                barrier.checkAlert();
                ThreadHints.onSpinWait();

                if (++waitTimes == waitTimesThreshold) {
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
