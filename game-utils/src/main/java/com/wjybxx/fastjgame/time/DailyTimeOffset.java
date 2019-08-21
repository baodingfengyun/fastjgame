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


import com.wjybxx.fastjgame.utils.TimeUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalTime;

/**
 * 每日时间，一天内的时间偏移量。
 *
 * 时间和分钟，它是相对于00:00的一个时间偏移量。
 * 表格格式 HH:mm 或 HH:mm:ss
 * 它是一个不可变对象。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/7 22:50
 * github - https://github.com/hl845740757
 */
public class DailyTimeOffset implements TimeOffset{

	private final LocalTime time;

	public DailyTimeOffset(int hour, int min) {
		this(hour, min, 0);
	}

	public DailyTimeOffset(int hour, int min, int sec) {
		this(LocalTime.of(hour, min, sec));
	}

	public DailyTimeOffset(LocalTime time) {
		this.time = time;
	}

	/**
	 * 转换为相对于00:00的偏移量
	 * @return offset
	 */
	@Override
	public long toOffset() {
		return time.toSecondOfDay() * TimeUtils.SEC;
	}

	@Override
	public String toString() {
		return "DailyTimeOffset{" +
				"time=" + time +
				'}';
	}

	/**
	 * 从配置中解析出小时和分钟
	 * @param confParam 表格配置，HH:mm 或 HH:mm:ss
	 * @return HourAndMin
	 */
	public static DailyTimeOffset parseFromConf(String confParam) {
		int count = StringUtils.countMatches(confParam, ':');
		if (count == 1) {
			// count == 1 表示两段
			return new DailyTimeOffset(LocalTime.parse(confParam, TimeUtils.HH_MM));
		} else if (count == 2){
			// count ==2 表示3段
			return new DailyTimeOffset(LocalTime.parse(confParam, TimeUtils.HH_MM_SS));
		} else {
			throw new ConfigFormatException("Unsupported HourAndMin format " + confParam);
		}
	}
}
