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

import javax.annotation.Nonnull;

/**
 * 带有缓冲区的sender的模板实现 - 将所有的消息发送变成任务。
 * @apiNote
 * 注意：如果任务无法发送，必须调用{@link BufferTask#cancel()}执行取消逻辑。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/4
 * github - https://github.com/hl845740757
 */
public abstract class AbstractBufferedSender extends AbstractSender{

	protected AbstractBufferedSender(AbstractSession session) {
		super(session);
	}

	@Override
	protected final void doSend(@Nonnull Object message) {
		addTask(new OneWayMessageTask(session, message));
	}

	@Override
	protected final void doAsyncRpc(Object request, RpcCallback callback, long timeoutMs) {
		addTask(new RpcRequestTask(session, request, timeoutMs, callback));
	}

	/**
	 * 子类通过实现该方法实现自己的缓冲策略！
	 * @apiNote
	 * 注意：该方法只有在{@link Session#isActive()}时才会调用，子类不必处理session状态。
	 *
	 * @param task 一个数据发送请求
	 */
	protected abstract void addTask(BufferTask task);

	@Override
	protected final <T> RpcResponseChannel<T> newAsyncRpcResponseChannel(long requestGuid) {
		return new BufferedResponseChannel<>(this, requestGuid);
	}

	protected interface BufferTask extends Runnable{

		/**
		 * 执行发送操作，运行在网络线程下。
		 * 实现{@link Runnable}接口可以减少lambda表达式。
		 */
		void run();

		/**
		 * 执行取消操作，运行在用户线程下（未来可能被删除）
		 */
		void cancel();
	}

	protected static class OneWayMessageTask implements BufferTask {

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

	protected static class RpcRequestTask implements BufferTask {

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

	protected static class RpcResponseTask implements BufferTask {

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

		private final AbstractBufferedSender bufferedSender;
		private final long requestGuid;

		private BufferedResponseChannel(AbstractBufferedSender bufferedSender, long requestGuid) {
			this.bufferedSender = bufferedSender;
			this.requestGuid = requestGuid;
		}

		@Override
		protected void doWrite(RpcResponse rpcResponse) {
			if (bufferedSender.session.isActive()) {
				bufferedSender.addTask(new RpcResponseTask(bufferedSender.session, requestGuid, rpcResponse));
			}
		}
	}
}
