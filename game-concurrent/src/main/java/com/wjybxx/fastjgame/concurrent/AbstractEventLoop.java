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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;

/**
 * {@link EventLoop}的抽象实现。
 * 它是叶子节点的顶层超类。
 *
 * 它继承了{@link AbstractExecutorService}，并实现了{@link EventLoop}。
 * 融合了两者的语义 => AbstractEventExecutor 是一个处理事件的ExecutorService
 *
 *
 * Abstract base class for {@link EventLoop} implementations.
 */
public abstract class AbstractEventLoop extends AbstractExecutorService implements EventLoop {
	private static final Logger logger = LoggerFactory.getLogger(AbstractEventLoop.class);

	/**
	 * 默认的关闭前的安静期 2S
	 */
	static final long DEFAULT_SHUTDOWN_QUIET_PERIOD = 2;
	/**
	 * 默认的等待关闭超时的时间， 15秒
	 */
	static final long DEFAULT_SHUTDOWN_TIMEOUT = 15;

	/**
	 * 父节点的引用
	 * 我是一个EventExecutor，是EventExecutorGroup中的一员(是它的子节点)。
	 *
	 */
	private final EventLoopGroup parent;
	/**
	 * 封装一个只包含自己的集合。方便实现跌打查询等等。
	 */
	private final Collection<EventLoop> selfCollection = Collections.<EventLoop>singleton(this);

	protected AbstractEventLoop() {
		this(null);
	}

	protected AbstractEventLoop(EventLoopGroup parent) {
		this.parent = parent;
	}

	@Override
	public EventLoopGroup parent() {
		return parent;
	}

	@Override
	public EventLoop next() {
		// 因为 EventExecutor 是叶子节点，是没有子节点的，因此请求的事件处理器都是自己
		return this;
	}

	@Override
	public Iterator<EventLoop> iterator() {
		return selfCollection.iterator();
	}

	@Override
	public ListenableFuture<?> shutdownGracefully() {
		return shutdownGracefully(DEFAULT_SHUTDOWN_QUIET_PERIOD, DEFAULT_SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
	}

	/**
	 * @deprecated {@link #shutdownGracefully(long, long, TimeUnit)} or {@link #shutdownGracefully()} instead.
	 */
	@Override
	@Deprecated
	public abstract void shutdown();

	/**
	 * @deprecated {@link #shutdownGracefully(long, long, TimeUnit)} or {@link #shutdownGracefully()} instead.
	 */
	@Override
	@Deprecated
	public List<Runnable> shutdownNow() {
		shutdown();
		return Collections.emptyList();
	}

	@Override
	public <V> Promise<V> newPromise() {
		return new DefaultPromise<V>(this);
	}

	@Override
	public <V> ListenableFuture<V> newSucceededFuture(V result) {
		return new SucceededFuture<V>(this, result);
	}

	@Override
	public <V> ListenableFuture<V> newFailedFuture(@Nonnull Throwable cause) {
		return new FailedFuture<V>(this, cause);
	}

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

	// 重要，重写newTaskFor放阿飞
	@Override
	protected final <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
		return new PromiseTask<T>(this, runnable, value);
	}

	@Override
	protected final <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
		return new PromiseTask<T>(this, callable);
	}
	// endregion

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