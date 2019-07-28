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

package com.wjybxx.fastjgame.concurrent.disruptor;

import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.LifecycleAware;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.wjybxx.fastjgame.annotation.UnstableApi;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandler;
import com.wjybxx.fastjgame.concurrent.*;
import com.wjybxx.fastjgame.utils.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * 基于Disruptor的事件循环，<b>该事件循环不会阻塞</b>。
 * 使用{@link DisruptorEventLoop}时，应尽量使用{@link #publishEvent(EventType, EventParam)}，而不是{@link #execute(Runnable)}
 *
 * 代码和{@link SingleThreadEventLoop}很像，类似的代码写两遍还是有点难受，但是又不完全一样。。。。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/24
 * github - https://github.com/hl845740757
 */
public class DisruptorEventLoop extends AbstractEventLoop {

	private static final Logger logger = LoggerFactory.getLogger(DisruptorEventLoop.class);

	/** 默认ringBuffer大小 */
	static final int DEFAULT_RING_BUFFER_SIZE = SystemUtils.getProperties().getAsInt("DisruptorEventLoop.ringBufferSize", 16 * 1024);

	// 线程的状态
	/** 初始状态，未启动状态 */
	private static final int ST_NOT_STARTED = 1;
	/** 已启动状态，运行状态 */
	private static final int ST_STARTED = 2;
	/** 正在关闭状态，正在尝试执行最后的任务 */
	private static final int ST_SHUTTING_DOWN = 3;
	/** 已关闭状态，正在进行最后的清理 */
	private static final int ST_SHUTDOWN = 4;
	/** 终止状态(二阶段终止模式 - 已关闭状态下进行最后的清理，然后进入终止状态) */
	private static final int ST_TERMINATED = 5;

	/** Disruptor线程 */
	private volatile Thread thread;
	/** 任务拒绝策略 */
	private final RejectedExecutionHandler rejectedExecutionHandler;
	/** 事件处理器 */
	private final EventHandler eventHandler;
	/** disruptor */
	private final Disruptor<Event> disruptor;

	/**
	 * execute提交的任务，主要使用event，这里不应该堆积大量的任务。
	 */
	private final ConcurrentLinkedQueue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();

	/** 事件队列 */
	private final RingBuffer<Event> ringBuffer;

	/** 线程状态 */
	private final AtomicInteger stateHolder = new AtomicInteger(ST_NOT_STARTED);

	/** 线程终止future */
	private final Promise<?> terminationFuture = new DefaultPromise(GlobalEventLoop.INSTANCE);

	/**
	 * @param parent 容器节点
	 * @param threadFactory 线程工厂
	 * @param eventHandler 事件处理器
	 */
	public DisruptorEventLoop(@Nullable EventLoopGroup parent, ThreadFactory threadFactory, EventHandler eventHandler) {
		this(parent, threadFactory, eventHandler, DEFAULT_RING_BUFFER_SIZE, RejectedExecutionHandlers.reject());
	}

	/**
	 * @param parent 容器节点
	 * @param threadFactory 线程工厂
	 * @param eventHandler 事件处理器
	 * @param ringBufferSize 事件缓冲区大小，当ringBuffer填充满以后，{@link #publishEvent(EventType, EventParam)}会阻塞
	 */
	public DisruptorEventLoop(@Nullable EventLoopGroup parent, ThreadFactory threadFactory, EventHandler eventHandler, int ringBufferSize) {
		this(parent, threadFactory, eventHandler, ringBufferSize, RejectedExecutionHandlers.reject());
	}

	/**
	 * @param parent 容器节点
	 * @param threadFactory 线程工厂
	 * @param eventHandler 事件处理器
	 * @param ringBufferSize 事件缓冲区大小，当ringBuffer填充满以后，{@link #publishEvent(EventType, EventParam)}会阻塞。
	 * @param rejectedExecutionHandler {@link #execute(Runnable)}提交任务失败时的处理策略
	 */
	public DisruptorEventLoop(@Nullable EventLoopGroup parent, ThreadFactory threadFactory, EventHandler eventHandler, int ringBufferSize, RejectedExecutionHandler rejectedExecutionHandler) {
		super(parent);
		this.rejectedExecutionHandler = rejectedExecutionHandler;
		this.eventHandler = eventHandler;

		this.disruptor = new Disruptor<Event>(new EventFactory(),
				ringBufferSize,
				new InnerThreadFactory(threadFactory),
				ProducerType.MULTI,
				new SleepingWaitExtendStrategy(this));

		// 加一层封装，避免EventLoop暴露Disruptor相关接口
		disruptor.handleEventsWith(new InnerEventHandler(this));

		this.ringBuffer = disruptor.getRingBuffer();
	}

	@Override
	public boolean inEventLoop() {
		return thread == Thread.currentThread();
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
	public ListenableFuture<?> terminationFuture() {
		return terminationFuture;
	}

	@Override
	public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
		return terminationFuture.await(timeout, unit);
	}

	@Override
	public void shutdown() {
		for (;;) {
			int oldState = stateHolder.get();
			if (isShuttingDown0(oldState)) {
				return;
			}
			if (stateHolder.compareAndSet(oldState, ST_SHUTTING_DOWN)){
				// 其它线程关闭EventLoop时需要确保EventLoop可关闭
				if (!inEventLoop()) {
					// 确保
					ensureThreadTerminable(oldState);
				}
				return;
			}
		}
	}

	@Override
	public void execute(@Nonnull Runnable task) {
		if (!inEventLoop()) {
			// 其它线程提交任务时，需要确保线程已启动
			ensureThreadStarted();
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
			// remove失败表示executor已经处理了该任务或已经被强制停止(shutdownNow) --- shutdownNow方法已删除
			if (isShuttingDown() && taskQueue.remove(task)) {
				reject(task);
			}
		} else {
			// executor已关闭 或 压入队列失败，拒绝
			reject(task);
		}
	}

	/**
	 * 发布一个事件，当没有足够空间时会阻塞直到有空间可用。
	 * (待优化)
	 * @param eventType 事件类型
	 * @param eventParam 事件对应的参数
	 */
	@UnstableApi
	public void publishEvent(EventType eventType, EventParam eventParam) {
		if (inEventLoop()) {
			// 防止死锁，自己发布事件时，如果没有足够空间，会导致死锁，需要压入队列
			taskQueue.offer(new EventTask(new Event(eventType, eventParam)));
		} else {
			ensureThreadStarted();
			// 直接调用next()的情况下，如果disruptor已关闭则可能死锁，消费者不再消费，生产者继续放就会死锁
			// TODO 优化？还是说就用next()，关闭的时候注意点？
			for (int tryTimes = 1; !isShuttingDown(); tryTimes++) {
				try {
					long sequence = ringBuffer.tryNext();
					try {
						Event event = ringBuffer.get(sequence);
						event.setType(eventType);
						event.setParam(eventParam);
					} finally {
						ringBuffer.publish(sequence);
					}
					return;
				} catch (InsufficientCapacityException ignore) {
					// 最多睡眠1毫秒
					int sleepTimes = (1 + ThreadLocalRandom.current().nextInt(Math.min(100, tryTimes))) * 10000;
					LockSupport.parkNanos(sleepTimes);
				}
			}
			throw new RejectedExecutionException("EventType " + eventType.name());
		}
	}

	protected final void reject(@Nonnull Runnable task) {
		rejectedExecutionHandler.rejected(task, this);
	}
	// ----------------------------- 生命周期begin ------------------------------

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

	/**
	 * 确保线程已启动
	 */
	private void ensureThreadStarted() {
		int state = stateHolder.get();
		if (state == ST_NOT_STARTED) {
			if (stateHolder.compareAndSet(ST_NOT_STARTED, ST_STARTED)) {
				disruptor.start();
			}
		}
	}

	/**
	 * 确保线程可终止。
	 * - terminable
	 * @param oldState 切换到shutdown之前的状态
	 */
	private void ensureThreadTerminable(int oldState) {
		if (oldState == ST_NOT_STARTED) {
			stateHolder.set(ST_TERMINATED);
			terminationFuture.trySuccess(null);
		}
		// else 其它状态下不应该阻塞，不需要唤醒
	}

	private void onStart() {
		if (!inEventLoop()){
			// 非真正启动，earlyExit
			return;
		}

		init();
		eventHandler.startUp(this);
	}

	/**
	 * 事件循环线程启动时的初始化操作。
	 */
	protected void init() {

	}

	private void onEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
		onEvent(event);
		if (endOfBatch) {
			// 处理一批事件，执行一批任务
			runAllTasks();
			// 检查是否需要关闭
			confirmShutdown();
		}
	}

	private void onEvent(Event event) {
		try {
			eventHandler.onEvent(event);
		} catch (Exception e) {
			logger.warn("onEvent caught exception.", e);
		} finally {
			event.close();
		}
	}

	void onWaitEvent() {
		try {
			eventHandler.onWaitEvent();
		} finally {
			// 等待期间，执行所有任务
			runAllTasks();
			// 检查是否需要关闭
			confirmShutdown();
		}
	}

	private void onShutdown() {
		if (!inEventLoop()){
			// 非真正退出，earlyExit
			return;
		}

		try {
			eventHandler.shutdown();
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

	/**
	 * 用于确认是否应该立即关闭
	 * @return 如果返回true，那么应该立即停止事件循环，准备退出。
	 */
	protected final boolean confirmShutdown() {
		// 用于EventLoop确认自己是否应该退出，不应该由外部线程调用
		assert inEventLoop();

		// 它放前面是因为更可能出现
		if (!isShuttingDown()) {
			return false;
		}

		// 它只在关闭阶段出现
		if (isShutdown()) {
			return true;
		}
		// shuttingDown状态下，已不会接收新的任务，执行完当前所有未执行的任务就可以退出了。
		runAllTasks();

		// TODO 对于未处理完的事件，还没想到好的办法处理，继续执行好像很危险，可能导致生产者继续生产

		// 切换至SHUTDOWN状态，准备执行最后的清理动作
		advanceRunState(ST_SHUTDOWN);

		// 真正关闭disruptor，不可以调用shutdown，否则可能导致死锁
		disruptor.halt();

		return true;
	}

	/**
	 * 线程退出前的清理动作
	 */
	protected void clean() {

	}

	/**
	 * 运行任务队列中当前所有的任务
	 */
	protected void runAllTasks() {
		Runnable task;
		while ((task = taskQueue.poll()) != null) {
			safeExecute(task);
		}
	}

	// ------------------------------ 生命周期end --------------------------------

	private class InnerThreadFactory implements ThreadFactory {

		private final ThreadFactory threadFactory;

		private InnerThreadFactory(ThreadFactory threadFactory) {
			this.threadFactory = threadFactory;
		}

		@Override
		public Thread newThread(@Nonnull Runnable r) {
			Thread thread = threadFactory.newThread(r);
			// 捕获运行线程
			DisruptorEventLoop.this.thread = thread;
			return thread;
		}
	}

	/** EventHandler内部实现，避免{@link DisruptorEventLoop}对外暴露这些接口 */
	private static class InnerEventHandler implements com.lmax.disruptor.EventHandler<Event>, LifecycleAware {

		private final DisruptorEventLoop disruptorEventLoop;

		private InnerEventHandler(DisruptorEventLoop disruptorEventLoop) {
			this.disruptorEventLoop = disruptorEventLoop;
		}

		@Override
		public void onStart() {
			disruptorEventLoop.onStart();
		}

		@Override
		public void onEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
			disruptorEventLoop.onEvent(event, sequence, endOfBatch);
		}

		@Override
		public void onShutdown() {
			disruptorEventLoop.onShutdown();
		}
	}

	/** 包装对象，用于自身发布事件时。 */
	private class EventTask implements Runnable {

		private final Event event;

		private EventTask(Event event) {
			this.event = event;
		}

		@Override
		public void run() {
			onEvent(event);
		}
	}

}
