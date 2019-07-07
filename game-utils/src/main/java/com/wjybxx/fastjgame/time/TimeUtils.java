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

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * 时间工具类
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/7 22:50
 * github - https://github.com/hl845740757
 */
public class TimeUtils {

	private TimeUtils(){

	}

	/** 时区偏移量 */
	public static final ZoneOffset ZONE_OFFSET = ZoneOffset.ofHours(8);
	/** 一秒的毫秒数 */
	public static final long SEC = 1000;
	/** 一分的毫秒数 */
	public static final long MIN = 60 * SEC;
	/** 一小时的毫秒数 */
	public static final long HOUR = 60 * MIN;
	/** 一天的毫秒数 */
	public static final long DAY = 24 * HOUR;
	/** 一周的毫秒数 */
	public static final long WEEK = 7 * DAY;

	/**
	 * 午夜 00:00:00
	 * The time of midnight at the start of the day, '00:00'.
	 */
	public static final LocalTime MIDNIGHT = LocalTime.MIDNIGHT;

	/**
	 * 默认的时间格式
	 */
	public static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";

	/** 默认时间格式器 */
	public static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_PATTERN);

	/**
	 * 将毫秒时间转换为{@link LocalDateTime}
	 * @param timeMs 毫秒时间
	 * @return LocalDateTime
	 */
	public static LocalDateTime toLocalDateTime(long timeMs) {
		long epochSecond = timeMs / 1000;
		int nanoOfSecond = (int) TimeUnit.MILLISECONDS.toNanos(timeMs % 1000);
		return LocalDateTime.ofEpochSecond(epochSecond, nanoOfSecond, ZONE_OFFSET);
	}

	/**
	 * 将 毫秒时间 格式化为 默认字符串格式{@link #DEFAULT_PATTERN}
	 * @param timeMs 毫秒时间
	 * @return 格式化后的字符串表示
	 */
	public static String formatTime(long timeMs) {
		LocalDateTime localDateTime = toLocalDateTime(timeMs);
		return DEFAULT_FORMATTER.format(localDateTime);
	}

	/**
	 * 将 毫秒时间 格式化为 指定字符串格式
	 * @param timeMs 毫秒时间
	 * @param pattern 时间格式
	 * @return 格式化后的字符串表示
	 */
	public static String formatTime(long timeMs, String pattern) {
		LocalDateTime localDateTime = toLocalDateTime(timeMs);
		return DateTimeFormatter.ofPattern(pattern).format(localDateTime);
	}

	/**
	 * 解析为毫秒时间戳
	 * @param confParam {@link #DEFAULT_PATTERN}格式的字符串
	 * @return millSecond
	 */
	public static long parseMillTime(String confParam) {
		return LocalDateTime.parse(confParam, DEFAULT_FORMATTER).toInstant(ZONE_OFFSET).toEpochMilli();
	}

	/**
	 * 获取指定时间戳所在日期的00:00:00的毫秒时间戳
	 * @param curTimeMs 指定时间戳，用于确定日期
	 * @return midnight of special day
	 */
	public static long getTimeBeginOfToday(long curTimeMs){
		LocalDateTime localDateTime = LocalDateTime.ofEpochSecond(curTimeMs / 1000, 0, ZONE_OFFSET);
		return localDateTime.with(MIDNIGHT).toEpochSecond(ZONE_OFFSET) * 1000;
	}

	/**
	 * 获取指定时间戳所在那一周的周一00:00:00的毫秒时间戳
	 * @param curTimeMs 指定时间戳，用于确定所在的周
	 * @return midnight of monday
	 */
	public static long getTimeBeginOfWeek(long curTimeMs) {
		long beginOfToday = getTimeBeginOfToday(curTimeMs);
		LocalDateTime localDateTime = LocalDateTime.ofEpochSecond(curTimeMs / 1000, 0, ZONE_OFFSET);
		return beginOfToday - (localDateTime.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue()) * DAY;
	}

	private static long getTimeBeginOfWeek2(long curTimeMs) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(curTimeMs);
		calendar.setFirstDayOfWeek(Calendar.MONDAY);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		return calendar.getTimeInMillis();
	}

	public static void main(String[] args) {
		// 7月1
		final long startTime = 1561910400000L;
		final long day = 24 * 3600 * 1000;

		for (int index=0;index<7;index++){
			long time = startTime +  index * day;
			System.out.println(formatTime(time));
			System.out.println(formatTime(getTimeBeginOfWeek(time)));
			System.out.println(formatTime(getTimeBeginOfWeek2(time)));
			System.out.println();
		}
	}
}
