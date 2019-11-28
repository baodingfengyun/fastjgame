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
 * 日志属性 - 可以考虑根据表格生成。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/27
 * github - https://github.com/hl845740757
 */
public enum LogKey {

    /**
     * 日志类型
     */
    LOG_TYPE,

    /**
     * 打印日志的时间
     */
    LOG_TIME,

    /**
     * 玩家名字
     */
    playerName,
    /**
     * 玩家唯一标识
     */
    playerGuid,

    /**
     * 聊天内容
     */
    chatContent(true);

    /**
     * 是否需要对该键对应的值进行过滤(检测特殊符号)
     * 由于只有部分特定日志属性可能需要过滤，因此默认不过滤，需要过滤的属性显式指定。
     */
    public final boolean doFilter;

    LogKey() {
        this(false);
    }

    LogKey(boolean doFilter) {
        this.doFilter = doFilter;
    }
}
