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
import com.wjybxx.fastjgame.net.local.*;
import com.wjybxx.fastjgame.net.misc.HostAndPort;
import com.wjybxx.fastjgame.net.misc.NetContext;
import com.wjybxx.fastjgame.net.rpc.LazySerializeSupportHandler;
import com.wjybxx.fastjgame.net.rpc.OneWaySupportHandler;
import com.wjybxx.fastjgame.net.rpc.RpcSupportHandler;
import com.wjybxx.fastjgame.net.session.*;
import com.wjybxx.fastjgame.net.socket.*;
import com.wjybxx.fastjgame.net.socket.inner.InnerConnectorHandler;
import com.wjybxx.fastjgame.net.socket.outer.OuterConnectorHandler;
import com.wjybxx.fastjgame.net.utils.NetUtils;
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.concurrent.Promise;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

/**
 * session连接管理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/4
 * github - https://github.com/hl845740757
 */
public class ConnectorManager implements SessionRegistry {

    private NetManagerWrapper netManagerWrapper;
    private final NettyThreadManager nettyThreadManager;
    private final DefaultSessionRegistry sessionRegistry = new DefaultSessionRegistry();

    @Inject
    public ConnectorManager(NettyThreadManager nettyThreadManager) {
        this.nettyThreadManager = nettyThreadManager;
    }

    public void setNetManagerWrapper(NetManagerWrapper netManagerWrapper) {
        this.netManagerWrapper = netManagerWrapper;
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
     * 退出前进行必要的清理
     */
    public void clean() {
        sessionRegistry.closeAll();
    }
    // --------------------------------------------分割线 -----------------------------------------------------

    public void connect(final ConnectRemoteRequest request) {
        final String sessionId = request.getSessionId();
        final long remoteGuid = request.getRemoteGuid();
        final HostAndPort remoteAddress = request.getRemoteAddress();
        final SocketSessionConfig config = request.getConfig();
        final ChannelInitializer<SocketChannel> initializer = request.getInitializer();
        final NetContext netContext = request.getNetContext();
        final Promise<Session> connectPromise = request.getConnectPromise();

        Session existSession = sessionRegistry.getSession(sessionId);
        if (existSession != null) {
            connectPromise.tryFailure(new IOException("session " + sessionId + " already registered"));
            return;
        }

        final SocketSessionImp session = new SocketSessionImp(netContext, sessionId, remoteGuid, config,
                netManagerWrapper, this);

        if (config.isAutoReconnect()) {
            // 异步建立连接
            session.pipeline().addLast(new OuterConnectorHandler(remoteAddress, initializer, nettyThreadManager, connectPromise));
        } else {
            ChannelFuture channelFuture = nettyThreadManager.connectAsyn(remoteAddress,
                    config.sndBuffer(),
                    config.rcvBuffer(),
                    config.connectTimeoutMs(),
                    initializer);

            // 异步建立连接
            session.pipeline().addLast(new InnerConnectorHandler(channelFuture, connectPromise));
        }
    }

    /**
     * 接收到一个建立连接响应
     *
     * @param connectResponseEvent 连接响应事件参数
     */
    public void onRcvConnectResponse(SocketConnectResponseEvent connectResponseEvent) {
        final SocketSession session = (SocketSession) sessionRegistry.getSession(connectResponseEvent.sessionId());
        if (session == null) {
            return;
        }
        if (session.config().isAutoReconnect()) {
            onRcvOuterConnectResponse(session, connectResponseEvent);
        } else {
            onRcvInnerConnectResponse(session, connectResponseEvent);
        }
    }

    private void onRcvOuterConnectResponse(SocketSession session, SocketConnectResponseEvent connectResponseEvent) {
        final SessionHandler first = session.pipeline().firstHandler();
        if (first instanceof OuterConnectorHandler) {
            // 第一个handler是用于建立连接的handler
            session.fireRead(connectResponseEvent);
        } else {
            // 错误的消息
            NetUtils.closeQuietly(connectResponseEvent.channel());
        }
    }

    /**
     * 接收到一个内网建立连接应答
     */
    private void onRcvInnerConnectResponse(SocketSession session, SocketConnectResponseEvent connectResponseEvent) {
        final SessionHandler first = session.pipeline().firstHandler();
        if (first instanceof InnerConnectorHandler) {
            // 此时session应该还未建立成功，第一个handler应该是用于建立连接的handler
            session.fireRead(connectResponseEvent);
        } else {
            // 错误的消息
            NetUtils.closeQuietly(connectResponseEvent.channel());
        }
    }

    /**
     * 接收到一个socket消息
     *
     * @param event 消息事件参数
     */
    public void onSessionEvent(SocketEvent event) {
        final Session session = sessionRegistry.getSession(event.sessionId());
        if (session != null) {
            session.fireRead(event);
        } else {
            NetUtils.closeQuietly(event.channel());
        }
    }

    public void connectLocal(ConnectLocalRequest request) {
        final String sessionId = request.getSessionId();
        final long remoteGuid = request.getRemoteGuid();
        final DefaultLocalPort localPort = request.getLocalPort();
        final LocalSessionConfig config = request.getConfig();
        final NetContext netContext = request.getNetContext();
        final Promise<Session> connectPromise = request.getConnectPromise();

        // 会话已存在
        if (sessionRegistry.getSession(sessionId) != null) {
            connectPromise.tryFailure(new IOException("session " + sessionId + " already registered"));
            return;
        }
        try {
            final LocalSessionImp remoteSession = netManagerWrapper.getAcceptorManager().onRcvConnectRequest(localPort, sessionId, netContext.localGuid());

            // 创建session
            LocalSessionImp session = new LocalSessionImp(netContext, sessionId, remoteGuid, config,
                    netManagerWrapper, this);

            // 初始化管道，入站 从上到下，出站 从下往上
            session.pipeline()
                    .addLast(new LocalTransferHandler())
                    .addLast(new LocalCodecHandler())
                    .addLast(new LazySerializeSupportHandler())
                    .addLast(new OneWaySupportHandler());

            // 是否开启rpc
            if (config.isRpcAvailable()) {
                session.pipeline().addLast(new RpcSupportHandler());
            }

            // 保存双方引用 - 实现传输
            setRemoteSession(session, remoteSession);
            setRemoteSession(remoteSession, session);

            // 激活双方
            if (connectPromise.trySuccess(session)) {
                session.tryActive();
                remoteSession.tryActive();

                session.pipeline().fireSessionActive();
                remoteSession.pipeline().fireSessionActive();
            } else {
                session.closeForcibly();
                remoteSession.closeForcibly();
            }
        } catch (Throwable e) {
            connectPromise.tryFailure(e);
        }
    }

    private void setRemoteSession(Session session, Session remoteSession) {
        final LocalTransferHandler first = (LocalTransferHandler) session.pipeline().firstHandler();
        assert null != first;
        first.setRemoteSession(remoteSession);
    }

}