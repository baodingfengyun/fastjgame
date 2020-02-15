/*
 * Copyright 2019 wjybxx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wjybxx.fastjgame.net.eventloop;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.wjybxx.fastjgame.net.manager.NettyThreadManager;
import com.wjybxx.fastjgame.net.misc.DefaultNetContext;
import com.wjybxx.fastjgame.net.module.NetEventLoopGroupModule;
import com.wjybxx.fastjgame.net.utils.NetUtils;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.concurrent.MultiThreadEventLoopGroup;
import com.wjybxx.fastjgame.utils.concurrent.RejectedExecutionHandler;
import com.wjybxx.fastjgame.utils.concurrent.event.EventLoopTerminalEvent;
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

    NetEventLoopGroupImp(int nThreads, @Nonnull ThreadFactory threadFactory,
                         @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                         @Nonnull GroupConfig groupConfig) {
        super(nThreads, threadFactory, rejectedExecutionHandler, groupConfig);

        // 初始化配置
        final NettyThreadManager nettyThreadManager = groupConfig.injector.getInstance(NettyThreadManager.class);
        nettyThreadManager.init(groupConfig.bossGroupThreadNum, groupConfig.workerGroupThreadNum);

        // 这里使用final可以保证初始化完成
        this.nettyThreadManager = nettyThreadManager;
    }

    @Override
    protected void clean() {
        appEventLoopSet.clear();
        // 关闭持有的线程资源
        ConcurrentUtils.safeExecute(nettyThreadManager::shutdown);
    }

    static class GroupConfig {

        private final Injector injector = Guice.createInjector(Stage.PRODUCTION, new NetEventLoopGroupModule());
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
                        ((NetEventLoop) eventLoop).post(terminalEvent);
                    }
                });
            }
        }

        return new DefaultNetContext(localGuid, appEventLoop, this, nettyThreadManager);
    }
}
