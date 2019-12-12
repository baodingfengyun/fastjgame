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

/**
 * 一个日志键值对，用于将构建日志逻辑从逻辑线程转移到日志线程。
 * <p>
 * 该设计有好处自然也有坏处。
 * 好处：
 * 1. 可以将耗时操作转移到日志线程，但是它的比重大吗？ 耗时操作如：字符过滤替换，Base64编码。
 * 2. 扩展将更为容易。
 * 坏处：
 * 1. 增加内存消耗，不管value使用String还是Object，都会增加内存消耗(产生中间对象)。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/30
 * github - https://github.com/hl845740757
 */
public class LogEntry {

    public final LogKey logKey;
    public final String value;

    public LogEntry(LogKey logKey, String value) {
        this.logKey = logKey;
        this.value = value;
    }

}
