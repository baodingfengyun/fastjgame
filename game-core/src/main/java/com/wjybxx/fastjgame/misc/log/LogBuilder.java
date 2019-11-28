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

/**
 * 日志内容构建器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/27
 * github - https://github.com/hl845740757
 */
public class LogBuilder {

    /**
     * 键与值之间的分隔符
     */
    private static final char KV_SEPARATOR = '=';
    /**
     * 键值对与键值对之间的分隔符
     */
    private static final char ENTRY_SEPARATOR = '&';

    /**
     * {@link #KV_SEPARATOR}的替换符
     */
    private static final char KV_SEPARATOR_REPLACER = '*';
    /**
     * {@link #ENTRY_SEPARATOR}的替换符
     */
    private static final char ENTRY_SEPARATOR_REPLACER = '*';

    /**
     * 游戏日志一般较长，尽量减少扩容操作 - 这个值可以在使用一段时间之后修正的更贴近。
     */
    private static final int BUILDER_INIT_CAPACITY = 150;

    /**
     * 日志类型
     */
    private final LogType logType;
    /**
     * 存放日志内容的builder
     */
    private final StringBuilder builder;

    public LogBuilder(LogType logType) {
        this(logType, BUILDER_INIT_CAPACITY);
    }

    public LogBuilder(LogType logType, int initCapacity) {
        this.logType = logType;
        this.builder = new StringBuilder(initCapacity);
    }

    public LogBuilder append(LogKey key, final String value) {
        appendKey(key);
        builder.append(doFilter(key, value));
        return this;
    }

    private void appendKey(LogKey key) {
        if (builder.length() > 0) {
            builder.append(ENTRY_SEPARATOR);
        }

        builder.append(key);
        builder.append(KV_SEPARATOR);
    }

    private String doFilter(LogKey key, String value) {
        if (key.doFilter) {
            value = value.replace(KV_SEPARATOR, KV_SEPARATOR_REPLACER);
            value = value.replace(ENTRY_SEPARATOR, ENTRY_SEPARATOR_REPLACER);
        }
        return value;
    }

    public LogBuilder append(LogKey key, final int value) {
        appendKey(key);
        builder.append(value);
        return this;
    }

    public LogBuilder append(LogKey key, final long value) {
        appendKey(key);
        builder.append(value);
        return this;
    }

    public LogBuilder append(LogKey key, final float value) {
        appendKey(key);
        builder.append(value);
        return this;
    }

    public LogBuilder append(LogKey key, final double value) {
        appendKey(key);
        builder.append(value);
        return this;
    }

    public LogBuilder append(LogKey key, final Enum value) {
        appendKey(key);
        builder.append(value);
        return this;
    }

    public LogBuilder append(LogKey key, final boolean value) {
        appendKey(key);
        builder.append(value);
        return this;
    }

    /**
     * 构建最终的日志内容
     *
     * @param curTimeMillis 当前系统时间
     * @return 日志完整内容
     */
    public String build(long curTimeMillis) {
        append(LogKey.LOG_TYPE, logType);
        append(LogKey.LOG_TIME, curTimeMillis);
        return builder.toString();
    }

    public LogType getLogType() {
        return logType;
    }

    public LogTopic getLogTopic() {
        return logType.topic;
    }
}
