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
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.util.concurrent.FluentFuture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletionException;

/**
 * 用于rpc客户端执行调用。
 * 用户应该使用门面为自己的应用层提供更好的API。
 *
 * <h3>实现要求</h3>
 * 1. 单向消息(send系列方法)：无论执行成功还是失败，实现必须忽略调用的方法的执行结果(最好不回传结果，而不是仅仅不上报给调用者)。
 * 2. Rpc调用(call系列方法)：如果调用的方法执行成功，则返回对应的结果。如果方法本身没有返回值，则返回null。如果执行失败，则应该返回对应的异常信息。
 * 3. {@code send} {@code call}之间都满足先发送的必然先到。这样的好处是编程更简单，缺点是同步rpc调用响应会变慢。<br>
 *
 * <h3>使用者注意</h3>
 * 1. 所有的消息都满足先发的先到。
 * 2. 但是要注意一个问题：{@link #syncCall(Session, RpcMethodSpec)}会打乱处理的顺序！同步Rpc调用的结果会被你提前处理，其它消息可能先到，但是由于你处于阻塞状态，而导致被延迟处理。<br>
 * 3. 先发送的请求不一定先获得结果！对方什么时候返回给你结果是不确定的！<br>
 *
 * <h3>session为null的处理</h3>
 * 如果session为null，则返回一个已失败的{@code Future}，且其异常为{@link RpcSessionNotFoundException}。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/9
 */
public interface RpcInvoker {

    /**
     * 发送一个单向消息给对方。
     *
     * @param session 服务器描述信息
     * @param message 单向消息
     * @param flush   是否刷新缓冲区
     */
    void send(@Nullable Session session, @Nonnull RpcMethodSpec<?> message, boolean flush);

    /**
     * 发送一个rpc请求给对方。
     *
     * @param session 服务器描述信息
     * @param request rpc请求对象
     * @param flush   是否刷新缓冲区
     */
    <V> FluentFuture<V> call(@Nullable Session session, @Nonnull RpcMethodSpec<V> request, boolean flush);

    /**
     * 发送一个rpc请求给对方，会立即刷新缓冲区，并阻塞到结果返回或超时。
     *
     * @param session 服务器描述信息
     * @param request rpc请求对象
     * @return 方法调用结果
     */
    @Nullable
    <V> V syncCall(@Nullable Session session, @Nonnull RpcMethodSpec<V> request) throws CompletionException;
}
