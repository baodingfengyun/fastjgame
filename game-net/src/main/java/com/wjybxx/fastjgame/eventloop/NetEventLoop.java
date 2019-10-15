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
import com.wjybxx.fastjgame.manager.ConnectorManager;
import com.wjybxx.fastjgame.net.common.RpcFuture;
import com.wjybxx.fastjgame.net.common.RpcPromise;
import com.wjybxx.fastjgame.net.common.RpcResponse;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.socket.SocketConnectResponseEvent;
import com.wjybxx.fastjgame.net.socket.SocketEvent;

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
     * 当监听到用户线程关闭时
     *
     * @param userEventLoop 终止的用户线程
     */
    void onUserEventLoopTerminal(EventLoop userEventLoop);
}
