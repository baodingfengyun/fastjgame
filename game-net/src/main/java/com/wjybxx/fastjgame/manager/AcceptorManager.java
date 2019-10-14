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

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;

/**
 * session接收器。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/9
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class AcceptorManager {

    private NetManagerWrapper netManagerWrapper;
    private final SessionRegistry sessionRegistry = new SessionRegistry();

    @Inject
    public AcceptorManager() {
    }

    public void setManagerWrapper(NetManagerWrapper managerWrapper) {
        this.netManagerWrapper = managerWrapper;
    }

    public void tick() {
        sessionRegistry.tick();
    }

    // --------------------------------------------------- socket ----------------------------------------------

    /**
     * 接收到一个请求建立连接事件
     *
     * @param connectRequestEvent 请求事件参数
     */
    public void onRcvConnectRequest(SocketConnectRequestEvent connectRequestEvent) {
        final Session existSession = sessionRegistry.getSession(connectRequestEvent.sessionId());
        if (existSession == null) {
            // 初始ack错误 - 证明是个失效逻辑
            if (connectRequestEvent.getAck() != MessageQueue.INIT_ACK) {
                NetUtils.closeQuietly(connectRequestEvent.channel());
                return;
            }
            final SocketPortContext portExtraInfo = connectRequestEvent.getPortExtraInfo();
            SocketSessionImp socketSessionImp = new SocketSessionImp(portExtraInfo.getNetContext(), connectRequestEvent.sessionId(), connectRequestEvent.remoteGuid(),
                    netManagerWrapper,
                    connectRequestEvent.channel(), portExtraInfo.getSessionConfig());
            sessionRegistry.registerSession(socketSessionImp);

            socketSessionImp.pipeline()
                    .addLast(new InnerSocketTransferHandler())
                    .addLast(new InnerSocketMessageSupportHandler())
                    .addLast(new InnerPongSupportHandler())
                    .addLast(new OneWaySupportHandler())
                    .addLast(new RpcSupportHandler());

            socketSessionImp.tryActive();
            socketSessionImp.pipeline().fireSessionActive();

            final SocketConnectResponse socketConnectResponse = new SocketConnectResponse(true, connectRequestEvent.getConnectRequest().getVerifyingTimes());
            socketSessionImp.fireWriteAndFlush(socketConnectResponse);
        } else {
            // TODO 断线重连验证
            NetUtils.closeQuietly(connectRequestEvent.channel());
        }
    }

    /**
     * 接收到一个socket消息
     *
     * @param socketEvent 消息事件参数
     */
    public void onSessionEvent(SocketEvent socketEvent) {
        final Session session = sessionRegistry.getSession(socketEvent.sessionId());
        if (session != null && session.isActive()) {
            // session 存活的情况下才读取消息
            session.fireRead(socketEvent);
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
        return new DefaultLocalPort(netContext, config);
    }

    /**
     * 接收到一个连接请
     *
     * @param localPort  本地“端口”
     * @param sessionId  session唯一标识
     * @param remoteGuid 对端唯一标识
     * @return session
     * @throws IOException error
     */
    public LocalSessionImp onRcvConnectRequest(DefaultLocalPort localPort, String sessionId, long remoteGuid) throws IOException {
        // 端口已关闭
        if (!localPort.isActive()) {
            throw new IOException("local port closed");
        }
        if (sessionRegistry.getSession(sessionId) != null) {
            throw new IOException("session " + sessionId + " is already registered");
        }
        // 创建session并保存
        LocalSessionImp session = new LocalSessionImp(localPort.getNetContext(), sessionId, remoteGuid, netManagerWrapper,
                localPort.getLocalConfig());
        sessionRegistry.registerSession(session);

        // 创建管道
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
