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

import com.lmax.disruptor.LifecycleAware;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.wjybxx.fastjgame.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于Disruptor的事件循环
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/24
 * github - https://github.com/hl845740757
 */
public class DisruptorEventLoop extends AbstractEventLoop {

	private static final Logger logger = LoggerFactory.getLogger(DisruptorEventLoop.class);

	private static final int CACHE_QUEUE_CAPACITY = 6 * 84;

	private static final int ST_NOT_START = 1;
	private static final int ST_STARTED = 2;
	private static final int ST_SHUTDOWN = 3;

	/** Disruptor线程 */
	private volatile Thread thread;
	/** 线程工厂 */
	private final ThreadFactory threadFactory;
	/** 事件处理器 */
	private final EventHandler eventHandler;
	private final ThreadCaptureHandler threadCapture;
	private final Disruptor<Event> disruptor;

	/** execute提交的任务 */
	private final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
	/**
	 * 缓存队列，用于批量的将{@link #taskQueue}中的任务拉取到本地线程下，减少锁竞争，
	 */
	private final List<Runnable> cacheQueue = new ArrayList<>(CACHE_QUEUE_CAPACITY);

	/** 线程状态 */
	private final AtomicInteger stateHolder = new AtomicInteger(ST_NOT_START);

	/** 线程终止future */
	private final Promise<?> terminationFuture = new DefaultPromise(GlobalEventLoop.INSTANCE);

	public DisruptorEventLoop(@Nullable EventLoopGroup parent, ThreadFactory threadFactory, EventHandler eventHandler) {
		super(parent);
		this.threadFactory = threadFactory;
		this.eventHandler = eventHandler;
		this.threadCapture = new ThreadCaptureHandler(this);
		// 临时
		this.disruptor = new Disruptor<Event>(new EventFactory(), 8 * 1024, threadFactory,
				ProducerType.MULTI,
				new SleepingWaitExtendStrategy(this));
	}

	@Override
	public boolean inEventLoop() {
		return thread == Thread.currentThread();
	}

	@Override
	public boolean isShuttingDown() {
		return stateHolder.get() >= ST_SHUTDOWN;
	}

	@Override
	public ListenableFuture<?> terminationFuture() {
		return terminationFuture;
	}

	@Override
	public void shutdown() {
		if (inEventLoop()) {
			eventHandler.shutdown();
			// shutdown是阻塞方法，当前线程调用会导致死锁
			disruptor.halt();
		} else {

		}
	}

	@Override
	public boolean isShutdown() {
		return stateHolder.get() >= ST_SHUTDOWN;
	}

	@Override
	public boolean isTerminated() {
		return stateHolder.get() >= ST_SHUTDOWN;
	}

	@Override
	public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
		return terminationFuture.await(timeout, unit);
	}

	@Override
	public void execute(@Nonnull Runnable command) {
		taskQueue.offer(command);
	}

	/**
	 * 发布一个事件
	 *
	 * @param eventType 事件类型
	 * @param eventParam 事件对应的参数
	 */
	public void publishEvent(EventType eventType, EventParam eventParam) {
		if (inEventLoop()) {
			// 防止死锁，自己发布事件时，如果没有足够空间，会导致死锁
			taskQueue.offer(new EventTask(new Event(eventType, eventParam)));
		} else {
			// TODO
		}
	}

	// ----------------------------- 生命周期begin ------------------------------

	private void onStart() {
		// 捕获线程
		thread = Thread.currentThread();
		eventHandler.startUp();
	}

	private void onEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
		onEvent(event);
		if (endOfBatch) {
			runAllTasks();
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
				safeExecute(runnable);
			}
			ranAtLeastOne = true;
			cacheQueue.clear();
		}
		return ranAtLeastOne;
	}

	void onWaitEvent() {
		eventHandler.onWaitEvent();
		runAllTasks();
	}

	private void onShutdown() {
		eventHandler.shutdown();
		clean();

	}

	private void clean() {

	}
	// ------------------------------ 生命周期end --------------------------------

	/** EventHandler内部实现，避免{@link DisruptorEventLoop}对外暴露这些接口 */
	private static class ThreadCaptureHandler implements com.lmax.disruptor.EventHandler<Event>, LifecycleAware {

		private final DisruptorEventLoop disruptorEventLoop;

		private ThreadCaptureHandler(DisruptorEventLoop disruptorEventLoop) {
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

	/** 包装对象 */
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
