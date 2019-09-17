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

/**
 * {@link DisruptorEventLoop}等待生产者生产数据时的策略。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/16
 * github - https://github.com/hl845740757
 */
public enum DisruptorWaitStrategyType {

    /**
     * 该策略下，当没有事件可消费时，阻塞一段时间，直到超时或有新事件到来。<b>
     * 该策略在等待时，延迟较长，对CPU资源很友好，各种情况下表现较为一致。<b>
     * 适用于那些延迟和吞吐量不那么重要的场景，eg: 日志，网络。
     * <p>
     * 注意：
     * 1. 复写{@link DisruptorEventLoop#timeoutInNano()}以指定超时时间。
     * 2. 等待期间不会执行{@link DisruptorEventLoop#loopOnce()}，而是在每次超时之后执行一次{@link DisruptorEventLoop#loopOnce()}
     */
    TIMEOUT,
    /**
     * 该策略下，当没有事件可消费时，使用sleep进行等待，直到有新的事件到来。<b>
     * 该策略在等待时，对CPU资源很友好，延迟不稳定。<b>
     * 适用于那些延迟和吞吐量不那么重要的场景，eg: 日志、网络。
     * <b>该策略是默认的策略</b>
     * <p>
     * 注意：
     * 1. 每等待一段时间才会执行一次{@link DisruptorEventLoop#loopOnce()}
     */
    SLEEP,
    /**
     * 该策略下，当没有事件可消费时，使用{@link Thread#yield()}进行等待。<b>
     * 该策略在等待时，延迟较低，但有较高的CPU使用率。但是当其它线程需要CPU资源时，比{@link #BUSY_SPIN}更容易让出CPU资源。<b>
     * 当CPU资源足够时，推荐使用该策略。
     * 注意：<p>
     * 1. 每等待一段时间才会执行一次{@link DisruptorEventLoop#loopOnce()}
     */
    YIELD,
    /**
     * 该策略使用自旋(空循环)来在barrier上等待，该策略通过占用CPU资源去比避免系统调用带来的延迟抖动。
     * 该策略在等待时，具有极低的延迟，极高的吞吐量，以及极高的CPU占用。
     * <p>
     * 由于会持续占用CPU资源，基本不会让出CPU资源，因此最好在线程能绑定到特定的CPU核心时使用。
     * 如果你要使用该等待策略，确保有足够的CPU资源，且你能接受它带来的CPU使用率。
     * <p>
     * 注意：
     * 1. 每自旋一定次数才会执行一次{@link DisruptorEventLoop#loopOnce()}
     */
    BUSY_SPIN,

}
