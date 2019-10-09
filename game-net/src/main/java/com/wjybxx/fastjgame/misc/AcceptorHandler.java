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

import javax.annotation.Nullable;

/**
 * 监听端口的一方相应的处理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/9
 * github - https://github.com/hl845740757
 */
public interface AcceptorHandler {

    /**
     * 接收到一个连接
     *
     * @param session   建立的session
     * @param extension 建立连接时发来的额外信息
     */
    void onAccept(Session session, @Nullable Object extension);

    /**
     * 当会话彻底断开连接时会被调用，只会调用一次。
     * 只有调用过{@link #onAccept(Session, Object)}方法，才会走到该方法。
     *
     * @param session 断开连接的session
     */
    void onDisconnect(Session session);

}
