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

package com.wjybxx.fastjgame.net;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.net.pipeline.SessionConfig;
import com.wjybxx.fastjgame.net.pipeline.SessionOutboundInvoker;
import com.wjybxx.fastjgame.net.pipeline.SessionPipeline;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * 一个连接的抽象，它可能是一个socket连接，也可能是JVM内的线程之间内的连接，不论它真的是什么，你都可以以相同的方式使用它们发送消息。
 *
 * <h3>时序问题</h3>
 * 1. {@link #send(Object)}、{@link #call(Object, RpcCallback)}、{@link #sync(Object)}、{@link RpcResponseChannel#write(RpcResponse)}
 * 之间都满足先发送的必然先到。这样的好处是编程更简单，缺点是同步rpc调用响应会变慢，如果真的需要插队的处理机制，未来再进行拓展（很容易）。
 * <p>
 * 2. 但是要注意一个问题：{@link #sync(Object)}会打乱处理的顺序！同步Rpc调用的结果会被你提前处理，其它消息可能先到，但是由于你处于阻塞状态，而导致被延迟处理。
 * <p>
 * 3. 先发送的请求不一定先获得结果！对方什么时候返回给你结果是不确定的！
 *
 * <p><br>
 * 注意：
 * 1. 特定的 localGuid 和 remoteGuid 在同一个NetEventLoop下只能建立一个链接！！！它俩确定唯一的一个session。
 * 并不支持在不同的端口的上以相同的id再建立连接，只能存在于不同于的{@link NetEventLoop}。<br>
 * 2. 这里提供的接口并不是那么的清晰易懂，偏原始、偏底层，应用层可以提供更良好的封装。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/29
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public interface Session extends SessionOutboundInvoker {

    /**
     * 会话关联的本地对象guid
     */
    long localGuid();

    /**
     * 会话关联的本地角色类型
     */
    RoleType localRole();

    /**
     * 远程的guid
     */
    long remoteGuid();

    /**
     * 远程的角色类型
     */
    RoleType remoteRole();

    /**
     * session相关的配置信息
     *
     * @return config
     */
    SessionConfig config();

    // ----------------------------------------------- 生命周期 ----------------------------------------------

    /**
     * 当且仅当session已成功和对方建立连接，且未断开的情况下返回true。
     */
    boolean isActive();

    /**
     * 移除当前session
     * <p>
     * 注意：
     * 逻辑层的校验+网络层的校验并不能保证在session活跃的状态下才有事件！
     * 因为事件会被提交到session所在的executor，因此即使 {@link #isActive() false}，也仍然可能收到该session的消息或事件。
     * 逻辑层必须加以处理，因为网络层并不知道这时候逻辑层到底需不需要这些消息。
     */
    ListenableFuture<?> close();

    // ------------------------------------------------ 发送消息 ------------------------------------------

    /**
     * 发送一个单向消息给对方
     *
     * @param message 单向消息
     */
    void send(@Nonnull Object message);

    /**
     * 发送一个rpc请求给对方，会使用默认的超时时间（配置文件中指定）。
     * 注意：
     * 1. {@link RpcCallback}执行在用户线程。如果是用户线程发起rpc请求，则不必担心线程安全问题。否则需要注意callback的线程安全问题。
     * <p>
     * Q: 为什么异步RPC调用不是返回一个Future? <br>
     * A: 使用future将增加额外的消耗，而且容易错误使用。
     *
     * @param request  rpc请求对象
     * @param callback 回调函数
     */
    void call(@Nonnull Object request, @Nonnull RpcCallback callback);

    /**
     * 发送一个rpc请求给对方。
     *
     * @param request   rpc请求对象
     * @param callback  回调函数
     * @param timeoutMs 超时时间，毫秒，必须大于0，必须有超时时间。
     * @see #call(Object, RpcCallback)
     */
    void call(@Nonnull Object request, @Nonnull RpcCallback callback, long timeoutMs);

    /**
     * 发送一个rpc请求给对方，并阻塞到结果返回或超时。
     *
     * @param request rpc请求对象
     * @return rpc返回结果
     */
    @Nonnull
    RpcResponse sync(@Nonnull Object request);

    /**
     * 发送一个rpc请求给对方，并阻塞到结果返回或超时。
     * 注意与{@link #sync(Object)}相同的警告。
     *
     * @param request   rpc请求对象
     * @param timeoutMs 超时时间，毫秒，必须大于0，否则死锁可能！！！
     * @return rpc返回结果
     */
    @Nonnull
    RpcResponse sync(@Nonnull Object request, long timeoutMs);

    // --------------------------------------- session运行的网络环境（不是给用户的API） -------------------------------------

    /**
     * session所属的网络线程。
     * - 注意：不一定和创建它的NetContext所属的线程一致
     * Q: why?
     * A: 为了更好的性能，消除不必要的同步！
     */
    NetEventLoop netEventLoop();

    /**
     * session所属的用户线程
     */
    EventLoop localEventLoop();

    /**
     * 获取session关联的pipeline，不是给用户的API
     *
     * @return pipeline
     */
    SessionPipeline pipeline();
}
