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
import com.wjybxx.fastjgame.manager.AcceptorManager;
import com.wjybxx.fastjgame.manager.ConnectorManager;
import com.wjybxx.fastjgame.manager.HttpSessionManager;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.net.common.RpcFuture;
import com.wjybxx.fastjgame.net.common.RpcPromise;
import com.wjybxx.fastjgame.net.common.RpcResponse;
import com.wjybxx.fastjgame.net.http.HttpRequestEvent;
import com.wjybxx.fastjgame.net.local.LocalPort;
import com.wjybxx.fastjgame.net.local.LocalSessionConfig;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.socket.SocketConnectRequestEvent;
import com.wjybxx.fastjgame.net.socket.SocketConnectResponseEvent;
import com.wjybxx.fastjgame.net.socket.SocketEvent;
import com.wjybxx.fastjgame.net.socket.SocketSessionConfig;

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
public interface NetEventLoop extends EventLoop, NetEventLoopGroup {

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

    // ---------------------------------------- socket ------------------------------------------------------

    /**
     * 该方法的被调用意味着它管理的某一个{@link Session}接收到对方的连接响应。
     * 它将导致{@link ConnectorManager#onRcvConnectResponse(SocketConnectResponseEvent)}方法被调用。
     *
     * @param event 接收到建立连接响应
     */
    void fireConnectResponse(SocketConnectResponseEvent event);

    /**
     * 该方法的被调用意味着它管理的某一个{@link Session}接收产生了一个事件。
     * 它将导致{@link ConnectorManager#onSessionEvent(SocketEvent)} 方法被调用。
     *
     * @param event 接收到的消息事件
     */
    void fireEvent_connector(SocketEvent event);

    /**
     * 接收到一个建立连接请求。
     * 它将导致一个<b>确定的</b>{@link NetEventLoop}的{@link AcceptorManager#onRcvConnectRequest(SocketConnectRequestEvent)}方法被调用。
     *
     * @param event 事件参数
     */
    void fireConnectRequest(SocketConnectRequestEvent event);

    /**
     * 该方法的被调用意味着它管理的某一个{@link Session}接收产生了一个事件。
     * 它将导致一个<b>确定的</b>{@link NetEventLoop}的{@link AcceptorManager#onSessionEvent(SocketEvent)} 方法被调用。
     *
     * @param event 事件参数
     */
    void fireEvent_acceptor(SocketEvent event);

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

    /**
     * 接收到一个http请求。
     * 它将导致一个<b>确定的</b>{@link NetEventLoop}的{@link HttpSessionManager#onRcvHttpRequest(HttpRequestEvent)} 方法被调用。
     *
     * @param event 事件参数
     */
    void fireHttpRequest(HttpRequestEvent event);
}
