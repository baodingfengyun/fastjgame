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

package com.wjybxx.fastjgame.misc.log;

import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandler;
import com.wjybxx.fastjgame.concurrent.disruptor.DisruptorEventLoop;
import com.wjybxx.fastjgame.concurrent.disruptor.SleepWaitStrategyFactory;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import javax.annotation.Nonnull;
import java.util.Properties;
import java.util.concurrent.ThreadFactory;

/**
 * 日志线程 - 该线程作为kafka日志生产者。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/27
 * github - https://github.com/hl845740757
 */
public class LogProducerEventLoop extends DisruptorEventLoop {
    /**
     * 日志线程任务缓冲区大小
     */
    private static final int PRODUCER_RING_BUFFER_SIZE = 64 * 1024;
    /**
     * 由于游戏打点日志并不是太多，可以将日志总是打在同一个partition下（可以获得全局的顺序性）
     */
    private static final Integer PARTITION_ID = 0;

    private final KafkaProducer<String, String> producer;
    private final LogDirector logDirector = new DefaultLogDirector();

    public LogProducerEventLoop(@Nonnull String brokerList,
                                @Nonnull ThreadFactory threadFactory,
                                @Nonnull RejectedExecutionHandler rejectedExecutionHandler) {
        super(null, threadFactory, rejectedExecutionHandler, PRODUCER_RING_BUFFER_SIZE, new SleepWaitStrategyFactory());
        this.producer = new KafkaProducer<>(newConfig(brokerList), new StringSerializer(), new StringSerializer());
    }

    @Override
    protected void init() throws Exception {
    }

    @Override
    protected void loopOnce() throws Exception {
    }

    @Override
    protected void clean() throws Exception {
        producer.close();
    }

    public void log(LogBuilder logBuilder) {
        execute(new KafkaLogTask(logBuilder));
    }

    private static Properties newConfig(String brokerList) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        // 不使用"all"机制是为了提高吞吐量。
        properties.put(ProducerConfig.ACKS_CONFIG, "1");
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, 16 * 1024);
        properties.put(ProducerConfig.LINGER_MS_CONFIG, 100);
        properties.put(ProducerConfig.RETRIES_CONFIG, 3);
        return properties;
    }

    private class KafkaLogTask implements Runnable {

        private final LogBuilder builder;

        KafkaLogTask(LogBuilder builder) {
            this.builder = builder;
        }

        @Override
        public void run() {
            try {
                final String content = logDirector.build(builder, System.currentTimeMillis());
                // 指定partitionId，不通过key分发
                producer.send(new ProducerRecord<>(builder.getLogTopic().toString(), PARTITION_ID,
                        null, content));
            } finally {
                logDirector.reset();
            }
        }
    }
}
