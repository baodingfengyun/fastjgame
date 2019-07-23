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


import com.wjybxx.fastjgame.utils.CollectionUtils;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * 单线程的事件循环，该类负责线程的生命周期管理
 * 事件循环架构如果不是单线程的将没有意义。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14
 * github - https://github.com/hl845740757
 */
public abstract class SingleThreadEventLoop extends AbstractEventLoop {

	private static final Logger logger = LoggerFactory.getLogger(SingleThreadEventLoop.class);

	/** 用于友好的唤醒当前线程的任务 */
	private static final Runnable WAKE_UP_TASK = () -> {};

	/**
	 * 当允许的任务数小于等于该值时使用{@link ArrayBlockingQueue}，能提高部分性能（空间换时间）。
	 * 过大时浪费内存。
	 */
	private static final int ARRAy_BLOCKING_QUEUE_CAPACITY = 8 * 1024;

	/**
	 * 缓存队列的大小，不宜过大，但也不能过小。
	 * 过大容易造成内存浪费，过小对于性能无太大意义。
	 */
	private static final int CACHE_QUEUE_CAPACITY = 8 * 64;

	/**
	 * 默认的最大任务数，默认无上限。
	 * 最小为1024
	 */
	private static final int DEFAULT_MAX_TASKS =
			Math.max(1024, SystemUtils.getProperties().getAsInt("SingleThreadEventLoop.maxTasks", Integer.MAX_VALUE));

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

	/** 线程工厂 */
	private final ThreadFactory threadFactory;

	/** 持有的线程 */
	private final Thread thread;

	/**
	 * 线程的生命周期标识。
	 * 未和netty一样使用{@link AtomicIntegerFieldUpdater}，需要更多的理解成本，对于不熟悉的人来说容易用错。
	 * 首先保证正确性，易分析。
	 */
	private final AtomicInteger stateHolder = new AtomicInteger(ST_NOT_STARTED);
	/**
	 * 所有提交的任务
	 */
	private final BlockingQueue<Runnable> taskQueue;

	/**
	 * 缓存队列，用于批量的将{@link #taskQueue}中的任务拉取到本地线程下，减少锁竞争，
	 */
	private final List<Runnable> cacheQueue = new ArrayList<>(CACHE_QUEUE_CAPACITY);

	/** 任务被拒绝时的处理策略 */
	private final RejectedExecutionHandler rejectedExecutionHandler;

	/** 线程终止future */
	private final Promise<?> terminationFuture = new DefaultPromise(GlobalEventLoop.INSTANCE);

	/**
	 * @param parent EventLoop所属的容器，nullable
	 * @param threadFactory 线程工厂，创建的线程不要直接启动，建议调用
	 * {@link Thread#setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler)}设置异常处理器
	 */
	public SingleThreadEventLoop(@Nullable EventLoopGroup parent, @Nonnull ThreadFactory threadFactory) {
		// 限定线程数的线程池，会导致异常
		this(parent, threadFactory, RejectedExecutionHandlers.reject());
	}

	/**
	 * @param parent EventLoop所属的容器，nullable
	 * @param threadFactory 线程工厂，创建的线程不要直接启动，建议调用
	 * {@link Thread#setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler)}设置异常处理器
	 * @param rejectedExecutionHandler 拒绝任务的策略
	 */
	public SingleThreadEventLoop(@Nullable EventLoopGroup parent, @Nonnull ThreadFactory threadFactory, @Nonnull RejectedExecutionHandler rejectedExecutionHandler) {
		super(parent);
		this.threadFactory = threadFactory;
		this.thread = Objects.requireNonNull(threadFactory.newThread(new Worker()), "newThread");

		// 记录异常退出日志
		UncaughtExceptionHandlers.logIfAbsent(thread, logger);

		this.rejectedExecutionHandler = rejectedExecutionHandler;
		this.taskQueue = newTaskQueue(DEFAULT_MAX_TASKS);
	}

	/**
	 * 闯江湖
	 * @param maxTaskNum 允许压入的最大任务数
	 * @return queue
	 */
	protected BlockingQueue<Runnable> newTaskQueue(int maxTaskNum) {
		return maxTaskNum > ARRAy_BLOCKING_QUEUE_CAPACITY ? new LinkedBlockingQueue<>(maxTaskNum): new ArrayBlockingQueue<>(maxTaskNum);
	}

	@Override
	public boolean inEventLoop() {
		return thread == Thread.currentThread();
	}
	// ------------------------------------------------ 线程生命周期 -------------------------------------

	/**
	 * 查询运行状态是否还没到指定状态。
	 * (参考自JDK {@link ThreadPoolExecutor})
	 *
	 * @param oldState 看见的状态值
	 * @param targetState 期望的还没到的状态
	 * @return 如果当前状态在指定状态之前，则返回true。
	 */
	private static boolean runStateLessThan(int oldState, int targetState) {
		return oldState < targetState;
	}

	/**
	 * 查询运行状态是否至少已到了指定状态。
	 * (参考自JDK {@link ThreadPoolExecutor})
	 *
	 * 该方法参考自{@link ThreadPoolExecutor}
	 * @param oldState 看见的状态值
	 * @param targetState 期望的至少到达的状态
	 * @return 如果当前状态为指定状态或指定状态的后续状态，则返回true。
	 */
	private static boolean runStateAtLeast(int oldState, int targetState) {
		return oldState >= targetState;
	}

	// 进入正在关闭状态
	@Override
	public void shutdown() {
		for (;;){
			// 为何要存为临时变量？表示我们是基于特定的状态执行代码，compareAndSet才有意义
			int oldState = stateHolder.get();
			if (isShuttingDown0(oldState)) {
				return;
			}
			// 尝试切换为正在关闭状态，不再接收新任务
			if (stateHolder.compareAndSet(oldState, ST_SHUTTING_DOWN)) {
				// 确保线程可进入终止状态
				ensureThreadTerminable(oldState);
				return;
			}
		}
	}

	/**
	 * 用于其它线程友好的唤醒executor的线程，默认实现是向taskQueue中填充一个任务。
	 * 如果填充任务不能唤醒线程，则子类需要复写该方法
	 *
	 * Q: 为什么默认实现是向taskQueue中插入一个任务，而不是中断线程{@link #interruptThread()} ?
	 * A: 我先不说这里能不能唤醒线程这个问题。
	 *    中断最致命的一点是：向目标线程发出中断请求以后，你并不知道目标线程接收到中断信号的时候正在做什么！！！
	 *    因此它并不是一种唤醒/停止目标线程的最佳方式，它可能导致一些需要原子执行的操作失败，也可能导致其它的问题。
	 *    因此最好是对症下药，默认实现里认为线程可能阻塞在taskQueue上，因此我们尝试压入一个任务以尝试唤醒它。
	 */
	protected void wakeUp() {
		assert !inEventLoop();
		taskQueue.offer(WAKE_UP_TASK);
	}

	/**
	 * 中断当前运行的线程，用于唤醒线程。
	 * 如果子类阻塞在taskQueue之外的的地方，但是可以通过中断唤醒线程时，那么可以选择中断。
	 *
	 * Interrupt the current running {@link Thread}.
	 */
	protected void interruptThread() {
		assert !inEventLoop();
		thread.interrupt();
	}

	@Nonnull
	@Override
	public List<Runnable> shutdownNow() {
		for (;;){
			int oldState = stateHolder.get();
			if (isShutdown0(oldState)) {
				return Collections.emptyList();
			}
			if (stateHolder.compareAndSet(oldState, ST_SHUTDOWN)) {
				// 停止所有任务
			 	// 注意：这个时候仍然可能有线程尝试添加任务，需要在添加任务那里进行处理
				LinkedList<Runnable> haltTasks = new LinkedList<>();
				CollectionUtils.drainQueue(taskQueue, haltTasks);
				haltTasks.removeIf(task -> task == WAKE_UP_TASK);

				// 确保线程可进入终止状态
				ensureThreadTerminable(oldState);
				return haltTasks;
			}
		}
	}

	@Override
	public boolean isShuttingDown() {
		return isShuttingDown0(stateHolder.get());
	}

	private static boolean isShuttingDown0(int state) {
		return state >= ST_SHUTTING_DOWN;
	}

	@Override
	public boolean isShutdown() {
		return isShutdown0(stateHolder.get());
	}

	private static boolean isShutdown0(int state) {
		return state >= ST_SHUTDOWN;
	}

	@Override
	public boolean isTerminated() {
		return stateHolder.get() == ST_TERMINATED;
	}

	@Override
	public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
		return terminationFuture.await(timeout, unit);
	}

	@Override
	public ListenableFuture<?> terminationFuture() {
		return terminationFuture;
	}

	// -------------------------------------------- 任务调度，事件循环 -----------------------------------

	/**
	 * 在开启事件循环之前的初始化动作
	 */
	protected void init() {

	}

	/**
	 * 子类自己决定如何实现事件循环.
	 * @apiNote
	 * 子类实现应该是一个死循环方法，并在适当的时候调用{@link #confirmShutdown()}确认是否需要退出循环。
	 * 子类可以有更多的判断，但是至少需要调用{@link #confirmShutdown()}确定是否需要退出。
	 */
	protected abstract void loop();

	/**
	 * 确认是否需要立即退出事件循环，即是否可以立即退出{@link #loop()}方法。
	 *
	 * Confirm that the shutdown if the instance should be done now!
	 */
	protected final boolean confirmShutdown() {
		// 用于EventLoop确认自己是否应该退出，不应该由外部线程调用
		assert inEventLoop();

		if (!isShuttingDown()) {
			return false;
		}

		// shuttingDown状态下，已不会接收新的任务，执行完当前所有未执行的任务就可以退出了。

		runAllTasks();

		return true;
	}

	/**
	 * 在退出事件循环之前的清理动作。
	 */
	protected void clean() {

	}

	/**
	 * 工作者线程
	 *
	 * 两阶段终止模式 --- 在终止前进行清理操作，安全的关闭线程不是一件容易的事情。
	 */
	private class Worker implements Runnable{

		@Override
		public void run() {
			try {
				init();

				loop();
			} catch (Throwable e) {
				if (ConcurrentUtils.isInterrupted(e)) {
					logger.info("thread exit due to interrupted!", e);
				} else {
					logger.error("thread exit due to exception!", e);
				}
			} finally {
				// 如果是非正常退出，需要切换到关闭状态
				advanceRunState(ST_SHUTTING_DOWN);
				try {
					// 非正常退出下也尝试执行完所有的任务 - 当然这也不是很安全
					// Run all remaining tasks and shutdown hooks.
					for (;;) {
						if (confirmShutdown()) {
							break;
						}
					}
				} finally {
					// 退出前进行必要的清理，释放系统资源
					try {
						clean();
					} finally {
						terminationFuture.setSuccess(null);
					}
				}
			}
		}
	}

	/**
	 * 将运行状态转换为给定目标，或者至少保留给定状态。
	 * 参考自{@code ThreadPoolExecutor#advanceRunState}
	 *
	 * @param targetState 期望的目标状态， {@link #ST_SHUTTING_DOWN} 或者 {@link #ST_SHUTDOWN}
	 */
	private void advanceRunState(int targetState) {
		for (;;) {
			int oldState = stateHolder.get();
			if (runStateAtLeast(oldState, targetState) || stateHolder.compareAndSet(oldState, targetState))
				break;
		}
	}

	@Override
	public void execute(@Nonnull Runnable task) {
		// 其它线程添加任务，需要先确保executor已启动过,自己添加任务的话，自然已经启动过了
		if (!inEventLoop()) {
			// 确保线程启动
			ensureStarted();
		}

		addTask(task);

	}

	/**
	 * 尝试添加一个任务到任务队列
	 * @param task 期望运行的任务
	 */
	private void addTask(@Nonnull Runnable task) {
		// 1. 在检测到未关闭的状态下尝试压入队列
		if (!isShuttingDown() && taskQueue.offer(task)) {
			// 2. 压入队列是一个过程！在压入队列的过程中，executor的状态可能改变，因此必须再次校验 - 以判断线程是否在任务压入队列之后已经开始关闭了
			// remove失败表示executor已经处理了该任务或已经被强制停止(shutdownNow)
			if (isShuttingDown() && taskQueue.remove(task)) {
				reject(task);
			}
		} else {
			// executor已关闭 或 压入队列失败，拒绝
			reject(task);
		}
	}

	/**
	 * 确保线程已启动。
	 *
	 * 外部线程提交任务后需要保证线程已启动。
	 */
	private void ensureStarted() {
		int state = stateHolder.get();
		if (state == ST_NOT_STARTED) {
			if (stateHolder.compareAndSet(ST_NOT_STARTED, ST_STARTED)) {
				thread.start();
			}
		}
	}

	/**
	 * 确保线程可终止。
	 * - terminable
	 * TODO 这个需要仔细思考
	 * @param oldState 切换到shutdown之前的状态
	 */
	private void ensureThreadTerminable(int oldState) {
		if (oldState == ST_NOT_STARTED) {
			stateHolder.set(ST_TERMINATED);
			terminationFuture.trySuccess(null);
		} else if (oldState == ST_STARTED) {
			if (!inEventLoop()) {
				// 不确定是活跃状态
				wakeUp();
			}
			// else 当前是活跃状态
		}
	}

	/**
	 * 阻塞式地从阻塞队列中获取一个任务。
	 *
	 * @return 如果executor被唤醒或被中断，则返回null
	 */
	@Nullable
	protected final Runnable takeTask() {
		assert inEventLoop();
		try {
			Runnable task = taskQueue.take();
			// 被唤醒
			if (task == WAKE_UP_TASK) {
				return null;
			} else {
				return task;
			}
		} catch (InterruptedException ignore) {
			// 被中断唤醒 wake up
		}
		return null;
	}

	/**
	 * 从任务队列中尝试获取一个有效任务
	 *
	 * @see Queue#poll()
	 * @return 如果没有课执行任务则返回null
	 */
	@Nullable
	protected Runnable pollTask() {
		assert inEventLoop();
		for (;;) {
			Runnable task = taskQueue.poll();
			if (task == WAKE_UP_TASK) {
				continue;
			}
			return task;
		}
	}

	/**
	 * @see Queue#peek()
	 */
	@Nullable
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
	 * Offers the task to the associated {@link RejectedExecutionHandler}.
	 *
	 * @param task to reject.
	 */
	protected final void reject(@Nonnull Runnable task) {
		rejectedExecutionHandler.rejected(task, this);
	}

	/**
	 * @see Queue#remove(Object)
	 */
	protected boolean removeTask(@Nonnull Runnable task) {
		return taskQueue.remove(task);
	}

	/**
	 * 将任务队列中的任务拉取到线程本地缓存队列中
	 * @param taskQueue 任务队列
	 * @return the number of elements transferred
	 */
	protected final int pollTaskFrom(BlockingQueue<Runnable> taskQueue) {
		return taskQueue.drainTo(cacheQueue, CACHE_QUEUE_CAPACITY);
	}

	/**
	 * 运行任务队列中当前所有的任务
	 *
	 * @return 至少有一个任务执行时返回true。
	 */
	protected boolean runAllTasks() {
		assert inEventLoop();

		boolean ranAtLeastOne = false;
		while (pollTaskFrom(taskQueue) > 0){
			for (Runnable runnable : cacheQueue) {
				if (runnable == WAKE_UP_TASK) {
					continue;
				}
				safeExecute(runnable);
				ranAtLeastOne = true;
			}
			cacheQueue.clear();
		}
		return ranAtLeastOne;
	}

	/**
	 * 尝试运行任务队列中当前所有的任务。
	 *
	 * @param max 执行的最大任务数，避免执行任务耗费太多时间。
	 * @return 至少有一个任务执行时返回true。
	 */
	protected boolean runAllTasks(int max) {
		assert inEventLoop();

		boolean ranAtLeastOne = false;
		int runTaskNum = 0;
		while (pollTaskFrom(taskQueue) > 0) {
			for (Runnable runnable : cacheQueue) {
				if (runnable == WAKE_UP_TASK) {
					continue;
				}
				safeExecute(runnable);
				ranAtLeastOne = true;
				runTaskNum++;
			}
			cacheQueue.clear();

			if (runTaskNum >= max) {
				break;
			}
		}
		return ranAtLeastOne;
	}
}
