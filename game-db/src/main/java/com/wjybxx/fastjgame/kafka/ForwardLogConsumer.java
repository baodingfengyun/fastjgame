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

package com.wjybxx.fastjgame.kafka;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * 仅仅转发日志的消费者 - 将消费逻辑转移到应用线程。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/15
 * github - https://github.com/hl845740757
 */
public class ForwardLogConsumer implements LogConsumer {

    private final EventLoop appEventLoop;
    private final LogConsumer logConsumer;

    public ForwardLogConsumer(EventLoop appEventLoop, LogConsumer logConsumer) {
        this.appEventLoop = appEventLoop;
        this.logConsumer = logConsumer;
    }

    @Override
    public void consume(ConsumerRecord consumerRecord) {
        if (appEventLoop.inEventLoop()) {
            logConsumer.consume(consumerRecord);
        } else {
            appEventLoop.execute(new ForwardTask(logConsumer, consumerRecord));
        }
    }

    private static class ForwardTask implements Runnable {
        private final LogConsumer logConsumer;
        private final ConsumerRecord consumerRecord;

        ForwardTask(LogConsumer logConsumer, ConsumerRecord consumerRecord) {
            this.logConsumer = logConsumer;
            this.consumerRecord = consumerRecord;
        }

        @Override
        public void run() {
            logConsumer.consume(consumerRecord);
        }
    }
}
