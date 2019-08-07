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
import com.wjybxx.fastjgame.concurrent.EventLoopGroup;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandler;
import com.wjybxx.fastjgame.concurrent.SingleThreadEventLoop;
import com.wjybxx.fastjgame.configwrapper.ConfigWrapper;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroup;
import com.wjybxx.fastjgame.function.AnyRunnable;
import com.wjybxx.fastjgame.module.GameEventLoopModule;
import com.wjybxx.fastjgame.module.WorldModule;
import com.wjybxx.fastjgame.mrg.CuratorMrg;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.EventLoopUtils;
import com.wjybxx.fastjgame.utils.MathUtils;
import com.wjybxx.fastjgame.utils.TimeUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.LockSupport;

/**
 * 游戏事件循环基本实现
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
public class GameEventLoopImp extends SingleThreadEventLoop implements GameEventLoop{

    private static final Logger logger = LoggerFactory.getLogger(GameEventLoopImp.class);
    /** 最多执行多少个任务，必须检测一次world循环 */
    private static final int MAX_BATCH_SIZE = 1024;
    /** 该EventLoop上注册的World */
    private final Long2ObjectMap<WorldFrameInfo> worldFrameInfoMap = new Long2ObjectOpenHashMap<>();
    /** 游戏世界需要的网络模块 */
    private final NetEventLoopGroup netEventLoopGroup;
    /** EventLoop需要使用的模块 */
    private final Injector eventLoopInjector;
    /** 需要管理的资源 */
    private final CuratorMrg curatorMrg;
    private final GameEventLoopMrg gameEventLoopMrg;

    GameEventLoopImp(@Nonnull GameEventLoopGroup parent,
                     @Nonnull ThreadFactory threadFactory,
                     @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                     @Nonnull NetEventLoopGroup netEventLoopGroup, Injector groupInjector) {

        super(parent, threadFactory, rejectedExecutionHandler);
        this.netEventLoopGroup = netEventLoopGroup;

        eventLoopInjector = groupInjector.createChildInjector(new GameEventLoopModule());
        curatorMrg = eventLoopInjector.getInstance(CuratorMrg.class);
        gameEventLoopMrg = eventLoopInjector.getInstance(GameEventLoopMrg.class);
    }

    @Override
    protected Queue<Runnable> newTaskQueue(int maxTaskNum) {
        return new ConcurrentLinkedQueue<>();
    }

    @Nullable
    @Override
    public GameEventLoopGroup parent() {
        return (GameEventLoopGroup) super.parent();
    }

    @Nonnull
    @Override
    public GameEventLoop next() {
        return (GameEventLoop) super.next();
    }

    @Override
    protected void init() throws Exception {
        super.init();
        gameEventLoopMrg.publish(this);
    }

    @Override
    protected void loop() {
        long curTimeMills;
        for (;;) {
            // 指定执行任务最大数，避免导致world延迟过高
            runAllTasks(MAX_BATCH_SIZE);

            curTimeMills = System.currentTimeMillis();

            // 游戏世界刷帧
            for (WorldFrameInfo worldFrameInfo:worldFrameInfoMap.values()) {
                if (worldFrameInfo.nextTickTimeMs < curTimeMills) {
                    continue;
                }
                worldFrameInfo.nextTickTimeMs = curTimeMills + worldFrameInfo.frameInterval;
                safeTick(worldFrameInfo.world, curTimeMills);
            }

            if (confirmShutdown()) {
                break;
            }

            // 睡眠1毫秒，避免占用太多cpu
            LockSupport.parkNanos(TimeUtils.NANO_PER_MILLISECOND);
        }
    }

    private static void safeTick(World world, long curTimeMills) {
        try {
            world.tick(curTimeMills);
        } catch (Exception e){
            logger.warn("world {}-{} tick caught exception.", world.worldRole(), world.worldGuid(), e);
        }
    }

    @Override
    protected void clean() throws Exception {
        super.clean();
        // 关闭所有游戏world
        for (WorldFrameInfo worldFrameInfo:worldFrameInfoMap.values()) {
            ConcurrentUtils.safeExecute((AnyRunnable) worldFrameInfo.world::shutdown);
        }
        // 关闭线程级资源
        curatorMrg.shutdown();
    }

    @Nonnull
    @Override
    public ListenableFuture<World> registerWorld(WorldModule worldModule, ConfigWrapper startArgs, int framesPerSecond) {
        // 校验帧率
        final long frameInterval = MathUtils.frameInterval(framesPerSecond);
        // world可能注册一个world，因此可能是本地线程调用
        return EventLoopUtils.submitOrRun(this, () -> registerWorldInternal(worldModule, startArgs, framesPerSecond, frameInterval));
    }

    @Nonnull
    @Override
    public NetEventLoopGroup netEventLoopGroup() {
        return netEventLoopGroup;
    }

    @Override
    public ListenableFuture<?> deregisterWorld(long worldGuid) {
        // 可能是world线程自己取消注册，因此可能是本地线程调用
        return EventLoopUtils.submitOrRun(this, () -> {
            deregisterWorldInternal(worldGuid);
        });
    }

    private World registerWorldInternal(WorldModule worldModule, ConfigWrapper startArgs, int framesPerSecond, long frameInterval) {
        final Injector worldInjector = eventLoopInjector.createChildInjector(worldModule);
        final World world = worldInjector.getInstance(World.class);
        try {
            world.startUp(startArgs, framesPerSecond);
            // 启动成功才放入
            WorldFrameInfo worldFrameInfo = new WorldFrameInfo(world, frameInterval);
            worldFrameInfoMap.put(world.worldGuid(), worldFrameInfo);
            return world;
        } catch (Exception e){
            // 出现任何异常，都尝试关闭world
            logger.warn("world startUp caught exception. will deregister.", e);
            try {
                world.shutdown();
            } catch (Exception ex) {
                logger.warn("world register shutdown caught exception.", ex);
            }
            // 重新抛出异常，避免用户对启动失败的world进行操作。
            ConcurrentUtils.rethrow(e);
            // unreachable
            return null;
        }
    }

    private void deregisterWorldInternal(long worldGuid) {
        WorldFrameInfo worldFrameInfo = worldFrameInfoMap.remove(worldGuid);
        if (null == worldFrameInfo) {
            return;
        }
        try {
            worldFrameInfo.world.shutdown();
        } catch (Exception e){
            logger.warn("world deregister shutdown caught exception.", e);
        }
    }

    private static class WorldFrameInfo {

        private final World world;
        private final long frameInterval;
        private long nextTickTimeMs;

        private WorldFrameInfo(World world, long frameInterval) {
            this.world = world;
            this.frameInterval = frameInterval;
        }
    }
}
