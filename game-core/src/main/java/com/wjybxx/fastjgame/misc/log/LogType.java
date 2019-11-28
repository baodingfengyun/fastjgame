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
 * 日志类型 - 可以考虑根据表格生成。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/27
 * github - https://github.com/hl845740757
 */
public enum LogType {

    /**
     * 测试用
     */
    TEST(LogTopic.TEST),

    /**
     * 创建角色日志
     */
    CREATE_ROLE,
    /**
     * 删除角色日志
     */
    DELETE_ROLE,

    /**
     * 登录日志
     */
    LOGIN,
    /**
     * 登出日志
     */
    LOGOUT,

    /**
     * 角色升级
     */
    LEVEL_UP,

    /**
     * 角色重命名
     */
    RENAME,

    /**
     * 聊天日志
     */
    CHAT,
    ;

    /**
     * 日志关联的topic
     * 由于角色日志占大多数，因此默认为角色日志
     */
    public final LogTopic topic;

    LogType() {
        this(LogTopic.PLAYER);
    }

    LogType(LogTopic topic) {
        this.topic = topic;
    }
}
