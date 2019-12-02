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

package com.wjybxx.fastjgame.utils;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 时间工具类 -- 以毫秒为基本单位。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/7 22:50
 * github - https://github.com/hl845740757
 */
public class TimeUtils {

    private TimeUtils() {

    }

    /**
     * 时区偏移量
     */
    public static final ZoneOffset ZONE_OFFSET = ZoneOffset.ofHours(8);
    /**
     * 一秒的毫秒数
     */
    public static final long SEC = 1000;
    /**
     * 一分的毫秒数
     */
    public static final long MIN = 60 * SEC;
    /**
     * 一小时的毫秒数
     */
    public static final long HOUR = 60 * MIN;
    /**
     * 一天的毫秒数
     */
    public static final long DAY = 24 * HOUR;
    /**
     * 一周的毫秒数
     */
    public static final long WEEK = 7 * DAY;
    /**
     * 1毫秒多少纳秒
     */
    public static final long NANO_PER_MILLISECOND = 100_0000;

    /**
     * 午夜 00:00:00
     * The time of midnight at the start of the day, '00:00'.
     */
    public static final LocalTime MIDNIGHT = LocalTime.MIDNIGHT;

    /**
     * 默认的时间格式
     */
    public static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";
    /**
     * 默认时间格式器
     */
    public static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_PATTERN);
    /**
     * 年月日的格式化器
     */
    public static final DateTimeFormatter YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /**
     * 时分秒的格式化器
     */
    public static final DateTimeFormatter HH_MM_SS = DateTimeFormatter.ofPattern("HH:mm:ss");
    /**
     * 时分的格式化器
     */
    public static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 将毫秒时间转换为{@link LocalDateTime}
     *
     * @param timeMs 毫秒时间
     * @return LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(long timeMs) {
        int nanoOfSecond = (int) ((timeMs % 1000) * NANO_PER_MILLISECOND);
        return LocalDateTime.ofEpochSecond(timeMs / 1000, nanoOfSecond, ZONE_OFFSET);
    }

    /**
     * 将毫秒时间转换为{@link LocalDateTime}，并忽略毫秒。
     *
     * @param timeMs 毫秒时间
     * @return LocalDateTime
     */
    private static LocalDateTime toLocalDateTimeIgnoreMs(long timeMs) {
        return LocalDateTime.ofEpochSecond(timeMs / 1000, 0, ZONE_OFFSET);
    }

    /**
     * 将 毫秒时间 格式化为 默认字符串格式{@link #DEFAULT_PATTERN}
     *
     * @param timeMs 毫秒时间
     * @return 格式化后的字符串表示
     */
    public static String formatTime(long timeMs) {
        return formatTime(timeMs, DEFAULT_FORMATTER);
    }

    /**
     * 将 毫秒时间 格式化为 指定字符串格式
     *
     * @param timeMs  毫秒时间
     * @param pattern 时间格式
     * @return 格式化后的字符串表示
     */
    public static String formatTime(long timeMs, String pattern) {
        return formatTime(timeMs, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 将 毫秒时间 格式化为 指定格式
     *
     * @param timeMs    毫秒时间
     * @param formatter 时间格式器
     * @return 格式化后的字符串表示
     */
    public static String formatTime(long timeMs, DateTimeFormatter formatter) {
        LocalDateTime localDateTime = toLocalDateTime(timeMs);
        return formatter.format(localDateTime);
    }

    /**
     * 解析为毫秒时间戳
     *
     * @param dateString {@link #DEFAULT_PATTERN}格式的字符串
     * @return millSecond
     */
    public static long parseTimeMillis(String dateString) {
        return LocalDateTime.parse(dateString, DEFAULT_FORMATTER).toInstant(ZONE_OFFSET).toEpochMilli();
    }

    /**
     * 获取指定时间戳所在日期的00:00:00的毫秒时间戳
     *
     * @param curTimeMs 指定时间戳，用于确定日期
     * @return midnight of special day
     */
    public static long getTimeBeginOfToday(long curTimeMs) {
        LocalDateTime localDateTime = toLocalDateTimeIgnoreMs(curTimeMs);
        return localDateTime.with(MIDNIGHT).toEpochSecond(ZONE_OFFSET) * 1000;
    }

    /**
     * 获取指定时间戳所在日期的23:59:59的毫秒时间戳
     *
     * @param curTimeMs 指定时间戳，用于确定日期
     * @return end time of special day
     */
    public static long getTimeEndOfToday(long curTimeMs) {
        return getTimeBeginOfToday(curTimeMs) + DAY - 1;
    }

    /**
     * 获取指定时间戳所在周的周一00:00:00的毫秒时间戳
     *
     * @param curTimeMs 指定时间戳，用于确定所在的周
     * @return midnight of monday
     */
    public static long getTimeBeginOfWeek(long curTimeMs) {
        long beginOfToday = getTimeBeginOfToday(curTimeMs);
        LocalDateTime localDateTime = toLocalDateTimeIgnoreMs(curTimeMs);
        return beginOfToday - (localDateTime.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue()) * DAY;
    }

    /**
     * 获取指定时间戳所在周的周日23:59:59的毫秒时间戳
     *
     * @param curTimeMs 指定时间戳，用于确定所在的周
     * @return midnight of monday
     */
    public static long getTimeEndOfWeek(long curTimeMs) {
        return getTimeBeginOfWeek(curTimeMs) + WEEK - 1;
    }

    /**
     * 判断两个时间是否是同一天
     *
     * @param time1 第一个时间戳
     * @param time2 第二个时间戳
     * @return true/false，如果是同一天则返回true，否则返回false。
     */
    public static boolean isSameDay(long time1, long time2) {
        return getTimeBeginOfToday(time1) == getTimeBeginOfToday(time2);
    }

    /**
     * 计算两个时间戳相差的天数
     *
     * @param time1 第一个时间戳
     * @param time2 第二个时间戳
     * @return >=0,同一天返回0，否则返回值大于0
     */
    public static int differDays(long time1, long time2) {
        return (int) (Math.abs(getTimeBeginOfToday(time1) - getTimeBeginOfToday(time2)) / DAY);
    }

    /**
     * 判断两个时间是否是同一周
     *
     * @param time1 第一个时间戳
     * @param time2 第二个时间戳
     * @return true/false，如果是同一周则返回true，否则返回false。
     */
    public static boolean isSameWeek(long time1, long time2) {
        return getTimeBeginOfWeek(time1) == getTimeBeginOfWeek(time2);
    }

    /**
     * 计算两个时间戳相差的周数
     *
     * @param time1 第一个时间戳
     * @param time2 第二个时间戳
     * @return >=0,同一周返回0，否则返回值大于0
     */
    public static int differWeeks(long time1, long time2) {
        return (int) Math.abs(getTimeBeginOfWeek(time1) - getTimeBeginOfWeek(time2) / WEEK);
    }

    public static void main(String[] args) {
        final long timeMillis = System.currentTimeMillis();
        System.out.println("Current " + formatTime(timeMillis));

        System.out.println("\nBeginOfToday " + formatTime(getTimeBeginOfToday(timeMillis)));
        System.out.println("EndOfToday " + formatTime(getTimeEndOfToday(timeMillis)));

        System.out.println("\nToday is same day? " + isSameDay(timeMillis, timeMillis));
        System.out.println("Yesterday is same day? " + isSameDay(timeMillis, timeMillis - DAY));
        System.out.println("Yesterday differ days = " + differDays(timeMillis, timeMillis - DAY));

        System.out.println("\nBeginOfWeek " + formatTime(getTimeBeginOfWeek(timeMillis)));
        System.out.println("EndOfWeek " + formatTime(getTimeEndOfWeek(timeMillis)));
    }
}
