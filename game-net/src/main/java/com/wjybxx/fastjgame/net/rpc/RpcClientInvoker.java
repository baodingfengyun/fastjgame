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

import com.wjybxx.fastjgame.net.session.Session;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletionException;

/**
 * rpc客户端真正执行调用的地方。
 * {@link RpcClient}负责通过{@link RpcServerSpec}寻找合适的{@link Session}，然后这里真正执行调用。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/9
 */
public interface RpcClientInvoker {

    /**
     * 发送一个单向消息给对方。
     *
     * @param session 服务器描述信息
     * @param message 单向消息
     */
    void send(@Nonnull Session session, @Nonnull RpcMethodSpec<?> message);

    /**
     * 发送一个单向消息给对方，并立即刷新缓冲区。
     *
     * @param session 服务器描述信息
     * @param message 单向消息
     */
    void sendAndFlush(@Nonnull Session session, @Nonnull RpcMethodSpec<?> message);

    /**
     * 发送一个rpc请求给对方。
     *
     * @param session 服务器描述信息
     * @param request rpc请求对象
     */
    <V> RpcFuture<V> call(@Nonnull Session session, @Nonnull RpcMethodSpec<V> request);

    /**
     * 发送一个rpc请求给对方，并立即刷新缓冲区。
     *
     * @param session 服务器描述信息
     * @param request rpc请求对象
     */
    <V> RpcFuture<V> callAndFlush(@Nonnull Session session, @Nonnull RpcMethodSpec<V> request);

    /**
     * 发送一个rpc请求给对方，会立即刷新缓冲区，并阻塞到结果返回或超时。
     *
     * @param session 服务器描述信息
     * @param request rpc请求对象
     * @return 方法调用结果
     */
    @Nullable
    <V> V syncCall(@Nonnull Session session, @Nonnull RpcMethodSpec<V> request) throws CompletionException;

}
