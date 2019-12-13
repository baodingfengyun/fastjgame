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
import com.google.inject.Stage;
import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.MultiThreadEventLoopGroup;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandler;
import com.wjybxx.fastjgame.concurrent.event.EventLoopTerminalEvent;
import com.wjybxx.fastjgame.manager.HttpClientManager;
import com.wjybxx.fastjgame.manager.NettyThreadManager;
import com.wjybxx.fastjgame.misc.DefaultNetContext;
import com.wjybxx.fastjgame.module.NetEventLoopGroupModule;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.NetUtils;
import io.netty.channel.Channel;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/5
 * github - https://github.com/hl845740757
 */
class NetEventLoopGroupImp extends MultiThreadEventLoopGroup implements NetEventLoopGroup {

    private final Set<EventLoop> appEventLoopSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final NettyThreadManager nettyThreadManager;
    private final HttpClientManager httpClientManager;

    NetEventLoopGroupImp(int nThreads, @Nonnull ThreadFactory threadFactory,
                         @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                         @Nonnull GroupConfig groupConfig) {
        super(nThreads, threadFactory, rejectedExecutionHandler, groupConfig);

        // 初始化配置
        final NettyThreadManager nettyThreadManager = groupConfig.injector.getInstance(NettyThreadManager.class);
        nettyThreadManager.init(groupConfig.bossGroupThreadNum, groupConfig.workerGroupThreadNum);

        final HttpClientManager httpClientManager = groupConfig.injector.getInstance(HttpClientManager.class);
        httpClientManager.init(groupConfig.httpRequestTimeout);

        // 这里使用final可以保证初始化完成
        this.nettyThreadManager = nettyThreadManager;
        this.httpClientManager = httpClientManager;
    }

    @Override
    protected void clean() {
        // 关闭持有的线程资源
        ConcurrentUtils.safeExecute((Runnable) nettyThreadManager::shutdown);
        ConcurrentUtils.safeExecute((Runnable) httpClientManager::shutdown);
    }

    static class GroupConfig {

        private final Injector injector = Guice.createInjector(Stage.PRODUCTION, new NetEventLoopGroupModule());
        private final int bossGroupThreadNum;
        private final int workerGroupThreadNum;
        private final int httpRequestTimeout;

        GroupConfig(int bossGroupThreadNum, int workerGroupThreadNum, int httpRequestTimeout) {
            this.bossGroupThreadNum = bossGroupThreadNum;
            this.workerGroupThreadNum = workerGroupThreadNum;
            this.httpRequestTimeout = httpRequestTimeout;
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
    public NetEventLoop select(@Nonnull String sessionId) {
        return select(NetUtils.fixedKey(sessionId));
    }

    @Nonnull
    @Override
    public NetEventLoop select(@Nonnull Channel channel) {
        return select(NetUtils.fixedKey(channel));
    }

    @Nonnull
    @Override
    protected NetEventLoop newChild(int childIndex, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler, Object context) {
        final GroupConfig groupConfig = (GroupConfig) context;
        return new NetEventLoopImp(this, threadFactory, rejectedExecutionHandler, groupConfig.injector);
    }

    @Override
    public NetContext createContext(long localGuid, @Nonnull EventLoop appEventLoop) {
        if (appEventLoop instanceof NetEventLoop) {
            throw new IllegalArgumentException("Bad EventLoop");
        }

        if (appEventLoopSet.add(appEventLoop)) {
            // 监听用户线程关闭
            final EventLoopTerminalEvent terminalEvent = new EventLoopTerminalEvent(appEventLoop);
            for (EventLoop eventLoop : this) {
                // 分开监听 -> 避免某一个出现异常导致其它EventLoop丢失信号
                appEventLoop.terminationFuture().addListener(future -> {
                    if (!eventLoop.isShuttingDown()) {
                        eventLoop.post(terminalEvent);
                    }
                });
            }
        }

        return new DefaultNetContext(localGuid, appEventLoop, this, httpClientManager, nettyThreadManager);
    }
}
