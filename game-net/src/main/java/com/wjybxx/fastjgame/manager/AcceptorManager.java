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
import com.wjybxx.fastjgame.misc.PortRange;
import com.wjybxx.fastjgame.misc.SessionRegistry;
import com.wjybxx.fastjgame.net.common.OneWaySupportHandler;
import com.wjybxx.fastjgame.net.common.RpcSupportHandler;
import com.wjybxx.fastjgame.net.local.*;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.socket.*;
import com.wjybxx.fastjgame.net.socket.inner.InnerPongSupportHandler;
import com.wjybxx.fastjgame.net.socket.inner.InnerSocketMessageSupportHandler;
import com.wjybxx.fastjgame.net.socket.inner.InnerSocketTransferHandler;
import com.wjybxx.fastjgame.utils.NetUtils;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

import java.io.IOException;
import java.net.BindException;

/**
 * session接收器。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/9
 * github - https://github.com/hl845740757
 */
public class AcceptorManager {

    private NetManagerWrapper netManagerWrapper;
    private final NettyThreadManager nettyThreadManager;
    private final SessionRegistry sessionRegistry = new SessionRegistry();

    @Inject
    public AcceptorManager(NettyThreadManager nettyThreadManager) {
        this.nettyThreadManager = nettyThreadManager;
    }

    public void setManagerWrapper(NetManagerWrapper managerWrapper) {
        this.netManagerWrapper = managerWrapper;
    }

    public void tick() {
        sessionRegistry.tick();
    }

    // --------------------------------------------------- socket ----------------------------------------------


    /**
     * 绑定到某个端口
     *
     * @param host        地址
     * @param portRange   端口范围
     * @param config      session配置信息
     * @param initializer channel初始化方式
     * @return Port
     * @throws BindException 找不到可用端口时抛出该异常
     */
    public SocketPort bindRange(String host, PortRange portRange, SocketSessionConfig config,
                                ChannelInitializer<SocketChannel> initializer) throws BindException {
        return nettyThreadManager.bindRange(host, portRange, config.sndBuffer(), config.rcvBuffer(), initializer);
    }

    /**
     * 接收到一个请求建立连接事件
     *
     * @param connectRequestEvent 请求事件参数
     */
    public void onRcvConnectRequest(SocketConnectRequestEvent connectRequestEvent) {
        final Session existSession = sessionRegistry.getSession(connectRequestEvent.localGuid(), connectRequestEvent.remoteGuid());
        if (existSession == null) {
            // TODO 首次建立连接验证
            final SocketPortExtraInfo portExtraInfo = connectRequestEvent.getPortExtraInfo();
            SocketSessionImp socketSessionImp = new SocketSessionImp(portExtraInfo.getNetContext(), netManagerWrapper, connectRequestEvent.remoteGuid(),
                    connectRequestEvent.channel(), portExtraInfo.getSessionConfig());
            sessionRegistry.registerSession(socketSessionImp);

            socketSessionImp.pipeline()
                    .addLast(new InnerSocketTransferHandler())
                    .addLast(new InnerSocketMessageSupportHandler())
                    .addLast(new InnerPongSupportHandler())
                    .addLast(new OneWaySupportHandler())
                    .addLast(new RpcSupportHandler())
                    .fireInit()
                    .fireSessionActive();

            final SocketConnectResponse connectResponse = new SocketConnectResponse(true, connectRequestEvent.getConnectRequest().getVerifyingTimes());
            socketSessionImp.fireWriteAndFlush(connectResponse);
        } else {
            // TODO 断线重连验证
            NetUtils.closeQuietly(connectRequestEvent.channel());
        }
    }


    /**
     * 接收到一个socket消息
     *
     * @param messageEvent 消息事件参数
     */
    public void onRcvMessage(SocketMessageEvent messageEvent) {
        final Session session = sessionRegistry.getSession(messageEvent.localGuid(), messageEvent.remoteGuid());
        if (session != null && session.isActive()) {
            // session 存活的情况下才读取消息
            session.fireRead(messageEvent);
        }
    }

    // -------------------------------------------------- 本地session支持 ------------------------------------------------

    /**
     * 绑定JVM内部端口
     *
     * @param netContext 用户网络上下文
     * @param config     创建session需要的配置
     * @return localPort
     */
    public LocalPort bindLocal(NetContext netContext, LocalSessionConfig config) {
        return new DefaultLocalPort(netContext, config, netManagerWrapper.getConnectorManager());
    }

    /**
     * 接收到一个连接请
     *
     * @param localPort   本地“端口”
     * @param remoteGuid session唯一标识
     * @return session
     * @throws IOException error
     */
    public LocalSessionImp onRcvConnectRequest(DefaultLocalPort localPort, long remoteGuid) throws IOException {
        final long localGuid = localPort.getNetContext().localGuid();
        if (sessionRegistry.getSession(localGuid, remoteGuid) != null) {
            throw new IOException("session " + remoteGuid + " is already registered");
        }
        // 创建session并保存
        LocalSessionImp session = new LocalSessionImp(localPort.getNetContext(), netManagerWrapper, localPort.getLocalConfig());
        sessionRegistry.registerSession(session);

        // 创建管道，但是这里还不能初始化 - 因为还没有对方的引用
        session.pipeline()
                .addLast(new LocalTransferHandler())
                .addLast(new LocalCodecHandler())
                .addLast(new OneWaySupportHandler())
                .addLast(new RpcSupportHandler());

        return session;
    }

    // -------------------------------------------------------- 用户线程关闭事件 ------------------------------------------------

    public void onUserEventLoopTerminal(EventLoop userEventLoop) {
        sessionRegistry.onUserEventLoopTerminal(userEventLoop);
    }

    public void clean() {
        sessionRegistry.closeAll();
    }

}
