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

package com.wjybxx.fastjgame.eventloop;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.EventLoopGroup;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.manager.AcceptorManager;
import com.wjybxx.fastjgame.manager.HttpSessionManager;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.net.http.HttpRequestEvent;
import com.wjybxx.fastjgame.net.local.LocalPort;
import com.wjybxx.fastjgame.net.local.LocalSessionConfig;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.socket.SocketConnectRequestEvent;
import com.wjybxx.fastjgame.net.socket.SocketMessageEvent;
import com.wjybxx.fastjgame.net.socket.SocketSession;
import com.wjybxx.fastjgame.net.socket.SocketSessionConfig;

import javax.annotation.Nonnull;

/**
 * 网络事件循环组。
 * <p>
 * Q: 哪些接口定义在{@link NetEventLoopGroup}，哪些接口定义在{@link NetEventLoop}？
 * A: 以下类型接口定义在{@link NetEventLoopGroup}:<br>
 * 1. 需要负载均衡的。
 * 2. 需要计算事件归属线程的。
 * <p>
 * Q: 为什么要计算归属线程？{@link #select(int)}？
 * A: 当{@link SocketSession}的channel发生改变时，我们需要能找到它之前所在的线程，{@link Session#sessionId()}就是key。
 * <p>
 * Q: {@code connectXXX}系列方法为什么不能放在{@link NetEventLoop}接口？
 * A: 如果将{@code connectXXX}系列方法放在{@link NetEventLoop}接口将无法处理重复调用问题，用户的同一个sessionId可能出现在多个{@link NetEventLoop}。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/5
 * github - https://github.com/hl845740757
 */
public interface NetEventLoopGroup extends EventLoopGroup {

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
     * 创建一个网络上下文，你通过该上下文可以更方便的使用这里提供的方法。
     *
     * @param localGuid      用户唯一标识
     * @param localEventLoop 方法的调用者所在的eventLoop
     * @return NetContext 创建的context可以用于监听，建立连接，和http请求
     */
    NetContext createContext(long localGuid, @Nonnull EventLoop localEventLoop);

    // --------------------------------------------------- 非用户接口 ----------------------------------------

    /**
     * 接收到一个建立连接请求。
     * 它将导致一个<b>确定的</b>{@link NetEventLoop}的{@link AcceptorManager#onRcvConnectRequest(SocketConnectRequestEvent)}方法被调用。
     *
     * @param event 事件参数
     */
    void fireConnectRequest(SocketConnectRequestEvent event);

    /**
     * 接收到一个session发来的消息。
     * 它将导致一个<b>确定的</b>{@link NetEventLoop}的{@link AcceptorManager#onRcvMessage(SocketMessageEvent)} 方法被调用。
     *
     * @param event 事件参数
     */
    void fireMessage_acceptor(SocketMessageEvent event);

    /**
     * 接收到一个http请求。
     * 它将导致一个<b>确定的</b>{@link NetEventLoop}的{@link HttpSessionManager#onRcvHttpRequest(HttpRequestEvent)} 方法被调用。
     *
     * @param event 事件参数
     */
    void fireHttpRequest(HttpRequestEvent event);

    /**
     * 以tcp方式连接远程某个端口
     *
     * @param sessionId     为要建立的session分配一个全局唯一的id，尽量保持有意义。
     * @param remoteGuid    远程对端唯一标识
     * @param remoteAddress 远程地址
     * @param config        session配置信息
     * @param netContext    调用方
     * @return future
     */
    ListenableFuture<Session> connectTcp(String sessionId, long remoteGuid, HostAndPort remoteAddress, SocketSessionConfig config, NetContext netContext);

    /**
     * 以websocket方式连接远程某个端口
     *
     * @param sessionId     为要建立的session分配一个全局唯一的id，尽量保持有意义。
     * @param remoteGuid    远程对端唯一标识
     * @param remoteAddress 远程地址
     * @param websocketUrl  升级为webSocket的地址
     * @param config        session配置信息
     * @param netContext    调用方
     * @return future 如果想消除同步，添加监听器时请绑定EventLoop
     */
    ListenableFuture<Session> connectWS(String sessionId, long remoteGuid, HostAndPort remoteAddress, String websocketUrl, SocketSessionConfig config, NetContext netContext);

    /**
     * 与JVM内的另一个线程建立session。
     * 注意：{@link LocalPort}必须是同一个{@link NetEventLoop}创建的。
     *
     * @param sessionId  为要建立的session分配一个全局唯一的id，尽量保持有意义。
     * @param remoteGuid 远程对端唯一标识
     * @param localPort  远程“端口”信息
     * @param config     配置信息
     * @param netContext 调用方
     * @return future 如果想消除同步，添加监听器时请绑定EventLoop
     */
    ListenableFuture<Session> connectLocal(String sessionId, long remoteGuid, LocalPort localPort, LocalSessionConfig config, NetContext netContext);
}
