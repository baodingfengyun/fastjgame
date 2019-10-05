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

package com.wjybxx.fastjgame.eventloop;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.manager.NetManagerWrapper;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.HttpPortExtraInfo;
import com.wjybxx.fastjgame.misc.PortRange;
import com.wjybxx.fastjgame.net.http.HttpRequestDispatcher;
import com.wjybxx.fastjgame.net.http.HttpServerInitializer;
import com.wjybxx.fastjgame.net.http.OkHttpCallback;
import com.wjybxx.fastjgame.net.local.LocalPort;
import com.wjybxx.fastjgame.net.local.LocalSessionConfig;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.socket.*;
import com.wjybxx.fastjgame.net.ws.WsClientChannelInitializer;
import com.wjybxx.fastjgame.net.ws.WsServerChannelInitializer;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import okhttp3.Response;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.net.BindException;
import java.util.Map;

/**
 * NetContext的基本实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/3
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class NetContextImp implements NetContext {

    private final EventLoop localEventLoop;
    private final NetEventLoop netEventLoop;
    private final NetManagerWrapper managerWrapper;

    NetContextImp(NetEventLoop netEventLoop, EventLoop localEventLoop, NetManagerWrapper managerWrapper) {
        this.localEventLoop = localEventLoop;
        this.netEventLoop = netEventLoop;
        this.managerWrapper = managerWrapper;
    }

    @Override
    public EventLoop localEventLoop() {
        return localEventLoop;
    }

    @Override
    public NetEventLoopGroup netEventLoop() {
        return netEventLoop;
    }

    @Override
    public ListenableFuture<SocketPort> bindTcpRange(String host, PortRange portRange, @Nonnull SocketSessionConfig config) {
        SocketPortExtraInfo portExtraInfo = new SocketPortExtraInfo(this, config);
        TCPServerChannelInitializer initializer = new TCPServerChannelInitializer(portExtraInfo, managerWrapper.getNetEventManager());
        return bindRange(host, portRange, config, initializer);
    }

    @Override
    public ListenableFuture<SocketPort> bindWSRange(String host, PortRange portRange, String websocketPath, @Nonnull SocketSessionConfig config) {
        SocketPortExtraInfo portExtraInfo = new SocketPortExtraInfo(this, config);
        WsServerChannelInitializer initializer = new WsServerChannelInitializer(websocketPath, portExtraInfo, managerWrapper.getNetEventManager());
        return bindRange(host, portRange, config, initializer);
    }

    private ListenableFuture<SocketPort> bindRange(String host, PortRange portRange, SocketSessionConfig config,
                                                   @Nonnull ChannelInitializer<SocketChannel> initializer) {
        // 这里一定不是网络层，只有逻辑层才会调用bind
        return netEventLoop.submit(() -> {
            try {
                return managerWrapper.getAcceptorManager().bindRange(host, portRange, config, initializer);
            } catch (BindException e) {
                ConcurrentUtils.rethrow(e);
                // unreachable
                return null;
            }
        });
    }

    @Override
    public ListenableFuture<Session> connectTcp(long remoteGuid, HostAndPort remoteAddress, byte[] token, @Nonnull SocketSessionConfig config) {
        final TCPClientChannelInitializer initializer = new TCPClientChannelInitializer(sessionId, config, managerWrapper.getNetEventManager());
        return connect(remoteGuid, remoteAddress, token, config, initializer);
    }

    @Override
    public ListenableFuture<Session> connectWS(long remoteGuid, HostAndPort remoteAddress, String websocketUrl, byte[] token, @Nonnull SocketSessionConfig config) {
        final WsClientChannelInitializer initializer = new WsClientChannelInitializer(sessionId, websocketUrl, config.maxFrameLength(),
                config.codec(), managerWrapper.getNetEventManager());
        return connect(remoteGuid, remoteAddress, token, config, initializer);
    }

    @Nonnull
    private ListenableFuture<Session> connect(long remoteGuid, HostAndPort remoteAddress, byte[] token, SocketSessionConfig config,
                                              ChannelInitializer<SocketChannel> initializer) {
        // 这里一定不是网络层，只有逻辑层才会调用connect
        return netEventLoop.submit(() -> {
            return managerWrapper.getConnectorManager().connect(this, remoteGuid, remoteAddress,
                    token, config, initializer);
        });
    }

    // ----------------------------------------------- 本地调用支持 --------------------------------------------

    @Override
    public ListenableFuture<LocalPort> bindLocal(@Nonnull LocalSessionConfig config) {
        return netEventLoop.submit(() -> {
            return managerWrapper.getAcceptorManager().bindLocal(this, config);
        });
    }

    @Override
    public ListenableFuture<Session> connectLocal(@Nonnull LocalPort localPort, byte[] token, @Nonnull LocalSessionConfig config) {
        return localPort.connect(this, , config);
    }

    // ------------------------------------------- http 实现 ----------------------------------------

    @Override
    public ListenableFuture<SocketPort> bindHttpRange(String host, PortRange portRange, @Nonnull HttpRequestDispatcher httpRequestDispatcher) {
        final HttpPortExtraInfo portExtraInfo = new HttpPortExtraInfo(this, httpRequestDispatcher);
        final HttpServerInitializer initializer = new HttpServerInitializer(managerWrapper.getNetEventManager(), portExtraInfo);
        // 这里一定不是网络层，只有逻辑层才会调用bind
        return netEventLoop.submit(() -> {
            try {
                return managerWrapper.getHttpSessionManager().bindRange(this, host, portRange, initializer);
            } catch (Exception e) {
                ConcurrentUtils.rethrow(e);
                // unreachable
                return null;
            }
        });
    }

    @Override
    public Response syncGet(String url, @Nonnull Map<String, String> params) throws IOException {
        return managerWrapper.getHttpClientManager().syncGet(url, params);
    }

    @Override
    public void asyncGet(String url, @Nonnull Map<String, String> params, @Nonnull OkHttpCallback okHttpCallback) {
        managerWrapper.getHttpClientManager().asyncGet(url, params, localEventLoop, okHttpCallback);
    }

    @Override
    public Response syncPost(String url, @Nonnull Map<String, String> params) throws IOException {
        return managerWrapper.getHttpClientManager().syncPost(url, params);
    }

    @Override
    public void asyncPost(String url, @Nonnull Map<String, String> params, @Nonnull OkHttpCallback okHttpCallback) {
        managerWrapper.getHttpClientManager().asyncPost(url, params, localEventLoop, okHttpCallback);
    }

}
