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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * {@link EventLoop}的抽象实现。这里负责一些简单的方法实现。
 *
 * Abstract base class for {@link EventLoop} implementations.
 */
public abstract class AbstractEventLoop extends AbstractExecutorService implements EventLoop {
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractEventLoop.class);

	/**
	 * 父节点的引用。
	 * 可能为null
	 */
	private final EventLoopGroup parent;
	/**
	 * 封装一个只包含自己的集合。方便实现迭代查询等等。
	 */
	private final Collection<EventLoop> selfCollection = Collections.<EventLoop>singleton(this);

	protected AbstractEventLoop(@Nullable EventLoopGroup parent) {
		this.parent = parent;
	}

	@Nullable
	@Override
	public EventLoopGroup parent() {
		return parent;
	}

	@Nonnull
	@Override
	public EventLoop next() {
		// 因为 EventExecutor 是叶子节点，是没有子节点的，因此请求的事件处理器都是自己
		return this;
	}

	@Nonnull
	@Override
	public <V> Promise<V> newPromise() {
		return new DefaultPromise<V>(this);
	}

	@Nonnull
	@Override
	public <V> ListenableFuture<V> newSucceededFuture(V result) {
		return new SucceededFuture<V>(this, result);
	}

	@Nonnull
	@Override
	public <V> ListenableFuture<V> newFailedFuture(@Nonnull Throwable cause) {
		return new FailedFuture<V>(this, cause);
	}

	// --------------------------------------- 任务提交 ----------------------------------------
	// region 重写 AbstractExecutorService中的部分方法,返回特定的Future类型
	@Nonnull
	@Override
	public ListenableFuture<?> submit(@Nonnull Runnable task) {
		return (ListenableFuture<?>) super.submit(task);
	}

	@Nonnull
	@Override
	public <T> ListenableFuture<T> submit(@Nonnull Runnable task, T result) {
		return (ListenableFuture<T>) super.submit(task, result);
	}

	@Nonnull
	@Override
	public <T> ListenableFuture<T> submit(@Nonnull Callable<T> task) {
		return (ListenableFuture<T>) super.submit(task);
	}

	// 重要，重写newTaskFor方法，返回具体的future类型
	@Override
	protected final <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
		return new PromiseTask<T>(this, runnable, value);
	}

	@Override
	protected final <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
		return new PromiseTask<T>(this, callable);
	}
	// endregion

	// ---------------------------------------- 迭代 ---------------------------------------
	@Nonnull
	@Override
	public Iterator<EventLoop> iterator() {
		return selfCollection.iterator();
	}

	@Override
	public void forEach(Consumer<? super EventLoop> action) {
		selfCollection.forEach(action);
	}

	@Override
	public Spliterator<EventLoop> spliterator() {
		return selfCollection.spliterator();
	}

	/**
	 * 使用lambda表达式封装不安全的运行任务，避免线程退出
	 * Try to execute the given {@link Runnable} and just log if it throws a {@link Throwable}.
	 */
	protected static void safeExecute(Runnable task) {
		try {
			task.run();
		} catch (Throwable t) {
			logger.warn("A task raised an exception. Task: {}", task, t);
		}
	}
}