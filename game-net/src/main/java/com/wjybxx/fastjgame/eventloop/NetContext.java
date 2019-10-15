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
import com.wjybxx.fastjgame.concurrent.GlobalEventLoop;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.PortRange;
import com.wjybxx.fastjgame.net.http.HttpRequestDispatcher;
import com.wjybxx.fastjgame.net.http.OkHttpCallback;
import com.wjybxx.fastjgame.net.local.LocalPort;
import com.wjybxx.fastjgame.net.local.LocalSession;
import com.wjybxx.fastjgame.net.local.LocalSessionConfig;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.socket.SocketPort;
import com.wjybxx.fastjgame.net.socket.SocketSessionConfig;
import okhttp3.Response;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.net.BindException;
import java.util.Map;

/**
 * 逻辑层使用的网络上下文 - 它主要负责用户与{@link NetEventLoop}之间的交互。
 * 提供封装 - 降低用户使用难度.
 * <p>
 * Q: 为什么绑定端口实现为了阻塞方法？
 * A: 绑定端口一般发生在服务器启动时，启动时阻塞影响较小，此外绑定失败更加安全，更容易处理。
 * -> 如果以后要改成异步的，可考虑提交到{@link GlobalEventLoop}阻塞执行。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/3
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public interface NetContext {

    /**
     * 网络层使用者标识
     */
    long localGuid();

    /**
     * 用户线程
     * 网络层保证所有的业务逻辑处理最终都会运行在该用户线程。
     */
    EventLoop localEventLoop();

    /**
     * 创建{@link NetContext}的{@link NetEventLoopGroup}.
     */
    NetEventLoopGroup netEventLoopGroup();

    // ----------------------------------- tcp/ws支持 ---------------------------------------

    /**
     * 在指定端口监听tcp连接
     *
     * @param host   地址
     * @param port   端口号
     * @param config session配置信息
     * @return netPort
     */
    default SocketPort bindTcp(String host, int port, @Nonnull SocketSessionConfig config) throws BindException {
        return this.bindTcpRange(host, new PortRange(port, port), config);
    }

    /**
     * 在指定端口范围内选择一个合适的端口监听tcp连接
     *
     * @param host      地址
     * @param portRange 端口范围
     * @param config    session配置信息
     * @return netPort
     */
    SocketPort bindTcpRange(String host, PortRange portRange, @Nonnull SocketSessionConfig config) throws BindException;

    /**
     * 以tcp方式连接远程某个端口
     *
     * @param sessionId     为要建立的session分配一个全局唯一的id，尽量保持有意义。
     * @param remoteGuid    远程对端标识
     * @param remoteAddress 远程地址
     * @param config        session配置信息
     * @return future
     */
    ListenableFuture<Session> connectTcp(String sessionId, long remoteGuid, HostAndPort remoteAddress, @Nonnull SocketSessionConfig config);

    /**
     * 在指定端口监听WebSocket连接
     *
     * @param host          地址
     * @param port          端口
     * @param websocketPath 触发websocket升级的地址
     * @param config        session配置信息
     * @return netPort
     */
    default SocketPort bindWS(String host, int port, String websocketPath, @Nonnull SocketSessionConfig config) throws BindException {
        return this.bindWSRange(host, new PortRange(port, port), websocketPath, config);
    }

    /**
     * 在指定端口范围内选择一个合适的端口监听WebSocket连接
     *
     * @param host          地址
     * @param portRange     端口范围
     * @param websocketPath 触发websocket升级的地址
     * @param config        session配置信息
     * @return netPort
     */
    SocketPort bindWSRange(String host, PortRange portRange, String websocketPath, @Nonnull SocketSessionConfig config) throws BindException;

    /**
     * 以websocket方式连接远程某个端口
     *
     * @param sessionId     为要建立的session分配一个全局唯一的id，尽量保持有意义。
     * @param remoteGuid    远程对端标识
     * @param remoteAddress 远程地址
     * @param config        session配置信息
     * @return future 如果想消除同步，添加监听器时请绑定EventLoop
     */
    ListenableFuture<Session> connectWS(String sessionId, long remoteGuid, HostAndPort remoteAddress, String websocketUrl, @Nonnull SocketSessionConfig config);


    // -------------------------------------- 用于支持JVM内部通信 -------------------------------

    /**
     * 绑定一个JVM端口，用于其它线程建立会话。
     * 其性能与socket的不在一个数量级，如果你的session的双方在同一个进程下，那么强烈建议使用{@link LocalSession}。
     * <pre>
     * 1. 它没有网络传输开销，纯粹的内存数据转移。
     * 2. 没有复杂的网络情况要处理。
     * </pre>
     *
     * @param config 配置信息
     * @return netPort
     */
    LocalPort bindLocal(@Nonnull LocalSessionConfig config);

    /**
     * 与JVM内的另一个线程建立session。
     * 注意：{@link LocalPort}必须是同一个{@link NetEventLoop}创建的。
     *
     * @param sessionId  为要建立的session分配一个全局唯一的id，尽量保持有意义。
     * @param remoteGuid 远程对端标识
     * @param localPort  远程“端口”信息
     * @param config     配置信息
     * @return future 如果想消除同步，添加监听器时请绑定EventLoop
     */
    ListenableFuture<Session> connectLocal(String sessionId, long remoteGuid, @Nonnull LocalPort localPort, @Nonnull LocalSessionConfig config);

    //  --------------------------------------- http支持 -----------------------------------------

    /**
     * 监听某个端口
     *
     * @param host                  地址
     * @param port                  指定端口号
     * @param httpRequestDispatcher 该端口上的协议处理器
     * @return netPort
     */
    default SocketPort bindHttp(String host, int port, @Nonnull HttpRequestDispatcher httpRequestDispatcher) throws BindException {
        return this.bindHttpRange(host, new PortRange(port, port), httpRequestDispatcher);
    }

    /**
     * 在指定端口范围内监听某一个端口。
     *
     * @param host                  地址
     * @param portRange             端口范围
     * @param httpRequestDispatcher 该端口上的协议处理器
     * @return future 可以等待绑定完成。
     */
    SocketPort bindHttpRange(String host, PortRange portRange, @Nonnull HttpRequestDispatcher httpRequestDispatcher) throws BindException;

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
     */
    void asyncGet(String url, @Nonnull Map<String, String> params, @Nonnull OkHttpCallback okHttpCallback);

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
     */
    void asyncPost(String url, @Nonnull Map<String, String> params, @Nonnull OkHttpCallback okHttpCallback);

}
