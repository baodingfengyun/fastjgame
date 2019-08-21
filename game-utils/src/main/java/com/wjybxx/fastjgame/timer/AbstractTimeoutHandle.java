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
 * 抽象的只执行一次的timer系统
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/14
 * github - https://github.com/hl845740757
 */
public abstract class AbstractTimeoutHandle extends AbstractTimerHandle implements TimeoutHandle{

	private long timeout;

	protected AbstractTimeoutHandle(TimerSystem timerSystem, long createTimeMs, TimerTask timerTask, long timeout) {
		super(timerSystem, timerTask, createTimeMs);
		this.timeout = timeout;
	}

	@Override
	public long timeout() {
		return timeout;
	}

	@Override
	public boolean setTimeoutImmediately(long timeout) {
		if (isTerminated()) {
			return false;
		}
		this.timeout = timeout;
		adjust();
		return true;
	}

	/**
	 * 当修改完timeout的时候，进行必要的调整，此时还未修改下次执行时间。
	 */
	protected abstract void adjust();

	@Override
	protected final void init(long curTimeMs) {
		updateNextExecuteTime();
	}

	/** 更新下一次的执行时间 */
	protected final void updateNextExecuteTime() {
		setNextExecuteTimeMs(createTimeMs() + timeout);
	}

	@Override
	protected final void afterExecute(long curTimeMs) {
		// 执行一次之后就结束了。
		setTerminated();
	}
}
