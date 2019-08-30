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

package com.wjybxx.fastjgame.net;

import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.EventLoopUtils;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.LinkedList;

/**
 * 带有缓冲区的sender。
 * 它只对用户自己发起的rpc请求进行了缓存，因为多线程的rpc请求之间无法确定顺序，缓存其它用户的请求意义不大。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/30
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class BufferedSender extends AbstractSender{

	/**
	 * 只缓存了用户线程的请求。
	 * 不是线程安全的，只由用户线程访问。
	 */
	private LinkedList<SenderTask> buffer = new LinkedList<>();

	public BufferedSender(AbstractSession session) {
		super(session);
	}

	@Override
	protected void doSend(@Nonnull Object message) {
		addTask(new OneWayMessageTask(session, message));
	}

	@Override
	protected void doRpcWithCallback(Object request, RpcCallback callback, long timeoutMs) {
		addTask(new RpcRequestTask(session, request, timeoutMs, null, callback));
	}

	@Override
	protected RpcFuture doRpc(Object request, long timeoutMs) {
		// 需要特殊的RpcPromise
		final BufferedRpcPromise rpcResponsePromise = new DefaultBufferedRpcPromise(netEventLoop(), userEventLoop(), timeoutMs);
		RpcRequestTask rpcRequestTask = new RpcRequestTask(session, request, timeoutMs, rpcResponsePromise, null);
		addTask(rpcRequestTask);
		return rpcResponsePromise;
	}

	/**
	 * 添加一个任务，先添加到用户缓存队列中。
	 * @param task 添加的任务
	 */
	private void addTask(SenderTask task) {
		// 只有用户线程的操作才需要缓存
		if (userEventLoop().inEventLoop()) {
			// 加入缓存
			buffer.add(task);
			// 检查是否需要清空缓冲区
			if (buffer.size() >= session.getNetConfigManager().flushThreshold()) {
				flush();
			}
		} else {
			// 直接提交到网络线程执行
			netEventLoop().execute(task);
		}
	}

	@Override
	public void flush() {
		// 只有用户线程有缓存
		if (userEventLoop().inEventLoop()) {
			if (buffer.size() == 0) {
				return;
			}
			if (!session.isActive()) {
				// session已关闭，取消所有任务
				cancelTasks();
			} else {
				// 清空缓冲区
				flushBuffer();
			}
		}
	}

	@Override
	public void cancelAll() {
		EventLoopUtils.executeOrRun(userEventLoop(), () -> {
			if (buffer.size() > 0) {
				cancelAll();
			}
		});
	}

	/**
	 * 交换缓冲区(用户线程下)
	 * @return oldBuffer
	 */
	private LinkedList<SenderTask> exchangeBuffer() {
		LinkedList<SenderTask> result = this.buffer;
		this.buffer = new LinkedList<>();
		return result;
	}

	/**
	 * 清空缓冲区(用户线程下)
	 */
	private void flushBuffer() {
		final LinkedList<SenderTask> oldBuffer = exchangeBuffer();
		netEventLoop().execute(()-> {
			for (SenderTask senderTask :oldBuffer) {
				senderTask.run();
			}
		});
	}

	/**
	 * 取消所有的任务(用户线程下)
	 */
	private void cancelTasks() {
		final LinkedList<SenderTask> oldBuffer = exchangeBuffer();
		for (SenderTask senderTask : oldBuffer) {
			ConcurrentUtils.safeExecute((Runnable) senderTask::cancel);
		}
	}

	@Nonnull
	@Override
	public <T> RpcResponseChannel<T> newResponseChannel(@Nonnull RpcRequestContext context) {
		DefaultRpcRequestContext realContext = (DefaultRpcRequestContext) context;
		if (realContext.sync) {
			// 同步调用，立即返回
			return DirectSender.newDirectResponseChannel(session, realContext);
		} else {
			// 非同步调用，可以压入缓冲区
			return new BufferedResponseChannel<>(this, realContext);
		}
	}

	private interface SenderTask extends Runnable{

		/**
		 * 执行发送操作，运行在网络线程下
		 */
		void run();

		/**
		 * 执行取消操作，运行在用户线程下
		 */
		void cancel();
	}

	private static class OneWayMessageTask implements SenderTask {

		private final AbstractSession session;
		private final Object message;

		private OneWayMessageTask(AbstractSession session, Object message) {
			this.session = session;
			this.message = message;
		}

		@Override
		public void run() {
			session.sendOneWayMessage(message);
		}

		@Override
		public void cancel() {
			// do nothing
		}
	}

	private static class RpcRequestTask implements SenderTask {

		private final AbstractSession session;
		private final Object request;
		private final long timeoutMs;
		private final BufferedRpcPromise rpcPromise;
		private final RpcCallback rpcCallback;

		private RpcRequestTask(AbstractSession session, Object request, long timeoutMs, BufferedRpcPromise rpcPromise, RpcCallback rpcCallback) {
			this.session = session;
			this.request = request;
			this.timeoutMs = timeoutMs;
			this.rpcPromise = rpcPromise;
			this.rpcCallback = rpcCallback;
		}

		@Override
		public void run() {
			if (rpcPromise != null) {
				session.sendAsyncRpcRequest(request, timeoutMs, rpcPromise);
				rpcPromise.setSent();
			} else {
				session.sendAsyncRpcRequest(request, timeoutMs, session.localEventLoop(), rpcCallback);
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

	private static class RpcResponseTask implements SenderTask {

		private final AbstractSession session;
		private final DefaultRpcRequestContext context;
		private final RpcResponse rpcResponse;

		private RpcResponseTask(AbstractSession session, DefaultRpcRequestContext context, RpcResponse rpcResponse) {
			this.session = session;
			this.context = context;
			this.rpcResponse = rpcResponse;
		}

		@Override
		public void run() {
			session.sendRpcResponse(context.sync, context.requestGuid, rpcResponse);
		}

		@Override
		public void cancel() {
			// do nothing
		}
	}

	/**
	 * 带缓冲区的sender的ResponseChannel，将结果写回缓冲区
	 */
	private static class BufferedResponseChannel<T> extends AbstractRpcResponseChannel<T> {

		private final BufferedSender bufferedSender;
		private final DefaultRpcRequestContext context;

		private BufferedResponseChannel(BufferedSender bufferedSender, DefaultRpcRequestContext context) {
			this.bufferedSender = bufferedSender;
			this.context = context;
		}

		@Override
		protected void doWrite(RpcResponse rpcResponse) {
			bufferedSender.addTask(new RpcResponseTask(bufferedSender.session, context, rpcResponse));
		}
	}
}
