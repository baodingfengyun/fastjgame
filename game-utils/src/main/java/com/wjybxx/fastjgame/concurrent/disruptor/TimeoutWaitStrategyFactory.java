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

import com.lmax.disruptor.LiteTimeoutBlockingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

/**
 * 该策略下，当没有事件可消费时，阻塞一段时间，直到超时或有新事件到来。<b>
 * 该策略在等待时，延迟较长，对CPU资源很友好，各种情况下表现较为一致。<b>
 * 适用于那些延迟和吞吐量不那么重要的场景，eg: 日志，网络。
 * <p>
 * 1. 等待期间不会执行{@link DisruptorEventLoop#loopOnce()}，而是在每次超时之后执行一次{@link DisruptorEventLoop#loopOnce()}
 * <p>
 * {@link LiteTimeoutBlockingWaitStrategy}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/28
 * github - https://github.com/hl845740757
 */
public class TimeoutWaitStrategyFactory implements WaitStrategyFactory {

    private static final int DEFAULT_TIMEOUT = 1;

    private final int timeout;
    private final TimeUnit timeUnit;

    public TimeoutWaitStrategyFactory() {
        this(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public TimeoutWaitStrategyFactory(int timeout, TimeUnit timeUnit) {
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    @Nonnull
    @Override
    public WaitStrategy newWaitStrategy(DisruptorEventLoop eventLoop) {
        return new LiteTimeoutBlockingWaitStrategy(timeout, timeUnit);
    }

}
