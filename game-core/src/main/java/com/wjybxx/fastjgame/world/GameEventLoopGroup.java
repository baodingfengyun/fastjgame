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

import com.wjybxx.fastjgame.concurrent.EventLoopGroup;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroup;

import javax.annotation.Nonnull;

/**
 * 游戏循环组
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
public interface GameEventLoopGroup extends EventLoopGroup {

    @Nonnull
    @Override
    GameEventLoop next();

    /**
     * 注册一个游戏World到某一个GameEventLoop上。
     * 在注册到EventLoop之前，world必须要先初始化guid!
     * 可以保证的是该方法happens-before于{@link World#startUp(GameEventLoop, com.wjybxx.fastjgame.eventloop.NetEventLoopGroup)}。
     * 注意：当World不再使用时，应该主动取消注册。
     * 可以调用{@link World#deregister()} 或 {@link GameEventLoop#deregisterWorld(long)}。
     *
     * @param world 游戏世界
     * @param frameInterval 游戏世界帧间隔(tick间隔)
     * @return future
     */
    @Nonnull
    ListenableFuture<?> registerWorld(World world, long frameInterval);

    /**
     * 获取游戏循环依赖的网络模块组件
     * @return 游戏模块依赖的网络模块
     */
    @Nonnull
    NetEventLoopGroup netEventLoopGroup();
}
