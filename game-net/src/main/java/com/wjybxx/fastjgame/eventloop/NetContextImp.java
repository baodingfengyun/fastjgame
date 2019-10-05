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
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.PortRange;
import com.wjybxx.fastjgame.net.http.HttpRequestDispatcher;
import com.wjybxx.fastjgame.net.http.OkHttpCallback;
import com.wjybxx.fastjgame.net.local.LocalPort;
import com.wjybxx.fastjgame.net.local.LocalSessionConfig;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.socket.SocketPort;
import com.wjybxx.fastjgame.net.socket.SocketSessionConfig;
import okhttp3.Response;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
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

    private final NetEventLoopGroup netEventLoopGroup;
    private final EventLoop localEventLoop;

    NetContextImp(NetEventLoopGroup NetEventLoopGroup, EventLoop localEventLoop) {
        this.localEventLoop = localEventLoop;
        this.netEventLoopGroup = NetEventLoopGroup;
    }

    @Override
    public EventLoop localEventLoop() {
        return localEventLoop;
    }

    @Override
    public NetEventLoopGroup netEventLoopGroup() {
        return netEventLoopGroup;
    }

    @Override
    public ListenableFuture<SocketPort> bindTcpRange(String host, PortRange portRange, @Nonnull SocketSessionConfig config) {
        return netEventLoopGroup.next().bindTcpRange(host, portRange, config, this);
    }

    @Override
    public ListenableFuture<SocketPort> bindWSRange(String host, PortRange portRange, String websocketPath, @Nonnull SocketSessionConfig config) {
        return netEventLoopGroup.next().bindWSRange(host, portRange, websocketPath, config, this);
    }

    @Override
    public ListenableFuture<Session> connectTcp(String sessionId, HostAndPort remoteAddress, byte[] token, @Nonnull SocketSessionConfig config) {
        return netEventLoopGroup.connect(sessionId, remoteAddress, token, config, this);
    }

    @Override
    public ListenableFuture<Session> connectWS(String sessionId, HostAndPort remoteAddress, String websocketUrl, byte[] token, @Nonnull SocketSessionConfig config) {
        return netEventLoopGroup.connect(sessionId, remoteAddress, websocketUrl, token, config, this);
    }

    // ----------------------------------------------- 本地调用支持 --------------------------------------------

    @Override
    public ListenableFuture<LocalPort> bindLocal(@Nonnull LocalSessionConfig config) {
        return netEventLoopGroup.next().bindLocal(this, config);
    }

    @Override
    public ListenableFuture<Session> connectLocal(@Nonnull LocalPort localPort, String sessionId, byte[] token, @Nonnull LocalSessionConfig config) {
        return netEventLoopGroup().connect(localPort, sessionId, token, config, this);
    }

    // ------------------------------------------- http 实现 ----------------------------------------

    @Override
    public ListenableFuture<SocketPort> bindHttpRange(String host, PortRange portRange, @Nonnull HttpRequestDispatcher httpRequestDispatcher) {
        return netEventLoopGroup().next().bindHttpRange(host, portRange, httpRequestDispatcher, this);
    }

    @Override
    public Response syncGet(String url, @Nonnull Map<String, String> params) throws IOException {
        return netEventLoopGroup.next().syncGet(url, params);
    }

    @Override
    public void asyncGet(String url, @Nonnull Map<String, String> params, @Nonnull OkHttpCallback okHttpCallback) {
        netEventLoopGroup.next().asyncGet(url, params, okHttpCallback, localEventLoop);
    }

    @Override
    public Response syncPost(String url, @Nonnull Map<String, String> params) throws IOException {
        return netEventLoopGroup.next().syncPost(url, params);
    }

    @Override
    public void asyncPost(String url, @Nonnull Map<String, String> params, @Nonnull OkHttpCallback okHttpCallback) {
        netEventLoopGroup.next().asyncPost(url, params, okHttpCallback, localEventLoop);
    }
}
