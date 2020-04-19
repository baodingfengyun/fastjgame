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

package com.wjybxx.fastjgame.kafka.logtest;

import com.wjybxx.fastjgame.log.core.GameLog;
import com.wjybxx.fastjgame.utils.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/15
 * github - https://github.com/hl845740757
 */
class GameLogTest implements GameLog {

    private final String topic;
    private final Map<String, Object> dataMap;

    public GameLogTest(String topic) {
        this.topic = topic;
        this.dataMap = new LinkedHashMap<>();
    }

    public GameLogTest(String topic, int expectedSize) {
        this.topic = topic;
        this.dataMap = CollectionUtils.newLinkedHashMapWithExpectedSize(expectedSize);
    }

    public GameLogTest(String topic, Map<String, Object> dataMap) {
        this.topic = topic;
        this.dataMap = dataMap;
    }

    public GameLogTest append(final String key, final String value) {
        dataMap.put(key, value);
        return this;
    }

    public GameLogTest append(final String key, final int value) {
        dataMap.put(key, value);
        return this;
    }

    public GameLogTest append(final String key, final long value) {
        dataMap.put(key, value);
        return this;
    }

    public GameLogTest append(final String key, final float value) {
        dataMap.put(key, value);
        return this;
    }

    public GameLogTest append(final String key, final double value) {
        dataMap.put(key, value);
        return this;
    }

    public GameLogTest append(final String key, final boolean value) {
        dataMap.put(key, value);
        return this;
    }

    public GameLogTest append(final String key, final byte value) {
        dataMap.put(key, value);
        return this;
    }

    public GameLogTest append(final String key, final short value) {
        dataMap.put(key, value);
        return this;
    }

    public GameLogTest append(final String key, final Object value) {
        dataMap.put(key, value);
        return this;
    }

    @Override
    public String topic() {
        return topic;
    }

    public Map<String, Object> getDataMap() {
        return dataMap;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) dataMap.get(key);
    }

    @Override
    public String toString() {
        return "GameLogTest{" +
                "topic='" + topic + '\'' +
                ", dataMap=" + dataMap +
                '}';
    }
}
