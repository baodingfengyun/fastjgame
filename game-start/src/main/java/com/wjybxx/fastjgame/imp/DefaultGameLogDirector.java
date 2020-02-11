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

import com.wjybxx.fastjgame.core.LogDirector;
import com.wjybxx.fastjgame.core.LogRecordDTO;
import com.wjybxx.fastjgame.misc.log.GameLogBuilder;
import com.wjybxx.fastjgame.misc.log.LogKey;

import javax.annotation.Nonnull;
import java.util.regex.Pattern;

/**
 * 默认的游戏日志建造指挥官实现。
 * 1. 它通过分隔符的方式组织内容， '='分隔键和值，'&'分隔键值对。
 * 2. 如果有内容是Base64编码的，那么'='可能造成一些问题。
 * 3. 构建为字符串是方便阅读(也方便检索)，序列化为字节数组虽然安全性好，但是不方便阅读和检索。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/30
 * github - https://github.com/hl845740757
 */
public class DefaultGameLogDirector implements LogDirector<GameLogBuilder> {

    /**
     * 替换换行符，回车符，制表符，反斜杠，'&' '='
     */
    private static final Pattern PATTERN = Pattern.compile("[\\r\\n\\t\\v\\f\\\\&=]");

    /**
     * 键与值之间的分隔符
     */
    static final char KV_SEPARATOR = '=';
    /**
     * 键值对与键值对之间的分隔符
     */
    static final char ENTRY_SEPARATOR = '&';
    /**
     * 替换字符
     */
    private static final String REPLACEMENT = "_";

    /**
     * 游戏日志一般较长，合适的初始容量可以减少扩容操作。
     * 这个值可以在使用一段时间之后修正的更贴近。
     */
    private static final int BUILDER_INIT_CAPACITY = 300;
    /**
     * 存放日志内容的builder
     */
    private final StringBuilder stringBuilder = new StringBuilder(BUILDER_INIT_CAPACITY);

    @Nonnull
    @Override
    public LogRecordDTO build(GameLogBuilder builder) {
        stringBuilder.setLength(0);

        appendKey(LogKey.LOG_TYPE);
        stringBuilder.append(builder.getLogType().toString());

        appendKey(LogKey.LOG_TIME);
        stringBuilder.append(System.currentTimeMillis());

        for (GameLogBuilder.LogEntry logEntry : builder.getEntryList()) {
            appendKey(logEntry.key);
            stringBuilder.append(doFilter(logEntry.key, logEntry.value));
        }

        return new LogRecordDTO(builder.getLogTopic().toString(), stringBuilder.toString());
    }

    private void appendKey(LogKey key) {
        if (stringBuilder.length() > 0) {
            stringBuilder.append(ENTRY_SEPARATOR);
        }

        stringBuilder.append(key.name());
        stringBuilder.append(KV_SEPARATOR);
    }

    private String doFilter(LogKey key, String value) {
        if (key.doFilter) {
            return PATTERN.matcher(value).replaceAll(REPLACEMENT);
        } else {
            return value;
        }
    }
}
