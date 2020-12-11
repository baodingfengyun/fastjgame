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

package com.wjybxx.fastjgame.util.misc;

/**
 * 频率调节器
 */
public class Regulator {

    private static final int FIX_DELAY = 0;
    private static final int FIX_RATE = 1;

    /**
     * 其实可以省略，根据{@link #updatePeriod}的正负判断，但为了提高可读性，还是不那么做
     */
    private final int type;
    private long updatePeriod;

    private long lastUpdateTimeMs;
    private long deltaTime;

    private Regulator(int type, long updatePeriod, long lastUpdateTimeMs) {
        this.type = type;
        this.updatePeriod = ensurePeriodGreaterThanZero(updatePeriod);

        this.lastUpdateTimeMs = lastUpdateTimeMs;
        this.deltaTime = 0;
    }

    private static long ensurePeriodGreaterThanZero(long period) {
        if (period <= 0) {
            throw new IllegalArgumentException("period must be greater than 0");
        }
        return period;
    }

    /**
     * @param updatePeriod  更新间隔
     * @param curTimeMillis 当前系统时间，如果期望首次能立即执行，可以传入0
     * @return 按固定延迟更新的调节器，它保证的是两次执行的间隔大于
     */
    public static Regulator newStartedFixedDelay(long updatePeriod, long curTimeMillis) {
        return new Regulator(FIX_DELAY, updatePeriod, curTimeMillis);
    }

    /**
     * 一般不常用这种调节器
     *
     * @param updatePeriod  更新间隔
     * @param curTimeMillis 当前系统时间，如果期望首次能立即执行，可以传入0
     * @return 按固定频率更新的调节器，它保证的
     */
    public static Regulator newStartedFixedRate(long updatePeriod, long curTimeMillis) {
        return new Regulator(FIX_RATE, updatePeriod, curTimeMillis);
    }

    /**
     * 重新启动调节器
     * （没有单独的start方法，是因为太过于冗余）
     *
     * @param curTimeMillis 当前系统时间，如果期望首次能立即执行，可以传入0
     */
    public void restart(long curTimeMillis) {
        lastUpdateTimeMs = curTimeMillis;
        deltaTime = 0;
    }

    /**
     * @param curTimeMillis 当前系统时间
     * @return 如果应该执行一次update或者tick，则返回true，否则返回flase。
     */
    public boolean isReady(long curTimeMillis) {
        if (curTimeMillis <= 0) {
            throw new IllegalArgumentException("curTimeMillis: " + curTimeMillis);
        }

        if (lastUpdateTimeMs <= 0) {
            // 未设定初始时间或初始时间设置为0的情况下查询
            if (type == FIX_DELAY) {
                deltaTime = Math.min(updatePeriod, curTimeMillis);
            }
            lastUpdateTimeMs = curTimeMillis;
            return true;
        }

        if (curTimeMillis - lastUpdateTimeMs >= updatePeriod) {
            if (type == FIX_DELAY) {
                deltaTime = curTimeMillis - lastUpdateTimeMs;
                // 真实时间
                lastUpdateTimeMs = curTimeMillis;
            } else {
                // 逻辑时间
                lastUpdateTimeMs = lastUpdateTimeMs + updatePeriod;
            }
            return true;
        } else {
            return false;
        }
    }

    public long getUpdatePeriod() {
        return updatePeriod;
    }

    public void setUpdatePeriod(long updatePeriod) {
        this.updatePeriod = ensurePeriodGreaterThanZero(updatePeriod);
    }

    public boolean isFixedDelay() {
        return type == FIX_DELAY;
    }

    public boolean isFixedRate() {
        return type == FIX_RATE;
    }

    /**
     * @return 两次逻辑帧之间的间隔。
     * @throws IllegalStateException 如果{@link #isFixedDelay()}为false，则抛出该异常。
     */
    public long getDeltaTime() {
        if (type != FIX_DELAY) {
            throw new IllegalStateException("This is not a fixed delay regulator");
        }
        return deltaTime;
    }

}
