/*
 *    Copyright 2019 wjybxx
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.wjybxx.fastjgame.net;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.PortRange;
import com.wjybxx.fastjgame.net.initializer.HttpServerInitializer;
import com.wjybxx.fastjgame.net.injvm.JVMPort;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import okhttp3.Response;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.Map;

/**
 * 逻辑层使用的网络上下文
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/3
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public interface NetContext {

    // --- 注册到网络模块时的信息 ---

    /**
     * 注册的本地guid
     */
    long localGuid();

    /**
     * 注册的本地角色
     */
    RoleType localRole();

    /**
     * 本地角色的运行环境，用于实现线程安全，
     * 可以保证{@link ProtocolDispatcher}运行在该{@link EventLoop}上。
     * (也就是说所有的网络事件处理最终都会运行在该EventLoop上)
     */
    EventLoop localEventLoop();

    /**
     * 该context绑定到的NetEventLoop。
     */
    NetEventLoop netEventLoop();

    /**
     * 从注册的NetEventLoop上取消注册，会关闭该context关联的所有会话。
     */
    ListenableFuture<?> deregister();

    // ----------------------------------- tcp/ws支持 ---------------------------------------


    /**
     * 在指定端口监听tcp连接
     *
     * @param host               地址
     * @param port               端口号
     * @param codec              网络协议解码器
     * @param lifecycleAware     生命周期监听器
     * @param protocolDispatcher 协议处理器
     * @param sessionSenderMode  session发送消息的方式
     * @return future
     */
    default ListenableFuture<HostAndPort> bindTcp(String host, int port,
                                                  @Nonnull ProtocolCodec codec,
                                                  @Nonnull SessionLifecycleAware lifecycleAware,
                                                  @Nonnull ProtocolDispatcher protocolDispatcher,
                                                  @Nonnull SessionSenderMode sessionSenderMode) {
        return this.bindTcpRange(host, new PortRange(port, port), codec, lifecycleAware, protocolDispatcher, sessionSenderMode);
    }

    /**
     * 在指定端口范围内选择一个合适的端口监听tcp连接
     *
     * @param host               地址
     * @param portRange          端口范围
     * @param codec              网络协议解码器
     * @param lifecycleAware     生命周期监听器
     * @param protocolDispatcher 协议处理器
     * @param sessionSenderMode  session发送消息的方式
     * @return future
     */
    ListenableFuture<HostAndPort> bindTcpRange(String host, PortRange portRange,
                                               @Nonnull ProtocolCodec codec,
                                               @Nonnull SessionLifecycleAware lifecycleAware,
                                               @Nonnull ProtocolDispatcher protocolDispatcher,
                                               @Nonnull SessionSenderMode sessionSenderMode);

    /**
     * 以tcp方式连接远程某个端口
     *
     * @param remoteGuid         远程角色guid
     * @param remoteRole         远程角色类型
     * @param remoteAddress      远程地址
     * @param codec              协议编解码器
     * @param lifecycleAware     生命周期监听器
     * @param protocolDispatcher 协议处理器
     * @param sessionSenderMode  session发送消息的方式
     * @return future，它并不是连接真正建立的future，而且连接操作是否被NetEventLoop响应的future
     */
    ListenableFuture<?> connectTcp(long remoteGuid, RoleType remoteRole, HostAndPort remoteAddress,
                                   @Nonnull ProtocolCodec codec,
                                   @Nonnull SessionLifecycleAware lifecycleAware,
                                   @Nonnull ProtocolDispatcher protocolDispatcher,
                                   @Nonnull SessionSenderMode sessionSenderMode);

    /**
     * 在指定端口监听WebSocket连接
     *
     * @param host               地址
     * @param port               端口
     * @param websocketUrl       触发websocket升级的地址
     * @param codec              网络协议解码器
     * @param lifecycleAware     生命周期监听器
     * @param protocolDispatcher 协议处理器
     * @param sessionSenderMode  session发送消息的方式
     * @return future
     */
    default ListenableFuture<HostAndPort> bindWS(String host, int port, String websocketUrl,
                                                 @Nonnull ProtocolCodec codec,
                                                 @Nonnull SessionLifecycleAware lifecycleAware,
                                                 @Nonnull ProtocolDispatcher protocolDispatcher,
                                                 @Nonnull SessionSenderMode sessionSenderMode) {
        return this.bindWSRange(host, new PortRange(port, port), websocketUrl,
                codec, lifecycleAware, protocolDispatcher, sessionSenderMode);
    }

    /**
     * 在指定端口范围内选择一个合适的端口监听WebSocket连接
     *
     * @param host               地址
     * @param portRange          端口范围
     * @param websocketUrl       触发websocket升级的地址
     * @param codec              网络协议解码器
     * @param lifecycleAware     生命周期监听器
     * @param protocolDispatcher 协议处理器
     * @param sessionSenderMode  session发送消息的方式
     * @return future
     */
    ListenableFuture<HostAndPort> bindWSRange(String host, PortRange portRange, String websocketUrl,
                                              @Nonnull ProtocolCodec codec,
                                              @Nonnull SessionLifecycleAware lifecycleAware,
                                              @Nonnull ProtocolDispatcher protocolDispatcher,
                                              @Nonnull SessionSenderMode sessionSenderMode);

    /**
     * 以websocket方式连接远程某个端口
     *
     * @param remoteGuid         远程角色guid
     * @param remoteRole         远程角色类型
     * @param remoteAddress      远程地址
     * @param codec              协议编解码器
     * @param lifecycleAware     生命周期监听器
     * @param protocolDispatcher 协议处理器
     * @param sessionSenderMode  session发送消息的方式
     * @return future，它并不是连接真正建立的future，而且连接操作是否被NetEventLoop响应的future
     */
    ListenableFuture<?> connectWS(long remoteGuid, RoleType remoteRole, HostAndPort remoteAddress, String websocketUrl,
                                  @Nonnull ProtocolCodec codec,
                                  @Nonnull SessionLifecycleAware lifecycleAware,
                                  @Nonnull ProtocolDispatcher protocolDispatcher,
                                  @Nonnull SessionSenderMode sessionSenderMode);


    //  --------------------------------------- http支持 -----------------------------------------

    /**
     * 监听某个端口
     *
     * @param host                  地址
     * @param port                  指定端口号
     * @param httpRequestDispatcher 该端口上的协议处理器
     * @return future 可以等待绑定完成。
     */
    default ListenableFuture<HostAndPort> bindHttp(String host, int port, @Nonnull HttpRequestDispatcher httpRequestDispatcher) {
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
    ListenableFuture<HostAndPort> bindHttpRange(String host, PortRange portRange, @Nonnull HttpRequestDispatcher httpRequestDispatcher);

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


    // -------------------------------------- 用于支持JVM内部通信 -------------------------------

    /**
     * 绑定一个JVM端口，用于其它线程建立会话
     *
     * @param lifecycleAware     生命周期监听器
     * @param protocolDispatcher 消息分发器
     * @param sessionSenderMode  消息的发送方式
     * @return future
     */
    ListenableFuture<JVMPort> bindInJVM(@Nonnull SessionLifecycleAware lifecycleAware,
                                        @Nonnull ProtocolDispatcher protocolDispatcher,
                                        @Nonnull SessionSenderMode sessionSenderMode);

    /**
     * 与JVM内的另一个线程建立session
     *
     * @param jvmPort            远程“端口”信息
     * @param lifecycleAware     生命周期监听器
     * @param protocolDispatcher 消息分发器
     * @param sessionSenderMode  消息的发送方式
     * @return future
     */
    ListenableFuture<?> connectInJVM(@Nonnull JVMPort jvmPort,
                                     @Nonnull SessionLifecycleAware lifecycleAware,
                                     @Nonnull ProtocolDispatcher protocolDispatcher,
                                     @Nonnull SessionSenderMode sessionSenderMode);
}
