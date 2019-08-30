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
 * Session发送消息的真正实现
 * @apiNote
 * 实现类必须是线程安全的。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/30
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public interface Sender {

	void send(@Nonnull Object message);

	void rpc(@Nonnull Object request, @Nonnull RpcCallback callback, long timeoutMs);

	@Nonnull
	RpcFuture rpc(@Nonnull Object request, long timeoutMs);

	@Nonnull
	RpcResponse syncRpc(@Nonnull Object request, long timeoutMs) throws InterruptedException;

	@Nonnull
	RpcResponse syncRpcUninterruptibly(@Nonnull Object request, long timeoutMs);

	<T> RpcResponseChannel<T> newResponseChannel(RpcRequestContext context);

	void flush();

	void cancelAll();
}
