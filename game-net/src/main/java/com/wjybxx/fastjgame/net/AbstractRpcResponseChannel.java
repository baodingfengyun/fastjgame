/*
 *    Copyright 2019 wjybxx
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.wjybxx.fastjgame.net;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RpcResponseChannel的骨架实现
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/16
 * github - https://github.com/hl845740757
 */
public abstract class AbstractRpcResponseChannel<T> implements RpcResponseChannel<T>{

	private final AtomicBoolean writable = new AtomicBoolean(true);

	/**
	 * 返回rpc调用结果，表示调用成功。
	 * @param body rpc调用结果
	 */
	public void writeSuccess(@Nonnull T body) {
		write(new RpcResponse(RpcResultCode.SUCCESS, body));
	}

	/**
	 * 返回rpc调用结果，表示调用失败。
	 * @param errorCode rpc调用错误码，注意：{@link RpcResultCode#hasBody(RpcResultCode)}必须返回false。
	 */
	public void writeFailure(@Nonnull RpcResultCode errorCode) {
		write(RpcResponse.newFailResponse(errorCode));
	}

	@Override
	public void write(@Nonnull RpcResultCode resultCode, @Nonnull Object body) {
		write(new RpcResponse(resultCode, body));
	}

	@Override
	public void write(@Nonnull RpcResponse rpcResponse) {
		if (writable.compareAndSet(true, false)) {
			doWrite(rpcResponse);
		} else {
			throw new IllegalStateException("ResponseChannel can't be reused!");
		}
	}

	/**
	 * 子类真正的进行发送
	 * @param rpcResponse rpc响应
	 */
	protected abstract void doWrite(RpcResponse rpcResponse);

	@Override
	public final boolean isVoid() {
		return false;
	}
}
