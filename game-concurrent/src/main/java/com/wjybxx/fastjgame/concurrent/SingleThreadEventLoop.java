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


import com.wjybxx.fastjgame.configwrapper.PropertiesConfigWrapper;
import com.wjybxx.fastjgame.utils.ConfigUtils;
import com.wjybxx.fastjgame.utils.SystemPropertiesUtils;
import io.netty.util.concurrent.ThreadPerTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * 单线程的事件循环，该类负责线程的生命周期管理
 * 事件循环架构如果不是单线程的将没有意义。
 */
public class SingleThreadEventLoop extends AbstractEventLoop {

	private static final Logger logger = LoggerFactory.getLogger(SingleThreadEventLoop.class);

	// 毒药任务
	/** 唤醒线程的任务 */
	private static final Runnable WAKEUP_TASK = () ->{};
	/** 填充用的任务 */
	private static final Runnable NOOP_TASK = () -> {};
	/** 有界线程池 */
	private static final int DEFAULT_MAX_TASKS = SystemPropertiesUtils.getSystemConfig().getAsInt("fastjgame.max.tasknum", 8192*8);


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
	/**
	 * 创建{@link #thread}的executor。
	 * 不直接创建线程，而是通过提交一个死循环任务获得线程。
	 */
	private final Executor executor;

	/** 持有的线程 */
	private volatile Thread thread;

	/**
	 * 线程的生命周期标识。
	 * 未何netty一样使用{@link AtomicIntegerFieldUpdater}，需要更多的理解成本，对于不熟悉的人来说容易用错。
	 * 首先保证正确性，易分析。
	 */
	private final AtomicInteger stateHolder = new AtomicInteger(ST_NOT_STARTED);
	/**
	 * 本次循环要执行的任务
	 */
	private final BlockingQueue<Runnable> taskQueue;
	/** 任务被拒绝时的处理策略 */
	private final RejectedExecutionHandler rejectedExecutionHandler;

	/** 是否有请求中断当前线程 */
	private volatile boolean interrupted = false;
	/** 线程终止future */
	private final Promise<?> terminationFuture = new DefaultPromise(GlobalEventLoop.INSTANCE);


	public SingleThreadEventLoop(EventLoopGroup parent, ThreadFactory threadFactory) {
		// 为何可以使用ThreadPerTaskExecutor？因为必须要能够创建一个新线程给当前对象
		// 限定线程数的线程池，会导致异常
		this(parent, new ThreadPerTaskExecutor(threadFactory), RejectedExecutionHandlers.reject());
	}

	public SingleThreadEventLoop(EventLoopGroup parent, Executor executor, RejectedExecutionHandler rejectedExecutionHandler) {
		super(parent);
		this.executor = executor;
		this.rejectedExecutionHandler = rejectedExecutionHandler;
		this.taskQueue = newTaskQueue();
	}

	protected BlockingQueue<Runnable> newTaskQueue(int maxTaskNum) {
		return new LinkedBlockingQueue<>();
	}

	@Override
	public boolean inEventLoop() {
		return thread == Thread.currentThread();
	}

	@Override
	public void shutdown() {

	}

	@Override
	public ListenableFuture<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
		return null;
	}

	@Override
	public ListenableFuture<?> terminationFuture() {
		return terminationFuture;
	}

	@Override
	public boolean isShuttingDown() {
		return stateHolder.get() >= ST_SHUTTING_DOWN;
	}

	@Override
	public boolean isShutdown() {
		return stateHolder.get() >= ST_SHUTDOWN;
	}

	@Override
	public boolean isTerminated() {
		return stateHolder.get() == ST_TERMINATED;
	}

	@Override
	public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
		return false;
	}

	@Override
	public void execute(@Nonnull Runnable command) {
		// TODO 真正执行任务的地方
	}


	/**
	 * @see Queue#poll()
	 */
	protected Runnable pollTask() {
		assert inEventLoop();
		return pollTaskFrom(taskQueue);
	}

	protected static Runnable pollTaskFrom(Queue<Runnable> taskQueue) {
		for (;;) {
			Runnable task = taskQueue.poll();
			if (task == WAKEUP_TASK) {
				continue;
			}
			return task;
		}
	}

	/**
	 * @see Queue#peek()
	 */
	protected Runnable peekTask() {
		assert inEventLoop();
		return taskQueue.peek();
	}

	/**
	 * @see Queue#isEmpty()
	 */
	protected boolean hasTasks() {
		assert inEventLoop();
		return !taskQueue.isEmpty();
	}

	/**
	 * Add a task to the task queue, or throws a {@link RejectedExecutionException} if this instance was shutdown
	 * before.
	 */
	protected void addTask(Runnable task) {
		if (task == null) {
			throw new NullPointerException("task");
		}
		if (!offerTask(task)) {
			reject(task);
		}
	}

	final boolean offerTask(Runnable task) {
		if (isShutdown()) {
			reject();
		}
		return taskQueue.offer(task);
	}

	@SuppressWarnings("unused")
	protected boolean wakesUpForTask(Runnable task) {
		return true;
	}

	protected static void reject() {
		throw new RejectedExecutionException("event executor terminated");
	}

	/**
	 * Offers the task to the associated {@link RejectedExecutionHandler}.
	 *
	 * @param task to reject.
	 */
	protected final void reject(Runnable task) {
		rejectedExecutionHandler.rejected(task, this);
	}

	/**
	 * @see Queue#remove(Object)
	 */
	protected boolean removeTask(Runnable task) {
		if (task == null) {
			throw new NullPointerException("task");
		}
		return taskQueue.remove(task);
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
