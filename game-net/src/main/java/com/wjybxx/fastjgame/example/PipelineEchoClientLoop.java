package com.wjybxx.fastjgame.example;

import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandler;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.concurrent.SingleThreadEventLoop;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroup;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroupImp;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.net.NetContext;
import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.net.initializer.TCPClientChannelInitializer;
import com.wjybxx.fastjgame.utils.JsonUtils;
import com.wjybxx.fastjgame.utils.NetUtils;
import com.wjybxx.fastjgame.utils.TimeUtils;
import okhttp3.Call;
import okhttp3.Response;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.LockSupport;

/**
 * 基于pipeline的客户端循环
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/16
 * github - https://github.com/hl845740757
 */
public class PipelineEchoClientLoop extends SingleThreadEventLoop {

	private final NetEventLoopGroup netGroup = new NetEventLoopGroupImp(1, new DefaultThreadFactory("NET-EVENT-LOOP"),
			RejectedExecutionHandlers.log());

	private NetContext netContext;

	/** 是否已建立tcp连接？ */
	private SessionPipeline pipeline;

	public PipelineEchoClientLoop(@Nonnull ThreadFactory threadFactory, @Nonnull RejectedExecutionHandler rejectedExecutionHandler) {
		super(null, threadFactory, rejectedExecutionHandler);
	}

	@Override
	protected void init() throws Exception {
		super.init();
		netContext = netGroup.createContext(ExampleConstants.pipelineClientGuid, ExampleConstants.pipelineClientRole, this).get();
		// 必须先启动服务器
		final TCPClientChannelInitializer initializer = netContext.newTcpClientInitializer(ExampleConstants.serverGuid,
				ExampleConstants.jsonBasedCodec);
		final HostAndPort address = new HostAndPort(NetUtils.getLocalIp(), ExampleConstants.tcpPort);
		netContext.connect(ExampleConstants.serverGuid, ExampleConstants.serverRole, address, () -> initializer,
				new ServerLifeAward(), new EchoProtocolDispatcher());
	}

	public void loop() {
		for (int index=0; index < 1000; index ++) {
			System.out.println("\n ------------------------------------" + index + "------------------------------------------");

			// 执行所有任务
			runAllTasks();

			if (pipeline != null) {
				trySendMessage(index);
			}

			if (confirmShutdown()) {
				break;
			}
			// X秒一个循环
			LockSupport.parkNanos(TimeUtils.NANO_PER_MILLISECOND * TimeUtils.SEC);
		}
	}

	private void trySendMessage(final int index) {
		// 发送单向消息
		{
			ExampleMessages.Hello hello = new ExampleMessages.Hello();
			hello.setId(index);
			hello.setMessage("SentOneWayMessage - " + System.currentTimeMillis());
			pipeline.enqueueMessage(hello);
		}
		// 发送rpc请求
		{
			ExampleMessages.Hello hello = new ExampleMessages.Hello();
			hello.setId(index);
			hello.setMessage("asyncRpcRequest - " + System.currentTimeMillis());
			pipeline.enqueueRpc(hello).addCallback(rpcResponse -> {
				System.out.println("\nasyncRpcResponse - " + JsonUtils.toJson(rpcResponse));
			});
		}
		// 发送rpc请求
		{
			ExampleMessages.Hello hello = new ExampleMessages.Hello();
			hello.setId(index);
			hello.setMessage("asyncRpcRequest without future - " + System.currentTimeMillis());
			pipeline.enqueueRpc(hello, rpcResponse -> {
				System.out.println("\nasyncRpcResponse without future - " + JsonUtils.toJson(rpcResponse));
			});
		}
		// 异步rpc请求，使用阻塞方式到完成 --- 一定会失败
		{
			try {
				ExampleMessages.Hello hello = new ExampleMessages.Hello();
				hello.setId(index);
				hello.setMessage("asyncRpcRequest with block - " + System.currentTimeMillis());
				RpcResponse rpcResponse = pipeline.enqueueRpc(hello).get();
				System.out.println("\nasyncRpcRequest with block response - " + JsonUtils.toJson(rpcResponse));
			} catch (Exception e){
				e.printStackTrace();
			}
		}
		// 发送同步rpc请求
		{
			ExampleMessages.Hello hello = new ExampleMessages.Hello();
			hello.setId(index);
			hello.setMessage("syncRpcRequest - " + System.currentTimeMillis());
			RpcResponse rpcResponse = pipeline.syncRpcUninterruptibly(hello);
			System.out.println("\nsyncRpcResponse - " + JsonUtils.toJson(rpcResponse));
		}
		// 清空缓冲区
		pipeline.flush();

		// 发起http请求
		String url = NetUtils.getLocalIp() + ":" + ExampleConstants.httpPort;
		// 异步get
		{
			HashMap<String, String> params = new HashMap<>();
			params.put("asyncGetIndex", String.valueOf(index));
			netContext.asyncGet(url, params, new OkHttpCallback() {
				@Override
				public void onFailure(@Nonnull Call call, @Nonnull IOException cause) {
					System.out.println("asycnGet  failure.");
				}

				@Override
				public void onResponse(@Nonnull Call call, @Nonnull Response response) throws IOException {
					System.out.println("\nasyncGet  response - " + response.body().string());
				}
			});
		}
		// 同步get
		{
			HashMap<String, String> params = new HashMap<>();
			params.put("syncGetIndex", String.valueOf(index));
			try {
				Response response = netContext.syncGet(url, params);
				System.out.println("\nsyncGet response - " + response.body().string());
			} catch (IOException e) {
				System.out.println("syncGet caught exception.");
				e.printStackTrace();
			}
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

	private class ServerLifeAward implements SessionLifecycleAware {

		@Override
		public void onSessionConnected(Session session) {
			System.out.println(" ============ onSessionConnected ===============");
			PipelineEchoClientLoop.this.pipeline = session.pipeline();
		}

		@Override
		public void onSessionDisconnected(Session session) {
			System.out.println(" =========== onSessionDisconnected ==============");
			PipelineEchoClientLoop.this.pipeline = null;
			// 断开连接后关闭
			shutdown();
		}
	}

	private class EchoProtocolDispatcher implements ProtocolDispatcher {

		@Override
		public void dispatchOneWayMessage(Session session, @Nullable Object message) throws Exception {
			System.out.println("\nonMessage - " + JsonUtils.toJson(message));
		}

		@Override
		public void dispatchRpcRequest(Session session, @Nullable Object request, RpcRequestContext context) throws Exception {
			// unreachable
		}
	}

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		PipelineEchoClientLoop pipelineEchoClientLoop = new PipelineEchoClientLoop(new DefaultThreadFactory("CLIENT"),
				RejectedExecutionHandlers.log());

		// 唤醒线程
		pipelineEchoClientLoop.execute(() -> {});
	}
}
