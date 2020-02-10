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

import java.util.Set;

/**
 * 仅仅转发日志的消费者 - 将消费逻辑转移到应用线程。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/15
 * github - https://github.com/hl845740757
 */
public class ForwardKafkaLogConsumer<T> implements KafkaLogConsumer<T> {

    private final EventLoop appEventLoop;
    private final KafkaLogConsumer<T> logConsumer;

    public ForwardKafkaLogConsumer(EventLoop appEventLoop, KafkaLogConsumer<T> logConsumer) {
        this.appEventLoop = appEventLoop;
        this.logConsumer = logConsumer;
    }

    @Override
    public Set<String> subscribedTopics() {
        return logConsumer.subscribedTopics();
    }

    @Override
    public void consume(T record) {
        if (appEventLoop.inEventLoop()) {
            logConsumer.consume(record);
        } else {
            appEventLoop.execute(new ForwardTask<>(logConsumer, record));
        }
    }

    private static class ForwardTask<T> implements Runnable {
        private final KafkaLogConsumer<T> logConsumer;
        private final T consumerRecord;

        ForwardTask(KafkaLogConsumer<T> logConsumer, T consumerRecord) {
            this.logConsumer = logConsumer;
            this.consumerRecord = consumerRecord;
        }

        @Override
        public void run() {
            logConsumer.consume(consumerRecord);
        }
    }
}
