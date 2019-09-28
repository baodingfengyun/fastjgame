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

package com.wjybxx.fastjgame.eventloop;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.net.*;

import javax.annotation.Nonnull;

/**
 * 网络循环。
 * <p>
 * Q: 为什么2.x开始不再使用{@code NetEventLoopGroup}了？
 * A: 主要是正确性问题，当使用{@code NetEventLoopGroup}时，要保证session在{@code NetEventLoopGroup}中的唯一性较为困难，
 * {@link NetEventLoop}的主要作用是管理连接，以及JVM内部通信的序列化，其实它的工作量很小，单线程也完全足够。
 * 因此去掉{@code NetEventLoopGroup}，使得模型更加简单。之前有些东西属于过度设计了，
 *
 * @author wjybxx
 * @version 2.0
 * date - 2019/8/3
 * github - https://github.com/hl845740757
 */
public interface NetEventLoop extends EventLoop {

    @Nonnull
    @Override
    NetEventLoop next();

    /**
     * 注册一个NetEventLoop的用户(创建一个网络上下文)。
     * 当用户不再使用NetEventLoop时，避免内存泄漏，必须调用
     * {@link NetContext#deregister()} 或 {@link NetEventLoop#deregisterContext(long)}取消注册。
     * <p>
     * 注意：一个localGuid表示一个用户，在同一个NetEventLoop下只能创建一个Context，必须在取消注册成功之后才能再次注册。
     *
     * @param localGuid      context绑定到的角色guid
     * @param localRole      context绑定到的角色类型
     * @param localEventLoop 方法的调用者所在的eventLoop
     * @return NetContext 创建的context可以用于监听，建立连接，和http请求
     */
    ListenableFuture<NetContext> createContext(long localGuid, RoleType localRole, @Nonnull EventLoop localEventLoop);

    /**
     * 取消context的注册
     *
     * @param localGuid 注册的用户
     * @return future
     */
    @Nonnull
    ListenableFuture<?> deregisterContext(long localGuid);

    /**
     * 创建一个RpcPromise
     *
     * @param userEventLoop 用户所在的EventLoop
     * @param timeoutMs     指定的过期时间
     * @return promise
     */
    @Nonnull
    RpcPromise newRpcPromise(@Nonnull EventLoop userEventLoop, long timeoutMs);

    /**
     * 创建rpcFuture，它关联的rpc操作早已完成。在它上面的监听会立即执行。
     *
     * @param userEventLoop 用户所在的EventLoop
     * @param rpcResponse   rpc调用结果
     * @return rpcFuture
     */
    @Nonnull
    RpcFuture newCompletedRpcFuture(@Nonnull EventLoop userEventLoop, @Nonnull RpcResponse rpcResponse);

}
