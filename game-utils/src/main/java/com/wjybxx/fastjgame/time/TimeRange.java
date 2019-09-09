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
 * 时间范围
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/7 22:50
 * github - https://github.com/hl845740757
 */
public class TimeRange {

    /**
     * 开始时间 （INCLUSIVE）
     */
    public final long startTime;
    /**
     * 结束时间 （inclusive）
     */
    public final long endTime;

    public TimeRange(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * 指定时间是否在该时间段内
     *
     * @param curTimeMs 某个瞬时时间
     * @return true/false
     */
    public boolean isBetweenTimeRange(long curTimeMs) {
        return curTimeMs >= startTime && curTimeMs <= endTime;
    }

    @Override
    public String toString() {
        return "TimeRange{" +
                "startTime=" + startTime + " =" + TimeUtils.formatTime(startTime) +
                ", endTime=" + endTime + " =" + TimeUtils.formatTime(endTime) +
                '}';
    }
}