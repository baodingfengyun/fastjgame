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
import com.wjybxx.fastjgame.annotation.EventLoopSingleton;
import com.wjybxx.fastjgame.annotation.WorldSingleton;
import com.wjybxx.fastjgame.misc.*;
import com.wjybxx.fastjgame.net.ProtocolDispatcher;
import com.wjybxx.fastjgame.net.RpcResponseChannel;
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
@EventLoopSingleton
@NotThreadSafe
public class ProtocolDispatcherMrg implements RpcFunctionRegistry, RpcCallDispatcher, ProtocolDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(ProtocolDispatcherMrg.class);

    private final DefaultRpcCallDispatcher rpcCallDispatcher = new DefaultRpcCallDispatcher();
    private boolean shutdown = false;

    @Inject
    public ProtocolDispatcherMrg() {

    }

    @Override
    public void register(int methodKey, @Nonnull RpcFunction function) {
        rpcCallDispatcher.register(methodKey, function);
    }

    @Override
    public void release() {
        rpcCallDispatcher.release();
        shutdown = true;
    }

    @Override
    public void post(@Nonnull Session session, @Nonnull RpcCall rpcCall, @Nonnull RpcResponseChannel<?> rpcResponseChannel) {
        rpcCallDispatcher.post(session, rpcCall, rpcResponseChannel);
    }

    @Override
    public final void postRpcRequest(Session session, @Nullable Object request, @Nonnull RpcResponseChannel<?> responseChannel) {
        if (shutdown) {
            return;
        }
        if (null == request) {
            logger.warn("{} - {} send null request", session.remoteRole(), session.remoteGuid());
            return;
        }
        rpcCallDispatcher.post(session, (RpcCall) request, responseChannel);
    }

    @Override
    public final void postOneWayMessage(Session session, @Nullable Object message) {
        if (shutdown) {
            return;
        }
        if (null == message) {
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
     *
     * @param session 所在的会话
     * @param message 单向消息
     */
    protected void dispatchOneWayMessage0(Session session, @Nonnull Object message) {
        logger.info("unhandled {}-{} message {}", session.remoteRole(), session.remoteGuid(), message.getClass().getSimpleName());
    }
}
