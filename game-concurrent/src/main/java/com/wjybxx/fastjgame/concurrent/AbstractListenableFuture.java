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

package com.wjybxx.fastjgame.concurrent;

import javax.annotation.Nonnull;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * ListenableFuture的抽象实现
 * @param <V>
 */
public abstract class AbstractListenableFuture<V> implements ListenableFuture<V>{

	@Override
	public V get() throws InterruptedException, ExecutionException {
		await();

		Throwable cause = cause();
		if (cause == null) {
			return tryGet();
		}
		if (cause instanceof CancellationException) {
			throw (CancellationException) cause;
		}
		throw new ExecutionException(cause);
	}

	@Override
	public V get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if (await(timeout, unit)) {
			Throwable cause = cause();
			if (cause == null) {
				return tryGet();
			}
			if (cause instanceof CancellationException) {
				throw (CancellationException) cause;
			}
			throw new ExecutionException(cause);
		}
		throw new TimeoutException();
	}


	/**
	 * {@link #await(long, TimeUnit)}的一个快捷方法。固定时间单位为毫秒。
	 */
	public boolean await(long timeoutMillis) throws InterruptedException {
		return await(timeoutMillis, TimeUnit.MILLISECONDS);
	}

	/**
	 * {@link #awaitUninterruptibly(long, TimeUnit)}的一个快捷方法。
	 * 固定时间单位为毫秒
	 */
	public boolean awaitUninterruptibly(long timeoutMillis) {
		return awaitUninterruptibly(timeoutMillis, TimeUnit.MILLISECONDS);
	}
}
