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

import com.wjybxx.fastjgame.net.Session;

/**
 * 单向消息处理器
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
@FunctionalInterface
public interface OneWayMessageHandler<T> {

    /**
     * 当接收到指定单向消息时。
     *
     * @param session 发送方的信息，可以通过session返回消息，发起rpc请求等。
     * @param message 单向消息内容
     */
    void onMessage(Session session, T message);

}
