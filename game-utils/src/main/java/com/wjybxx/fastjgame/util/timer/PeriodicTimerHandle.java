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

package com.wjybxx.fastjgame.util.timer;

/**
 * 周期性执行的timer的句柄
 *
 * @author wjybxx
 * date - 2019/8/7
 * github - https://github.com/hl845740757
 */
public interface PeriodicTimerHandle extends TimerHandle {

    /**
     * 指定的初始延迟，不可修改。
     *
     * @return 毫秒
     */
    long initialDelay();

    /**
     * 循环时的执行间隔。
     *
     * @return 毫秒
     */
    long period();

    /**
     * 尝试修改timer的执行间隔，修改成功立即生效!
     *
     * @param period 循环时的执行间隔，必须大于0
     * @return 如果timer已关闭则返回false，否则返回true。
     */
    boolean setPeriod(long period);

    /**
     * 修改timer下一次执行的间隔，对当前“排期”不生效。
     *
     * @param period 循环时的执行间隔，必须大于0
     * @return 如果timer已关闭则返回false，否则返回true。
     */
    boolean setPeriodLazy(long period);

    /**
     * <b>Notes:</b>该值在每次执行回调后更新，使用该值时务必注意。
     *
     * @return <b>已执行次数</b>，大于等于0。
     */
    int executedTimes();

}
