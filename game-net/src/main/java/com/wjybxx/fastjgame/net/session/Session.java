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

package com.wjybxx.fastjgame.net.session;

import com.wjybxx.fastjgame.net.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.net.rpc.RpcFuture;
import com.wjybxx.fastjgame.net.rpc.RpcMethodSpec;
import com.wjybxx.fastjgame.net.rpc.RpcServerSpec;
import com.wjybxx.fastjgame.utils.annotation.Internal;
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * 一个连接的抽象，它可能是一个socket连接，也可能是JVM内的线程之间内的连接。
 *
 * <h3>使用者注意</h3>
 * 1. 所有的消息都满足先发的先到。
 * 2. 但是要注意一个问题：{@link #syncCall(RpcMethodSpec)}会打乱处理的顺序！同步Rpc调用的结果会被你提前处理，其它消息可能先到，但是由于你处于阻塞状态，而导致被延迟处理。<br>
 * 3. 先发送的请求不一定先获得结果！对方什么时候返回给你结果是不确定的！<br>
 *
 * <h3>实现要求</h3>
 * 1. 单向消息(send系列方法)：无论执行成功还是失败，实现必须忽略调用的方法的执行结果(最好不回传结果，而不是仅仅不上报给调用者)。
 * 2. Rpc调用(call系列方法)：如果调用的方法执行成功，则返回对应的结果。如果方法本身没有返回值，则返回null。如果执行失败，则应该返回对应的异常信息。
 * 3. {@code send} {@code call}之间都满足先发送的必然先到。这样的好处是编程更简单，缺点是同步rpc调用响应会变慢。<br>
 *
 * <p>
 * 暂时并不提供完全的线程安全性保障，如果不是那么有必要的话，便不添加，我的意识里需要session是完全的线程安全的情况并不多。
 *
 * @author wjybxx
 * @version 1.2
 * date - 2019/7/29
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public interface Session extends RpcServerSpec, Comparable<Session> {

    /**
     * 用户为session分配的sessionId。
     * 注意：
     * 1. 必须是全局唯一
     * 2. 尽量有意义
     * <p>
     * 它的重要意义：
     * 1. 线程封闭。
     * 2. 允许连接池。允许同一个客户端与服务器建立多个连接。
     * 3. 允许消息确认机制，跨channel断线重连。
     * <p>
     * Q: 为什么要用户分配？而不是网络层自动分配？
     * A:
     * 1. 用户比网络层更容易做到唯一。<br>
     * 2. 用户能赋予sessionId更确切的含义。<br>
     * 3. 用户可以使sessionId更短，你肯定不想每个sessionId都是60个字节 - 比如使用{@link io.netty.channel.ChannelId}<br>
     */
    String sessionId();

    /**
     * 用户标识
     */
    long localGuid();

    /**
     * session对端唯一标识 - 它还是有必要的
     */
    long remoteGuid();

    /**
     * session所属的用户线程
     */
    EventLoop appEventLoop();

    /**
     * session所属的网络线程。
     */
    NetEventLoop netEventLoop();

    // ---------------------------------------------- 配置信息 ----------------------------------------------

    /**
     * session相关的配置信息
     *
     * @return config
     */
    SessionConfig config();

    /**
     * 设置附加属性。
     * 注意：
     * 1. attachment在session关闭时不会自动删除，当你不需要使用时，可以尽早的释放它(设置为null)。
     * 2. 只有用户线程可以使用它，目前并没有做多线程访问支持。
     *
     * @param newData 新值
     * @return 之前的值，如果不存在，则返回null
     */
    @Nullable
    <T> T attach(@Nullable Object newData);

    /**
     * 返回当前的附加属性（上一次attach的值）。
     * 注意：只有用户线程可以使用它，目前并没有做多线程访问支持。
     *
     * @param <T> 结果类型，方便强制类型转换
     * @return nullable，如果未调用过attach，一定为null
     */
    @Nullable
    <T> T attachment();

    // ----------------------------------------------- 生命周期 ----------------------------------------------

    /**
     * 当且仅当session已成功和对方建立连接，且未断开的情况下返回true。
     */
    boolean isActive();

    /**
     * @return 查询session是否已开始关闭，已开始关闭则返回true
     */
    boolean isClosed();

    /**
     * 关闭当前session
     * <p>
     * 注意：
     * 逻辑层的校验+网络层的校验并不能保证在session活跃的状态下才有事件！
     * 因为事件会被提交到session所在的executor，因此即使 {@link #isClosed()} true}，也仍然可能收到该session的消息或事件。
     * 逻辑层必须加以处理，因为网络层并不知道这时候逻辑层到底需不需要这些消息。
     */
    void close();

    // ------------------------------------------- 内部API，其它线程调用会抛出异常 -----------------------------------

    @Internal
    SessionPipeline pipeline();

    @Internal
    void fireRead(@Nullable Object msg);

    @Internal
    void fireWrite(@Nonnull Object msg);

    @Internal
    void fireWriteAndFlush(@Nonnull Object msg);
}
