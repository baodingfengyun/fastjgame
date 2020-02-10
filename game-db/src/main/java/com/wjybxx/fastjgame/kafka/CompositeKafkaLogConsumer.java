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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 为多个consumer提供单一视图
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/29
 * github - https://github.com/hl845740757
 */
class CompositeKafkaLogConsumer<T> implements KafkaLogConsumer<T> {

    private static final Logger logger = LoggerFactory.getLogger(CompositeKafkaLogConsumer.class);

    /**
     * 订阅的共同的topic
     */
    private final String topic;
    /**
     * 这些子节点订阅了相同的topic
     */
    private final List<KafkaLogConsumer<T>> children = new ArrayList<>(2);

    CompositeKafkaLogConsumer(final String topic,
                              @Nonnull KafkaLogConsumer<T> first,
                              @Nonnull KafkaLogConsumer<T> second) {
        this.topic = topic;
        children.add(first);
        children.add(second);
    }

    @Override
    public Set<String> subscribedTopics() {
        return Collections.singleton(topic);
    }

    void addChild(@Nonnull KafkaLogConsumer<T> child) {
        children.add(child);
    }

    @Override
    public void consume(T record) {
        for (KafkaLogConsumer<T> child : children) {
            try {
                child.consume(record);
            } catch (Throwable e) {
                logger.warn("child.consume caught exception", e);
            }
        }
    }

}
