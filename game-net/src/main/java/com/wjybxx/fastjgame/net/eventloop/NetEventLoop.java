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

import com.wjybxx.fastjgame.net.common.RpcFuture;
import com.wjybxx.fastjgame.net.common.RpcPromise;
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.eventbus.EventDispatcher;

import javax.annotation.Nonnull;

/**
 * 网络循环。
 * <p>
 * 最开始一直想的是做一个网络层，因此取名为{@link NetEventLoop}，后面发现吧，其实就是事件分发线程！
 *
 * @author wjybxx
 * @version 2.0
 * date - 2019/8/3
 * github - https://github.com/hl845740757
 */
public interface NetEventLoop extends EventLoop, NetEventLoopGroup, EventDispatcher {

    /**
     * 创建一个RpcPromise
     *
     * @param appEventLoop 用户所在的EventLoop
     * @param timeoutMs    指定的过期时间
     * @return promise
     */
    @Nonnull
    <V> RpcPromise<V> newRpcPromise(@Nonnull EventLoop appEventLoop, long timeoutMs);

    /**
     * 创建rpcFuture，它关联的rpc操作早已完成。在它上面的监听会立即执行。
     *
     * @param appEventLoop 用户所在的EventLoop
     * @param cause        造成失败的原因
     * @return rpcFuture
     */
    @Nonnull
    <V> RpcFuture<V> newFailedRpcFuture(@Nonnull EventLoop appEventLoop, @Nonnull Throwable cause);

    /**
     * {@inheritDoc}
     *
     * @apiNote 时序保证：实现必须和{@link #execute(Runnable)}具有相同的时序保证，也就是底层是{@link #execute(Runnable)}的一个封装。
     */
    @Override
    void post(@Nonnull Object event);
}
