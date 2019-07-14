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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

public class MultiThreadEventLoopGroup implements EventLoopGroup {

	@Override
	public boolean isShuttingDown() {
		return false;
	}

	@Override
	public ListenableFuture<?> shutdownGracefully() {
		return null;
	}

	@Override
	public ListenableFuture<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
		return null;
	}

	@Override
	public ListenableFuture<?> terminationFuture() {
		return null;
	}

	@Override
	public void shutdown() {

	}

	@Nonnull
	@Override
	public List<Runnable> shutdownNow() {
		return null;
	}

	@Override
	public EventLoop next() {
		return null;
	}

	@Nonnull
	@Override
	public Iterator<EventLoop> iterator() {
		return null;
	}

	@Override
	public boolean isShutdown() {
		return false;
	}

	@Override
	public boolean isTerminated() {
		return false;
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return false;
	}

	@Override
	public <T> ListenableFuture<T> submit(Callable<T> task) {
		return null;
	}

	@Override
	public <T> ListenableFuture<T> submit(Runnable task, T result) {
		return null;
	}

	@Override
	public ListenableFuture<?> submit(Runnable task) {
		return null;
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		return null;
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
		return null;
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		return null;
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return null;
	}

	@Override
	public void execute(@Nonnull Runnable command) {
		next().execute(command);
	}
}
