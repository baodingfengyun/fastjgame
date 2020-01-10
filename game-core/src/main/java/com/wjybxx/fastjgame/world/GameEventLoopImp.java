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

import com.google.inject.Injector;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandler;
import com.wjybxx.fastjgame.concurrent.SingleThreadEventLoop;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroup;
import com.wjybxx.fastjgame.mgr.GameEventLoopMgr;
import com.wjybxx.fastjgame.module.WorldGroupModule;
import com.wjybxx.fastjgame.timer.DefaultTimerSystem;
import com.wjybxx.fastjgame.timer.TimerHandle;
import com.wjybxx.fastjgame.timer.TimerSystem;
import com.wjybxx.fastjgame.world.GameEventLoopGroupImp.WorldStartInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ThreadFactory;

import static com.wjybxx.fastjgame.utils.ConcurrentUtils.sleepQuietly;

/**
 * 游戏事件循环基本实现。
 * <p>
 * 修改为继承{@link SingleThreadEventLoop}。
 * 避免死锁可能，或同步rpc调用时超时(同时阻塞网络线程)。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
public class GameEventLoopImp extends SingleThreadEventLoop implements GameEventLoop {

    private static final Logger logger = LoggerFactory.getLogger(GameEventLoopImp.class);

    private static final int TASK_BATCH_SIZE = 8192;

    /**
     * 游戏世界需要的网络模块
     */
    private final NetEventLoopGroup netEventLoopGroup;

    /**
     * {@link WorldGroupModule}管理的线程安全的控制器
     */
    private final Injector groupInjector;

    /**
     * 要启动的world信息
     */
    private final WorldStartInfo worldStartInfo;
    private final TimerSystem timerSystem = new DefaultTimerSystem(1);
    /**
     * world
     */
    private World world;

    GameEventLoopImp(@Nullable GameEventLoopGroup parent,
                     @Nonnull ThreadFactory threadFactory,
                     @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                     @Nonnull NetEventLoopGroup netEventLoopGroup,
                     @Nonnull Injector groupInjector,
                     @Nonnull WorldStartInfo worldStartInfo) {
        super(parent, threadFactory, rejectedExecutionHandler);
        this.netEventLoopGroup = netEventLoopGroup;
        this.groupInjector = groupInjector;
        this.worldStartInfo = worldStartInfo;
    }

    @Nullable
    @Override
    public GameEventLoopGroup parent() {
        return (GameEventLoopGroup) super.parent();
    }

    @Nonnull
    @Override
    public GameEventLoop next() {
        return this;
    }

    @Nonnull
    @Override
    public GameEventLoop select(int key) {
        return this;
    }

    @Override
    protected void init() throws Exception {
        super.init();
        final Injector worldInjector = groupInjector.createChildInjector(worldStartInfo.worldModule);
        // 发布自己，使得world内部可以访问 - 现在的模型下使用threadLocal也是可以的。
        worldInjector.getInstance(GameEventLoopMgr.class).publish(this);

        // 创建world并尝试启动
        world = worldInjector.getInstance(World.class);
        world.startUp(worldStartInfo.startArgs);

        // init 出现任何异常，都会导致线程关闭，world会在clean的时候调用shutdown
        timerSystem.newFixedDelay(worldStartInfo.frameInterval, this::safeTickWorld);
    }

    @Override
    protected void loop() {
        while (true) {
            try {
                runTasksBatch(TASK_BATCH_SIZE);

                timerSystem.tick();

                if (!confirmShutdown()) {
                    break;
                }

                sleepQuietly(1);
            } catch (Throwable e) {
                logger.warn("", e);
            }
        }
    }

    /**
     * 游戏世界刷帧
     */
    private void safeTickWorld(TimerHandle handle) {
        try {
            world.tick(System.currentTimeMillis());
        } catch (Throwable e) {
            logger.warn("world {}-{} tick caught exception.", world.worldRole(), world.worldGuid(), e);
        }
    }

    @Override
    protected void clean() throws Exception {
        // 关闭游戏world
        if (world != null) {
            shutdownWorldSafely();
        }
    }

    private void shutdownWorldSafely() {
        try {
            world.shutdown();
        } catch (Throwable ex) {
            logger.warn("world shutdown caught exception.", ex);
        }
    }

    @Override
    protected void wakeUp() {
        // 游戏线程可能阻塞在了某个地方 - curator中存在阻塞可能
        interruptThread();
    }

    @Nonnull
    @Override
    public NetEventLoopGroup netEventLoopGroup() {
        return netEventLoopGroup;
    }

}
