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
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.concurrent.Promise;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.PortRange;
import com.wjybxx.fastjgame.misc.SessionRegistry;
import com.wjybxx.fastjgame.net.NetContext;
import com.wjybxx.fastjgame.net.RoleType;
import com.wjybxx.fastjgame.net.Session;
import com.wjybxx.fastjgame.net.handler.OneWaySupportHandler;
import com.wjybxx.fastjgame.net.handler.RpcSupportHandler;
import com.wjybxx.fastjgame.net.handler.SessionLifeCycleAwareHandler;
import com.wjybxx.fastjgame.net.local.*;
import com.wjybxx.fastjgame.net.socket.SocketPort;
import com.wjybxx.fastjgame.net.socket.SocketSessionConfig;
import com.wjybxx.fastjgame.net.socket.ordered.OrderedConnectRequestEvent;
import com.wjybxx.fastjgame.net.socket.ordered.OrderedConnectResponseEvent;
import com.wjybxx.fastjgame.net.socket.ordered.OrderedMessageEvent;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.BindException;

/**
 * session管理器 -  算是一个大黑板
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/9
 * github - https://github.com/hl845740757
 */
public class SessionManager {

    private NetManagerWrapper netManagerWrapper;
    private final NetTimeManager netTimeManager;
    private final NetTimerManager netTimerManager;
    private final AcceptorManager acceptorManager;
    private final SessionRegistry sessionRegistry = new SessionRegistry();

    @Inject
    public SessionManager(NetTimeManager netTimeManager, NetTimerManager netTimerManager, AcceptorManager acceptorManager) {
        this.netTimeManager = netTimeManager;
        this.netTimerManager = netTimerManager;
        this.acceptorManager = acceptorManager;
    }

    public void setManagerWrapper(NetManagerWrapper managerWrapper) {
        this.netManagerWrapper = managerWrapper;
    }

    public void tick() {
        sessionRegistry.tick();
    }

    // --------------------------------------------- 事件处理 -----------------------------------------

    public void onRcvConnectRequest(OrderedConnectRequestEvent eventParam) {

    }

    public void onRcvConnectResponse(OrderedConnectResponseEvent eventParam) {

    }

    public void onRcvMessage(OrderedMessageEvent eventParam) {
        final Session session = sessionRegistry.getSession(eventParam.localGuid(), eventParam.remoteGuid());
        if (session != null && session.isActive()) {
            // session 存活的情况下才读取消息
            session.fireRead(eventParam);
        }
    }

    public void registerSession(Session session) {
        sessionRegistry.registerSession(session);
    }

    @Nullable
    public Session getSession(long localGuid, long remoteGuid) {
        return sessionRegistry.getSession(localGuid, remoteGuid);
    }

    @Nullable
    public Session removeSession(long localGuid, long remoteGuid) {
        return sessionRegistry.removeSession(localGuid, remoteGuid);
    }

    public boolean removeSession(Session session) {
        return sessionRegistry.removeSession(session.localGuid(), session.remoteGuid()) != null;
    }

    public boolean containsSession(Session session) {
        return getSession(session.localGuid(), session.remoteGuid()) != null;
    }


    public void onUserEventLoopTerminal(EventLoop userEventLoop) {

    }

    public void closeUserSession(long localGuid) {

    }


    // ---------------------------------------------------------------

    public LocalPort bindLocal(NetContext netContext, LocalSessionConfig config) {
        return new DefaultLocalPort(netContext, config, this);
    }

    private void connectLocal(NetContext netContext, DefaultLocalPort localPort, LocalSessionConfig config, Promise<Session> promise) {
        // 端口已关闭
        if (!localPort.active) {
            promise.tryFailure(new IOException("local port closed"));
            return;
        }

        final long localGuid = netContext.localGuid();
        final long remoteGuid = localPort.localGuid();
        // 会话已存在
        if (sessionRegistry.getSession(localGuid, remoteGuid) != null ||
                sessionRegistry.getSession(remoteGuid, localGuid) != null) {
            promise.tryFailure(new IOException("session already registered"));
            return;
        }
        // 创建session
        LocalSessionImp connectorSession = new LocalSessionImp(netContext, netManagerWrapper, config);
        LocalSessionImp acceptorSession = new LocalSessionImp(localPort.localContext, netManagerWrapper, localPort.localConfig);

        // 保存双方引用
        connectorSession.setRemoteSession(acceptorSession);
        acceptorSession.setRemoteSession(connectorSession);

        // 初始化管道
        initLocalSessionPipeline(connectorSession, acceptorSession);
        initLocalSessionPipeline(acceptorSession, connectorSession);

        if (promise.trySuccess(connectorSession)) {
            // 保存
            sessionRegistry.registerSession(connectorSession);
            sessionRegistry.registerSession(acceptorSession);

            // 传递激活事件
            connectorSession.pipeline().fireSessionActive();
            acceptorSession.pipeline().fireSessionActive();
        }
        // else 丢弃session
    }

    private static void initLocalSessionPipeline(LocalSession session, LocalSession remoteSession) {
        // 入站 从上到下
        // 出站 从下往上
        session.pipeline()
                .addLast(new LocalTransferHandler(remoteSession))
                .addLast(new LocalCodecHandler())
                .addLast(new OneWaySupportHandler())
                .addLast(new RpcSupportHandler())
                .addLast(new SessionLifeCycleAwareHandler())
                .fireInit();
    }

    public void connect(NetContext netContext, long remoteGuid, RoleType remoteRole, HostAndPort remoteAddress,
                        SocketSessionConfig config, ChannelInitializer<SocketChannel> initializer,
                        Promise<Session> promise) {
        ChannelFuture channelFuture = acceptorManager.connectAsyn(remoteAddress, config.sndBuffer(), config.rcvBuffer(), initializer)
                .syncUninterruptibly();

    }

    public SocketPort bindRange(String host, PortRange portRange, SocketSessionConfig config,
                                ChannelInitializer<SocketChannel> initializer) throws BindException {
        return acceptorManager.bindRange(host, portRange, config.sndBuffer(), config.rcvBuffer(), initializer);
    }

    public void clean() {

    }


    private static class DefaultLocalPort implements LocalPort {

        /**
         * 监听者的信息
         */
        private final NetContext localContext;
        /**
         * session配置信息
         */
        private final LocalSessionConfig localConfig;
        /**
         * 建立连接的管理器
         */
        private final SessionManager sessionManager;
        /**
         * 激活状态
         */
        private volatile boolean active = true;

        private DefaultLocalPort(NetContext localContext, LocalSessionConfig localConfig, SessionManager sessionManager) {
            this.localContext = localContext;
            this.localConfig = localConfig;
            this.sessionManager = sessionManager;
        }

        public long localGuid() {
            return localContext.localGuid();
        }

        @Override
        public ListenableFuture<Session> connect(@Nonnull NetContext netContext, @Nonnull LocalSessionConfig config) {
            // 提交到绑定端口的用户所在的NetEventLoop - 消除同步的关键
            final Promise<Session> promise = localContext.netEventLoop().newPromise();
            localContext.netEventLoop().execute(() -> {
                sessionManager.connectLocal(netContext, this, config, promise);
            });
            return promise;
        }

        @Override
        public void close() {
            active = false;
        }
    }
}
