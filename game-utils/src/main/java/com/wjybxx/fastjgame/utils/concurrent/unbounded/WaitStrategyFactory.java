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

/**
 * 这里的等待策略和disruptor包的等待策略意义一致。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/24
 */
public interface WaitStrategyFactory {

    WaitStrategy newInstance();

    interface WaitStrategy {

        /**
         * 如果当前没有可执行的任务，则等待，直到有新的任务到达或超时，或被中断等。
         * 该方法由消费者自身调用。
         *
         * @param eventLoop 等待任务的消费者
         */
        void waitFor(TemplateEventLoop eventLoop) throws ShuttingDownException, TimeoutException, InterruptedException;

        /**
         * 唤醒阻塞的消费者，表示有新的任务到达。
         * 如果有消费者在{@link #waitFor(TemplateEventLoop)}中将自己挂起了，则需要唤醒。
         * 该方法由生产者调用。
         */
        void signalAllWhenBlocking();

    }
}
