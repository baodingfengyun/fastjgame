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

import com.wjybxx.fastjgame.core.LogBuilder;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.LinkedList;
import java.util.List;

/**
 * 游戏日志内容构建器。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/27
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class GameLogBuilder implements LogBuilder {

    private final LogType logType;
    private final List<LogEntry> entryList = new LinkedList<>();

    public GameLogBuilder(LogType logType) {
        this.logType = logType;
    }

    public GameLogBuilder append(LogKey key, final String value) {
        entryList.add(new LogEntry(key, value));
        return this;
    }

    public GameLogBuilder append(LogKey key, final int value) {
        append(key, Integer.toString(value));
        return this;
    }

    public GameLogBuilder append(LogKey key, final long value) {
        append(key, Long.toString(value));
        return this;
    }

    public GameLogBuilder append(LogKey key, final float value) {
        append(key, Float.toString(value));
        return this;
    }

    public GameLogBuilder append(LogKey key, final double value) {
        append(key, Double.toString(value));
        return this;
    }

    public GameLogBuilder append(LogKey key, final Enum value) {
        append(key, value.toString());
        return this;
    }

    public GameLogBuilder append(LogKey key, final boolean value) {
        append(key, Boolean.toString(value));
        return this;
    }

    public GameLogBuilder append(LogKey key, byte value) {
        append(key, Byte.toString(value));
        return this;
    }

    public GameLogBuilder append(LogKey key, short value) {
        append(key, Short.toString(value));
        return this;
    }

    public GameLogBuilder append(LogKey key, char value) {
        append(key, Character.toString(value));
        return this;
    }

    public LogTopic getLogTopic() {
        return logType.topic;
    }

    public LogType getLogType() {
        return logType;
    }

    public List<LogEntry> getEntryList() {
        return entryList;
    }

    /**
     * 一个日志键值对，保持原始日志信息。
     * <p>
     * 该设计有好处自然也有坏处。
     * 好处：
     * 1. 可以将耗时操作转移到日志线程，如：字符过滤替换，Base64编码 - 但是它的比重大吗？
     * 2. 扩展将更为容易。
     * 坏处：
     * 1. 增加内存消耗，不管value使用String还是Object，都会增加内存消耗(产生中间对象)。
     */
    public static class LogEntry {

        public final LogKey key;
        public final String value;

        LogEntry(LogKey key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return "LogEntry{" +
                    "key=" + key +
                    ", value='" + value + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "GameLogBuilder{" +
                "logType=" + logType +
                ", entryList=" + entryList +
                '}';
    }
}
