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

package com.wjybxx.fastjgame.manager;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.eventloop.NetContext;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.SessionRegistry;
import com.wjybxx.fastjgame.net.common.OneWaySupportHandler;
import com.wjybxx.fastjgame.net.common.RpcSupportHandler;
import com.wjybxx.fastjgame.net.local.*;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.session.SessionHandler;
import com.wjybxx.fastjgame.net.socket.*;
import com.wjybxx.fastjgame.net.socket.inner.InnerConnectorHandler;
import com.wjybxx.fastjgame.utils.NetUtils;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

import java.io.IOException;

/**
 * session连接管理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/4
 * github - https://github.com/hl845740757
 */
public class ConnectorManager {

    private NetManagerWrapper netManagerWrapper;
    private final NettyThreadManager nettyThreadManager;
    private final SessionRegistry sessionRegistry = new SessionRegistry();

    @Inject
    public ConnectorManager(NettyThreadManager nettyThreadManager) {
        this.nettyThreadManager = nettyThreadManager;
    }

    public void setNetManagerWrapper(NetManagerWrapper netManagerWrapper) {
        this.netManagerWrapper = netManagerWrapper;
    }

    public void tick() {
        sessionRegistry.tick();
    }

    public Session connect(String sessionId, long remoteGuid, HostAndPort remoteAddress, SocketSessionConfig config,
                           ChannelInitializer<SocketChannel> initializer, NetContext netContext) throws IOException {
        Session existSession = sessionRegistry.getSession(sessionId);
        if (existSession != null) {
            throw new IOException("session " + sessionId + " already registered");
        }

        final SocketSessionImp session = new SocketSessionImp(netContext, sessionId, remoteGuid, config, netManagerWrapper);
        // 注册session
        sessionRegistry.registerSession(session);

        ChannelFuture channelFuture = nettyThreadManager.connectAsyn(remoteAddress,
                config.sndBuffer(),
                config.rcvBuffer(),
                config.connectTimeoutMs(),
                initializer);

        // 异步建立连接
        session.pipeline().addLast(new InnerConnectorHandler(channelFuture));
        return session;
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

    }

    /**
     * 接收到一个内网建立连接应答
     */
    private void onRcvInnerConnectResponse(SocketSession session, SocketConnectResponseEvent connectResponseEvent) {
        final SessionHandler first = session.pipeline().first();
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
     * @param messageEvent 消息事件参数
     */
    public void onSessionEvent(SocketEvent messageEvent) {
        final Session session = sessionRegistry.getSession(messageEvent.sessionId());
        if (session != null) {
            session.fireRead(messageEvent);
        } else {
            NetUtils.closeQuietly(messageEvent.channel());
        }
    }

    public Session connectLocal(String sessionId, long remoteGuid, DefaultLocalPort localPort,
                                LocalSessionConfig config, NetContext netContext) throws IOException {
        // 会话已存在
        if (sessionRegistry.getSession(sessionId) != null) {
            throw new IOException("session " + sessionId + " already registered");
        }
        final LocalSessionImp remoteSession = netManagerWrapper.getAcceptorManager().onRcvConnectRequest(localPort, sessionId, netContext.localGuid());

        // 创建session并保存
        LocalSessionImp session = new LocalSessionImp(netContext, sessionId, remoteGuid, config, netManagerWrapper);
        sessionRegistry.registerSession(session);

        // 初始化管道，入站 从上到下，出站 从下往上
        session.pipeline()
                .addLast(new LocalTransferHandler())
                .addLast(new LocalCodecHandler())
                .addLast(new OneWaySupportHandler())
                .addLast(new RpcSupportHandler());

        // 保存双方引用 - 实现传输
        setRemoteSession(session, remoteSession);
        setRemoteSession(remoteSession, session);

        // 激活双方
        if (session.tryActive() && remoteSession.tryActive()) {
            session.pipeline().fireSessionActive();
            remoteSession.pipeline().fireSessionActive();
        } else {
            session.closeForcibly();
            remoteSession.closeForcibly();
        }
        return session;
    }

    private void setRemoteSession(Session session, Session remoteSession) {
        final LocalTransferHandler first = (LocalTransferHandler) session.pipeline().first();
        assert null != first;
        first.setRemoteSession(remoteSession);
    }

    public void onUserEventLoopTerminal(EventLoop userEventLoop) {
        sessionRegistry.onUserEventLoopTerminal(userEventLoop);
    }

    public void clean() {
        sessionRegistry.closeAll();
    }

}
