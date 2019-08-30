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
import javax.annotation.concurrent.ThreadSafe;

/**
 * 直接发送的sender，在消息数量不是很多的情况下，拥有更低的延迟，吞吐量也较好。
 * 但是如果消息数量非常大，那么延迟会很高(高度竞争)，吞吐量也较差。
 *
 * 该实现没有任何缓存，逻辑简单，是线程安全的，消息顺序也很容易保证。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/30
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class DirectSender extends AbstractSender {

	public DirectSender(AbstractSession session) {
		super(session);
	}

	@Override
	public void doSend(@Nonnull Object message) {
		// 直接提交到网络层执行
		netEventLoop().execute(() -> {
			session.sendOneWayMessage(message);
		});
	}

	@Override
	public void doRpcWithCallback(@Nonnull Object request, @Nonnull RpcCallback callback, long timeoutMs) {
		// 直接提交到网络层执行
		netEventLoop().execute(() -> {
			session.sendAsyncRpcRequest(request, timeoutMs, userEventLoop(), callback);
		});
	}

	@Nonnull
	@Override
	public final RpcFuture doRpc(@Nonnull Object request, long timeoutMs) {
		final RpcPromise rpcPromise = netEventLoop().newRpcPromise(userEventLoop(), timeoutMs);
		// 直接提交到网络层执行
		netEventLoop().execute(() -> {
			session.sendAsyncRpcRequest(request, timeoutMs, rpcPromise);
		});
		// 返回给调用者
		return rpcPromise;
	}

	@Nonnull
	@Override
	public <T> RpcResponseChannel<T> newResponseChannel(@Nonnull RpcRequestContext context) {
		return new DirectRpcResponseChannel<>(session, (DefaultRpcRequestContext) context);
	}

	@Override
	public void flush() {
		// 没有缓冲区，因此什么都不做
	}

	@Override
	public void cancelAll() {
		// do nothing
	}

	/**
	 * 创建一个立即返回结果的通道
	 * @param context rpc请求对应的上下文
	 * @param <T> 返回值类型
	 * @return channel
	 */
	public static <T> DirectRpcResponseChannel<T> newDirectResponseChannel(@Nonnull AbstractSession session, @Nonnull DefaultRpcRequestContext context) {
		return new DirectRpcResponseChannel<>(session, context);
	}

	/**
	 * 立即返回结果的channel
	 */
	private static class DirectRpcResponseChannel<T> extends AbstractRpcResponseChannel<T> {

		private final AbstractSession session;
		private final DefaultRpcRequestContext context;

		private DirectRpcResponseChannel(AbstractSession session, DefaultRpcRequestContext context) {
			this.session = session;
			this.context = context;
		}

		@Override
		protected void doWrite(RpcResponse rpcResponse) {
			// 直接提交到网络层
			session.netEventLoop().execute(() -> {
				session.sendRpcResponse(context.sync, context.requestGuid, rpcResponse);
			});
		}
	}
}


