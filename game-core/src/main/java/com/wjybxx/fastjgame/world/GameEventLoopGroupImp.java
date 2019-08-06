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

package com.wjybxx.fastjgame.world;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.wjybxx.fastjgame.concurrent.EventLoopChooserFactory;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.concurrent.MultiThreadEventLoopGroup;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandler;
import com.wjybxx.fastjgame.configwrapper.ConfigWrapper;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroup;
import com.wjybxx.fastjgame.module.GameEventLoopGroupModule;
import com.wjybxx.fastjgame.module.WorldModule;
import com.wjybxx.fastjgame.mrg.CuratorClientMrg;
import com.wjybxx.fastjgame.mrg.GlobalExecutorMrg;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ThreadFactory;

/**
 * 游戏事件循环组基本实现。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
public class GameEventLoopGroupImp extends MultiThreadEventLoopGroup implements GameEventLoopGroup {

    public GameEventLoopGroupImp(int nThreads,
                                 @Nonnull ThreadFactory threadFactory,
                                 @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                                 @Nonnull NetEventLoopGroup netEventLoopGroup) {
        this(nThreads, threadFactory, rejectedExecutionHandler, null, netEventLoopGroup);
    }

    public GameEventLoopGroupImp(int nThreads,
                          @Nonnull ThreadFactory threadFactory,
                          @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                          @Nullable EventLoopChooserFactory chooserFactory,
                          @Nonnull NetEventLoopGroup netEventLoopGroup) {
        super(nThreads, threadFactory, rejectedExecutionHandler, chooserFactory, new RealContext(netEventLoopGroup));

        init();
    }

    @Nonnull
    @Override
    public GameEventLoop next() {
        return (GameEventLoop) super.next();
    }

    @Nonnull
    @Override
    protected GameEventLoop newChild(ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler, Object context) {
        RealContext realContext = (RealContext) context;
        return new GameEventLoopImp(this, threadFactory, rejectedExecutionHandler,
                realContext.netEventLoopGroup, realContext.groupInjector);
    }

    @Nonnull
    @Override
    public ListenableFuture<World> registerWorld(WorldModule worldModule, ConfigWrapper startArgs, int framesPerSecond) {
        return next().registerWorld(worldModule, startArgs, framesPerSecond);
    }

    @Nonnull
    @Override
    public NetEventLoopGroup netEventLoopGroup() {
        return getContext().netEventLoopGroup;
    }

    private RealContext getContext() {
        return (RealContext) context;
    }

    private void init() {
        // 目前没有需要手动启动的类
    }

    /**
     * 清理全局资源，EventLoopGroup级别资源
     */
    @Override
    protected void clean() {
        Injector groupModule = getContext().groupInjector;
        groupModule.getInstance(GlobalExecutorMrg.class).shutdown();
        groupModule.getInstance(CuratorClientMrg.class).shutdown();
    }


    private static class RealContext {
        /**
         * GameEventLoopGroup级别的单例
         */
        private final Injector groupInjector = Guice.createInjector(new GameEventLoopGroupModule());
        /**
         * 网络模块
         */
        private final NetEventLoopGroup netEventLoopGroup;

        private RealContext(NetEventLoopGroup netEventLoopGroup) {
            this.netEventLoopGroup = netEventLoopGroup;
        }
    }

    // -------------------------------------------------- Builder ----------------------------------------

    public static class Builder {

        private int nThreads;
        private ThreadFactory threadFactory;
        private RejectedExecutionHandler rejectedExecutionHandler;
        private EventLoopChooserFactory chooserFactory;
        private NetEventLoopGroup netEventLoopGroup;
        private GameEventLoopGroupModule gameEventLoopGroupModule;

        public Builder setNThreads(int nThreads) {
            this.nThreads = nThreads;
            return this;
        }

        public Builder setThreadFactory(ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
            return this;
        }

        public Builder setRejectedExecutionHandler(RejectedExecutionHandler rejectedExecutionHandler) {
            this.rejectedExecutionHandler = rejectedExecutionHandler;
            return this;
        }

        public Builder setChooserFactory(EventLoopChooserFactory chooserFactory) {
            this.chooserFactory = chooserFactory;
            return this;
        }

        public Builder setNetEventLoopGroup(NetEventLoopGroup netEventLoopGroup) {
            this.netEventLoopGroup = netEventLoopGroup;
            return this;
        }

        public Builder setGameEventLoopGroupModule(GameEventLoopGroupModule gameEventLoopGroupModule) {
            this.gameEventLoopGroupModule = gameEventLoopGroupModule;
            return this;
        }

        public GameEventLoopGroup build() {
            return new GameEventLoopGroupImp(nThreads, threadFactory, rejectedExecutionHandler, chooserFactory, netEventLoopGroup);
        }
        // -------------------------------------------------- 简单的getter --------------------------------------------

        public int getNThreads() {
            return nThreads;
        }

        public ThreadFactory getThreadFactory() {
            return threadFactory;
        }

        public RejectedExecutionHandler getRejectedExecutionHandler() {
            return rejectedExecutionHandler;
        }

        public EventLoopChooserFactory getChooserFactory() {
            return chooserFactory;
        }

        public NetEventLoopGroup getNetEventLoopGroup() {
            return netEventLoopGroup;
        }

        public GameEventLoopGroupModule getGameEventLoopGroupModule() {
            return gameEventLoopGroupModule;
        }
    }
}
