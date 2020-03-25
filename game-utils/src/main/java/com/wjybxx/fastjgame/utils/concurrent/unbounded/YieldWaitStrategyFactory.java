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
 * date - 2020/3/24
 */
public class YieldWaitStrategyFactory implements WaitStrategyFactory {

    private static final int SPIN_TRIES = 100;
    private static final int DEFAULT_LOOP_ONCE_SPIN_TRIES = 1024;

    private final int spinTries;
    private final int loopOnceSpinTries;

    public YieldWaitStrategyFactory() {
        this(SPIN_TRIES, DEFAULT_LOOP_ONCE_SPIN_TRIES);
    }

    /**
     * @param spinTries         最大自旋次数，超过等待次数后尝试让出CPU
     * @param loopOnceSpinTries 每自旋多少次执行一次事件循环
     */
    public YieldWaitStrategyFactory(int spinTries, int loopOnceSpinTries) {
        this.spinTries = spinTries;
        this.loopOnceSpinTries = loopOnceSpinTries;
    }

    @Override
    public WaitStrategy newInstance() {
        return new YieldWaitStrategy(spinTries, loopOnceSpinTries);
    }

    private static class YieldWaitStrategy implements WaitStrategy {
        private final int spinTries;
        private final int loopOnceSpinTries;

        YieldWaitStrategy(int spinTries, int loopOnceSpinTries) {
            this.spinTries = spinTries;
            this.loopOnceSpinTries = loopOnceSpinTries;
        }

        @Override
        public void waitTask(UnboundedEventLoop eventLoop) throws ShuttingDownException, TimeoutException, InterruptedException {
            int counter = spinTries;
            int spinTries = 0;

            while (eventLoop.isTaskQueueEmpty()) {
                counter = applyWaitMethod(eventLoop, counter);

                // 每隔一段时间执行一次循环
                if (++spinTries == loopOnceSpinTries) {
                    spinTries = 0;
                    eventLoop.safeLoopOnce();
                }
            }
        }

        private int applyWaitMethod(final UnboundedEventLoop eventLoop, int counter) throws ShuttingDownException {
            // 检查停止
            eventLoop.checkShuttingDown();

            if (counter > 0) {
                --counter;
            } else {
                Thread.yield();
            }
            return counter;
        }

        @Override
        public void signalAllWhenBlocking() {
            // 没有消费者在这里阻塞，因此什么也不做
        }
    }
}
