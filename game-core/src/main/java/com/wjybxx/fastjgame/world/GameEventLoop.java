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

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Q: 为什么{@link GameEventLoop} 要继承{@link GameEventLoopGroup}？
 * A: {@link GameEventLoop}是一个特殊的{@link GameEventLoopGroup}，表示它内部只有它一个{@link GameEventLoop}，
 * 这样的好处是可以使得{@link GameEventLoop}代表{@link GameEventLoopGroup}，我们可以在GameEventLoop上注册world，
 * 这样有关联的world可以注册到同一个{@link GameEventLoop}上，从而消除不必要的同步。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
public interface GameEventLoop extends GameEventLoopGroup, EventLoop {

    @Nullable
    @Override
    GameEventLoopGroup parent();

    @Nonnull
    @Override
    GameEventLoop next();

    /**
     * 将worldGuid对应的world从EventLoop上取消注册。
     * @param worldGuid world的id
     * @return 取消注册成功，或早已取消注册则返回true，否则返回false。
     */
    ListenableFuture<?> deregisterWorld(long worldGuid);
}
