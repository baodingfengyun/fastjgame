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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.concurrent.MultiThreadEventLoopGroup;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandler;
import com.wjybxx.fastjgame.manager.HttpClientManager;
import com.wjybxx.fastjgame.manager.NettyThreadManager;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.module.NetEventLoopGroupModule;
import com.wjybxx.fastjgame.net.http.HttpRequestEvent;
import com.wjybxx.fastjgame.net.local.LocalPort;
import com.wjybxx.fastjgame.net.local.LocalSessionConfig;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.socket.SocketConnectRequestEvent;
import com.wjybxx.fastjgame.net.socket.SocketEvent;
import com.wjybxx.fastjgame.net.socket.SocketSessionConfig;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.NetUtils;

import javax.annotation.Nonnull;
import java.util.concurrent.ThreadFactory;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/5
 * github - https://github.com/hl845740757
 */
public class NetEventLoopGroupImp extends MultiThreadEventLoopGroup implements NetEventLoopGroup {

    private final NettyThreadManager nettyThreadManager;
    private final HttpClientManager httpClientManager;

    NetEventLoopGroupImp(int nThreads, @Nonnull ThreadFactory threadFactory,
                         @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                         @Nonnull GroupConfig groupConfig) {
        super(nThreads, threadFactory, rejectedExecutionHandler, groupConfig);

        final NettyThreadManager nettyThreadManager = groupConfig.injector.getInstance(NettyThreadManager.class);
        final HttpClientManager httpClientManager = groupConfig.injector.getInstance(HttpClientManager.class);
        nettyThreadManager.init(groupConfig.bossGroupThreadNum, groupConfig.workerGroupThreadNum);

        this.nettyThreadManager = nettyThreadManager;
        this.httpClientManager = httpClientManager;
    }

    @Override
    public HttpClientManager getHttpClientManager() {
        return httpClientManager;
    }

    @Override
    public NettyThreadManager getNettyThreadManager() {
        return nettyThreadManager;
    }

    @Override
    protected void clean() {
        // 关闭持有的线程资源
        ConcurrentUtils.safeExecute((Runnable) nettyThreadManager::shutdown);
        ConcurrentUtils.safeExecute((Runnable) httpClientManager::shutdown);
    }

    static class GroupConfig {

        private final Injector injector = Guice.createInjector(new NetEventLoopGroupModule());
        private final int bossGroupThreadNum;
        private final int workerGroupThreadNum;

        GroupConfig(int bossGroupThreadNum, int workerGroupThreadNum) {
            this.bossGroupThreadNum = bossGroupThreadNum;
            this.workerGroupThreadNum = workerGroupThreadNum;
        }
    }

    @Override
    protected GroupConfig getContext() {
        return (GroupConfig) super.getContext();
    }

    @Nonnull
    @Override
    public NetEventLoop next() {
        return (NetEventLoop) super.next();
    }

    @Nonnull
    @Override
    public NetEventLoop select(int key) {
        return (NetEventLoop) super.select(key);
    }

    @Nonnull
    @Override
    protected NetEventLoop newChild(int childIndex, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler, Object context) {
        final GroupConfig groupConfig = (GroupConfig) context;
        return new NetEventLoopImp(this, threadFactory, rejectedExecutionHandler, groupConfig.injector);
    }

    @Override
    public NetContext createContext(long localGuid, @Nonnull EventLoop localEventLoop) {
        if (localEventLoop instanceof NetEventLoop) {
            throw new IllegalArgumentException("Bad EventLoop");
        }
        // 监听用户线程关闭
        for (EventLoop eventLoop : this) {
            localEventLoop.terminationFuture().addListener(future -> {
                ((NetEventLoop) eventLoop).onUserEventLoopTerminal(localEventLoop);
            });
        }
        return new NetContextImp(localGuid, localEventLoop, this);
    }

    @Override
    public void fireConnectRequest(SocketConnectRequestEvent event) {
        select(NetUtils.fixedKey(event.sessionId())).fireConnectRequest(event);
    }

    @Override
    public void fireEvent_acceptor(SocketEvent event) {
        select(NetUtils.fixedKey(event.sessionId())).fireEvent_acceptor(event);
    }

    @Override
    public ListenableFuture<Session> connectTcp(String sessionId, long remoteGuid, HostAndPort remoteAddress, SocketSessionConfig config, NetContext netContext) {
        return select(NetUtils.fixedKey(sessionId)).connectTcp(sessionId, remoteGuid, remoteAddress, config, netContext);
    }

    @Override
    public ListenableFuture<Session> connectWS(String sessionId, long remoteGuid, HostAndPort remoteAddress, String websocketUrl, SocketSessionConfig config, NetContext netContext) {
        return select(NetUtils.fixedKey(sessionId)).connectWS(sessionId, remoteGuid, remoteAddress, websocketUrl, config, netContext);
    }

    @Override
    public ListenableFuture<Session> connectLocal(String sessionId, long remoteGuid, LocalPort localPort, LocalSessionConfig config, NetContext netContext) {
        return select(NetUtils.fixedKey(sessionId)).connectLocal(sessionId, remoteGuid, localPort, config, netContext);
    }

    @Override
    public void fireHttpRequest(HttpRequestEvent event) {
        select(NetUtils.fixedKey(event.channel())).fireHttpRequest(event);
    }
}
