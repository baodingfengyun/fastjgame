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

import com.wjybxx.fastjgame.concurrent.Promise;

import javax.annotation.Nonnull;

/**
 * RpcPromise信息
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/3
 * github - https://github.com/hl845740757
 */
public class RpcPromiseInfo {

	// promise与callback二者存一
	/**
	 * promise，如果promise不为null，则表示同步rpc调用，否则表示异步rpc调用
	 */
	public final Promise<RpcResponse> rpcPromise;
	/**
	 * 回调，如果回调存在的话，表示是一个异步rpc调用。
	 */
	public final RpcCallback rpcCallback;

	/** rpc超时时间 */
	public final long timeoutMs;

	private RpcPromiseInfo(Promise<RpcResponse> rpcPromise, RpcCallback rpcCallback, long timeoutMs) {
		this.rpcPromise = rpcPromise;
		this.rpcCallback = rpcCallback;
		this.timeoutMs = timeoutMs;
	}

	/** 是否是同步rpc调用 */
	public boolean isSync() {
		return null != rpcPromise;
	}

	public static RpcPromiseInfo newInstance(@Nonnull Promise<RpcResponse> rpcPromise, long timeoutMs) {
		return new RpcPromiseInfo(rpcPromise, null, timeoutMs);
	}

	public static RpcPromiseInfo newInstance(@Nonnull RpcCallback rpcCallback, long timeoutMs) {
		return new RpcPromiseInfo(null, rpcCallback, timeoutMs);
	}
}
