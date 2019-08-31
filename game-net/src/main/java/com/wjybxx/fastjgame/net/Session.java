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

import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * 会话信息，用于获取一个链接的双方的基本信息 和 提供发送异步消息和同步消息的接口。
 *
 * <h3>消息分类</h3>
 * 异步消息：单向消息、异步Rpc请求、异步Rpc调用的结果<br>
 * 同步消息：同步rpc请求、同步Rpc调用的结果
 *
 * <h3>消息顺序问题(极其重要)</h3>
 * 网络层提供以下保证：
 * <li>任意两个异步消息之间，都满足先发送的先到。</li>
 * <li>任意两个同步消息之间，都满足先发送的先到。</li>
 * <li>任意两个异步消息之间，都满足先接收到的先提交给应用层。</li>
 * <li>任意两个同步消息之间，都满足先接收到的先提交给应用层。</li>
 *
 * 警惕：
 * <li>先发送的请求不一定先获得结果！对方什么时候返回给你结果是不确定的！</li>
 *
 * <h3>常见问题</h3>
 * Q: 为什么不提供同步调用 与 异步调用 之间的顺序保证？<br>
 * A: 基于这样的考虑：同步调用表示一种更迫切的需求，期望更快的处理，更快的返回，而异步调用没有这样的语义。
 *
 * Q: 为何删除了返回{@link RpcFuture}的rpc接口？<br>
 * A: 返回{@link RpcFuture}固然可以，但是会导致使用复杂度的急剧攀升。如果没有理解地特别清楚，很容易造成一些奇怪的错误。
 * 此外，返回{@link RpcFuture}的rpc请求不能被优化，因此决定干脆删除它，也可以降低网络层的复杂度。
 *
 * <p><br>
 * Q: Netty线程和用户线程之间会竞争NetEventLoop资源，这个竞争可能非常激烈，如何降低Netty线程和用户线程之间的竞争？<br>
 * A: Session接口中的所有方法都不会对请求进行缓存，都会立即发送到网络层。为了解决这个问题，提炼了{@link Sender}接口，你可以使用带有缓冲的sender，
 * 可以对用户的异步消息进行一定的缓存，使得可以进行批量发送，可以有效的降低竞争。
 * (之前设计了pipeline，但是发现出现多个发送消息的接口以后，会导致使用复杂度的提升，不安全！)
 *
 * <p><br>
 * Q: 为何抽象层没有提供address之类的信息？<br>
 * A: 因为底层会自动处理断线重连等等，这些信息可能会变化，暂时不提供。
 *
 * <p><br>
 * 注意事项：
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
public interface Session {

    // ------------------------------------------------ 注册消息 -----------------------------------------
    /**
     * 该session所在的上下文
     */
    NetContext netContext();

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
     * 创建会话时指定的消息发送方式。
     */
    SessionSenderMode senderMode();

    /**
     * 获取该session关联的消息发送器，不建议应用层使用。
     */
    Sender sender();

    // ----------------------------------------------- 生命周期 ----------------------------------------------
    /**
     * 当前仅当session已成功和对方建立连接，且未断开的情况下返回true。
     */
    boolean isActive();

    /**
     * 关闭当前session
     *
     * 注意：
     * 逻辑层的校验+网络层的校验并不能保证在session活跃的状态下才有事件！
     * 因为事件会被提交到session所在的executor，因此即使 {@link #isActive() false}，也仍然可能收到该session的消息或事件。
     * 逻辑层必须加以处理，因为网络层并不知道这时候逻辑层到底需不需要这些消息。
     */
    ListenableFuture<?> close();

    // ------------------------------------------------ 发送消息 ------------------------------------------
    /**
     * 发送一个单向消息给对方
     * @param message 单向消息
     */
    void send(@Nonnull Object message);

    // ------------------------------------------------ 异步Rpc请求 ---------------------------------------------
    /**
     * 发送一个**异步**rpc请求给对方，会使用默认的超时时间（配置文件中指定）。
     * 注意：
     * 1. 它不返回{@link RpcFuture}，该调用可以被优化，有更好的吞吐量。
     * 2. 表示用户默认，即使RpcCallback内部是多个RpcCallback，那么发起请求的用户收不到响应，其他用户也收不到响应。
     * 3. {@link RpcCallback}执行在当前线程或用户线程。如果是用户线程发起rpc请求，则不必担心线程安全问题。否则需要注意callback的线程安全问题。
     *
     * @param request rpc请求对象
     * @param callback 回调函数
     */
    void rpc(@Nonnull Object request, @Nonnull RpcCallback callback);

    /**
     * 发送一个**异步**rpc请求给对方。
     * @see #rpc(Object, RpcCallback)
     *
     * @param request rpc请求对象
     * @param callback 回调函数
     * @param timeoutMs 超时时间，毫秒，必须大于0，必须有超时时间。
     */
    void rpc(@Nonnull Object request, @Nonnull RpcCallback callback, long timeoutMs);

    // ------------------------------------------------ 同步Rpc请求 ---------------------------------------------
    /**
     * 发送一个**同步**rpc请求给对方，并阻塞到结果返回或超时，或中断，会使用默认的超时时间（配置文件中指定）。
     *
     * @apiNote
     * 注意：同步rpc调用会尽可能地立即发送，它的结果对方也会尽可能地立即返回，因此它与{@link #send(Object)}{@link #rpc(Object, RpcCallback)}之间
     * 没有顺序保证，只保证与其它的**同步**rpc调用之间的顺序！
     *
     * @param request rpc请求对象
     * @return rpc返回结果
     */
    @Nonnull
    RpcResponse syncRpc(@Nonnull Object request) throws InterruptedException;

    /**
     * 发送一个**同步**rpc请求给对方，并阻塞到结果返回或超时或被中断。
     * 注意与{@link #syncRpc(Object)}相同的警告。
     *
     * @param request rpc请求对象
     * @param timeoutMs 超时时间，毫秒，必须大于0，否则死锁可能！！！
     * @return rpc返回结果
     */
    @Nonnull
    RpcResponse syncRpc(@Nonnull Object request, long timeoutMs) throws InterruptedException;

    /**
     * 发送一个**同步**rpc请求给对方，并阻塞到结果返回或超时或被中断。
     * 注意与{@link #syncRpc(Object)}相同的警告。
     * @param request rpc请求对象
     * @return rpc返回结果
     */
    @Nonnull
    RpcResponse syncRpcUninterruptibly(@Nonnull Object request);

    /**
     * 发送一个**同步**rpc请求给对方，并阻塞到结果返回或超时。
     * 注意与{@link #syncRpc(Object)}相同的警告。
     *
     * @param request rpc请求对象
     * @param timeoutMs 超时时间，毫秒，必须大于0，否则死锁可能！！！
     * @return rpc返回结果
     */
    @Nonnull
    RpcResponse syncRpcUninterruptibly(@Nonnull Object request, long timeoutMs);

    // --------------------------------------------  缓冲区处理 -------------------------------------------------

    /**
     * 如果存在缓冲，则清空缓冲区。
     * 注意：如果为session创建的是带有缓冲的sender，那么必须调用flush，否则可能有消息残留。
     */
    void flush();
}
