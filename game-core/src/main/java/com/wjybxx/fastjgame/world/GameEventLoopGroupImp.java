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

import com.wjybxx.fastjgame.concurrent.EventLoopChooserFactory;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.concurrent.MultiThreadEventLoopGroup;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroup;

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

    public GameEventLoopGroupImp(int nThreads, @Nonnull ThreadFactory threadFactory, NetEventLoopGroup netEventLoopGroup) {
        super(nThreads, threadFactory, netEventLoopGroup);
    }

    public GameEventLoopGroupImp(int nThreads, @Nonnull ThreadFactory threadFactory, @Nullable EventLoopChooserFactory chooserFactory, NetEventLoopGroup netEventLoopGroup) {
        super(nThreads, threadFactory, chooserFactory, netEventLoopGroup);
    }

    @Nonnull
    @Override
    public GameEventLoop next() {
        return (GameEventLoop) super.next();
    }

    @Nonnull
    @Override
    protected GameEventLoop newChild(ThreadFactory threadFactory, Object context) {
        return new GameEventLoopImp(this, threadFactory, (NetEventLoopGroup)context);
    }

    @Nonnull
    @Override
    public ListenableFuture<?> registerWorld(World world, long frameInterval) {
        return next().registerWorld(world, frameInterval);
    }

    @Nonnull
    @Override
    public NetEventLoopGroup netEventLoopGroup() {
        return (NetEventLoopGroup) context;
    }
}
