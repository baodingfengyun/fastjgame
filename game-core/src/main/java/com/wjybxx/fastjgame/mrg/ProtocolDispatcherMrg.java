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

package com.wjybxx.fastjgame.mrg;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.annotation.WorldSingleton;
import com.wjybxx.fastjgame.misc.DefaultRpcCallDispatcher;
import com.wjybxx.fastjgame.misc.RpcCall;
import com.wjybxx.fastjgame.misc.VoidRpcResponseChannel;
import com.wjybxx.fastjgame.net.ProtocolDispatcher;
import com.wjybxx.fastjgame.net.RpcRequestContext;
import com.wjybxx.fastjgame.net.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * 协议分发管理器。
 * 实现TCP/Ws长链接的 [单向消息] 和 [rpc请求] 的分发。
 * 注意：不同的world有不同的协议处理器，单例级别为world级别。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
@WorldSingleton
@NotThreadSafe
public class ProtocolDispatcherMrg extends DefaultRpcCallDispatcher implements ProtocolDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(ProtocolDispatcherMrg.class);

    @Inject
    public ProtocolDispatcherMrg() {

    }

    @Override
    public final void postOneWayMessage(Session session, @Nullable Object message) {
        if (null == message){
            logger.warn("{} - {} send null message", session.remoteRole(), session.remoteGuid());
            return;
        }
        if (message instanceof RpcCall) {
            post(session, (RpcCall) message, VoidRpcResponseChannel.INSTANCE);
        } else {
            dispatchOneWayMessage0(session, message);
        }
    }

    /**
     * 分发一个单向消息
     * @param session 所在的会话
     * @param message 单向消息
     * @throws Exception error
     */
    protected void dispatchOneWayMessage0(Session session, @Nonnull Object message) {
        logger.info("unhandled {}-{} message {}", session.remoteRole(), session.remoteGuid(), message.getClass().getSimpleName());
    }

    @Override
    public final void postRpcRequest(Session session, @Nullable Object request, RpcRequestContext context) {
        if (null == request){
            logger.warn("{} - {} send null request", session.remoteRole(), session.remoteGuid());
            return;
        }
        // 目前版本直接session创建responseChannel，后期再考虑缓存的事情
        post(session, (RpcCall) request, session.newResponseChannel(context));
    }

}
