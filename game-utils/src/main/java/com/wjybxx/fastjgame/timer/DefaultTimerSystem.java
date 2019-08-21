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

package com.wjybxx.fastjgame.timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * 定时器系统的默认实现。
 * 注意查看测试用例 {@code TimerSystemTest}的输出结果。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/7
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class DefaultTimerSystem implements TimerSystem{

	private static final Logger logger = LoggerFactory.getLogger(DefaultTimerSystem.class);

	/** 默认空间大小，使用JDK默认大小 */
	private static final int DEFAULT_INITIAL_CAPACITY = 11;
	/** 默认的时间提供器 */
	private static final SystemTimeProvider DEFAULT_TIME_PROVIDER = SystemTimeProviders.getRealtimeProvider();

	/** timer队列 */
	private final PriorityQueue<AbstractTimerHandle> timerQueue;
	/** 用于获取当前时间 */
	private final SystemTimeProvider timeProvider;

	/** 是否已关闭 */
	private boolean closed = false;

	public DefaultTimerSystem() {
		this(DEFAULT_TIME_PROVIDER, DEFAULT_INITIAL_CAPACITY);
	}

	public DefaultTimerSystem(int initCapacity) {
		this(DEFAULT_TIME_PROVIDER, initCapacity);
	}

	public DefaultTimerSystem(SystemTimeProvider timeProvider) {
		this(timeProvider, DEFAULT_INITIAL_CAPACITY);
	}

	/**
	 * @param timeProvider 时间提供函数
	 * @param initCapacity 初始timer空间
	 */
	public DefaultTimerSystem(SystemTimeProvider timeProvider, int initCapacity) {
		timerQueue = new PriorityQueue<>(initCapacity, AbstractTimerHandle.timerComparator);
		this.timeProvider = timeProvider;
	}

	@Override
	public void tick() {
		if (closed){
			return;
		}
		tickTimer();
	}

	/**
	 * 安全的执行timer的回调。
	 * @param timerHandle timer
	 * @param curMillTime 当前时间戳
	 */
	@SuppressWarnings("unchecked")
	private static void callbackSafely(AbstractTimerHandle timerHandle, long curMillTime) {
		try {
			timerHandle.timerTask().run(timerHandle);
			timerHandle.afterExecute(curMillTime);
		} catch (Exception e){
			// 取消执行
			timerHandle.setTerminated();
			logger.warn("timer callback caught exception!", e);
		}
	}

	/**
	 * 检查周期性执行的timer
	 */
	private void tickTimer() {
		AbstractTimerHandle timerHandle;
		final long curMillTime = timeProvider.getSystemMillTime();

		while ((timerHandle = timerQueue.peek()) != null) {
			// 优先级最高的timer不需要执行，那么后面的也不需要执行
			if (curMillTime < timerHandle.getNextExecuteTimeMs()) {
				return;
			}
			// 先弹出队列
			timerQueue.poll();

			do {
				callbackSafely(timerHandle, curMillTime);
				// 可能由于延迟导致需要执行多次(可以避免在当前轮反复压入弹出)，也可能在执行回调之后被取消了。do while用的不甚习惯...
			} while (!timerHandle.isTerminated() && curMillTime >= timerHandle.getNextExecuteTimeMs());

			// 出现异常的timer会被取消，不再压入队列

			if (!timerHandle.isTerminated()){
				// 如果未取消的话，压入队列稍后执行
				timerQueue.offer(timerHandle);
			}
		}
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}
		closeQueue(timerQueue);
		closed = true;
	}

	@Nonnull
	@Override
	public SystemTimeProvider timeProvider() {
		return timeProvider;
	}

	/**
	 * 关闭一个队列的全部timer
	 * @param queue timer所在的队列
	 */
	private static void closeQueue(Queue<AbstractTimerHandle> queue) {
		AbstractTimerHandle handle;
		while ((handle = queue.poll()) != null) {
			handle.setTerminated();
		}
	}

	/**
	 * 将timer压入队列，并进行适当的初始化。
	 */
	private <T extends AbstractTimerHandle> T tryAddTimerAndInit(Queue<AbstractTimerHandle> queue, T timerHandle) {
		if (closed) {
			// timer系统已关闭，不压入队列
			timerHandle.setTerminated();
			return timerHandle;
		} else {
			timerHandle.init(timerHandle.createTimeMs());
			queue.add(timerHandle);
			return timerHandle;
		}
	}

	@Nonnull
	@Override
	public TimeoutHandle newTimeout(long timeout, @Nonnull TimerTask<TimeoutHandle> task) {
		TimeoutHandleImp timeoutHandleImp = new TimeoutHandleImp(this, task, timeProvider.getSystemMillTime(), timeout);
		return tryAddTimerAndInit(timerQueue, timeoutHandleImp);
	}

	@Nonnull
	@Override
	public FixedDelayHandle newFixedDelay(long initialDelay, long delay, @Nonnull TimerTask<FixedDelayHandle> task) {
		AbstractFixedDelayHandle.ensureDelay(delay);
		FixedDelayHandleImp fixedDelayHandleImp = new FixedDelayHandleImp(this, task, timeProvider.getSystemMillTime(), initialDelay, delay);
		return tryAddTimerAndInit(timerQueue, fixedDelayHandleImp);
	}

	@Nonnull
	@Override
	public FixedRateHandle newFixRate(long initialDelay, long period, @Nonnull TimerTask<FixedRateHandle> task) {
		AbstractFixRateHandle.ensurePeriod(period);
		FixRateHandleImp fixRateHandleImp = new FixRateHandleImp(this, task, timeProvider.getSystemMillTime(), initialDelay, period);
		return tryAddTimerAndInit(timerQueue, fixRateHandleImp);
	}

	// ------------------------------------- handle实现 --------------------------------------------

	private static class TimeoutHandleImp extends AbstractTimeoutHandle {

		TimeoutHandleImp(DefaultTimerSystem timerSystem, TimerTask timerTask, long createTimeMs,
						 long timeout) {
			super(timerSystem, createTimeMs, timerTask, timeout);
		}

		@Nonnull
		@Override
		public DefaultTimerSystem timerSystem() {
			return (DefaultTimerSystem) super.timerSystem();
		}

		@Override
		protected void doCancel() {
			timerSystem().timerQueue.remove(this);
		}

		@Override
		protected void adjust() {
			DefaultTimerSystem.adjust(timerSystem(), this, TimeoutHandleImp::updateNextExecuteTime);
		}
	}

	private static class FixedDelayHandleImp extends AbstractFixedDelayHandle {

		FixedDelayHandleImp(DefaultTimerSystem timerSystem, TimerTask timerTask, long createTimeMs,
							long initialDelay, long delay) {
			super(timerSystem, createTimeMs, timerTask, initialDelay, delay);
		}

		@Nonnull
		@Override
		public DefaultTimerSystem timerSystem() {
			return (DefaultTimerSystem) super.timerSystem();
		}

		@Override
		protected void doCancel() {
			timerSystem().timerQueue.remove(this);
		}

		@Override
		protected void adjust() {
			DefaultTimerSystem.adjust(timerSystem(), this, FixedDelayHandleImp::updateNextExecuteTime);
		}
	}

	private static class FixRateHandleImp extends AbstractFixRateHandle{

		FixRateHandleImp(DefaultTimerSystem timerSystem, TimerTask timerTask, long createTimeMs,
						 long initialDelay, long period) {
			super(timerSystem, createTimeMs, timerTask, initialDelay, period);
		}

		@Nonnull
		@Override
		public DefaultTimerSystem timerSystem() {
			return (DefaultTimerSystem) super.timerSystem();
		}

		@Override
		protected void doCancel() {
			timerSystem().timerQueue.remove(this);
		}

		@Override
		protected void adjust() {
			DefaultTimerSystem.adjust(timerSystem(), this, FixRateHandleImp::updateNextExecuteTime);
		}
	}

	/**
	 * 调整handle在timerSystem中的优先级(暂时先删除再插入，如果切换底层数据结构，那么可能会修改)
	 * @param timerSystem 绑定的timerSystem
	 * @param handle 定时器句柄
	 * @param adjustAction 调整优先级的函数
	 * @param <T> 定时器句柄类型
	 */
	private static <T extends AbstractTimerHandle> void adjust(DefaultTimerSystem timerSystem, T handle, Consumer<T> adjustAction) {
		timerSystem.timerQueue.remove(handle);
		adjustAction.accept(handle);
		timerSystem.timerQueue.add(handle);
	}
}