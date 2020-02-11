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

package com.wjybxx.fastjgame.imp;

import com.wjybxx.fastjgame.core.LogBuilder;
import com.wjybxx.fastjgame.utils.CollectionUtils;

import java.util.Map;

/**
 * {@link LogBuilder}的默认实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/15
 * github - https://github.com/hl845740757
 */
public class DefaultLogBuilder implements LogBuilder {

    private static final int DEFAULT_EXPECTED_SIZE = (int) (16 * 0.75 - 1);

    private final String topic;
    private final Map<String, Object> dataMap;

    public DefaultLogBuilder(String topic) {
        this(topic, DEFAULT_EXPECTED_SIZE);
    }

    public DefaultLogBuilder(String topic, int expectedSize) {
        this.topic = topic;
        this.dataMap = CollectionUtils.newLinkedHashMapWithExpectedSize(expectedSize);
    }

    public DefaultLogBuilder append(final String key, final String value) {
        dataMap.put(key, value);
        return this;
    }

    public DefaultLogBuilder append(final String key, final int value) {
        dataMap.put(key, value);
        return this;
    }

    public DefaultLogBuilder append(final String key, final long value) {
        dataMap.put(key, value);
        return this;
    }

    public DefaultLogBuilder append(final String key, final float value) {
        dataMap.put(key, value);
        return this;
    }

    public DefaultLogBuilder append(final String key, final double value) {
        dataMap.put(key, value);
        return this;
    }

    public DefaultLogBuilder append(final String key, final boolean value) {
        dataMap.put(key, value);
        return this;
    }

    public DefaultLogBuilder append(final String key, final byte value) {
        dataMap.put(key, value);
        return this;
    }

    public DefaultLogBuilder append(final String key, final short value) {
        dataMap.put(key, value);
        return this;
    }

    public DefaultLogBuilder append(final String key, final Object value) {
        dataMap.put(key, value);
        return this;
    }

    String getTopic() {
        return topic;
    }

    Map<String, Object> getDataMap() {
        return dataMap;
    }

    @Override
    public String toString() {
        return "DefaultLogBuilder{" +
                "topic='" + topic + '\'' +
                ", dataMap=" + dataMap +
                '}';
    }
}
