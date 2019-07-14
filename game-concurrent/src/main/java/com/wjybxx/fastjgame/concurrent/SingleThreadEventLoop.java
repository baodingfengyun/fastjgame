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


import io.netty.util.concurrent.ThreadPerTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class SingleThreadEventLoop extends AbstractExecutorService implements EventLoop {

	private static final Logger logger = LoggerFactory.getLogger(SingleThreadEventLoop.class);

	static final long DEFAULT_SHUTDOWN_QUIET_PERIOD = 2;
	static final long DEFAULT_SHUTDOWN_TIMEOUT = 15;

	// 线程的状态
	/** 初始状态，未启动状态 */
	private static final int ST_NOT_STARTED = 1;
	/** 已启动状态，运行状态 */
	private static final int ST_STARTED = 2;
	/** 正在关闭状态 */
	private static final int ST_SHUTTING_DOWN = 3;
	/** 已关闭状态 */
	private static final int ST_SHUTDOWN = 4;
	/** 终止状态(二阶段终止模式 - 已关闭状态下进行最后的清理，然后进入终止状态) */
	private static final int ST_TERMINATED = 5;

	// 毒药任务，特殊的任务
	/** 唤醒线程的任务 */
	private static final Runnable WAKEUP_TASK = () ->{};
	/** 填充用的任务 */
	private static final Runnable NOOP_TASK = () -> {};

	private final EventLoopGroup parent;
	private final Collection<EventLoop> selfCollection = Collections.<EventLoop>singleton(this);

	/** 持有的线程 */
	private volatile Thread thread;

	/**
	 * 线程的生命周期标识。
	 * 未何netty一样使用{@link AtomicIntegerFieldUpdater}，需要更多的理解成本，对于不熟悉的人来说容易用错。
	 * 首先保证正确性，易分析。
	 */
	private final AtomicInteger state = new AtomicInteger(ST_NOT_STARTED);
	/**
	 * 本次循环要执行的任务
	 */
	private final BlockingQueue<Runnable> taskQueue;

	/**
	 * 创建{@link #thread}的executor。
	 * 不直接创建线程，而是通过提交一个死循环任务获得线程。
	 */
	private final Executor executor;

	/** 是否有请求中断当前线程 */
	private volatile boolean interrupted = false;

	public SingleThreadEventLoop(EventLoopGroup parent, ThreadFactory threadFactory) {
		// 为何可以使用ThreadPerTaskExecutor？因为必须要能够创建一个新线程给当前对象
		// 限定线程数的线程池，会导致异常
		this(parent, new ThreadPerTaskExecutor(threadFactory));
	}

	public SingleThreadEventLoop(EventLoopGroup parent, Executor executor) {
		this.parent = parent;
		this.executor = executor;
		this.taskQueue = newTaskQueue();
	}

	protected BlockingQueue<Runnable> newTaskQueue() {
		return new LinkedBlockingQueue<>();
	}

	@Override
	public EventLoopGroup parent() {
		return parent;
	}

	@Override
	public boolean inEventLoop() {
		return thread == Thread.currentThread();
	}

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

	@Nonnull
	@Override
	public Iterator<EventLoop> iterator() {
		return selfCollection.iterator();
	}

	@Override
	public void shutdown() {

	}

	@Nonnull
	@Override
	public List<Runnable> shutdownNow() {
		// TODO
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
	public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
		return false;
	}

	@Override
	public <V> ListenableFuture<V> newFailedFuture(@Nonnull Throwable e) {
		return new FailedFuture<>(this, e);
	}

	@Override
	public <V> ListenableFuture<V> newSucceededFuture(@Nullable V value) {
		return new SucceededFuture<>(this, value);
	}

	/**
	 * Interrupt the current running {@link Thread}.
	 */
	protected void interruptThread() {
		Thread currentThread = thread;
		if (currentThread == null) {
			// 线程还未启动，先将请求保存下来
			interrupted = true;
		} else {
			currentThread.interrupt();
		}
	}


	@Override
	public void execute(@Nonnull Runnable command) {
		// TODO 真正做事情的地方
	}


	/**
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
