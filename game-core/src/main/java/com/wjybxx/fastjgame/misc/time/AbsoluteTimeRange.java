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

/**
 * 绝对时间段。
 * eg: 2019-06-19 00:00:00 到 2019-06-19 23:59:59
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/7 22:50
 * github - https://github.com/hl845740757
 */
public class AbsoluteTimeRange implements DynamicTimeRange {

    private final TimeRange triggerTimeRange;

    public AbsoluteTimeRange(long startTime, long endTime) {
        this.triggerTimeRange = new TimeRange(startTime, endTime);
    }

    public AbsoluteTimeRange(AbsoluteTimeOffset startNode, AbsoluteTimeOffset endNode) {
        this.triggerTimeRange = new TimeRange(startNode.toOffset(), endNode.toOffset());
    }

    @Override
    public TimeRange triggeringTimeRange(long timeMs) {
        if (triggerTimeRange.isBetweenTimeRange(timeMs)) {
            return triggerTimeRange;
        } else {
            return null;
        }
    }

    @Override
    public TimeRange nextTriggerTimeRange(long timeMs) {
        if (timeMs < triggerTimeRange.startTime) {
            return triggerTimeRange;
        } else {
            return null;
        }
    }

    @Override
    public TimeRange preTriggerTimeRange(long timeMs) {
        if (timeMs > triggerTimeRange.endTime) {
            return triggerTimeRange;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "AbsoluteTimeRange{" +
                "triggerTimeRange=" + triggerTimeRange +
                '}';
    }

    /**
     * 从开始节点配置和结束时间配置中解析时间段
     *
     * @param startConf 格式 yyyy-MM-dd HH:mm:ss
     * @param endConf   格式 yyyy-MM-dd HH:mm:ss
     * @return AbsoluteTimeRange
     */
    public static AbsoluteTimeRange parseFromConf(String startConf, String endConf) {
        AbsoluteTimeOffset start = AbsoluteTimeOffset.parseFromConf(startConf);
        AbsoluteTimeOffset end = AbsoluteTimeOffset.parseFromConf(endConf);
        return new AbsoluteTimeRange(start, end);
    }

    public static void main(String[] args) {
        test("2018-12-30 12:00:00", "2019-03-01 12:00:00");
        test("2019-03-02 12:00:00", "2019-06-01 12:00:00");
        test("2019-07-01 12:00:00", "2019-08-01 12:00:00");
        test("2019-12-01 12:00:00", "2019-12-31 12:00:00");
    }

    private static void test(String startConf, String endConf) {
        System.out.println(startConf + " " + endConf);

        AbsoluteTimeRange absoluteTimeRange = AbsoluteTimeRange.parseFromConf(startConf, endConf);
        long curTimeMs = System.currentTimeMillis();

        System.out.println("isBetweenTimeRange      = " + absoluteTimeRange.isBetweenTimeRange(curTimeMs));
        System.out.println("triggeringTimeRange     = " + absoluteTimeRange.triggeringTimeRange(curTimeMs));
        System.out.println("nextTriggerTimeRange    = " + absoluteTimeRange.nextTriggerTimeRange(curTimeMs));
        System.out.println("preTriggerTimeRange     = " + absoluteTimeRange.preTriggerTimeRange(curTimeMs));

        System.out.println();
    }
}
