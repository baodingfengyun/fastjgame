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

import java.util.*;

/**
 * 组合动态时间段。
 * 除了{@link #isBetweenTimeRange(long)}不需要理解成本外，其它接口一定慎用。
 * 只有当children的有效时间段不重叠的时候，使用起来是安全的，其它时候慎用。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/7 22:50
 * github - https://github.com/hl845740757
 */
public class CompositeDynamicTimeRange implements DynamicTimeRange {

	/** 所有子节点 */
	private final List<DynamicTimeRange> children = new ArrayList<>();

	/**
	 * @param children children.size() > 0
	 */
	public CompositeDynamicTimeRange(List<DynamicTimeRange> children) {
		this.children.addAll(children);
	}

	public CompositeDynamicTimeRange(DynamicTimeRange first, DynamicTimeRange... others) {
		children.add(first);
		Collections.addAll(children, others);
	}

	@Override
	public TimeRange triggeringTimeRange(long timeMs) {
		// 获取所有触发的里面startTime最小的
		Optional<TimeRange> min = children.stream()
				.map(e -> e.triggeringTimeRange(timeMs))
				.filter(Objects::nonNull)
				.min(CompositeDynamicTimeRange::compareNextTriggerTimeRange);
		// 可能一个都没有触发，因此结果可能不存在
		return min.orElse(null);
	}

	@Override
	public TimeRange nextTriggerTimeRange(long timeMs) {
		// 获取下一个触发的里面startTime最小的
		Optional<TimeRange> min = children.stream()
				.map(e -> e.nextTriggerTimeRange(timeMs))
				.filter(Objects::nonNull)
				.min(CompositeDynamicTimeRange::compareNextTriggerTimeRange);
		// 可能都没有下一个触发点
		return min.orElse(null);
	}

	@Override
	public TimeRange preTriggerTimeRange(long timeMs) {
		// 获取结束的里面endTime最大的
		Optional<TimeRange> min = children.stream()
				.map(e -> e.preTriggerTimeRange(timeMs))
				.filter(Objects::nonNull)
				.max(CompositeDynamicTimeRange::comparePreTriggerTimeRange);
		// 可能都没有上一个触发点
		return min.orElse(null);
	}

	private static int compareNextTriggerTimeRange(TimeRange a, TimeRange b){
		// startTime越小越靠前
		final int startTimeCompareResult = Long.compare(a.startTime, b.startTime);
		if (startTimeCompareResult != 0) {
			return startTimeCompareResult;
		}
		// endTime越大越靠前（前面的包容后面的）
		return Long.compare(b.endTime, a.endTime);
	}

	private static int comparePreTriggerTimeRange(TimeRange a, TimeRange b){
		// endTime越小越靠前
		final int endTimeCompareResult = Long.compare(a.endTime, b.endTime);
		if (endTimeCompareResult != 0){
			return endTimeCompareResult;
		}
		// startTime越大越靠前（后面的包容前面的）
		return Long.compare(b.startTime, a.startTime);
	}

	@Override
	public String toString() {
		return "CompositeTimeRange{" +
				"children=" + children +
				'}';
	}

	public static void main(String[] args) {
		List<DynamicTimeRange> children = new ArrayList<>();

//		children.add(WeeklyTimeRange.parseFromConf("1_12:00:00", "2_12:00:00"));
//		children.add(WeeklyTimeRange.parseFromConf("3_12:00:00", "4_12:00:00"));
//		children.add(WeeklyTimeRange.parseFromConf("5_12:00:00", "6_12:00:00"));
//		children.add(WeeklyTimeRange.parseFromConf("7_12:00:00", "2_12:00:00"));

		children.add(DailyTimeRange.parseFromConf("09:00-12:00"));
		children.add(DailyTimeRange.parseFromConf("14:00-15:00"));
		children.add(DailyTimeRange.parseFromConf("17:00-18:00"));
		children.add(DailyTimeRange.parseFromConf("22:00-08:00"));

		CompositeDynamicTimeRange compositeDynamicTimeRange = new CompositeDynamicTimeRange(children);
		System.out.println(compositeDynamicTimeRange);

		long curTimeMs = System.currentTimeMillis();
		System.out.println("isBetweenTimeRange         = " + compositeDynamicTimeRange.isBetweenTimeRange(curTimeMs));
		System.out.println("triggeringTimeRange        = " + compositeDynamicTimeRange.triggeringTimeRange(curTimeMs));
		System.out.println("nextTriggerTimeRange       = " + compositeDynamicTimeRange.nextTriggerTimeRange(curTimeMs));
		System.out.println("preTriggerTimeRange        = " + compositeDynamicTimeRange.preTriggerTimeRange(curTimeMs));

		System.out.println();
	}
}
