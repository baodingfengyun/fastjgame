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
import com.wjybxx.fastjgame.concurrent.*;
import com.wjybxx.fastjgame.configwrapper.ArrayConfigWrapper;
import com.wjybxx.fastjgame.configwrapper.ConfigWrapper;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.module.WorldGroupModule;
import com.wjybxx.fastjgame.module.WorldModule;
import com.wjybxx.fastjgame.mrg.CuratorClientMrg;
import com.wjybxx.fastjgame.mrg.GlobalExecutorMrg;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.MathUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    private GameEventLoopGroupImp(@Nonnull ThreadFactory threadFactory,
                                  @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                                  @Nullable EventLoopChooserFactory chooserFactory,
                                  @Nonnull RealContext realContext) {
        super(realContext.children.size(), threadFactory, rejectedExecutionHandler, chooserFactory, realContext);
    }

    @Nonnull
    @Override
    public GameEventLoop next() {
        return (GameEventLoop) super.next();
    }

    @Nonnull
    @Override
    public GameEventLoop select(int key) {
        return (GameEventLoop) super.select(key);
    }

    @Nonnull
    @Override
    protected GameEventLoop newChild(int childIndex, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler, Object context) {
        RealContext realContext = (RealContext) context;
        return new GameEventLoopImp(this, threadFactory, rejectedExecutionHandler,
                realContext.netEventLoop, realContext.groupInjector, realContext.children.get(childIndex));
    }

    @Nonnull
    @Override
    public NetEventLoop netEventLoop() {
        return getContext().netEventLoop;
    }

    private RealContext getContext() {
        return (RealContext) context;
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

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {

        private ThreadFactory threadFactory = new DefaultThreadFactory("WORLD");
        private RejectedExecutionHandler rejectedExecutionHandler = RejectedExecutionHandlers.abort();
        private EventLoopChooserFactory chooserFactory = null;

        private NetEventLoop netEventLoop;
        private final List<WorldStartInfo> children = new ArrayList<>();

        private Builder() {
        }

        public Builder setThreadFactory(@Nonnull ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
            return this;
        }

        public Builder setRejectedExecutionHandler(@Nonnull RejectedExecutionHandler rejectedExecutionHandler) {
            this.rejectedExecutionHandler = rejectedExecutionHandler;
            return this;
        }

        public Builder setChooserFactory(@Nullable EventLoopChooserFactory chooserFactory) {
            this.chooserFactory = chooserFactory;
            return this;
        }

        public Builder setNetEventLoop(@Nonnull NetEventLoop netEventLoop) {
            this.netEventLoop = netEventLoop;
            return this;
        }

        public Builder addWorld(@Nonnull WorldModule worldModule, @Nonnull String[] startArgs, int framesPerSecond) {
            return addWorld(worldModule, new ArrayConfigWrapper(startArgs), framesPerSecond);
        }

        public Builder addWorld(@Nonnull WorldModule worldModule, @Nonnull ConfigWrapper startArgs, int framesPerSecond) {
            final long frameInterval = MathUtils.frameInterval(framesPerSecond);
            children.add(new WorldStartInfo(worldModule, startArgs, frameInterval));
            return this;
        }

        public GameEventLoopGroupImp build() {
            if (null == netEventLoop) {
                throw new IllegalStateException("netEventLoop is null");
            }
            final RealContext realContext = new RealContext(netEventLoop, children);
            final GameEventLoopGroupImp gameEventLoopGroup = new GameEventLoopGroupImp(threadFactory, rejectedExecutionHandler, chooserFactory, realContext);
            // 构造完成之后，启动所有线程
            gameEventLoopGroup.forEach(eventLoop -> eventLoop.execute(ConcurrentUtils.WEAK_UP_TASK));
            // 返回引用
            return gameEventLoopGroup;
        }
    }

    private static class RealContext {
        /**
         * GameEventLoopGroup级别的单例
         */
        private final Injector groupInjector = Guice.createInjector(new WorldGroupModule());
        /**
         * 网络模块
         */
        private final NetEventLoop netEventLoop;
        /**
         * 所有的子节点
         */
        private final List<WorldStartInfo> children;

        private RealContext(NetEventLoop netEventLoop, List<WorldStartInfo> children) {
            this.netEventLoop = netEventLoop;
            this.children = Collections.unmodifiableList(children);
        }
    }

    static class WorldStartInfo {
        /**
         * world所有类
         */
        final WorldModule worldModule;
        /**
         * 启动参数
         */
        final ConfigWrapper startArgs;
        /**
         * 帧间隔
         */
        final long frameInterval;

        WorldStartInfo(WorldModule worldModule, ConfigWrapper startArgs, long frameInterval) {
            this.worldModule = worldModule;
            this.startArgs = startArgs;
            this.frameInterval = frameInterval;
        }
    }
}
