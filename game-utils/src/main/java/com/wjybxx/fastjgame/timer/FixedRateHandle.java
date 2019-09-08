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

import javax.annotation.concurrent.NotThreadSafe;

/**
 * 固定频率的定时器的句柄。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/7
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public interface FixedRateHandle extends TimerHandle{

	/**
	 * 指定的初始延迟，不可修改。
	 *
	 * @return 毫秒
	 */
	long initialDelay();

	/**
	 * 指定的执行周期。
	 *
	 * @return 毫秒
	 */
	long period();

	/**
	 * 尝试修改timer的执行周期。
	 * @apiNote
	 * 注意：该方法实现不保证立即生效，但保证其后续的调用的周期。
	 *
	 * @param period 执行周期
	 * @return 当且仅当成功修改TimerTask的执行周期时返回true，否则返回false(比如已取消，或已终止)。
	 */
	boolean setPeriod(long period);

	/**
	 * 尝试修改timer的执行周期，修改成功时立即生效!
	 *
	 * @param period 执行周期
	 * @return 当且仅当成功修改TimerTask的执行周期时返回true，否则返回false(比如已取消，或已终止)。
	 */
	boolean setPeriodImmediately(long period);
}
