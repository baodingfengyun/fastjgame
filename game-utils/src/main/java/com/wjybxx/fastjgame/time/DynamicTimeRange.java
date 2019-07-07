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

package com.wjybxx.fastjgame.time;


/**
 * 动态的时间段；
 * 子类实现必须是不可变对象，避免误用。
 * 为何做这个？cron表达式是一个过于全的东西，实际大多数都用不到，配置起来也不方便，游戏用到的只是很少的一部分。
 * 并不如自己实现的方便配置。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/7 22:50
 * github - https://github.com/hl845740757
 */
public interface DynamicTimeRange {

	/**
	 * 当前是否在有效时间段内
	 * @param curTimeMs 特定瞬时时间
	 * @return 当前处于时间段内返回true，否则返回false。
	 */
	default boolean isBetweenTimeRange(long curTimeMs) {
		return null != triggeringTimeRange(curTimeMs);
	}

	/**
	 * 获取指定时间戳触发的时间段。
	 * 如果返回null，你可能需要{@link #nextTriggerTimeRange(long)}
	 * @param timeMs 毫秒时间戳
	 * @return TimeRange nullable
	 */
	TimeRange triggeringTimeRange(long timeMs);

	/**
	 * 获取下一个即将触发的时间段。
	 * @param timeMs 毫秒时间戳
	 * @return TimeRange nullable
	 */
	TimeRange nextTriggerTimeRange(long timeMs);

	/**
	 * 获取上一个触发的时间段
	 * @param timeMs 毫秒时间戳
	 * @return TimeRange nullable
	 */
	TimeRange preTriggerTimeRange(long timeMs);

}
