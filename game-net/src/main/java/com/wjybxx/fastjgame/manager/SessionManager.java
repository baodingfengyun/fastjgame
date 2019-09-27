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
import com.wjybxx.fastjgame.concurrent.Promise;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.PortRange;
import com.wjybxx.fastjgame.misc.SessionRepository;
import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.net.initializer.ChannelInitializerSupplier;
import com.wjybxx.fastjgame.net.injvm.JVMPort;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

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

    public void onRcvConnectRequest(ConnectRequestEventParam eventParam) {

    }

    public void onRcvConnectResponse(ConnectResponseEventParam eventParam) {

    }

    public void onRcvAckPing(AckPingPongEventParam eventParam) {

    }

    public void onRevAckPong(AckPingPongEventParam eventParam) {

    }

    public void onRcvRpcRequest(RpcRequestEventParam eventParam) {

    }

    public void onRcvRpcResponse(RpcResponseEventParam eventParam) {

    }

    public void onRcvOneWayMessage(OneWayMessageEventParam eventParam) {

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

    public void connect(NetContext netContext, long remoteGuid, RoleType remoteRole, HostAndPort remoteAddress, ChannelInitializerSupplier initializerSupplier, SessionLifecycleAware lifecycleAware, ProtocolDispatcher protocolDispatcher, Promise<Session> promise) {

    }

    public JVMPort bind(NetContext netContext, ProtocolCodec codec, PortContext portContext) {
        return null;
    }


}
