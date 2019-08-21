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

/**
 * 抽象的固定频率的TimerHandle实现
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/14
 * github - https://github.com/hl845740757
 */
public abstract class AbstractFixRateHandle extends AbstractTimerHandle implements FixedRateHandle {

	private final long initialDelay;

	private long period;

	/** 理论上的上次执行时间 */
	private long logicLastExecuteTimeMs;

	protected AbstractFixRateHandle(TimerSystem timerSystem, long createTimeMs, TimerTask timerTask,
					 long initialDelay, long period) {
		super(timerSystem, timerTask, createTimeMs);
		this.initialDelay = initialDelay;
		this.period = period;
	}

	@Override
	public long initialDelay() {
		return initialDelay;
	}

	@Override
	public long period() {
		return period;
	}

	@Override
	public final boolean setPeriod(long period) {
		ensurePeriod(period);
		if (isTerminated()) {
			return false;
		} else {
			this.period = period;
			return true;
		}
	}

	@Override
	public boolean setPeriodImmediately(long period) {
		if (setPeriod(period)) {
			adjust();
			return true;
		} else {
			return false;
		}
	}

	@Override
	protected final void init(long curTimeMs) {
		logicLastExecuteTimeMs = curTimeMs + initialDelay;
		updateNextExecuteTime();
	}

	@Override
	protected final void afterExecute(long curTimeMs) {
		// 上次执行时间非真实时间，而是加上一个周期
		logicLastExecuteTimeMs += period;
		updateNextExecuteTime();
	}

	/** 更新下一次的执行时间 */
	protected final void updateNextExecuteTime() {
		setNextExecuteTimeMs(logicLastExecuteTimeMs + period);
	}

	/**
	 * 当修改完period的时候，进行必要的调整，此时还未修改下次执行时间。
	 * 注意：虽然三个抽象类中都有{@link #adjust()} {@link #updateNextExecuteTime()}两个函数，
	 * 但是不代表它们应该提炼到父类！
	 */
	@SuppressWarnings("JavaDoc")
	protected abstract void adjust();

	static void ensurePeriod(long period) {
		if (period <= 0) {
			throw new IllegalArgumentException("period " + period);
		}
	}
}
