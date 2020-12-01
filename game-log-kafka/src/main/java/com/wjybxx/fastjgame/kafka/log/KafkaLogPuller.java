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

package com.wjybxx.fastjgame.kafka.log;

import com.google.common.collect.Maps;
import com.wjybxx.fastjgame.log.core.GameLog;
import com.wjybxx.fastjgame.log.core.LogConsumer;
import com.wjybxx.fastjgame.log.core.LogDecoder;
import com.wjybxx.fastjgame.log.core.LogPuller;
import com.wjybxx.fastjgame.log.imp.CompositeLogConsumer;
import com.wjybxx.fastjgame.log.imp.DefaultLogRecord;
import com.wjybxx.fastjgame.log.utils.LogConsumerUtils;
import com.wjybxx.fastjgame.util.CloseableUtils;
import com.wjybxx.fastjgame.util.concurrent.RejectedExecutionHandler;
import com.wjybxx.fastjgame.util.concurrent.disruptor.DisruptorEventLoop;
import com.wjybxx.fastjgame.util.concurrent.disruptor.TimeoutBlockingWaitStrategyFactory;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * kafka消费者事件循环
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/28
 * github - https://github.com/hl845740757
 */
public class KafkaLogPuller<T extends GameLog> extends DisruptorEventLoop implements LogPuller {

    private static final Logger logger = LoggerFactory.getLogger(KafkaLogPuller.class);

    /**
     * 日志线程任务缓冲区大小
     * 消费者需要响应的事件不多，因此较小
     */
    private static final int CONSUMER_RING_BUFFER_SIZE = 64 * 1024;
    private static final int CONSUMER_TASK_BATCH_SIZE = 1024;

    /**
     * 无事件消费时阻塞等待时间
     */
    private static final int CONSUMER_BLOCK_TIME_MS = 50;

    /**
     * 消费者拉取数据最长阻塞时间
     */
    private static final Duration CONSUMER_POLL_DURATION = Duration.ofMillis(100);
    /**
     * kafka消费者客户端
     */
    private final KafkaConsumer<String, String> kafkaConsumer;

    private final LogDecoder<DefaultLogRecord, T> decoder;
    private final Map<String, LogConsumer<T>> logConsumerMap;

    public KafkaLogPuller(@Nonnull ThreadFactory threadFactory,
                          @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                          @Nonnull String brokerList,
                          @Nonnull String groupId,
                          @Nonnull LogDecoder<DefaultLogRecord, T> decoder,
                          @Nonnull Collection<LogConsumer<T>> consumers) {
        super(null, threadFactory, rejectedExecutionHandler, newWaitStrategyFactory(), CONSUMER_RING_BUFFER_SIZE, CONSUMER_TASK_BATCH_SIZE);
        this.decoder = decoder;
        this.logConsumerMap = indexConsumers(consumers);
        this.kafkaConsumer = new KafkaConsumer<>(newConfig(brokerList, groupId), new StringDeserializer(), new StringDeserializer());
        this.kafkaConsumer.subscribe(logConsumerMap.keySet());
    }

    @Nonnull
    private static TimeoutBlockingWaitStrategyFactory newWaitStrategyFactory() {
        return new TimeoutBlockingWaitStrategyFactory(CONSUMER_BLOCK_TIME_MS, TimeUnit.MILLISECONDS);
    }

    private static <T extends GameLog> Map<String, LogConsumer<T>> indexConsumers(Collection<LogConsumer<T>> consumers) {
        final Map<String, LogConsumer<T>> logConsumerMap = Maps.newHashMapWithExpectedSize(consumers.size());
        for (LogConsumer<T> logConsumer : consumers) {
            addConsumer(logConsumerMap, logConsumer);
        }
        return logConsumerMap;
    }

    private static <T extends GameLog> void addConsumer(Map<String, LogConsumer<T>> logConsumerMap, LogConsumer<T> logConsumer) {
        for (String topic : logConsumer.subscribedTopics()) {
            LogConsumer<T> existLogConsumer = logConsumerMap.get(topic);
            if (existLogConsumer == null) {
                logConsumerMap.put(topic, logConsumer);
                continue;
            }

            if (existLogConsumer instanceof CompositeLogConsumer) {
                ((CompositeLogConsumer<T>) existLogConsumer).addChild(logConsumer);
            } else {
                logConsumerMap.put(topic, new CompositeLogConsumer<>(topic, existLogConsumer, logConsumer));
            }
        }
    }

    private static Properties newConfig(final String brokerList, final String groupId) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1024);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return properties;
    }

    @Override
    protected void init() throws Exception {

    }

    @Override
    protected void loopOnce() throws Exception {
        final ConsumerRecords<String, String> records = kafkaConsumer.poll(CONSUMER_POLL_DURATION);
        if (records.isEmpty()) {
            return;
        }

        try {
            for (ConsumerRecord<String, String> record : records) {
                consumeSafely(record);
            }
        } finally {
            // 提交消费记录 - 如果使用自动提交，参数设置不当时，容易导致重复消费。
            kafkaConsumer.commitSync();
        }
    }

    private void consumeSafely(ConsumerRecord<String, String> consumerRecord) {
        try {
            final T record = decoder.decode(new DefaultLogRecord(consumerRecord.topic(), consumerRecord.value()));
            final LogConsumer<T> logConsumer = logConsumerMap.get(consumerRecord.topic());
            LogConsumerUtils.consumeSafely(logConsumer, record);
        } catch (Throwable e) {
            logger.warn("consume caught exception, record {}", consumerRecord, e);
        }
    }

    @Override
    protected void clean() throws Exception {
        CloseableUtils.closeQuietly(kafkaConsumer);
    }

}
