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
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/23
 */
public class BusySpanWaitStrategyFactory implements WaitStrategyFactory {

    private static final int DEFAULT_LOOP_ONCE_SPIN_TRIES = 2048;

    private final int loopOnceSpinTries;

    public BusySpanWaitStrategyFactory() {
        this(DEFAULT_LOOP_ONCE_SPIN_TRIES);
    }

    public BusySpanWaitStrategyFactory(int loopOnceSpinTries) {
        this.loopOnceSpinTries = loopOnceSpinTries;
    }

    @Override
    public WaitStrategy newInstance() {
        return new BusySpanWaitStrategy(loopOnceSpinTries);
    }

    public static class BusySpanWaitStrategy implements WaitStrategy {

        private final int loopOnceSpinTries;

        BusySpanWaitStrategy(int loopOnceSpinTries) {
            this.loopOnceSpinTries = loopOnceSpinTries;
        }

        @Override
        public void waitTask(UnboundedEventLoop eventLoop) throws ShuttingDownException, TimeoutException, InterruptedException {
            int spinTries = 0;
            while (eventLoop.isTaskQueueEmpty()) {
                eventLoop.checkShuttingDown();
                Thread.onSpinWait();

                if (++spinTries == loopOnceSpinTries) {
                    spinTries = 0;
                    eventLoop.safeLoopOnce();
                }
            }
        }

        @Override
        public void signalAllWhenBlocking() {
            // 没有阻塞的消费者，什么也不做
        }

    }
}
