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


import com.wjybxx.fastjgame.utils.LocalNumberUtils;

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

	/** 小时 0-23 */
	public final int hour;
	/** 分钟 0-59 */
	public final int min;
	/** 秒 0-59 */
	public final int sec;

	public DailyTimeOffset(int hour, int min) {
		this(hour, min, 0);
	}

	public DailyTimeOffset(int hour, int min, int sec) {
		this.hour = hour;
		this.min = min;
		this.sec = sec;
	}

	/**
	 * 转换为相对于00:00的偏移量
	 * @return offset
	 */
	@Override
	public long toOffset() {
		return hour * TimeUtils.HOUR + min * TimeUtils.MIN + sec * TimeUtils.SEC;
	}

	@Override
	public String toString() {
		return "DailyTimeOffset{" +
				"hour=" + hour +
				", min=" + min +
				", sec=" + sec +
				'}';
	}

	/**
	 * 从配置中解析出小时和分钟
	 * @param confParam 表格配置，HH:mm 或 HH:mm:ss
	 * @return HourAndMin
	 */
	public static DailyTimeOffset parseFromConf(String confParam) {
		String[] split = confParam.split(":");
		if (split.length == 2) {
			int hour = LocalNumberUtils.toInt(split[0]);
			int minute = LocalNumberUtils.toInt(split[1]);
			return new DailyTimeOffset(hour, minute, 0);
		} else if (split.length == 3){
			int hour = LocalNumberUtils.toInt(split[0]);
			int minute = LocalNumberUtils.toInt(split[1]);
			int sec = LocalNumberUtils.toInt(split[2]);
			return new DailyTimeOffset(hour, minute, sec);
		} else {
			throw new ConfigFormatException("Unsupported HourAndMin format " + confParam);
		}
	}
}
