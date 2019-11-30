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

import java.util.LinkedList;

/**
 * 日志内容构建器。
 * 1. 它通过分隔符的方式组织内容， '='分隔键和值，'&'分隔键值对。
 * 2. 如果有内容是Base64编码的，那么'='可能造成一些问题。
 * 3. 目前的实现是在当前线程(逻辑线程)进行过滤，可能影响逻辑线程性能。
 * <p>
 * 直接构建为字符串是方便阅读，更安全的方式是序列化为字节数组。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/27
 * github - https://github.com/hl845740757
 */
public class LogBuilder {

    private final LogType logType;
    private final LinkedList<LogEntry> entryList = new LinkedList<>();

    public LogBuilder(LogType logType) {
        this.logType = logType;
    }

    public LogBuilder append(LogKey key, final String value) {
        entryList.add(new LogEntry(key, value));
        return this;
    }

    public LogBuilder append(LogKey key, final int value) {
        append(key, Integer.toString(value));
        return this;
    }

    public LogBuilder append(LogKey key, final long value) {
        append(key, Long.toString(value));
        return this;
    }

    public LogBuilder append(LogKey key, final float value) {
        append(key, Float.toString(value));
        return this;
    }

    public LogBuilder append(LogKey key, final double value) {
        append(key, Double.toString(value));
        return this;
    }

    public LogBuilder append(LogKey key, final Enum value) {
        append(key, value.toString());
        return this;
    }

    public LogBuilder append(LogKey key, final boolean value) {
        append(key, Boolean.toString(value));
        return this;
    }

    public LogType getLogType() {
        return logType;
    }

    public LogTopic getLogTopic() {
        return logType.topic;
    }

    public LinkedList<LogEntry> getEntryList() {
        return entryList;
    }
}
