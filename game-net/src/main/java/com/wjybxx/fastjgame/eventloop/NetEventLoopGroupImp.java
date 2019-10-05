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
import com.wjybxx.fastjgame.net.socket.SocketMessageEvent;
import com.wjybxx.fastjgame.net.socket.SocketSessionConfig;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.NetUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ThreadFactory;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/5
 * github - https://github.com/hl845740757
 */
public class NetEventLoopGroupImp extends MultiThreadEventLoopGroup implements NetEventLoopGroup {

    NetEventLoopGroupImp(int nThreads, @Nonnull ThreadFactory threadFactory,
                         @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                         @Nullable GroupConfig context) {
        super(nThreads, threadFactory, rejectedExecutionHandler, context);
        init();
    }

    private void init() {
        final GroupConfig groupConfig = getContext();
        final NettyThreadManager nettyThreadManager = groupConfig.injector.getInstance(NettyThreadManager.class);
        // 在子类线程启动之前赋值，提供安全保障
        nettyThreadManager.start(groupConfig.bossGroupThreadNum, groupConfig.workerGroupThreadNum);
    }


    @Override
    protected void clean() {
        final GroupConfig groupConfig = getContext();
        final NettyThreadManager nettyThreadManager = groupConfig.injector.getInstance(NettyThreadManager.class);
        final HttpClientManager httpClientManager = groupConfig.injector.getInstance(HttpClientManager.class);
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
    public NetContext createContext(@Nonnull EventLoop localEventLoop) {
        return new NetContextImp(this, localEventLoop);
    }

    @Override
    public void fireConnectRequest(SocketConnectRequestEvent event) {
        select(NetUtils.fixedKey(event.sessionId())).fireConnectRequest(event);
    }

    @Override
    public void fireMessage_acceptor(SocketMessageEvent event) {
        select(NetUtils.fixedKey(event.sessionId())).fireMessage_acceptor(event);
    }

    @Override
    public void fireHttpRequest(HttpRequestEvent event) {
        select(NetUtils.fixedKey(event.channel())).fireHttpRequest(event);
    }

    @Override
    public ListenableFuture<Session> connectTcp(String sessionId, HostAndPort remoteAddress, byte[] token, SocketSessionConfig config, NetContext netContext) {
        return select(NetUtils.fixedKey(sessionId)).connectTcp(sessionId, remoteAddress, token, config, netContext);
    }

    @Override
    public ListenableFuture<Session> connectWS(String sessionId, HostAndPort remoteAddress, String websocketUrl, byte[] token, SocketSessionConfig config, NetContext netContext) {
        return select(NetUtils.fixedKey(sessionId)).connectWS(sessionId, remoteAddress, websocketUrl, token, config, netContext);
    }

    @Override
    public ListenableFuture<Session> connectLocal(LocalPort localPort, String sessionId, byte[] token, LocalSessionConfig config, NetContext netContext) {
        return select(NetUtils.fixedKey(sessionId)).connectLocal(localPort, sessionId, token, config, netContext);
    }
}
