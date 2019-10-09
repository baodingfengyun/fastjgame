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

package com.wjybxx.fastjgame.misc;

import com.wjybxx.fastjgame.net.session.Session;

/**
 * 连接的发起方的处理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/9
 * github - https://github.com/hl845740757
 */
public interface ConnectorHandler {

    /**
     * 建立连接失败
     *
     * @param sessionId  分配的sessionId
     * @param remoteGuid 远程对端标识
     */
    void onConnectFailed(String sessionId, long remoteGuid);

    /**
     * 当会话第一次成功建立时调用，表示会话正式可用，只会调用一次
     * 网络底层的消息确认机制不会触发这里
     *
     * @param session 注册时的会话信息
     */
    void onConnectSuccess(Session session);

    /**
     * 当会话彻底断开连接(无法继续断线重连)时会被调用，只会调用一次
     * 只有调用过{@link #onConnectSuccess(Session)}方法，才会走到该方法
     *
     * @param session 注册时的会话信息
     */
    void onDisconnect(Session session);
}
