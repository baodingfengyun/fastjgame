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

import com.wjybxx.fastjgame.net.misc.NetContext;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.concurrent.EventLoopGroup;
import com.wjybxx.fastjgame.utils.concurrent.FixedEventLoopGroup;
import io.netty.channel.Channel;

import javax.annotation.Nonnull;

/**
 * 网络事件循环组。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/5
 * github - https://github.com/hl845740757
 */
public interface NetEventLoopGroup extends FixedEventLoopGroup {

    @Nonnull
    @Override
    NetEventLoop next();

    /**
     * {@inheritDoc}
     * 对于网络层而言，这个key一般是{@link Session#sessionId()}计算得到的。
     */
    @Nonnull
    @Override
    NetEventLoop select(int key);

    /**
     * 根据sessionId选择一个{@link NetEventLoop}
     *
     * @apiNote 对于同一个sessionId，它选中的{@link NetEventLoop}是不变的
     */
    @Nonnull
    NetEventLoop select(@Nonnull String sessionId);

    /**
     * 根据{@link Channel}选择一个{@link NetEventLoop}
     *
     * @apiNote 对于同一个Channel，它选中的{@link NetEventLoop}是不变的
     */
    @Nonnull
    NetEventLoop select(@Nonnull Channel channel);

    /**
     * 创建一个网络上下文，你通过该上下文可以更方便的使用这里提供的方法。
     *
     * @param localGuid    用户唯一标识
     * @param appEventLoop 方法的调用者所在的eventLoop
     * @return NetContext 创建的context可以用于监听，建立连接，和http请求
     */
    NetContext createContext(long localGuid, @Nonnull EventLoop appEventLoop);

}
