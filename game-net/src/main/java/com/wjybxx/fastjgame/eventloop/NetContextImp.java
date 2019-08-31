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
import com.wjybxx.fastjgame.manager.NetManagerWrapper;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.LocalResponseChannel;
import com.wjybxx.fastjgame.misc.PortRange;
import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.net.initializer.*;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.net.BindException;
import java.util.Map;

/**
 * NetContext的基本实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/3
 * github - https://github.com/hl845740757
 */
@ThreadSafe
class NetContextImp implements NetContext {

	private static final Logger logger = LoggerFactory.getLogger(NetContextImp.class);

	private final long localGuid;
	private final RoleType localRole;
	private final EventLoop localEventLoop;
	private final NetEventLoopImp netEventLoop;
	private final NetManagerWrapper managerWrapper;

	NetContextImp(long localGuid, RoleType localRole, EventLoop localEventLoop,
						  NetEventLoopImp netEventLoop, NetManagerWrapper managerWrapper) {
		this.localGuid = localGuid;
		this.localRole = localRole;
		this.localEventLoop = localEventLoop;
		this.netEventLoop = netEventLoop;
		this.managerWrapper = managerWrapper;

		logger.info("User {}-{} create NetContext!", localRole, localGuid);
	}

	@Override
	public long localGuid() {
		return localGuid;
	}

	@Override
	public RoleType localRole() {
		return localRole;
	}

	@Override
	public EventLoop localEventLoop() {
		return localEventLoop;
	}

	@Override
	public NetEventLoop netEventLoop() {
		return netEventLoop;
	}

	@Override
	public TCPServerChannelInitializer newTcpServerInitializer(ProtocolCodec codec) {
		return new TCPServerChannelInitializer(localGuid, managerWrapper.getNetConfigManager().maxFrameLength(),
				codec, managerWrapper.getNetEventManager());
	}

	@Override
	public TCPClientChannelInitializer newTcpClientInitializer(long remoteGuid, ProtocolCodec codec) {
		return new TCPClientChannelInitializer(localGuid, remoteGuid, managerWrapper.getNetConfigManager().maxFrameLength(),
				codec, managerWrapper.getNetEventManager());
	}

	@Override
	public WsServerChannelInitializer newWsServerInitializer(String websocketUrl, ProtocolCodec codec) {
		return new WsServerChannelInitializer(localGuid, websocketUrl, managerWrapper.getNetConfigManager().maxFrameLength(),
				codec, managerWrapper.getNetEventManager());
	}

	@Override
	public WsClientChannelInitializer newWsClientInitializer(long remoteGuid, String websocketUrl, ProtocolCodec codec) {
		return new WsClientChannelInitializer(localGuid, remoteGuid, websocketUrl, managerWrapper.getNetConfigManager().maxFrameLength(),
				codec, managerWrapper.getNetEventManager());
	}

	@Override
	public ListenableFuture<?> deregister() {
		// 逻辑层调用
		return netEventLoop.deregisterContext(localGuid);
	}

	void afterRemoved() {
		// 尝试删除自己的痕迹
		managerWrapper.getS2CSessionManager().removeUserSession(localGuid, "deregister");
		managerWrapper.getC2SSessionManager().removeUserSession(localGuid, "deregister");
		managerWrapper.getHttpSessionManager().removeUserSession(localGuid);

		logger.info("User {}-{} NetContext removed!", localRole, localGuid);
	}

	@Override
	public ListenableFuture<HostAndPort> bindRange(String host, @Nonnull PortRange portRange,
												   @Nonnull ChannelInitializer<SocketChannel> initializer,
												   @Nonnull SessionLifecycleAware lifecycleAware,
												   @Nonnull ProtocolDispatcher protocolDispatcher,
												   @Nonnull SessionSenderMode sessionSenderMode) {
		// 这里一定不是网络层，只有逻辑层才会调用bind
		return netEventLoop.submit(() -> {
			try {
				return managerWrapper.getS2CSessionManager().bindRange(this, host, portRange,
						initializer, lifecycleAware, protocolDispatcher, sessionSenderMode);
			} catch (BindException e){
				ConcurrentUtils.rethrow(e);
				// unreachable
				return null;
			}
		});
	}

	@Override
	public ListenableFuture<?> connect(long remoteGuid, RoleType remoteRole,
									   @Nonnull HostAndPort remoteAddress,
									   @Nonnull ChannelInitializerSupplier initializerSupplier,
									   @Nonnull SessionLifecycleAware lifecycleAware,
									   @Nonnull ProtocolDispatcher protocolDispatcher,
									   @Nonnull SessionSenderMode sessionSenderMode) {
		// 这里一定不是网络层，只有逻辑层才会调用connect
		return netEventLoop.submit(() -> {
			managerWrapper.getC2SSessionManager().connect(this, remoteGuid, remoteRole, remoteAddress,
					initializerSupplier, lifecycleAware, protocolDispatcher, sessionSenderMode);
		});
	}

	// ------------------------------------------- http 实现 ----------------------------------------

	@Override
	public HttpServerInitializer newHttpServerInitializer() {
		return new HttpServerInitializer(localGuid, managerWrapper.getNetEventManager());
	}

	@Override
	public ListenableFuture<HostAndPort> bindRange(String host, PortRange portRange,
												   @Nonnull ChannelInitializer<SocketChannel> initializer,
												   @Nonnull HttpRequestDispatcher httpRequestDispatcher) {
		// 这里一定不是网络层，只有逻辑层才会调用bind
		return netEventLoop.submit(() -> {
			try {
				return managerWrapper.getHttpSessionManager().bindRange(this, host, portRange, initializer, httpRequestDispatcher);
			} catch (Exception e){
				ConcurrentUtils.rethrow(e);
				// unreachable
				return null;
			}
		});
	}

	@Override
	public Response syncGet(String url, @Nonnull Map<String, String> params) throws IOException {
		return managerWrapper.getHttpClientManager().syncGet(url, params);
	}

	@Override
	public void asyncGet(String url, @Nonnull Map<String, String> params, @Nonnull OkHttpCallback okHttpCallback) {
		managerWrapper.getHttpClientManager().asyncGet(url, params, localEventLoop, okHttpCallback);
	}

	@Override
	public Response syncPost(String url, @Nonnull Map<String, String> params) throws IOException {
		return managerWrapper.getHttpClientManager().syncPost(url, params);
	}

	@Override
	public void asyncPost(String url, @Nonnull Map<String, String> params, @Nonnull OkHttpCallback okHttpCallback) {
		managerWrapper.getHttpClientManager().asyncPost(url, params, localEventLoop, okHttpCallback);
	}

	// ----------------------------------------------- 本地调用支持 --------------------------------------------

	@Override
	public <T> LocalResponseChannel<T> newLocalResponseChannel() {
		return new LocalResponseChannel<>(netEventLoop, localEventLoop, managerWrapper.getNetConfigManager().rpcCallbackTimeoutMs());
	}

	@Override
	public <T> LocalResponseChannel<T> newLocalResponseChannel(long timeoutMs) {
		return new LocalResponseChannel<>(netEventLoop, localEventLoop, timeoutMs);
	}
}
