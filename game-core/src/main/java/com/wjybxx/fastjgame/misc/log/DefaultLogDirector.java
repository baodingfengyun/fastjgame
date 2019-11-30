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

import javax.annotation.Nonnull;
import java.util.regex.Pattern;

/**
 * 建造指挥者默认实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/30
 * github - https://github.com/hl845740757
 */
public class DefaultLogDirector implements LogDirector {

    /**
     * 替换换行符，回车符，制表符，反斜杠，'&' '='
     */
    private static final Pattern PATTERN = Pattern.compile("[\\s\\\\&=?]");

    /**
     * 键与值之间的分隔符
     */
    private static final char KV_SEPARATOR = '=';
    /**
     * 键值对与键值对之间的分隔符
     */
    private static final char ENTRY_SEPARATOR = '&';
    /**
     * 替换字符
     */
    private static final String REPLACEMENT = "_";

    /**
     * 游戏日志一般较长，尽量减少扩容操作 - 这个值可以在使用一段时间之后修正的更贴近。
     */
    private static final int BUILDER_INIT_CAPACITY = 150;
    /**
     * 存放日志内容的builder
     */
    private final StringBuilder stringBuilder = new StringBuilder(BUILDER_INIT_CAPACITY);

    @Nonnull
    @Override
    public String build(LogBuilder logBuilder, long curTimeMillis) {
        appendKey(LogKey.LOG_TYPE);
        stringBuilder.append(logBuilder.getLogType().toString());

        appendKey(LogKey.LOG_TIME);
        stringBuilder.append(curTimeMillis);

        for (LogEntry logEntry : logBuilder.getEntryList()) {
            appendKey(logEntry.logKey);
            stringBuilder.append(doFilter(logEntry.logKey, logEntry.value));
        }

        return stringBuilder.toString();
    }

    @Override
    public void reset() {
        stringBuilder.setLength(0);
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
