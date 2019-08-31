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
	protected void doAsyncRpc(Object request, RpcCallback callback, long timeoutMs) {
		addTask(new RpcRequestTask(session, request, timeoutMs, callback));
	}

	/**
	 * 添加一个任务，先添加到用户缓存队列中。
	 * 该方法是{@link BufferedSender}的核心。
	 * @param task 添加的任务
	 */
	private void addTask(SenderTask task) {
		// 只有用户线程的操作才需要缓存
		if (userEventLoop().inEventLoop()) {
			// 加入缓存
			buffer.add(task);
			// 检查是否需要清空缓冲区
			if (buffer.size() >= session.getNetConfigManager().flushThreshold()) {
				flushBuffer();
			}
		} else {
			// 直接提交到网络线程执行
			netEventLoop().execute(task);
		}
	}

	@Override
	protected <T> RpcResponseChannel<T> newAsyncRpcResponseChannel(long requestGuid) {
		return new BufferedResponseChannel<>(this, requestGuid);
	}

	@Override
	public void flush() {
		// 只有用户线程有缓存
		if (userEventLoop().inEventLoop()) {
			if (buffer.size() == 0) {
				return;
			}
			if (session.isActive()) {
				flushBuffer();
			}
		}
	}

	@Override
	public void clearBuffer() {
		// 这是在关闭session时调用的，需要确保用户能取消掉所有的任务。
		EventLoopUtils.executeOrRun(userEventLoop(), () -> {
			buffer.clear();
		});
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
			session.sendAsyncRpcRequest(request, timeoutMs, session.localEventLoop(), rpcCallback);
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
