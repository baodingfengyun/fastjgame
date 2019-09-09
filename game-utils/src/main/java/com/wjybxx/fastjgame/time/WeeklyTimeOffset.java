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


import com.wjybxx.fastjgame.utils.ConfigUtils;
import com.wjybxx.fastjgame.utils.TimeUtils;

import java.time.DayOfWeek;

/**
 * 星期时间，一周内的时间偏移量。
 * <p>
 * 使用1-7表示周一到周日，要符合人的正常思维。
 * <p>
 * 格式:  d_HH:mm 或 d_HH:mm:ss
 * eg:   1_00:00  表示周一00:00
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/7 22:50
 * github - https://github.com/hl845740757
 */
public class WeeklyTimeOffset implements TimeOffset {

    /**
     * 周几 1-7
     */
    public final DayOfWeek dayOfWeek;

    /**
     * 时分
     */
    public final DailyTimeOffset dailyTimeOffset;

    /**
     * @param dayOfWeek 1-7
     * @param hour      小时
     * @param min       分钟
     * @param sec       秒
     */
    public WeeklyTimeOffset(int dayOfWeek, int hour, int min, int sec) {
        this.dayOfWeek = DayOfWeek.of(dayOfWeek);
        this.dailyTimeOffset = new DailyTimeOffset(hour, min, sec);
    }

    public WeeklyTimeOffset(DayOfWeek dayOfWeek, DailyTimeOffset dailyTimeOffset) {
        this.dayOfWeek = dayOfWeek;
        this.dailyTimeOffset = dailyTimeOffset;
    }

    @Override
    public long toOffset() {
        // 相对于周一的偏移量
        return (dayOfWeek.getValue() - DayOfWeek.MONDAY.getValue()) * TimeUtils.DAY + dailyTimeOffset.toOffset();
    }

    @Override
    public String toString() {
        return "WeeklyTimeOffset{" +
                "dayOfWeek=" + dayOfWeek +
                ", dailyTimeOffset=" + dailyTimeOffset +
                '}';
    }

    /**
     * 从配置中解析
     *
     * @param confParam 格式 d_HH:mm 或 d_HH:mm:ss 周一到周日由 1 - 7 表示
     * @return DayOfWeekTime
     */
    public static WeeklyTimeOffset parseFromConf(String confParam) {
        String[] params = confParam.split("_");
        if (params.length != 2) {
            throw new ConfigFormatException("Unsupported DayOfWeekTime format " + confParam);
        }
        int dayOfWeek = ConfigUtils.getAsInt(params[0]);
        DailyTimeOffset dailyTimeOffset = DailyTimeOffset.parseFromConf(params[1]);
        return new WeeklyTimeOffset(DayOfWeek.of(dayOfWeek), dailyTimeOffset);
    }
}
