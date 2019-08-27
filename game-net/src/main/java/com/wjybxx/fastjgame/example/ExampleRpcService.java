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

package com.wjybxx.fastjgame.example;

import com.wjybxx.fastjgame.annotation.RpcMethod;
import com.wjybxx.fastjgame.annotation.RpcService;
import com.wjybxx.fastjgame.concurrent.*;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroup;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroupImp;
import com.wjybxx.fastjgame.misc.DefaultRpcCallDispatcher;
import com.wjybxx.fastjgame.misc.RpcCallDispatcher;
import com.wjybxx.fastjgame.net.NetContext;
import com.wjybxx.fastjgame.net.RpcResponseChannel;
import com.wjybxx.fastjgame.net.Session;
import com.wjybxx.fastjgame.net.SessionLifecycleAware;
import com.wjybxx.fastjgame.net.initializer.TCPServerChannelInitializer;
import com.wjybxx.fastjgame.utils.NetUtils;
import com.wjybxx.fastjgame.utils.TimeUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.LockSupport;

/**
 * 示例rpcService
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/26
 * github - https://github.com/hl845740757
 */
@RpcService(serviceId = 32700)
public class ExampleRpcService {

	@RpcMethod(methodId = 1)
	public String hello(String name) {
		return name;
	}

	@RpcMethod(methodId = 2)
	public int queryId(String name) {
		return name.hashCode();
	}

	@RpcMethod(methodId = 3)
	public int inc(final int number) {
		return number + 1;
	}

	/**
	 *
	 * @param number 待加的数
	 * @param session 会话信息。
	 *                该参数不会出现在客户端的代理中，Session参数可以出现在任意位置，注解处理器会处理，不要求在特定位置
	 * @return 增加后的值
	 */
	@RpcMethod(methodId = 4)
	public int incWithSession(final int number, Session session) {
		return number + 2;
	}

	/**
	 *
	 * @param number 待加的数
	 * @param responseChannel 返回结果的通道，表示该方法可能不能立即返回结果，需要持有channel以便在未来返回结果。
	 *                        该参数不会出现在客户端的代理中，Channel参数可以出现在任意位置，注解处理器会处理，不要求在特定位置
	 */
	@RpcMethod(methodId = 5)
	public void incWithChannel(final int number, RpcResponseChannel<Integer> responseChannel) {
		responseChannel.writeSuccess(number + 3);
	}

	@RpcMethod(methodId = 6)
	public void incWithSessionAndChannel(Session session, final int number, RpcResponseChannel<Integer> responseChannel) {
		responseChannel.writeSuccess(number + 4);
	}

	private static class ServiceLoop extends SingleThreadEventLoop {

		private final NetEventLoopGroup netGroup = new NetEventLoopGroupImp(1, new DefaultThreadFactory("NET-EVENT-LOOP"),
				RejectedExecutionHandlers.log());
		private final RpcCallDispatcher dispatcher;

		private NetContext netContext;

		public ServiceLoop(@Nullable EventLoopGroup parent,
						   @Nonnull ThreadFactory threadFactory,
						   @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
						   @Nonnull RpcCallDispatcher dispatcher) {
			super(parent, threadFactory, rejectedExecutionHandler);
			this.dispatcher = dispatcher;
		}

		@Override
		protected void init() throws Exception {
			super.init();
			// 创建网络环境
			netContext = netGroup.createContext(ExampleConstants.serverGuid, ExampleConstants.serverRole, this).get();

			// 监听tcp端口
			TCPServerChannelInitializer initializer = netContext.newTcpServerInitializer(ExampleConstants.reflectBasedCodec);
			netContext.bind(NetUtils.getLocalIp(), ExampleConstants.tcpPort, initializer, new ClientLifeAware(),
					new ExampleRpcDispatcher(dispatcher));
		}

		@Override
		protected void loop() {
			final long starrTime = System.currentTimeMillis();
			for (;;) {
				// 执行所有任务
				runAllTasks();
				// 循环x分钟
				if (System.currentTimeMillis() - starrTime > TimeUtils.MIN * 3) {
					break;
				}
				// 确认是否退出
				if (confirmShutdown()) {
					break;
				}
				// 睡10毫秒
				LockSupport.parkNanos(TimeUtils.NANO_PER_MILLISECOND * 10);
			}
		}

		@Override
		protected void clean() throws Exception {
			super.clean();
			if (null != netContext) {
				netContext.deregister();
			}
			netGroup.shutdown();
		}
	}

	private static class ClientLifeAware implements SessionLifecycleAware {

		@Override
		public void onSessionConnected(Session session) {
			System.out.println("-----------------onSessionConnected----------------------");
		}

		@Override
		public void onSessionDisconnected(Session session) {
			System.out.println("----------------onSessionDisconnected---------------------");
		}
	}

	public static void main(String[] args) {
		final DefaultRpcCallDispatcher dispatcher = new DefaultRpcCallDispatcher();
		ExampleRpcServiceRpcRegister.register(dispatcher, new ExampleRpcService());
		final ServiceLoop serviceLoop = new ServiceLoop(null, new DefaultThreadFactory("SERVICE"),
				RejectedExecutionHandlers.log(), dispatcher);
		// 唤醒线程
		serviceLoop.execute(() -> {});
	}
}
