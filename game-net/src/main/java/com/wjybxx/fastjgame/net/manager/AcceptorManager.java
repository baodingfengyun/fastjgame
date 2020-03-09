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

package com.wjybxx.fastjgame.net.manager;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.net.local.DefaultLocalPort;
import com.wjybxx.fastjgame.net.local.LocalCodecHandler;
import com.wjybxx.fastjgame.net.local.LocalSessionImp;
import com.wjybxx.fastjgame.net.local.LocalTransferHandler;
import com.wjybxx.fastjgame.net.rpc.LazySerializeSupportHandler;
import com.wjybxx.fastjgame.net.rpc.OneWaySupportHandler;
import com.wjybxx.fastjgame.net.rpc.RpcSupportHandler;
import com.wjybxx.fastjgame.net.session.AbstractSession;
import com.wjybxx.fastjgame.net.session.DefaultSessionRegistry;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.session.SessionRegistry;
import com.wjybxx.fastjgame.net.socket.SocketConnectRequestEvent;
import com.wjybxx.fastjgame.net.socket.SocketEvent;
import com.wjybxx.fastjgame.net.socket.inner.InnerAcceptorHandler;
import com.wjybxx.fastjgame.net.socket.outer.OuterAcceptorHandler;
import com.wjybxx.fastjgame.net.utils.NetUtils;
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;

/**
 * session接收器 - 就像一个大型的{@link io.netty.channel.ServerChannel}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/9
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class AcceptorManager implements SessionRegistry {

    private NetManagerWrapper netManagerWrapper;
    private final DefaultSessionRegistry sessionRegistry = new DefaultSessionRegistry();

    @Inject
    public AcceptorManager() {
    }

    public void setManagerWrapper(NetManagerWrapper managerWrapper) {
        this.netManagerWrapper = managerWrapper;
    }

    @Override
    public void registerSession(@Nonnull AbstractSession session) {
        sessionRegistry.registerSession(session);
    }

    @Override
    @Nullable
    public AbstractSession removeSession(@Nonnull String sessionId) {
        return sessionRegistry.removeSession(sessionId);
    }

    @Override
    @Nullable
    public AbstractSession getSession(@Nonnull String sessionId) {
        return sessionRegistry.getSession(sessionId);
    }

    @Override
    public void onAppEventLoopTerminal(EventLoop appEventLoop) {
        sessionRegistry.onAppEventLoopTerminal(appEventLoop);
    }

    @Override
    public void closeAll() {
        sessionRegistry.closeAll();
    }

    /**
     * 线程退出时进行必要的清理
     */
    public void clean() {
        sessionRegistry.closeAll();
    }
    // --------------------------------------------------- socket ----------------------------------------------

    /**
     * 接收到一个请求建立连接事件
     *
     * @param connectRequestEvent 请求事件参数
     */
    public void onRcvConnectRequest(SocketConnectRequestEvent connectRequestEvent) {
        if (connectRequestEvent.getPortExtraInfo().getSessionConfig().isAutoReconnect()) {
            // 外网逻辑 - 带有消息确认机制
            OuterAcceptorHandler.onRcvConnectRequest(connectRequestEvent, netManagerWrapper, this);
        } else {
            // 内网逻辑 - 不带消息确认机制
            InnerAcceptorHandler.onRcvInnerConnectRequest(connectRequestEvent, netManagerWrapper, this);
        }
    }

    /**
     * 接收到一个socket消息
     *
     * @param socketEvent 消息事件参数
     */
    public void onSessionEvent(SocketEvent socketEvent) {
        final Session session = sessionRegistry.getSession(socketEvent.sessionId());
        if (session != null) {
            session.fireRead(socketEvent);
        } else {
            NetUtils.closeQuietly(socketEvent.channel());
        }
    }

    // -------------------------------------------------- 本地session支持 ------------------------------------------------

    /**
     * 接收到一个连接请求
     *
     * @param localPort 本地“端口”
     * @param sessionId session唯一标识
     * @return session
     * @throws IOException error
     */
    LocalSessionImp onRcvConnectRequest(DefaultLocalPort localPort, String sessionId) throws IOException {
        // 端口已关闭
        if (!localPort.isActive()) {
            throw new IOException("local port closed");
        }
        if (sessionRegistry.getSession(sessionId) != null) {
            throw new IOException("session " + sessionId + " is already registered");
        }
        // 创建session
        LocalSessionImp session = new LocalSessionImp(localPort.getNetContext(), sessionId, localPort.getLocalConfig(),
                netManagerWrapper, this);

        // 创建管道
        session.pipeline()
                .addLast(new LocalTransferHandler())
                .addLast(new LocalCodecHandler())
                .addLast(new LazySerializeSupportHandler())
                .addLast(new OneWaySupportHandler());

        // 是否开启rpc
        if (localPort.getLocalConfig().isRpcAvailable()) {
            session.pipeline().addLast(new RpcSupportHandler());
        }

        return session;
    }

}
