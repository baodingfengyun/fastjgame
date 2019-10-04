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
import com.wjybxx.fastjgame.eventloop.NetContext;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.SessionRegistry;
import com.wjybxx.fastjgame.net.common.OneWaySupportHandler;
import com.wjybxx.fastjgame.net.common.RpcSupportHandler;
import com.wjybxx.fastjgame.net.local.*;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.socket.*;
import com.wjybxx.fastjgame.net.socket.inner.InnerPingSupportHandler;
import com.wjybxx.fastjgame.net.socket.inner.InnerSocketMessageSupportHandler;
import com.wjybxx.fastjgame.net.socket.inner.InnerSocketTransferHandler;
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

    public Session connect(NetContext netContext, long sessionGuid, HostAndPort remoteAddress, byte[] token, SocketSessionConfig config,
                           ChannelInitializer<SocketChannel> initializer) throws IOException {
        Session existSession = sessionRegistry.getSession(sessionGuid);
        if (existSession != null) {
            throw new IOException("session " + sessionGuid + " already registered");
        }
        // TODO 异步化、连接超时
        ChannelFuture channelFuture = nettyThreadManager.connectAsyn(remoteAddress, config.sndBuffer(), config.rcvBuffer(), initializer)
                .syncUninterruptibly();

        final SocketSessionImp socketSessionImp = new SocketSessionImp(sessionGuid, netContext, netManagerWrapper, channelFuture.channel(), config);
        sessionRegistry.registerSession(socketSessionImp);

        socketSessionImp.pipeline()
                .addLast(new InnerSocketTransferHandler())
                .addLast(new InnerSocketMessageSupportHandler())
                .addLast(new InnerPingSupportHandler())
                .addLast(new OneWaySupportHandler())
                .addLast(new RpcSupportHandler())
                .fireInit();

        socketSessionImp.fireWriteAndFlush(new SocketConnectRequest(1, token));

        return socketSessionImp;
    }

    /**
     * 接收到一个建立连接响应
     *
     * @param connectResponseEvent 连接响应事件参数
     */
    public void onRcvConnectResponse(SocketConnectResponseEvent connectResponseEvent) {
        final Session session = sessionRegistry.getSession(connectResponseEvent.sessionGuid());
        if (session == null) {
            return;
        }
        // 建立session失败 - 关闭session
        if (!connectResponseEvent.isSuccess()) {
            session.close();
            return;
        }
        // TODO 详细验证
        session.pipeline().fireSessionActive();
    }

    /**
     * 接收到一个socket消息
     *
     * @param messageEvent 消息事件参数
     */
    public void onRcvMessage(SocketMessageEvent messageEvent) {
        final Session session = sessionRegistry.getSession(messageEvent.sessionGuid());
        if (session != null && session.isActive()) {
            // session 存活的情况下才读取消息
            session.fireRead(messageEvent);
        }
    }

    public Session connectLocal(DefaultLocalPort localPort, NetContext netContext, long sessionGuid, LocalSessionConfig config) throws IOException {
        // 端口已关闭
        if (!localPort.isActive()) {
            throw new IOException("local port closed");
        }
        // 会话已存在
        if (sessionRegistry.getSession(sessionGuid) != null) {
            throw new IOException("session " + sessionGuid + " already registered");
        }
        final LocalSessionImp remoteSession = netManagerWrapper.getAcceptorManager().onRcvConnectRequest(localPort, sessionGuid);

        // 创建session并保存
        LocalSessionImp session = new LocalSessionImp(sessionGuid, netContext, netManagerWrapper, config);
        sessionRegistry.registerSession(session);

        // 初始化管道，入站 从上到下，出站 从下往上
        session.pipeline()
                .addLast(new LocalTransferHandler())
                .addLast(new LocalCodecHandler())
                .addLast(new OneWaySupportHandler())
                .addLast(new RpcSupportHandler());

        // 保存双方引用
        session.setRemoteSession(remoteSession);
        remoteSession.setRemoteSession(session);

        // 触发双方的init和active
        session.pipeline().fireInit().fireSessionActive();
        remoteSession.pipeline().fireInit().fireSessionActive();

        return session;
    }
}
