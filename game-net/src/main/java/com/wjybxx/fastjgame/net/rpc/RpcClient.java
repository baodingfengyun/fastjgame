/*
 *  Copyright 2019 wjybxx
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to iBn writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.wjybxx.fastjgame.net.rpc;

import com.wjybxx.fastjgame.net.exception.RpcSessionNotFoundException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletionException;

/**
 * Rpc客户端
 *
 * <h3>主要职责</h3>：
 * 1. 管理session。
 * 2. 通过{@link RpcServerSpec}选择合适的session，然后调用对应的方法。
 *
 * <h3>应用自己实现</h3>
 * Q: 为什么要自己实现？
 * A: 只有应用自己实现，才具有最贴近应用的{@link RpcServerSpec}，才有最适合应用的session管理。
 *
 * <h3>实现约定</h3>
 * 如果找不到对于的session，应该返回{@link FailedRpcFuture},其异常应该为{@link RpcSessionNotFoundException}。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/9
 * github - https://github.com/hl845740757
 */
public interface RpcClient {

    /**
     * 发送一个单向消息给对方。
     *
     * @param serverSpec 服务器描述信息
     * @param message    单向消息
     */
    void send(@Nonnull RpcServerSpec serverSpec, @Nonnull RpcMethodSpec<?> message);

    /**
     * 发送一个单向消息给对方，并立即刷新缓冲区。
     *
     * @param serverSpec 服务器描述信息
     * @param message    单向消息
     */
    void sendAndFlush(@Nonnull RpcServerSpec serverSpec, @Nonnull RpcMethodSpec<?> message);

    /**
     * 发送一个rpc请求给对方。
     *
     * @param serverSpec 服务器描述信息
     * @param request    rpc请求对象
     */
    <V> RpcFuture<V> call(@Nonnull RpcServerSpec serverSpec, @Nonnull RpcMethodSpec<V> request);

    /**
     * 发送一个rpc请求给对方，并立即刷新缓冲区。
     *
     * @param serverSpec 服务器描述信息
     * @param request    rpc请求对象
     */
    <V> RpcFuture<V> callAndFlush(@Nonnull RpcServerSpec serverSpec, @Nonnull RpcMethodSpec<V> request);

    /**
     * 发送一个rpc请求给对方，会立即刷新缓冲区，并阻塞到结果返回或超时。
     *
     * @param serverSpec 服务器描述信息
     * @param request    rpc请求对象
     * @return 方法调用结果
     */
    @Nullable
    <V> V syncCall(@Nonnull RpcServerSpec serverSpec, @Nonnull RpcMethodSpec<V> request) throws CompletionException;

}
