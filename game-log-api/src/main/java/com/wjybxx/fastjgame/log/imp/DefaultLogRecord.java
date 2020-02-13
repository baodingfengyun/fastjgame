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

package com.wjybxx.fastjgame.log.imp;

import java.util.Map;

/**
 * 默认的日志记录视图对象
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/10
 * github - https://github.com/hl845740757
 */
public class DefaultLogRecord {

    private final String topic;
    private final Map<String, Object> dataMap;

    DefaultLogRecord(String topic, Map<String, Object> dataMap) {
        this.topic = topic;
        this.dataMap = dataMap;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) dataMap.get(key);
    }

    public String getTopic() {
        return topic;
    }

    public Map<String, Object> getDataMap() {
        return dataMap;
    }

    @Override
    public String toString() {
        return "DefaultLogRecord{" +
                "topic='" + topic + '\'' +
                ", dataMap=" + dataMap +
                '}';
    }
}
