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

package com.wjybxx.fastjgame.log;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.LinkedList;

/**
 * 日志内容构建器。
 * 注意：
 * 1. 每条日志务必使用新的对象，发布之后再修改会导致线程安全问题。(加了检测又去掉了，我相信很容易遵守)
 * 2. 添加完数据之后，调用{@link LogProducerEventLoop#publish(LogBuilder)}发布自己。
 * 3. {@link #append(LogKey, String)}是关键方法，其它方法都是它的简单包装，添加新的api时需要注意。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/27
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
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

    LinkedList<LogEntry> getEntryList() {
        return entryList;
    }
}
