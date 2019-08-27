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

package com.wjybxx.fastjgame.manager;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.eventloop.NetEventLoopManager;
import com.wjybxx.fastjgame.misc.*;
import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.timer.FixedDelayHandle;
import com.wjybxx.fastjgame.utils.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import javax.annotation.concurrent.NotThreadSafe;
import java.net.BindException;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * HttpSession管理器。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/1
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class HttpSessionManager {

	private NetManagerWrapper managerWrapper;
	private final NetEventLoopManager netEventLoopManager;
	private final NetConfigManager netConfigManager;
	private final NetTimeManager netTimeManager;
	private final AcceptorManager acceptorManager;

	/**
	 * 所有使用HttpSession的用户信息
	 */
	private final Long2ObjectMap<UserInfo> userInfoMap = new Long2ObjectOpenHashMap<>();

	@Inject
	public HttpSessionManager(NetTimerManager netTimerManager, NetEventLoopManager netEventLoopManager, NetConfigManager netConfigManager,
							  NetTimeManager netTimeManager, AcceptorManager acceptorManager) {
		this.netEventLoopManager = netEventLoopManager;
		this.netConfigManager = netConfigManager;
		this.netTimeManager = netTimeManager;
		this.acceptorManager = acceptorManager;

		netTimerManager.newFixedDelay(this.netConfigManager.httpSessionTimeout() * TimeUtils.SEC, this::checkSessionTimeout);
	}

	/** 解决循环依赖 */
	public void setManagerWrapper(NetManagerWrapper managerWrapper) {
		this.managerWrapper = managerWrapper;
	}

	/**
	 * @see AcceptorManager#bindRange(String, PortRange, ChannelInitializer)
	 */
	public HostAndPort bindRange(NetContext netContext, String host, PortRange portRange,
								 ChannelInitializer<SocketChannel> initializer,
								 HttpRequestDispatcher httpRequestDispatcher) throws BindException {
		assert netEventLoopManager.inEventLoop();
		// 绑定端口
		BindResult bindResult = acceptorManager.bindRange(host, portRange, initializer);
		// 保存用户信息
		userInfoMap.computeIfAbsent(netContext.localGuid(), localGuid -> new UserInfo(netContext, bindResult, initializer, httpRequestDispatcher));

		return bindResult.getHostAndPort();
	}

	/**
	 * 当接收到用户所在eventLoop终止时
	 * @param eventLoop 用户所在的eventLoop
	 */
	public void onUserEventLoopTerminal(EventLoop eventLoop) {
		assert netEventLoopManager.inEventLoop();
		FastCollectionsUtils.removeIfAndThen(userInfoMap,
				(long k, UserInfo userInfo) -> userInfo.netContext.localEventLoop() == eventLoop,
				(long k, UserInfo userInfo) -> removeUserSession(userInfo));
	}

	/**
	 * 删除某个会话的所有session
	 * @param localGuid 用户id
	 */
	public void removeUserSession(long localGuid) {
		UserInfo userInfo = userInfoMap.remove(localGuid);
		if (null == userInfo){
			return;
		}
		removeUserSession(userInfo);
	}

	private void removeUserSession(UserInfo userInfo) {
		// 如果 用户 持有了httpSession的引用，长时间没有完成响应的话，这里关闭可能导致一些错误
		CollectionUtils.removeIfAndThen(userInfo.sessionWrapperMap, FunctionUtils::TRUE, this::afterRemoved);
		// 绑定的端口需要释放
		NetUtils.closeQuietly(userInfo.bindResult.getChannel());
	}

	/**
	 * 检查session超时
	 */
	private void checkSessionTimeout(FixedDelayHandle handle){
		for (UserInfo userInfo : userInfoMap.values()) {
			// 如果用户持有了httpSession的引用，长时间没有完成响应的话，这里关闭可能导致一些错误
			CollectionUtils.removeIfAndThen(userInfo.sessionWrapperMap,
					(channel, sessionWrapper) -> netTimeManager.getSystemSecTime() >= sessionWrapper.getSessionTimeout(),
					this::afterRemoved);
		}
	}

	/**
	 * 当收到http请求时
	 * @param requestEventParam 请参数
	 */
	public void onRcvHttpRequest(HttpRequestEventParam requestEventParam){
		final Channel channel = requestEventParam.channel();
		final UserInfo userInfo = userInfoMap.get(requestEventParam.localGuid());
		if (userInfo == null) {
			// 请求的逻辑用户不存在
			channel.writeAndFlush(HttpResponseHelper.newNotFoundResponse(), channel.voidPromise());
			return;
		}
		// 保存session
		SessionWrapper sessionWrapper = userInfo.sessionWrapperMap.computeIfAbsent(channel,
				k -> new SessionWrapper(new HttpSessionImp(userInfo.netContext, userInfo.bindResult.getHostAndPort(), this, channel)));

		// 保持一段时间的活性
		sessionWrapper.setSessionTimeout(netConfigManager.httpSessionTimeout() + netTimeManager.getSystemSecTime());

		final HttpSessionImp httpSession = sessionWrapper.session;
		final String path = requestEventParam.getPath();
		final HttpRequestParam param = requestEventParam.getParams();

		// 处理请求，提交到用户所在的线程，实现线程安全
		ConcurrentUtils.tryCommit(userInfo.netContext.localEventLoop(), () -> {
			try {
				userInfo.httpRequestDispatcher.post(httpSession, path, param);
			} catch (Exception e) {
				ConcurrentUtils.rethrow(e);
			}
		});
	}

	/**
	 * 关闭session。
	 */
	public void removeSession(HttpSessionImp httpSession, Channel channel) {
		UserInfo userInfo = userInfoMap.get(httpSession.localGuid());
		if (null == userInfo) {
			return;
		}
		SessionWrapper sessionWrapper = userInfo.sessionWrapperMap.remove(channel);
		if (null == sessionWrapper) {
			return;
		}
		afterRemoved(channel, sessionWrapper);
	}

	private void afterRemoved(Channel channel, SessionWrapper sessionWrapper) {
		sessionWrapper.session.setClosed();
		NetUtils.closeQuietly(channel);
	}

	/**
	 * http客户端使用者信息
	 */
	private static class UserInfo {
		/** 用户关联的上下文 */
		private final NetContext netContext;
		/** 绑定的端口信息等，关联的channel需要再用户取消注册后关闭 */
		private final BindResult bindResult;
		/** 端口初始化类 */
		private final ChannelInitializer<SocketChannel>  initializer;
		/** http请求处理器 */
		private final HttpRequestDispatcher httpRequestDispatcher;

		/** 该用户关联的所有的会话 */
		private final Map<Channel, SessionWrapper> sessionWrapperMap = new IdentityHashMap<>();

		private UserInfo(NetContext netContext, BindResult bindResult,
						 ChannelInitializer<SocketChannel> initializer,
						 HttpRequestDispatcher httpRequestDispatcher) {
			this.netContext = netContext;
			this.bindResult = bindResult;
			this.initializer = initializer;
			this.httpRequestDispatcher = httpRequestDispatcher;
		}
	}

	private static class SessionWrapper {

		private final HttpSessionImp session;
		/**
		 * 会话超时时间 - 避免对外，线程安全问题
		 */
		private int sessionTimeout;

		private SessionWrapper(HttpSessionImp session) {
			this.session = session;
		}

		HttpSessionImp getSession() {
			return session;
		}

		int getSessionTimeout() {
			return sessionTimeout;
		}

		void setSessionTimeout(int sessionTimeout) {
			this.sessionTimeout = sessionTimeout;
		}
	}
}
