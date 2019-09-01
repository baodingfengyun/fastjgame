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
 * 带有缓冲区的sender，数据缓存在用户线程。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/30
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class BufferedSender extends AbstractSender{

	/**
	 * 缓存的消息。
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
	protected void doAsyncRpc(Object request, RpcCallback callback, long timeoutMs) {
		addTask(new RpcRequestTask(session, request, timeoutMs, callback));
	}

	/**
	 * 添加一个任务，先添加到用户缓存队列中。
	 * 该方法是{@link BufferedSender}的核心。
	 * @param task 添加的任务
	 */
	private void addTask(SenderTask task) {
		EventLoopUtils.executeOrRun(userEventLoop(), () -> {
			// 加入缓存
			buffer.add(task);
			// 检查是否需要清空缓冲区
			if (buffer.size() >= session.getNetConfigManager().flushThreshold()) {
				flushBuffer();
			}
		});
	}

	@Override
	protected <T> RpcResponseChannel<T> newAsyncRpcResponseChannel(long requestGuid) {
		return new BufferedResponseChannel<>(this, requestGuid);
	}

	@Override
	public void flush() {
		EventLoopUtils.executeOrRun(userEventLoop(), () -> {
			if (buffer.size() == 0) {
				return;
			}
			if (session.isActive()) {
				flushBuffer();
			}
			// else 等待清除
		});
	}

	@Override
	public void clearBuffer() {
		// 这是在关闭session时调用的，需要确保用户能取消掉所有的任务。用户线程可能关闭了，所以是tryCommit
		ConcurrentUtils.tryCommit(userEventLoop(), this::cancelAll);
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
	private void cancelAll() {
		SenderTask senderTask;
		while ((senderTask = buffer.pollFirst()) != null) {
			ConcurrentUtils.safeExecute((Runnable) senderTask::cancel);
		}
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

	private interface SenderTask extends Runnable{

		/**
		 * 执行发送操作，运行在网络线程下
		 */
		void run();

		/**
		 * 执行取消操作，运行在用户线程下（未来可能被删除）
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
		private final RpcCallback rpcCallback;

		private RpcRequestTask(AbstractSession session, Object request, long timeoutMs, RpcCallback rpcCallback) {
			this.session = session;
			this.request = request;
			this.timeoutMs = timeoutMs;
			this.rpcCallback = rpcCallback;
		}

		@Override
		public void run() {
			session.sendAsyncRpcRequest(request, timeoutMs, rpcCallback);
		}

		@Override
		public void cancel() {
			rpcCallback.onComplete(RpcResponse.SESSION_CLOSED);
		}
	}

	private static class RpcResponseTask implements SenderTask {

		private final AbstractSession session;
		private final long requestGuid;
		private final RpcResponse rpcResponse;

		private RpcResponseTask(AbstractSession session, long requestGuid, RpcResponse rpcResponse) {
			this.session = session;
			this.requestGuid = requestGuid;
			this.rpcResponse = rpcResponse;
		}

		@Override
		public void run() {
			session.sendRpcResponse(requestGuid, false, rpcResponse);
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
		private final long requestGuid;

		public BufferedResponseChannel(BufferedSender bufferedSender, long requestGuid) {
			this.bufferedSender = bufferedSender;
			this.requestGuid = requestGuid;
		}

		@Override
		protected void doWrite(RpcResponse rpcResponse) {
			bufferedSender.addTask(new RpcResponseTask(bufferedSender.session, requestGuid, rpcResponse));
		}
	}
}
