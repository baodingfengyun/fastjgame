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

import com.wjybxx.fastjgame.concurrent.*;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroup;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroupImp;
import com.wjybxx.fastjgame.misc.DefaultRpcCallDispatcher;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.RpcBuilder;
import com.wjybxx.fastjgame.net.NetContext;
import com.wjybxx.fastjgame.net.SessionSenderMode;
import com.wjybxx.fastjgame.net.Session;
import com.wjybxx.fastjgame.net.SessionLifecycleAware;
import com.wjybxx.fastjgame.net.initializer.TCPClientChannelInitializer;
import com.wjybxx.fastjgame.utils.NetUtils;
import com.wjybxx.fastjgame.utils.TimeUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.IntStream;

/**
 * rpc请求客户端示例
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/26
 */
public class ExampleRpcClientLoop extends SingleThreadEventLoop {

	private final NetEventLoopGroup netGroup = new NetEventLoopGroupImp(1, new DefaultThreadFactory("NET-EVENT-LOOP"),
			RejectedExecutionHandlers.log());

	private NetContext netContext;
	/** 是否已建立tcp连接 */
	private Session session;

	public ExampleRpcClientLoop(@Nullable EventLoopGroup parent,
								@Nonnull ThreadFactory threadFactory,
								@Nonnull RejectedExecutionHandler rejectedExecutionHandler) {
		super(parent, threadFactory, rejectedExecutionHandler);
	}

	@Override
	protected void init() throws Exception {
		super.init();
		netContext = netGroup.createContext(ExampleConstants.clientGuid, ExampleConstants.clientRole, this).get();
		// 必须先启动服务器
		final TCPClientChannelInitializer initializer = netContext.newTcpClientInitializer(ExampleConstants.serverGuid,
				ExampleConstants.reflectBasedCodec);
		// 请求建立连接
		final HostAndPort address = new HostAndPort(NetUtils.getLocalIp(), ExampleConstants.tcpPort);
		netContext.connect(ExampleConstants.serverGuid,
				ExampleConstants.serverRole, address,
				() -> initializer,
				new ServerLifeAward(),
				new ExampleRpcDispatcher(new DefaultRpcCallDispatcher()), SessionSenderMode.DIRECT);
	}

	@Override
	protected void loop() {
		for (int index=0; index < 1000; index ++) {
			System.out.println("\n ------------------------------------" + index + "------------------------------------------");

			// 执行所有任务
			runAllTasks();

			if (session != null) {
				sendRequest(index);
			}

			if (confirmShutdown()) {
				break;
			}
			// X秒一个循环
			LockSupport.parkNanos(TimeUtils.NANO_PER_MILLISECOND * TimeUtils.SEC * 2);
		}
	}

	private void sendRequest(final int index) {
		// 方法无返回值，但是还是可以监听，只要调用的是call, sync, syncCall都可以获知调用结果
		ExampleRpcServiceRpcProxy.hello("wjybxx- " + index)
				.ifSuccess(result -> System.out.println("hello - " + index + " - " + result))
				.call(session);

		ExampleRpcServiceRpcProxy.queryId("wjybxx-" + index)
				.ifSuccess(result -> System.out.println("queryId - " + index + " - " + result))
				.call(session);

		ExampleRpcServiceRpcProxy.inc(index)
				.ifSuccess(result -> System.out.println("inc - " + index + " - " + result))
				.call(session);

		ExampleRpcServiceRpcProxy.incWithSession(index)
				.ifSuccess(result -> System.out.println("incWithSession - " + index + " - " + result))
				.call(session);

		ExampleRpcServiceRpcProxy.incWithChannel(index)
				.ifSuccess(result -> System.out.println("incWithChannel - " + index + " - " + result))
				.call(session);

		ExampleRpcServiceRpcProxy.incWithSessionAndChannel(index)
				.ifSuccess(result -> System.out.println("incWithSessionAndChannel - " + index + " - " + result))
				.call(session);

		// --- 如果关闭netEventLoop的帧间隔控制，一次同步调用耗时应该在10ms左右
		// 在保持现有机制的情况下，只能说最差情况下需要等待4次，最好的情况是完全不阻塞， 10ms是最快极限，90ms是最差极限。 50-60应该更多
		final long start = System.currentTimeMillis();
		final String callResult = ExampleRpcServiceRpcProxy.combine("wjybxx", String.valueOf(index)).syncCall(session);
		System.out.println("SyncCall - " + index + " - " + callResult + " , cost timeMs " + (System.currentTimeMillis() - start));

		// 模拟广播X次
		final RpcBuilder<?> builder = ExampleRpcServiceRpcProxy.notifySuccess(index);
		IntStream.rangeClosed(1, 3).forEach(i -> builder.send(session));
	}

	@Override
	protected void clean() throws Exception {
		super.clean();
		if (null != netContext) {
			netContext.deregister();
		}
		netGroup.shutdown();
	}

	private class ServerLifeAward implements SessionLifecycleAware {

		@Override
		public void onSessionConnected(Session session) {
			System.out.println(" ============ onSessionConnected ===============");
			ExampleRpcClientLoop.this.session = session;
		}

		@Override
		public void onSessionDisconnected(Session session) {
			System.out.println(" =========== onSessionDisconnected ==============");
			ExampleRpcClientLoop.this.session = null;
			// 断开连接后关闭
			shutdown();
		}
	}

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		ExampleRpcClientLoop echoClientLoop = new ExampleRpcClientLoop(null,
				new DefaultThreadFactory("CLIENT"),
				RejectedExecutionHandlers.log());

		// 唤醒线程
		echoClientLoop.execute(() -> {});
	}
}
