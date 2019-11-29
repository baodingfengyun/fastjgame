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
 * 可循环的动态时间段。
 * 一个时间段由起始时间信息和结束时间信息构成，因此必须提供由开始时间和结束时间两个配置解析的方法。
 * 如果方便也可以提供由一个配置解析的方法，如果不方便可不提供。
 *
 * @param <E> the type of start and end
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/7 22:50
 * github - https://github.com/hl845740757
 */
public abstract class LoopDynamicTimeRange<E extends TimeOffset> implements DynamicTimeRange {

    /**
     * 时间段开始节点
     */
    protected final E startNode;
    /**
     * 时间段结束节点
     */
    protected final E endNode;

    protected LoopDynamicTimeRange(E startNode, E endNode) {
        this.startNode = startNode;
        this.endNode = endNode;
    }

    /**
     * 当前可能触发当前周期或上一个周期的有效时间段，一定不会触发下一个周期的时间段。
     *
     * @param timeMs 毫秒时间戳
     * @return nullable
     */
    @Override
    public TimeRange triggeringTimeRange(long timeMs) {
        TimeRange preTimeRange = preLoopTimeRange(timeMs);
        if (preTimeRange.isBetweenTimeRange(timeMs)) {
            // 上一个周期的有效时间段还未结束
            // eg: 22:00 - 08:00 当前 05:00
            return preTimeRange;
        }
        TimeRange curTimeRange = curLoopTimeRange(timeMs);
        if (curTimeRange.isBetweenTimeRange(timeMs)) {
            // 当前周期有效时间段还未结束
            // eg: 22:00 - 08:00 当前 22:30
            return curTimeRange;
        }
        return null;
    }

    /**
     * 下一个触发的可能是当前周期的，也可能是下一个周期的。
     *
     * @param timeMs 毫秒时间戳
     * @return nullable
     */
    @Override
    public TimeRange nextTriggerTimeRange(long timeMs) {
        TimeRange curTimeRange = curLoopTimeRange(timeMs);
        if (timeMs < curTimeRange.startTime) {
            // 当前周期的还未开始
            return curTimeRange;
        } else {
            return nextLoopTimeRange(timeMs);
        }
    }

    /**
     * 下一个触发的可能是当前周期的，也可能是上一个周期的。
     *
     * @param timeMs 毫秒时间戳
     * @return nullable
     */
    @Override
    public TimeRange preTriggerTimeRange(long timeMs) {
        TimeRange curTimeRange = curLoopTimeRange(timeMs);
        if (timeMs > curTimeRange.endTime) {
            // 当前周期已结束
            return curTimeRange;
        } else {
            return preLoopTimeRange(timeMs);
        }
    }

    /**
     * 返回curTimeMs所在循环周期的有效时间段。
     * <p>
     * eg: 22:00-8:00
     * 解释：循环周期为一天，有效时间段为 当日22:00 到 明日 8:00
     * 调用该方法，始终返回今天22点 到 明天8点；
     *
     * @param timeMs 毫秒时间戳
     * @return TimeRange
     */
    public final TimeRange curLoopTimeRange(long timeMs) {
        long loopStartTime = getPeriodStartTime(timeMs);
        long startTime = loopStartTime + startNode.toOffset();
        long endTime = loopStartTime + endNode.toOffset();

        if (endTime >= startTime) {
            // 开始时间和结束时间在同一个循环周期
            return new TimeRange(startTime, endTime);
        }
        // 结束时间点在下一个循环周期
        return new TimeRange(startTime, endTime + getPeriod());
    }

    /**
     * 返回curTimeMs所在循环周期的下一个周期的有效时间段。
     * <p>
     * 1. 首先获取curTimeMs所处的循环周期的有效时间段。
     * 2. 如果有下一个循环周期，则返回下一个循环周期的有效时间段。
     * 3. 否则返回null。
     * <p>
     * eg: 22:00-8:00
     * 解释：循环周期为一天，有效时间段为 当日22:00 到 明日 8:00
     * 调用该方法始终返回 明天22:00 到 后天08:00
     *
     * @param timeMs 毫秒时间戳
     * @return TimeRange nullable
     */
    public final TimeRange nextLoopTimeRange(long timeMs) {
        TimeRange curTimeRange = curLoopTimeRange(timeMs);
        return new TimeRange(curTimeRange.startTime + getPeriod(), curTimeRange.endTime + getPeriod());
    }

    /**
     * 返回curTimeMs所在循环周期的上一个周期的有效时间段。
     * <p>
     * 1. 首先获取curTimeMs所处的循环周期的有效时间段。
     * 2. 如果有上一个循环周期，则返回上一个循环周期的有效时间段。
     * 3. 否则返回null。
     * <p>
     * eg: 22:00-8:00
     * 解释：循环周期为一天，有效时间段为 当日22:00 到 明日 8:00
     * 调用该方法始终返回 昨天22:00 到 今天08:00
     *
     * @param timeMs 毫秒时间戳
     * @return TimeRange nullable
     */
    public final TimeRange preLoopTimeRange(long timeMs) {
        TimeRange curTimeRange = curLoopTimeRange(timeMs);
        return new TimeRange(curTimeRange.startTime - getPeriod(), curTimeRange.endTime - getPeriod());
    }

    /**
     * 获取循环周期
     *
     * @return long
     */
    protected abstract long getPeriod();

    /**
     * 获取指定时间所在周期的开始时间。
     * eg: 每天00:00:00， 每周一00:00:00
     *
     * @param curTimeMs 某个瞬时时间
     * @return 周期开始时间
     */
    protected abstract long getPeriodStartTime(long curTimeMs);

}
