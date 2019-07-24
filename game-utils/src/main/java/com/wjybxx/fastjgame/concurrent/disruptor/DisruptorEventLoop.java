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

import com.wjybxx.fastjgame.concurrent.AbstractEventLoop;
import com.wjybxx.fastjgame.concurrent.EventLoopGroup;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * 基于Disruptor的事件循环
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/24
 * github - https://github.com/hl845740757
 */
public class DisruptorEventLoop extends AbstractEventLoop {

	/** Disruptor线程 */
	private volatile Thread thread;
	/** 线程工厂 */
	private final ThreadFactory threadFactory;
	/** 事件处理器 */
	private final EventHandler eventHandler;

	public DisruptorEventLoop(@Nullable EventLoopGroup parent, ThreadFactory threadFactory) {
		super(parent);
		this.threadFactory = threadFactory;
		this.eventHandler = new InnerEventHandler(this);
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
	public ListenableFuture<?> terminationFuture() {
		return null;
	}

	@Override
	public void shutdown() {

	}

	@Nonnull
	@Override
	public List<Runnable> shutdownNow() {
		return Collections.emptyList();
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
	public void execute(@Nonnull Runnable command) {

	}

	/**
	 * 发布一个事件
	 *
	 * @param eventType 事件类型
	 * @param eventParam 事件对应的参数
	 */
	public void publishEvent(EventType eventType, EventParam eventParam) {

	}

	// ----------------------------- 生命周期begin ------------------------------

	private void onStart() {
		// 捕获线程
		thread = Thread.currentThread();
	}

	private void onEvent(Event event, long sequence, boolean endOfBatch) throws Exception {

	}

	private void tryLoop() {

	}

	private void onWaitEvent() {

	}

	private void onShutdown() {

	}
	// ------------------------------ 生命周期end --------------------------------

	/** EventHandler内部实现，避免{@link DisruptorEventLoop}对外暴露这些接口 */
	private static class InnerEventHandler implements EventHandler{

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
		public void tryLoop() {
			disruptorEventLoop.tryLoop();
		}

		@Override
		public void onWaitEvent() {
			disruptorEventLoop.onWaitEvent();
		}

		@Override
		public void onShutdown() {
			disruptorEventLoop.onShutdown();
		}
	}

}
