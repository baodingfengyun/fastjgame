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
import com.wjybxx.fastjgame.misc.PortRange;
import com.wjybxx.fastjgame.net.common.RpcFuture;
import com.wjybxx.fastjgame.net.common.RpcPromise;
import com.wjybxx.fastjgame.net.common.RpcResponse;
import com.wjybxx.fastjgame.net.http.HttpRequestDispatcher;
import com.wjybxx.fastjgame.net.http.OkHttpCallback;
import com.wjybxx.fastjgame.net.local.LocalPort;
import com.wjybxx.fastjgame.net.local.LocalSessionConfig;
import com.wjybxx.fastjgame.net.socket.SocketConnectResponseEvent;
import com.wjybxx.fastjgame.net.socket.SocketMessageEvent;
import com.wjybxx.fastjgame.net.socket.SocketPort;
import com.wjybxx.fastjgame.net.socket.SocketSessionConfig;
import okhttp3.Response;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;

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

    void fireConnectResponse(SocketConnectResponseEvent event);

    void fireMessage_connector(SocketMessageEvent event);

    ListenableFuture<SocketPort> bindTcpRange(String host, PortRange portRange, SocketSessionConfig config, NetContext netContext);

    ListenableFuture<SocketPort> bindWSRange(String host, PortRange portRange, String websocketPath, SocketSessionConfig config, NetContext netContext);

    // ---------------------------------------- localSession 支持 ---------------------------------------------

    ListenableFuture<LocalPort> bindLocal(NetContext netContext, LocalSessionConfig config);

    //  --------------------------------------- http支持 -------------------------------------------------------

    /**
     * 在指定端口范围内监听某一个端口。
     *
     * @param host                  地址
     * @param portRange             端口范围
     * @param httpRequestDispatcher 该端口上的协议处理器
     * @param netContext            用户上下文
     * @return future 可以等待绑定完成。
     */
    ListenableFuture<SocketPort> bindHttpRange(String host, PortRange portRange, @Nonnull HttpRequestDispatcher httpRequestDispatcher, @Nonnull NetContext netContext);

    /**
     * 同步get请求
     *
     * @param url    url
     * @param params get参数
     */
    Response syncGet(String url, @Nonnull Map<String, String> params) throws IOException;

    /**
     * 异步get请求
     *
     * @param url            url
     * @param params         get参数
     * @param okHttpCallback 回调
     * @param localEventLoop 用户线程 - 回调执行线程
     */
    void asyncGet(String url, @Nonnull Map<String, String> params, @Nonnull OkHttpCallback okHttpCallback, @Nonnull EventLoop localEventLoop);

    /**
     * 同步post请求
     *
     * @param url    url
     * @param params post参数
     */
    Response syncPost(String url, @Nonnull Map<String, String> params) throws IOException;

    /**
     * 异步post请求
     *
     * @param url            url
     * @param params         post参数
     * @param okHttpCallback 回调
     * @param localEventLoop 用户线程 - 回调执行线程
     */
    void asyncPost(String url, @Nonnull Map<String, String> params, @Nonnull OkHttpCallback okHttpCallback, EventLoop localEventLoop);
}
