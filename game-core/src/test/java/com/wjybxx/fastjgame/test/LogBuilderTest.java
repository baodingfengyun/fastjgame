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

package com.wjybxx.fastjgame.test;

import com.wjybxx.fastjgame.misc.log.LogBuilder;
import com.wjybxx.fastjgame.misc.log.LogKey;
import com.wjybxx.fastjgame.misc.log.LogType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志构建器测试
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/27
 * github - https://github.com/hl845740757
 */
public class LogBuilderTest {

    /**
     * 替换换行符，回车符，制表符，反斜杠，'&' '='
     */
    private static final Pattern PATTERN = Pattern.compile("[\\s\\\\&=?]");

    public static void main(String[] args) {
        final String content = new LogBuilder(LogType.CHAT)
                .append(LogKey.playerGuid, 123456789)
                .append(LogKey.playerName, "wjybxx")
                .append(LogKey.chatContent, "&=\r\n\t\f\\rn\\\\rn&=")
                .build(System.currentTimeMillis());

        System.out.println(content);
    }
}
