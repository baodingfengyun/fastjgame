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

package com.wjybxx.fastjgame.misc;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.net.DefaultRpcPromise;
import com.wjybxx.fastjgame.net.RpcResponse;
import com.wjybxx.fastjgame.net.RpcResponseChannel;
import com.wjybxx.fastjgame.utils.EventLoopUtils;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 用于本地调用异步方法的responseChannel，不可以在上面阻塞。
 * (其实不推荐使用该方式，你一定可以有别的方式替代)
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/27
 * github - https://github.com/hl845740757
 */
public class LocalResponseChannel<T> extends DefaultRpcPromise implements RpcResponseChannel<T>{

	private final AtomicBoolean writable = new AtomicBoolean(true);

	public LocalResponseChannel(@Nonnull NetEventLoop workerEventLoop, @Nonnull EventLoop userEventLoop, long timeoutMs) {
		super(workerEventLoop, userEventLoop, timeoutMs);
	}

	@Override
	protected void checkDeadlock() {
		EventLoopUtils.checkDeadLock(getUserEventLoop());
	}

	@Override
	public void write(@Nonnull RpcResponse rpcResponse) {
		if (writable.compareAndSet(true, false)) {
			trySuccess(rpcResponse);
		} else {
			throw new IllegalStateException("ResponseChannel can't be reused!");
		}
	}

	@Override
	public boolean isVoid() {
		return false;
	}
}
