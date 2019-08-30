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

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * {@link Sender}的抽象实现，实现了一些不变的方法。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/30
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public abstract class AbstractSender implements Sender{

	private static final Logger logger = LoggerFactory.getLogger(AbstractSender.class);

	protected final AbstractSession session;

	protected AbstractSender(AbstractSession session) {
		this.session = session;
	}

	@Override
	public final void send(@Nonnull Object message) {
		// 逻辑层检测，会话已关闭，立即返回
		if (!isActive()) {
			logger.info("session is already closed, send message failed.");
			return;
		}
		doSend(message);
	}

	/**
	 * 子类真正执行发送单向消息逻辑
	 * @param message 待发送的单向消息
	 */
	protected abstract void doSend(@Nonnull Object message);

	@Override
	public final void rpc(@Nonnull Object request, @Nonnull RpcCallback callback, long timeoutMs) {
		// 参数校验，必须有过期时间
		if (timeoutMs <= 0) {
			throw new IllegalArgumentException("timeoutMs");
		}
		// 逻辑层校验，会话已关闭，提交回调
		if (!isActive()) {
			// 直接执行回调，执行在发起请求的线程，是安全的
			callback.onComplete(RpcResponse.SESSION_CLOSED);
			return;
		}
		doRpcWithCallback(request, callback, timeoutMs);
	}

	protected abstract void doRpcWithCallback(Object request, RpcCallback callback, long timeoutMs);

	@Nonnull
	@Override
	public final RpcFuture rpc(@Nonnull Object request, long timeoutMs) {
		// 参数校验，必须有过期时间
		if (timeoutMs <= 0) {
			throw new IllegalArgumentException("timeoutMs");
		}
		// 逻辑层校验，会话已关闭，立即返回结果
		if (!isActive()) {
			return netEventLoop().newCompletedRpcFuture(userEventLoop(), RpcResponse.SESSION_CLOSED);
		}
		return doRpc(request, timeoutMs);
	}

	protected abstract RpcFuture doRpc(Object request, long timeoutMs);

	@Nonnull
	@Override
	public final RpcResponse syncRpc(@Nonnull Object request, long timeoutMs) throws InterruptedException {
		if (timeoutMs <= 0) {
			throw new IllegalArgumentException("timeoutMs");
		}
		// 逻辑层校验，会话已关闭，立即返回结果
		if (!isActive()) {
			return RpcResponse.SESSION_CLOSED;
		}
		final RpcPromise rpcPromise = netEventLoop().newRpcPromise(userEventLoop(), timeoutMs);
		// 直接提交到网络层执行
		netEventLoop().execute(() -> {
			session.sendSyncRpcRequest(request, timeoutMs, rpcPromise);
		});
		// RpcPromise保证了不会等待超过限时时间
		return rpcPromise.get();
	}

	@Nonnull
	@Override
	public final RpcResponse syncRpcUninterruptibly(@Nonnull Object request, long timeoutMs) {
		if (timeoutMs <= 0) {
			throw new IllegalArgumentException("timeoutMs");
		}
		// 逻辑层校验，会话已关闭，立即返回结果
		if (!isActive()) {
			return RpcResponse.SESSION_CLOSED;
		}
		final RpcPromise rpcPromise = netEventLoop().newRpcPromise(userEventLoop(), timeoutMs);
		// 直接提交到网络层执行
		netEventLoop().execute(() -> {
			session.sendSyncRpcRequest(request, timeoutMs, rpcPromise);
		});
		// RpcPromise保证了不会等待超过限时时间
		rpcPromise.awaitUninterruptibly();
		// 一定有结果
		return rpcPromise.getNow();
	}

	protected boolean isActive() {
		return session.isActive();
	}

	protected NetEventLoop netEventLoop() {
		return session.netContext().netEventLoop();
	}

	protected EventLoop userEventLoop() {
		return session.netContext().localEventLoop();
	}

}
