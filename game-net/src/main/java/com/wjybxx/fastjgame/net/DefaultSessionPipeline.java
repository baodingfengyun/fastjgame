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

package com.wjybxx.fastjgame.net;

import com.wjybxx.fastjgame.annotation.UnstableApi;
import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.EventLoopUtils;

import javax.annotation.Nonnull;
import java.util.LinkedList;

/**
 * 默认的管道实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/11
 * github - https://github.com/hl845740757
 */
@UnstableApi
public class DefaultSessionPipeline implements SessionPipeline{

	private final AbstractSession session;

	/** 不是线程安全的，只由用户线程访问 */
	private LinkedList<PipelineTask> buffer = new LinkedList<>();

	public DefaultSessionPipeline(AbstractSession session) {
		this.session = session;
	}

	@Override
	public Session session() {
		return session;
	}

	private EventLoop userEventLoop(){
		return netContext().localEventLoop();
	}

	private void assertInUserEventLoop() {
		assert userEventLoop().inEventLoop();
	}

	@Override
	public void enqueueMessage(@Nonnull Object message) {
		assertInUserEventLoop();
		if (!session.isActive()) {
			return;
		}
		addTask(new OneWayMessageTask(session, message));
	}

	@Override
	public void enqueueRpc(@Nonnull Object request, RpcCallback rpcCallback) {
		enqueueRpc(request, rpcCallback, session.getNetConfigManager().rpcCallbackTimeoutMs());
	}

	@Override
	public void enqueueRpc(@Nonnull Object request, RpcCallback rpcCallback, long timeoutMs) {
		assertInUserEventLoop();

		if (!session.isActive()) {
			rpcCallback.onComplete(RpcResponse.SESSION_CLOSED);
			return;
		}
		// 执行或提交
		RpcRequestTask requestTask = new RpcRequestTask(session, request, timeoutMs, null, rpcCallback);
		addTask(requestTask);
	}

	@Nonnull
	@Override
	public PipelineRpcFuture enqueueRpc(@Nonnull Object request) {
		return enqueueRpc(request, session.getNetConfigManager().rpcCallbackTimeoutMs());
	}

	@Nonnull
	@Override
	public PipelineRpcFuture enqueueRpc(@Nonnull Object request, long timeoutMs) {
		assertInUserEventLoop();

		if (!session.isActive()) {
			// session已关闭，立即结束
			return session.netContext().netEventLoop().newCompletedPipelineRpcFuture(userEventLoop(), RpcResponse.SESSION_CLOSED);
		}
		// 需要特殊的RpcPromise
		final PipelineRpcPromise rpcResponsePromise = session.netContext().netEventLoop().newPipelineRpcPromise(userEventLoop(), timeoutMs);

		RpcRequestTask rpcRequestTask = new RpcRequestTask(session, request, timeoutMs, rpcResponsePromise, null);
		addTask(rpcRequestTask);

		return rpcResponsePromise;
	}

	@Nonnull
	@Override
	public <T> RpcResponseChannel<T> newResponseChannel(@Nonnull RpcRequestContext context) {
		assertInUserEventLoop();

		DefaultRpcRequestContext realContext = (DefaultRpcRequestContext) context;
		if (realContext.sync) {
			return session.newResponseChannel(realContext);
		} else {
			return new PipelineResponseChannel<>(this, realContext);
		}
	}

	/**
	 * 添加一个任务
	 * @param task 添加的任务
	 */
	private void addTask(PipelineTask task) {
		buffer.add(task);
		checkFlush();
	}

	/**
	 * 检查清空缓冲区
	 */
	private void checkFlush() {
		if (size() >= session.getNetConfigManager().flushThreshold()) {
			flush();
		}
	}

	@Override
	public int size() {
		assertInUserEventLoop();
		return buffer.size();
	}

	@Override
	public void flush() {
		assertInUserEventLoop();

		// 没有缓存
		if (size() == 0) {
			return;
		}

		if (!session.isActive()) {
			// session已关闭，取消所有任务
			cancelTasks();
		} else {
			flushBuffer();
		}
	}

	/**
	 * 清空缓冲区
	 */
	private void flushBuffer() {
		// 此时在用户线程
		final LinkedList<PipelineTask> oldBuffer = exchangeBuffer();
		session.netContext().netEventLoop().execute(()-> {
			// 此时在网络线程
			for (PipelineTask pipelineTask :oldBuffer) {
				pipelineTask.send();
			}
		});
	}

	/**
	 * 取消所有的任务
	 */
	private void cancelTasks() {
		// 此时在用户线程
		final LinkedList<PipelineTask> oldBuffer = exchangeBuffer();
		for (PipelineTask pipelineTask :oldBuffer) {
			ConcurrentUtils.safeExecute((Runnable) pipelineTask::cancel);
		}
	}

	/**
	 * 交换缓冲区
	 * @return oldBuffer
	 */
	private LinkedList<PipelineTask> exchangeBuffer() {
		LinkedList<PipelineTask> result = this.buffer;
		this.buffer = new LinkedList<>();
		return result;
	}

	@Override
	public ListenableFuture<?> close(boolean flush) {
		assertInUserEventLoop();
		// 又需要发送的消息则发送。
		if (flush && size() > 0) {
			flush();
		}
		return session.close();
	}

	// -------------------------- 代理Session中的方法，使得Pipeline的使用者可以脱离session -------------------------

	@Override
	public NetContext netContext() {
		return session.netContext();
	}

	@Override
	public long localGuid() {
		return session.localGuid();
	}

	@Override
	public RoleType localRole() {
		return session.localRole();
	}

	@Override
	public long remoteGuid() {
		return session.remoteGuid();
	}

	@Override
	public RoleType remoteRole() {
		return session.remoteRole();
	}

	@Override
	@Nonnull
	public RpcResponse syncRpc(@Nonnull Object request) throws InterruptedException {
		return session.syncRpc(request);
	}

	@Override
	@Nonnull
	public RpcResponse syncRpc(@Nonnull Object request, long timeoutMs) throws InterruptedException {
		return session.syncRpc(request, timeoutMs);
	}

	@Override
	@Nonnull
	public RpcResponse syncRpcUninterruptibly(@Nonnull Object request) {
		return session.syncRpcUninterruptibly(request);
	}

	@Override
	@Nonnull
	public RpcResponse syncRpcUninterruptibly(@Nonnull Object request, long timeoutMs) {
		return session.syncRpcUninterruptibly(request, timeoutMs);
	}

	@Override
	public boolean isActive() {
		return session.isActive();
	}

	private interface PipelineTask {

		/**
		 * 执行发送操作，运行在网络线程下
		 */
		void send();

		/**
		 * 执行取消操作，运行在用户线程下
		 */
		void cancel();
	}

	private static class OneWayMessageTask implements PipelineTask {

		private final AbstractSession session;
		private final Object message;

		private OneWayMessageTask(AbstractSession session, Object message) {
			this.session = session;
			this.message = message;
		}

		@Override
		public void send() {
			session.sendOneWayMessage(message);
		}

		@Override
		public void cancel() {
			// do nothing
		}
	}

	private static class RpcRequestTask implements PipelineTask {

		private final AbstractSession session;
		private final Object request;
		private final long timeoutMs;
		private final PipelineRpcPromise rpcPromise;
		private final RpcCallback rpcCallback;

		private RpcRequestTask(AbstractSession session, Object request, long timeoutMs, PipelineRpcPromise rpcPromise, RpcCallback rpcCallback) {
			this.session = session;
			this.request = request;
			this.timeoutMs = timeoutMs;
			this.rpcPromise = rpcPromise;
			this.rpcCallback = rpcCallback;
		}

		@Override
		public void send() {
			if (rpcPromise != null) {
				session.sendAsyncRpcRequest(request, timeoutMs, rpcPromise);
				rpcPromise.setSent();
			} else {
				session.sendAsyncRpcRequest(request, timeoutMs, session.netContext().localEventLoop(), rpcCallback);
			}
		}

		@Override
		public void cancel() {
			if (rpcPromise != null) {
				rpcPromise.trySuccess(RpcResponse.SESSION_CLOSED);
				rpcPromise.setSent();
			} else {
				rpcCallback.onComplete(RpcResponse.SESSION_CLOSED);
			}
		}
	}

	private static class RpcResponseTask implements PipelineTask {

		private final AbstractSession session;
		private final DefaultRpcRequestContext context;
		private final RpcResponse rpcResponse;

		private RpcResponseTask(AbstractSession session, DefaultRpcRequestContext context, RpcResponse rpcResponse) {
			this.session = session;
			this.context = context;
			this.rpcResponse = rpcResponse;
		}

		@Override
		public void send() {
			session.sendRpcResponse(context.sync, context.requestGuid, rpcResponse);
		}

		@Override
		public void cancel() {
			// do nothing
		}
	}

	/**
	 * 管道的RpcResponseChannel，将结果写回管道
	 */
	private static class PipelineResponseChannel<T> extends AbstractRpcResponseChannel<T> {

		private final DefaultSessionPipeline pipeline;
		private final DefaultRpcRequestContext context;

		private PipelineResponseChannel(DefaultSessionPipeline pipeline, DefaultRpcRequestContext context) {
			this.pipeline = pipeline;
			this.context = context;
		}

		@Override
		protected void doWrite(RpcResponse rpcResponse) {
			EventLoopUtils.executeOrRun(pipeline.userEventLoop(), () -> {
				if (pipeline.isActive()) {
					pipeline.addTask(new RpcResponseTask(pipeline.session, context, rpcResponse));
				}
			});
		}
	}
}
