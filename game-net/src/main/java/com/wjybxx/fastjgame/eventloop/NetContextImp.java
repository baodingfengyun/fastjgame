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
import com.wjybxx.fastjgame.concurrent.Promise;
import com.wjybxx.fastjgame.manager.NetManagerWrapper;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.PortRange;
import com.wjybxx.fastjgame.net.NetContext;
import com.wjybxx.fastjgame.net.RoleType;
import com.wjybxx.fastjgame.net.Session;
import com.wjybxx.fastjgame.net.http.HttpRequestDispatcher;
import com.wjybxx.fastjgame.net.http.HttpServerInitializer;
import com.wjybxx.fastjgame.net.http.OkHttpCallback;
import com.wjybxx.fastjgame.net.local.LocalPort;
import com.wjybxx.fastjgame.net.local.LocalSessionConfig;
import com.wjybxx.fastjgame.net.socket.SocketPort;
import com.wjybxx.fastjgame.net.socket.SocketSessionConfig;
import com.wjybxx.fastjgame.net.socket.TCPClientChannelInitializer;
import com.wjybxx.fastjgame.net.socket.TCPServerChannelInitializer;
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

    private final long localGuid;
    private final RoleType localRole;
    private final EventLoop localEventLoop;
    private final NetEventLoop netEventLoop;
    private final NetManagerWrapper managerWrapper;

    public NetContextImp(long localGuid, RoleType localRole, EventLoop localEventLoop,
                         NetEventLoop netEventLoop, NetManagerWrapper managerWrapper) {
        this.localGuid = localGuid;
        this.localRole = localRole;
        this.localEventLoop = localEventLoop;
        this.netEventLoop = netEventLoop;
        this.managerWrapper = managerWrapper;
    }

    @Override
    public long localGuid() {
        return localGuid;
    }

    @Override
    public RoleType localRole() {
        return localRole;
    }

    @Override
    public EventLoop localEventLoop() {
        return localEventLoop;
    }

    @Override
    public NetEventLoop netEventLoop() {
        return netEventLoop;
    }

    @Override
    public ListenableFuture<?> deregister() {
        return netEventLoop.submit(() -> {
            managerWrapper.getNetContextManager().deregister(this);
        });
    }

    @Override
    public ListenableFuture<SocketPort> bindTcpRange(String host, PortRange portRange, @Nonnull SocketSessionConfig config) {
        TCPServerChannelInitializer initializer = new TCPServerChannelInitializer(localGuid, config, managerWrapper.getNetEventManager());
        return bindRange(host, portRange, config, initializer);
    }

    @Override
    public ListenableFuture<SocketPort> bindWSRange(String host, PortRange portRange, String websocketPath, @Nonnull SocketSessionConfig config) {
        WsServerChannelInitializer initializer = new WsServerChannelInitializer(localGuid, websocketPath, config, managerWrapper.getNetEventManager());
        return bindRange(host, portRange, config, initializer);
    }

    private ListenableFuture<SocketPort> bindRange(String host, PortRange portRange, SocketSessionConfig config,
                                                   @Nonnull ChannelInitializer<SocketChannel> initializer) {
        // 这里一定不是网络层，只有逻辑层才会调用bind
        return netEventLoop.submit(() -> {
            try {
                return managerWrapper.getSessionManager().bindRange(host, portRange, config, initializer);
            } catch (BindException e) {
                ConcurrentUtils.rethrow(e);
                // unreachable
                return null;
            }
        });
    }

    @Override
    public ListenableFuture<Session> connectTcp(long remoteGuid, RoleType remoteRole, HostAndPort remoteAddress, @Nonnull SocketSessionConfig config) {
        final TCPClientChannelInitializer initializer = new TCPClientChannelInitializer(localGuid, remoteGuid, config, managerWrapper.getNetEventManager());
        return connect(remoteGuid, remoteRole, remoteAddress, config, initializer);
    }

    @Override
    public ListenableFuture<Session> connectWS(long remoteGuid, RoleType remoteRole, HostAndPort remoteAddress, String websocketUrl, @Nonnull SocketSessionConfig config) {
        final WsClientChannelInitializer initializer = new WsClientChannelInitializer(localGuid, remoteGuid, websocketUrl, config.maxFrameLength(),
                config.codec(), managerWrapper.getNetEventManager());
        return connect(remoteGuid, remoteRole, remoteAddress, config, initializer);
    }

    @Nonnull
    private ListenableFuture<Session> connect(long remoteGuid, RoleType remoteRole, HostAndPort remoteAddress,
                                              SocketSessionConfig config,
                                              ChannelInitializer<SocketChannel> initializer) {
        final Promise<Session> promise = netEventLoop.newPromise();
        // 这里一定不是网络层，只有逻辑层才会调用connect
        netEventLoop.execute(() -> {
            managerWrapper.getSessionManager().connect(this, remoteGuid, remoteRole, remoteAddress,
                    config, initializer, promise);
        });
        return promise;
    }


    // ----------------------------------------------- 本地调用支持 --------------------------------------------

    @Override
    public ListenableFuture<LocalPort> bindInJVM(@Nonnull LocalSessionConfig config) {
        return netEventLoop.submit(() -> {
            return managerWrapper.getSessionManager().bindInJVM(this, config);
        });
    }

    @Override
    public ListenableFuture<Session> connectInJVM(@Nonnull LocalPort localPort, @Nonnull LocalSessionConfig config) {
        return localPort.connect(this, config);
    }

    // ------------------------------------------- http 实现 ----------------------------------------

    @Override
    public ListenableFuture<HostAndPort> bindHttpRange(String host, PortRange portRange, @Nonnull HttpRequestDispatcher httpRequestDispatcher) {
        final HttpServerInitializer initializer = new HttpServerInitializer(localGuid, httpRequestDispatcher, managerWrapper.getNetEventManager());
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
