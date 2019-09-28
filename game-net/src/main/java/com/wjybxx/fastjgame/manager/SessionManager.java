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
import com.wjybxx.fastjgame.misc.*;
import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.net.handler.OneWaySupportHandler;
import com.wjybxx.fastjgame.net.handler.RpcSupportHandler;
import com.wjybxx.fastjgame.net.handler.SessionLifeCycleAwareHandler;
import com.wjybxx.fastjgame.net.injvm.*;
import com.wjybxx.fastjgame.net.socket.ConnectRequestEvent;
import com.wjybxx.fastjgame.net.socket.ConnectResponseEvent;
import com.wjybxx.fastjgame.net.socket.ordered.OrderedMessageEvent;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

import javax.annotation.Nonnull;
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
    private final SessionRepository sessionRepository = new SessionRepository();

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
        sessionRepository.tick();
    }

    // --------------------------------------------- 事件处理 -----------------------------------------

    public void onRcvConnectRequest(ConnectRequestEvent eventParam) {

    }

    public void onRcvConnectResponse(ConnectResponseEvent eventParam) {

    }

    public void onRcvMessage(OrderedMessageEvent eventParam) {
        final Session session = sessionRepository.getSession(eventParam.localGuid(), eventParam.remoteGuid());
        if (session != null) {
            session.fireRead(eventParam);
        }
    }

    // ---------------------------------------------------------------

    public void onUserEventLoopTerminal(EventLoop userEventLoop) {
        sessionRepository.removeUserSession(userEventLoop);
    }

    public void removeSession(Session session) {
        sessionRepository.removeSession(session.localGuid(), session.remoteGuid());
    }

    public void removeUserSession(long userGuid) {
        sessionRepository.removeUserSession(userGuid);
    }

    public HostAndPort bindRange(NetContext netContextImp, String host, PortRange portRange, ChannelInitializer<SocketChannel> initializer) throws BindException {

        return null;
    }

    public void connect(NetContext netContext, long remoteGuid, RoleType remoteRole,
                        HostAndPort remoteAddress, ChannelInitializerSupplier initializerSupplier,
                        SessionLifecycleAware lifecycleAware,
                        ProtocolDispatcher protocolDispatcher,
                        Promise<Session> promise) {

    }


    public JVMPort bindInJVM(NetContext netContext, JVMSessionConfig config) {
        return new JVMPortImp(netContext, config, this);
    }

    private void connectInJVM(NetContext netContext, JVMPortImp jvmPort,
                              JVMSessionConfig config,
                              Promise<Session> promise) {
        // 端口已关闭
        if (!jvmPort.active) {
            promise.tryFailure(new IOException("remote node not exist"));
        }

        final long localGuid = netContext.localGuid();
        final long remoteGuid = jvmPort.localGuid();
        // 会话已存在
        if (sessionRepository.getSession(localGuid, remoteGuid) != null ||
                sessionRepository.getSession(remoteGuid, localGuid) != null) {
            promise.tryFailure(new IOException("session already registered."));
            return;
        }
        // 创建session
        JVMSessionImp connectorSession = new JVMSessionImp(netContext, netManagerWrapper, config);
        JVMSessionImp acceptorSession = new JVMSessionImp(jvmPort.localContext, netManagerWrapper, jvmPort.localConfig);

        // 保存双方引用
        connectorSession.setRemoteSession(acceptorSession);
        acceptorSession.setRemoteSession(connectorSession);

        // 初始化管道
        initJVMSessionPipeline(connectorSession);
        initJVMSessionPipeline(acceptorSession);

        if (promise.trySuccess(connectorSession)) {
            // 保存
            sessionRepository.registerSession(connectorSession);
            sessionRepository.registerSession(acceptorSession);

            // 传递激活事件
            connectorSession.pipeline().fireSessionActive();
            acceptorSession.pipeline().fireSessionActive();
        }
        // else 丢弃session
    }

    private static void initJVMSessionPipeline(JVMSession session) {
        SessionPipeline pipeline = session.pipeline();
        // 入站 从上到下
        // 出站 从下往上
        pipeline.addLast(new JVMTransferHandler());
        pipeline.addLast(new JVMCodecHandler());

        pipeline.addLast(new OneWaySupportHandler());
        pipeline.addLast(new RpcSupportHandler());
        pipeline.addLast(new SessionLifeCycleAwareHandler());

        pipeline.fireInit();
    }

    private static class JVMPortImp implements JVMPort {

        /**
         * 监听者的信息
         */
        private final NetContext localContext;
        /**
         * session配置信息
         */
        private final JVMSessionConfig localConfig;
        /**
         * 建立连接的管理器
         */
        private final SessionManager sessionManager;
        /**
         * 激活状态
         */
        private volatile boolean active = true;

        JVMPortImp(NetContext localContext, JVMSessionConfig localConfig, SessionManager sessionManager) {
            this.localContext = localContext;
            this.localConfig = localConfig;
            this.sessionManager = sessionManager;
        }

        public long localGuid() {
            return localContext.localGuid();
        }

        public RoleType localRole() {
            return localContext.localRole();
        }

        @Override
        public ListenableFuture<Session> connect(@Nonnull NetContext netContext, @Nonnull JVMSessionConfig config) {
            final Promise<Session> promise = localContext.netEventLoop().newPromise();
            // 注意：这里是提交到jvmPort所在的NetEventLoop, 是实现线程安全，消除同步的关键
            localContext.netEventLoop().execute(() -> {
                sessionManager.connectInJVM(netContext, this, config, promise);
            });
            return promise;
        }

        @Override
        public ListenableFuture<?> close() {
            active = false;
            return localContext.netEventLoop().newSucceededFuture(null);
        }
    }
}
