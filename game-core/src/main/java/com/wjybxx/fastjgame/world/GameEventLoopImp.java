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

import com.wjybxx.fastjgame.concurrent.*;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroup;
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
    private static final int FORCE_LOOP_TASK = 1000;

    private final Long2ObjectMap<WorldFrameInfo> worldFrameInfoMap = new Long2ObjectOpenHashMap<>();
    /** 游戏世界需要的网络模块 */
    private final NetEventLoopGroup netEventLoopGroup;

    public GameEventLoopImp(@Nullable EventLoopGroup parent, @Nonnull ThreadFactory threadFactory,
                            @Nonnull NetEventLoopGroup netEventLoopGroup) {
        this(parent, threadFactory, RejectedExecutionHandlers.reject(), netEventLoopGroup);
    }

    public GameEventLoopImp(@Nullable EventLoopGroup parent, @Nonnull ThreadFactory threadFactory,
                            @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                            @Nonnull NetEventLoopGroup netEventLoopGroup) {
        super(parent, threadFactory, rejectedExecutionHandler);
        this.netEventLoopGroup = netEventLoopGroup;
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
    protected void loop() {
        long curTimeMills;
        for (;;) {
            runAllTasks(FORCE_LOOP_TASK);
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
    public ListenableFuture<?> registerWorld(World world, long frameInterval) {
        return submit(() -> {
            registerWorldInternal(world, frameInterval);
        });
    }

    @Override
    public ListenableFuture<?> deregisterWorld(long worldGuid) {
        return submit(() -> {
            deregisterWorldInternal(worldGuid);
        });
    }

    private void registerWorldInternal(World world, long frameInterval) {
        if (worldFrameInfoMap.containsKey(world.worldGuid())) {
            throw new IllegalArgumentException("world " + world.worldGuid() + " already registered");
        }
        try {
            world.startUp(this, netEventLoopGroup);
            // 启动成功才放入
            WorldFrameInfo worldFrameInfo = new WorldFrameInfo(world, frameInterval);
            worldFrameInfoMap.put(world.worldGuid(), worldFrameInfo);
        } catch (Exception e){
            logger.warn("world startUp caught exception. will deregister.", e);
            try {
                world.shutdown();
            } catch (Exception ex) {
                logger.warn("world register shutdown caught exception.", ex);
            }
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
