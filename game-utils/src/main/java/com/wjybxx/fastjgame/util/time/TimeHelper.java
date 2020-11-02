/*
 *  Copyright 2019 wjybxx
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to iBn writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.wjybxx.fastjgame.util.time;

import java.time.*;
import java.time.format.DateTimeFormatter;

import static com.wjybxx.fastjgame.util.time.TimeUtils.DAY;

/**
 * 绑定特定时区的时间辅助类
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/8/28
 */
public final class TimeHelper {

    /**
     * 中国时区对应的辅助类实例
     */
    public static final TimeHelper CST = new TimeHelper(TimeUtils.CST);

    /**
     * 系统时区对应的辅助类实例
     */
    public static final TimeHelper SYSTEM = new TimeHelper(TimeUtils.SYSTEM_ZONE_OFFSET);

    private final ZoneOffset zoneOffset;

    private TimeHelper(ZoneOffset zoneOffset) {
        this.zoneOffset = zoneOffset;
    }

    public static TimeHelper of(ZoneOffset zoneOffset) {
        if (zoneOffset.equals(CST.zoneOffset)) {
            return CST;
        }
        if (zoneOffset.equals(SYSTEM.zoneOffset)) {
            return SYSTEM;
        }
        return new TimeHelper(zoneOffset);
    }

    /**
     * 将毫秒时间转换为{@link LocalDateTime}
     *
     * @param timeMs 毫秒时间
     * @return LocalDateTime
     */
    public LocalDateTime toLocalDateTime(long timeMs) {
        int nanoOfSecond = (int) ((timeMs % 1000) * TimeUtils.NANO_PER_MILLISECOND);
        return LocalDateTime.ofEpochSecond(timeMs / 1000, nanoOfSecond, zoneOffset);
    }

    /**
     * 将毫秒时间转换为{@link LocalDateTime}，并忽略毫秒。
     *
     * @param timeMs 毫秒时间
     * @return LocalDateTime
     */
    private LocalDateTime toLocalDateTimeIgnoreMs(long timeMs) {
        return LocalDateTime.ofEpochSecond(timeMs / 1000, 0, zoneOffset);
    }

    /**
     * 将 毫秒时间 格式化为 默认字符串格式{@link TimeUtils#DEFAULT_PATTERN}
     *
     * @param timeMs 毫秒时间
     * @return 格式化后的字符串表示
     */
    public String formatTime(long timeMs) {
        return formatTime(timeMs, TimeUtils.DEFAULT_FORMATTER);
    }

    /**
     * 将 毫秒时间 格式化为 指定字符串格式
     *
     * @param timeMs  毫秒时间
     * @param pattern 时间格式
     * @return 格式化后的字符串表示
     */
    public String formatTime(long timeMs, String pattern) {
        if (pattern.equals(TimeUtils.DEFAULT_PATTERN)) {
            return formatTime(timeMs, TimeUtils.DEFAULT_FORMATTER);
        } else {
            return formatTime(timeMs, DateTimeFormatter.ofPattern(pattern));
        }
    }

    /**
     * 将 毫秒时间 格式化为 指定格式
     *
     * @param timeMs    毫秒时间
     * @param formatter 时间格式器
     * @return 格式化后的字符串表示
     */
    public String formatTime(long timeMs, DateTimeFormatter formatter) {
        LocalDateTime localDateTime = toLocalDateTime(timeMs);
        return formatter.format(localDateTime);
    }

    /**
     * 解析为毫秒时间戳
     *
     * @param dateString {@link TimeUtils#DEFAULT_PATTERN}格式的字符串
     * @return millSecond
     */
    public long parseTimeMillis(String dateString) {
        return LocalDateTime.parse(dateString, TimeUtils.DEFAULT_FORMATTER).toInstant(zoneOffset).toEpochMilli();
    }

    /**
     * 获取指定时间戳所在日期的00:00:00的毫秒时间戳
     *
     * @param curTimeMs 指定时间戳，用于确定日期
     * @return midnight of special day
     */
    public long getTimeBeginOfToday(long curTimeMs) {
        LocalDateTime localDateTime = toLocalDateTimeIgnoreMs(curTimeMs);
        return localDateTime.with(TimeUtils.MIDNIGHT).toEpochSecond(zoneOffset) * 1000;
    }

    /**
     * 获取指定时间戳所在日期的23:59:59的毫秒时间戳
     *
     * @param curTimeMs 指定时间戳，用于确定日期
     * @return end time of special day
     */
    public long getTimeEndOfToday(long curTimeMs) {
        return getTimeBeginOfToday(curTimeMs) + DAY - 1;
    }

    /**
     * 获取指定时间戳所在周的周一00:00:00的毫秒时间戳
     *
     * @param curTimeMs 指定时间戳，用于确定所在的周
     * @return midnight of monday
     */
    public long getTimeBeginOfWeek(long curTimeMs) {
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
    public long getTimeEndOfWeek(long curTimeMs) {
        return getTimeBeginOfWeek(curTimeMs) + TimeUtils.WEEK - 1;
    }

    /**
     * 判断两个时间是否是同一天
     *
     * @param time1 第一个时间戳
     * @param time2 第二个时间戳
     * @return true/false，如果是同一天则返回true，否则返回false。
     */
    public boolean isSameDay(long time1, long time2) {
        return toEpochDay(time1) - toEpochDay(time2) == 0;
    }

    /**
     * 计算时间戳对应的纪元天数
     *
     * @param millis 毫秒时间
     * @return 基于纪元的天数
     */
    public int toEpochDay(long millis) {
        return (int) LocalDate.ofInstant(Instant.ofEpochMilli(millis), zoneOffset).toEpochDay();
    }

    /**
     * 计算两个时间戳相差的天数
     *
     * @param time1 第一个时间戳
     * @param time2 第二个时间戳
     * @return >=0,同一天返回0，否则返回值大于0
     */
    public int differDays(long time1, long time2) {
        return Math.abs(toEpochDay(time1) - toEpochDay(time2));
    }

    /**
     * 判断两个时间是否是同一周
     *
     * @param time1 第一个时间戳
     * @param time2 第二个时间戳
     * @return true/false，如果是同一周则返回true，否则返回false。
     */
    public boolean isSameWeek(long time1, long time2) {
        return getTimeBeginOfWeek(time1) == getTimeBeginOfWeek(time2);
    }

    /**
     * 计算两个时间戳相差的周数
     *
     * @param time1 第一个时间戳
     * @param time2 第二个时间戳
     * @return >=0,同一周返回0，否则返回值大于0
     */
    public int differWeeks(long time1, long time2) {
        return (int) Math.abs(getTimeBeginOfWeek(time1) - getTimeBeginOfWeek(time2) / TimeUtils.WEEK);
    }

    public static void main(String[] args) {
        final long timeMillis = System.currentTimeMillis();
        final TimeHelper timeHelper = TimeHelper.CST;

        System.out.println("Current " + timeHelper.formatTime(timeMillis));

        System.out.println("\nBeginOfToday " + timeHelper.formatTime(timeHelper.getTimeBeginOfToday(timeMillis)));
        System.out.println("EndOfToday " + timeHelper.formatTime(timeHelper.getTimeEndOfToday(timeMillis)));

        System.out.println("\nToday is same day? " + timeHelper.isSameDay(timeMillis, timeMillis));
        System.out.println("Yesterday is same day? " + timeHelper.isSameDay(timeMillis, timeMillis - DAY));
        System.out.println("Yesterday differ days = " + timeHelper.differDays(timeMillis, timeMillis - DAY));

        System.out.println("\nBeginOfWeek " + timeHelper.formatTime(timeHelper.getTimeBeginOfWeek(timeMillis)));
        System.out.println("EndOfWeek " + timeHelper.formatTime(timeHelper.getTimeEndOfWeek(timeMillis)));
    }

}
