/*
 * Copyright 2019 wjybxx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wjybxx.fastjgame.misc;

/**
 * 建立连接失败的原因
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/9 17:03
 * github - https://github.com/hl845740757
 */
public enum FailReason {
    /**
     * server不存在
     */
    SERVER_NOT_EXIST,
    /**
     * ack校验错误
     */
    ACK_ERROR,
    /**
     * 旧请求
     */
    OLD_REQUEST,
}
