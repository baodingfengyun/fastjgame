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
import com.wjybxx.fastjgame.utils.TimeUtils;

import java.util.concurrent.locks.LockSupport;

/**
 * 消费者等待策略 (也就是我们的游戏世界等待网络事件时的策略)。
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 10:06
 * github - https://github.com/hl845740757
 */
public class SleepingWaitExtendStrategy implements WaitStrategy {

    private static final int INVOKE_ON_WAIT_EVENT_INTERVAL = 0x3F;
    private static final int DEFAULT_RETRIES = 200;
    private static final long DEFAULT_SLEEP = TimeUtils.NANO_PER_MILLISECOND;

    private final int retries;
    private final long sleepTimeNs;

    private final DisruptorEventLoop eventLoop;

    public SleepingWaitExtendStrategy(DisruptorEventLoop eventLoop)
    {
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
        // 在等待生产者生产数据的过程中，尝试执行游戏世界循环
        while ((availableSequence = dependentSequence.get()) < sequence)
        {
            counter = applyWaitMethod(barrier, counter);
            // 每隔一段时间执行一次onWaitEvent
            if ((counter & INVOKE_ON_WAIT_EVENT_INTERVAL) == 0){
                eventLoop.onWaitEvent();
            }
        }

        return availableSequence;
    }

    @Override
    public void signalAllWhenBlocking()
    {
    }

    private int applyWaitMethod(final SequenceBarrier barrier, int counter)
            throws AlertException
    {
        barrier.checkAlert();

        if (counter > 100) {
            // 大于100时自旋
            --counter;
        }
        else if (counter > 0) {
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
