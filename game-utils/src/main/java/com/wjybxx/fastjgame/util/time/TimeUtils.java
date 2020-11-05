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
     * 中国时区
     */
    public static final ZoneOffset ZONE_OFFSET_CST = ZoneOffset.ofHours(8);

    /**
     * UTC时间
     */
    public static final ZoneOffset ZONE_OFFSET_UTC = ZoneOffset.UTC;

    /**
     * 系统时区
     */
    public static final ZoneOffset ZONE_OFFSET_SYSTEM = ZoneOffset.systemDefault().getRules().getOffset(LocalDateTime.now());

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

}
