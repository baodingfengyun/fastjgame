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

/**
 * 每周的某个时间段;
 * 使用1-7表示周一到周日，要符合人的正常思维。
 *
 * eg: 1_00:00-3_00:00 表示周一的00:00 到 周三的00:00
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/7 22:50
 * github - https://github.com/hl845740757
 */
public class WeeklyTimeRange extends LoopDynamicTimeRange<WeeklyTimeOffset> {

	public WeeklyTimeRange(WeeklyTimeOffset startNode, WeeklyTimeOffset endNode) {
		super(startNode, endNode);
	}

	@Override
	protected long getPeriod() {
		return TimeUtils.DAY * 7;
	}

	@Override
	protected long getPeriodStartTime(long curTimeMs) {
		return TimeUtils.getTimeBeginOfWeek(curTimeMs);
	}

	@Override
	public String toString() {
		return "WeeklyTimeRange{" +
				"startNode=" + startNode +
				", endNode=" + endNode +
				'}';
	}

	/**
	 * 由一个完整的配置解析时间段。
	 * @param confParam 格式 d_HH:mm-d_HH:mm
	 *                  eg: 1_00:00-3_00:00 表示周一的00:00 到 周三的00:00
	 * @return WeeklyTimeRange
	 */
	public static WeeklyTimeRange parseFromConf(String confParam){
		String[] params = confParam.split("-");
		if (params.length != 2){
			throw new ConfigFormatException("Unsupported WeeklyTimeRange format " + confParam);
		}
		return parseFromConf(params[0], params[1]);
	}

	/**
	 * 由开始时间和结束时间两个配置构成时间段。
	 * @param startConf 开始时间点配置 格式 d_HH:mm
	 * @param endConf 结束时间点配置 格式 d_HH:mm
	 * @return WeeklyTimeRange
	 */
	public static WeeklyTimeRange parseFromConf(String startConf, String endConf){
		WeeklyTimeOffset start = WeeklyTimeOffset.parseFromConf(startConf);
		WeeklyTimeOffset end = WeeklyTimeOffset.parseFromConf(endConf);
		return new WeeklyTimeRange(start, end);
	}

	public static void main(String[] args) {
		test("1_12:00:00", "2_12:00:00");
		test("3_12:00:00", "4_12:00:00");
		test("5_12:00:00", "6_12:00:00");
		test("7_12:00:00", "2_12:00:00");
	}

	private static void test(String startConf, String endConf) {
		System.out.println(startConf + " " + endConf);

		WeeklyTimeRange weeklyTimeRange = WeeklyTimeRange.parseFromConf(startConf, endConf);
		System.out.println(weeklyTimeRange);

		long curTimeMs = System.currentTimeMillis();
		
		System.out.println("isBetweenTimeRange       = " + weeklyTimeRange.isBetweenTimeRange(curTimeMs));
		System.out.println("triggeringTimeRange      = " + weeklyTimeRange.triggeringTimeRange(curTimeMs));
		System.out.println("nextTriggerTimeRange     = " + weeklyTimeRange.nextTriggerTimeRange(curTimeMs));
		System.out.println("preTriggerTimeRange      = " + weeklyTimeRange.preTriggerTimeRange(curTimeMs));

		System.out.println("curLoopTimeRange         = " + weeklyTimeRange.curLoopTimeRange(curTimeMs));
		System.out.println("nextLoopTimeRange        = " + weeklyTimeRange.nextLoopTimeRange(curTimeMs));
		System.out.println("preLoopTimeRange         = " + weeklyTimeRange.preLoopTimeRange(curTimeMs));

		System.out.println();
	}
}
