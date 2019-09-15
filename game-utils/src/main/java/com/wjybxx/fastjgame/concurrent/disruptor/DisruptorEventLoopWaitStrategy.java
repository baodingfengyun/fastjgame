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

package com.wjybxx.fastjgame.concurrent.disruptor;

import com.lmax.disruptor.AlertException;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.WaitStrategy;

import java.util.concurrent.locks.LockSupport;

/**
 * {@link DisruptorEventLoop}等待策略
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 10:06
 * github - https://github.com/hl845740757
 */
class DisruptorEventLoopWaitStrategy implements WaitStrategy {

    private static final int INVOKE_ON_WAIT_EVENT_INTERVAL = 0x3F;
    private static final int DEFAULT_RETRIES = 200;
    private static final long DEFAULT_SLEEP = 1000;

    private final int retries;
    private final long sleepTimeNs;

    private final DisruptorEventLoop eventLoop;

    DisruptorEventLoopWaitStrategy(DisruptorEventLoop eventLoop) {
        this.eventLoop = eventLoop;
        this.retries = DEFAULT_RETRIES;
        this.sleepTimeNs = DEFAULT_SLEEP;
    }

    @Override
    public long waitFor(final long sequence, Sequence cursor, final Sequence dependentSequence,
                        final SequenceBarrier barrier) throws AlertException {
        long availableSequence;
        int counter = retries;

        // dependentSequence 该项目组织架构中，其实只是生产者的sequence。
        while ((availableSequence = dependentSequence.get()) < sequence) {
            counter = applyWaitMethod(barrier, counter);
            // 每隔一段时间执行一次循环
            if ((counter & INVOKE_ON_WAIT_EVENT_INTERVAL) == 0) {
                eventLoop.loopOnce();
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
