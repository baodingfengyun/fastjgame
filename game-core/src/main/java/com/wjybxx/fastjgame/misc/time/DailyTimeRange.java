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

package com.wjybxx.fastjgame.misc.time;


import com.wjybxx.fastjgame.utils.TimeUtils;

/**
 * 每天的时间段。
 * 无天数概念的几点几分到几点几分。可以跨天。
 * 表格格式 HH:mm-HH:mm 或 HH:mm:ss-HH:mm:ss
 * eg: 22:00-8:00
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/7 22:50
 * github - https://github.com/hl845740757
 */
public class DailyTimeRange extends LoopDynamicTimeRange<DailyTimeOffset> {

    public DailyTimeRange(DailyTimeOffset startNode, DailyTimeOffset endNode) {
        super(startNode, endNode);
    }

    @Override
    protected long getPeriod() {
        return TimeUtils.DAY;
    }

    @Override
    protected long getPeriodStartTime(long curTimeMs) {
        return TimeUtils.getTimeBeginOfToday(curTimeMs);
    }

    @Override
    public String toString() {
        return "DailyTimeRange{" +
                "startNode=" + startNode +
                ", endNode=" + endNode +
                '}';
    }

    /**
     * 从配置中解析小时和分钟的时间段
     *
     * @param confParam 配置参数，格式 HH:mm-HH:mm
     * @return timeRange
     */
    public static DailyTimeRange parseFromConf(String confParam) {
        String[] hoursInfoArray = confParam.split("-");
        if (hoursInfoArray.length != 2) {
            throw new ConfigFormatException("Unsupported DailyTimeRange format " + confParam);
        }
        return parseFromConf(hoursInfoArray[0], hoursInfoArray[1]);
    }

    /**
     * 有两个小时和分钟的配置组件时间段
     *
     * @param startConf 格式 HH:mm
     * @param endConf   格式 HH:mm
     * @return DailyTimeRange
     */
    public static DailyTimeRange parseFromConf(String startConf, String endConf) {
        DailyTimeOffset begin = DailyTimeOffset.parseFromConf(startConf);
        DailyTimeOffset end = DailyTimeOffset.parseFromConf(endConf);
        return new DailyTimeRange(begin, end);
    }

    public static void main(String[] args) {
        test("00:00:00-23:59:59");
        test("08:00:00-10:00:00");
        test("12:00:00-14:00:00");
        test("17:00:00-18:00:00");
        test("22:00:00-08:00:00");
    }

    private static void test(String config) {
        System.out.println(config);
        DailyTimeRange dailyTimeRange = DailyTimeRange.parseFromConf(config);
        long curTimeMs = System.currentTimeMillis();

        System.out.println("isBetweenTimeRange       = " + dailyTimeRange.isBetweenTimeRange(curTimeMs));
        System.out.println("triggeringTimeRange      = " + dailyTimeRange.triggeringTimeRange(curTimeMs));
        System.out.println("nextTriggerTimeRange     = " + dailyTimeRange.nextTriggerTimeRange(curTimeMs));
        System.out.println("preTriggerTimeRange      = " + dailyTimeRange.preTriggerTimeRange(curTimeMs));

        System.out.println("curLoopTimeRange         = " + dailyTimeRange.curLoopTimeRange(curTimeMs));
        System.out.println("nextLoopTimeRange        = " + dailyTimeRange.nextLoopTimeRange(curTimeMs));
        System.out.println("preLoopTimeRange         = " + dailyTimeRange.preLoopTimeRange(curTimeMs));
        System.out.println();
    }
}
