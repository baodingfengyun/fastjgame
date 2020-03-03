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

package com.wjybxx.fastjgame.net.misc;

import com.wjybxx.fastjgame.net.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.net.eventloop.NetEventLoopGroup;
import com.wjybxx.fastjgame.net.http.HttpPortConfig;
import com.wjybxx.fastjgame.net.http.HttpPortContext;
import com.wjybxx.fastjgame.net.http.HttpServerInitializer;
import com.wjybxx.fastjgame.net.local.ConnectLocalRequest;
import com.wjybxx.fastjgame.net.local.DefaultLocalPort;
import com.wjybxx.fastjgame.net.local.LocalPort;
import com.wjybxx.fastjgame.net.local.LocalSessionConfig;
import com.wjybxx.fastjgame.net.manager.NettyThreadManager;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.socket.*;
import com.wjybxx.fastjgame.net.ws.WsClientChannelInitializer;
import com.wjybxx.fastjgame.net.ws.WsServerChannelInitializer;
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.utils.concurrent.Promise;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.net.BindException;

/**
 * NetContext的基本实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/3
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class DefaultNetContext implements NetContext {

    private final long localGuid;
    private final EventLoop appEventLoop;
    private final NetEventLoopGroup netEventLoopGroup;
    private final NettyThreadManager nettyThreadManager;

    public DefaultNetContext(long localGuid, EventLoop appEventLoop, NetEventLoopGroup NetEventLoopGroup,
                             NettyThreadManager nettyThreadManager) {
        this.localGuid = localGuid;
        this.appEventLoop = appEventLoop;
        this.netEventLoopGroup = NetEventLoopGroup;
        this.nettyThreadManager = nettyThreadManager;
    }

    @Override
    public long localGuid() {
        return localGuid;
    }

    @Override
    public EventLoop appEventLoop() {
        return appEventLoop;
    }

    @Override
    public NetEventLoopGroup netEventLoopGroup() {
        return netEventLoopGroup;
    }

    @Nonnull
    private NetEventLoop selectNetEventLoop(String sessionId) {
        return netEventLoopGroup.select(sessionId);
    }

    // ----------------------------------------------------- socket 支持 --------------------------------------------------

    @Override
    public SocketPort bindTcpRange(String host, PortRange portRange, @Nonnull SocketSessionConfig config) throws BindException {
        SocketPortContext portExtraInfo = new SocketPortContext(this, config);
        TCPServerChannelInitializer initializer = new TCPServerChannelInitializer(portExtraInfo);
        return nettyThreadManager.bindRange(host, portRange, config.sndBuffer(), config.rcvBuffer(), initializer);
    }

    @Override
    public ListenableFuture<Session> connectTcp(String sessionId, long remoteGuid, HostAndPort remoteAddress, @Nonnull SocketSessionConfig config) {
        final NetEventLoop netEventLoop = selectNetEventLoop(sessionId);
        final TCPClientChannelInitializer initializer = new TCPClientChannelInitializer(sessionId, localGuid, config, netEventLoop);

        final Promise<Session> connectPromise = netEventLoop.newPromise();
        netEventLoop.post(new ConnectRemoteRequest(sessionId, remoteGuid, remoteAddress, config, initializer, this, connectPromise));
        return connectPromise;
    }

    @Override
    public SocketPort bindWSRange(String host, PortRange portRange, String websocketPath, @Nonnull SocketSessionConfig config) throws BindException {
        SocketPortContext portExtraInfo = new SocketPortContext(this, config);
        WsServerChannelInitializer initializer = new WsServerChannelInitializer(websocketPath, portExtraInfo);
        return nettyThreadManager.bindRange(host, portRange, config.sndBuffer(), config.rcvBuffer(), initializer);
    }

    @Override
    public ListenableFuture<Session> connectWS(String sessionId, long remoteGuid, HostAndPort remoteAddress, String websocketUrl, @Nonnull SocketSessionConfig config) {
        final NetEventLoop netEventLoop = selectNetEventLoop(sessionId);
        final WsClientChannelInitializer initializer = new WsClientChannelInitializer(sessionId, remoteGuid, websocketUrl, config, netEventLoop);

        final Promise<Session> connectPromise = netEventLoop.newPromise();
        netEventLoop.post(new ConnectRemoteRequest(sessionId, remoteGuid, remoteAddress, config, initializer, this, connectPromise));
        return connectPromise;
    }

    // ----------------------------------------------- 本地调用支持 --------------------------------------------

    @Override
    public LocalPort bindLocal(@Nonnull LocalSessionConfig config) {
        return new DefaultLocalPort(this, config);
    }

    @Override
    public ListenableFuture<Session> connectLocal(String sessionId, long remoteGuid, @Nonnull LocalPort localPort, @Nonnull LocalSessionConfig config) {
        final NetEventLoop netEventLoop = selectNetEventLoop(sessionId);
        if (!(localPort instanceof DefaultLocalPort)) {
            throw new UnsupportedOperationException();
        }

        final Promise<Session> connectPromise = netEventLoop.newPromise();
        netEventLoop.post(new ConnectLocalRequest(sessionId, remoteGuid, (DefaultLocalPort) localPort, config, this, connectPromise));
        return connectPromise;
    }

    // ------------------------------------------------- http 实现 --------------------------------------------

    @Override
    public SocketPort bindHttpRange(String host, PortRange portRange, @Nonnull HttpPortConfig config) throws BindException {
        final HttpPortContext httpPortContext = new HttpPortContext(this, config);
        final HttpServerInitializer initializer = new HttpServerInitializer(httpPortContext);
        return nettyThreadManager.bindRange(host, portRange, config.getSndBuffer(), config.getRcvBuffer(), initializer);
    }

}
