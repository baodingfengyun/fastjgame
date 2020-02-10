/*
 * Copyright 2019 wjybxx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wjybxx.fastjgame.kafka;

import com.wjybxx.fastjgame.core.LogBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link LogBuilder}的默认实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/15
 * github - https://github.com/hl845740757
 */
public class DefaultKafkaLogBuilder implements LogBuilder {

    private final String topic;
    private final Map<String, Object> dataMap = new LinkedHashMap<>();

    public DefaultKafkaLogBuilder(String topic) {
        this.topic = topic;
    }

    public DefaultKafkaLogBuilder append(final String key, final String value) {
        dataMap.put(key, value);
        return this;
    }

    public DefaultKafkaLogBuilder append(final String key, final int value) {
        dataMap.put(key, value);
        return this;
    }

    public DefaultKafkaLogBuilder append(final String key, final long value) {
        dataMap.put(key, value);
        return this;
    }

    public DefaultKafkaLogBuilder append(final String key, final float value) {
        dataMap.put(key, value);
        return this;
    }

    public DefaultKafkaLogBuilder append(final String key, final double value) {
        dataMap.put(key, value);
        return this;
    }

    public DefaultKafkaLogBuilder append(final String key, final boolean value) {
        dataMap.put(key, value);
        return this;
    }

    public DefaultKafkaLogBuilder append(final String key, final byte value) {
        dataMap.put(key, value);
        return this;
    }

    public DefaultKafkaLogBuilder append(final String key, final short value) {
        dataMap.put(key, value);
        return this;
    }

    public DefaultKafkaLogBuilder append(final String key, final Object value) {
        dataMap.put(key, value);
        return this;
    }

    String getTopic() {
        return topic;
    }

    Map<String, Object> getDataMap() {
        return dataMap;
    }
}
