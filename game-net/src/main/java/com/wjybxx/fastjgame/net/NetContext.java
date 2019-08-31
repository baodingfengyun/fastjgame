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
import com.wjybxx.fastjgame.net.initializer.*;
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
	 * （可实现多个NetContext绑定到相同的NetEventLoop，可消除不必要的同步）
	 */
	NetEventLoop netEventLoop();

	/**
	 * 从注册的NetEventLoop上取消注册，会关闭该context关联的所有会话。
	 */
	ListenableFuture<?> deregister();

	// ----------------------------------- tcp/ws支持 ---------------------------------------

	/**
	 * 监听某个端口
	 * @param host 地址
	 * @param port 指定端口号
	 * @param initializer 如何初始化channel
	 * @param lifecycleAware 生命周期监听器
	 * @param protocolDispatcher 协议处理器
	 * @param sessionSenderMode session发送消息的方式
	 * @return future 可以等待绑定完成。
	 */
	default ListenableFuture<HostAndPort> bind(String host, int port,
											   @Nonnull ChannelInitializer<SocketChannel> initializer,
											   @Nonnull SessionLifecycleAware lifecycleAware,
											   @Nonnull ProtocolDispatcher protocolDispatcher,
											   @Nonnull SessionSenderMode sessionSenderMode) {
		return this.bindRange(host, new PortRange(port, port), initializer, lifecycleAware, protocolDispatcher, sessionSenderMode);
	}

	/**
	 * 监听某个端口
	 * @param host 地址
	 * @param portRange 端口范围
	 * @param initializer 如何初始化channel
	 * @param lifecycleAware 生命周期监听器
	 * @param protocolDispatcher 协议处理器
	 * @param sessionSenderMode session发送消息的方式
	 * @return future 可以等待绑定完成。
	 */
	ListenableFuture<HostAndPort> bindRange(String host, @Nonnull PortRange portRange,
											@Nonnull ChannelInitializer<SocketChannel> initializer,
											@Nonnull SessionLifecycleAware lifecycleAware,
											@Nonnull ProtocolDispatcher protocolDispatcher,
											@Nonnull SessionSenderMode sessionSenderMode);

	/**
	 * 连接远程某个端口
	 * @param remoteGuid 远程角色guid
	 * @param remoteRole 远程角色类型
	 * @param remoteAddress 远程地址
	 * @param initializerSupplier 如何初始化channel，supplier是因为断线重连可能需要新的initializer。
	 * @param lifecycleAware 生命周期监听器
	 * @param protocolDispatcher 协议处理器
	 * @param sessionSenderMode session发送消息的方式
	 * @return future，它并不是连接真正建立的future，而且连接操作是否被NetEventLoop响应的future
	 */
	ListenableFuture<?> connect(long remoteGuid, RoleType remoteRole,
								@Nonnull HostAndPort remoteAddress,
								@Nonnull ChannelInitializerSupplier initializerSupplier,
								@Nonnull SessionLifecycleAware lifecycleAware,
								@Nonnull ProtocolDispatcher protocolDispatcher,
								@Nonnull SessionSenderMode sessionSenderMode);

	/**
	 * 工厂方法，创建一个用于tcp监听的Initializer.
	 *
	 * @param codec 消息包编解码帮助类
	 * @return 用于初始化http端口
	 */
	TCPServerChannelInitializer newTcpServerInitializer(ProtocolCodec codec);

	/**
	 * 工厂方法，创建一个用于进行tcp连接的initializer
	 *
	 * @param remoteGuid 远程用户id
	 * @param codec 消息包编解码帮助类
	 * @return 用于初始化http端口
	 */
	TCPClientChannelInitializer newTcpClientInitializer(long remoteGuid, ProtocolCodec codec);

	/**
	 * 工厂方法，创建一个用于websokect监听的Initializer.
	 *
	 * @param websocketUrl 触发websocket升级的地址
	 * @param codec 消息包编解码帮助类
	 * @return 用于初始化websocket端口
	 */
	WsServerChannelInitializer newWsServerInitializer(String websocketUrl, ProtocolCodec codec);
	/**
	 * 工厂方法，创建一个用于websocket连接的initializer
	 *
	 * @param remoteGuid  远程地址
	 * @param websocketUrl 触发websocket升级的地址
	 * @param codec 消息包编解码帮助类
	 * @return 用于初始化websocket端口
	 */

	WsClientChannelInitializer newWsClientInitializer(long remoteGuid, String websocketUrl, ProtocolCodec codec);

	//  --------------------------------------- http支持 -----------------------------------------

	/**
	 * 监听某个端口
	 * @param host 地址
	 * @param port 指定端口号
	 * @param initializer 如何初始化channel
	 * @param httpRequestDispatcher http请求处理器
	 * @return future 可以等待绑定完成。
	 */
	default ListenableFuture<HostAndPort> bind(String host, int port,
											   @Nonnull ChannelInitializer<SocketChannel> initializer,
											   @Nonnull HttpRequestDispatcher httpRequestDispatcher) {
		return this.bindRange(host, new PortRange(port, port), initializer, httpRequestDispatcher);
	}

	/**
	 * 在指定端口范围内监听某一个端口。
	 *
	 * @param host 地址
	 * @param portRange 端口范围
	 * @param initializer 如何初始化channel
	 * @param httpRequestDispatcher http请求处理器
	 * @return future 可以等待绑定完成。
	 */
	ListenableFuture<HostAndPort> bindRange(String host, PortRange portRange,
											@Nonnull ChannelInitializer<SocketChannel> initializer,
											@Nonnull HttpRequestDispatcher httpRequestDispatcher);

	/**
	 * 同步get请求
	 * @param url url
	 * @param params get参数
	 */
	Response syncGet(String url, @Nonnull Map<String,String> params) throws IOException;
	/**
	 * 异步get请求
	 * @param url url
	 * @param params get参数
	 * @param okHttpCallback 回调
	 */
	void asyncGet(String url, @Nonnull Map<String,String> params, @Nonnull OkHttpCallback okHttpCallback);

	/**
	 * 同步post请求
	 * @param url url
	 * @param params post参数
	 */
	Response syncPost(String url, @Nonnull Map<String,String> params) throws IOException;

	/**
	 * 异步post请求
	 * @param url url
	 * @param params post参数
	 * @param okHttpCallback 回调
	 */
	void asyncPost(String url, @Nonnull Map<String,String> params, @Nonnull OkHttpCallback okHttpCallback);

	/**
	 * 工厂方法，创建一个用于http监听的Initializer.
	 *
	 * @return 用于初始化http端口
	 */
	HttpServerInitializer newHttpServerInitializer();

}
