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

package com.wjybxx.fastjgame.utils;

import com.google.common.math.IntMath;
import com.wjybxx.fastjgame.misc.IntPair;
import com.wjybxx.fastjgame.misc.ShortPair;

import java.math.RoundingMode;

/**
 * 数学计算辅助类
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/12 16:08
 * github - https://github.com/hl845740757
 */
public class MathUtils {

    public static final float PI = (float) Math.PI;
    /**
     * 2PI这个变量名不好取
     */
    public static final float DOUBLE_PI = PI * 2;
    /**
     * 二分之一 PI
     */
    public static final float HALF_PI = PI / 2;
    /**
     * float类型可忽略误差值
     * 当两个float的差值小于该值的时候，我们可以认为两个float相等
     */
    public static final float FLOAT_DEVIATION = 0.0001f;
    /**
     * double类型可忽略误差值
     * 当两个double的差值小于该值的时候，我们可以认为两个double相等
     */
    public static final double DOUBLE_DEVIATION = 0.0000001d;

    private MathUtils() {

    }

    /**
     * 将两个int聚合为long
     *
     * @param a 高32位
     * @param b 低32位
     * @return long
     */
    public static long composeIntToLong(int a, int b) {
        // 保留b符号扩充以后的低32位
        return ((long) a << 32) | ((long) b & 0xFF_FF_FF_FFL);
    }

    public static int higherIntOfLong(long value) {
        return (int) (value >>> 32);
    }

    public static int lowerIntOfLong(long value) {
        return (int) value;
    }

    /**
     * 将一个{@link #composeIntToLong(int, int)} 得到的long分解为原始的int
     */
    public static IntPair decomposeLongToIntPair(long value) {
        return new IntPair(higherIntOfLong(value), lowerIntOfLong(value));
    }

    /**
     * 将两个short聚合为int
     *
     * @param a 高16位
     * @param b 低16位
     * @return int
     */
    public static int composeShortToInt(short a, short b) {
        // 保留b符号扩充以后的低16位
        return ((int) a << 16) | ((int) b & 0xFF_FF);
    }

    public static short higherShortOfInt(int value) {
        return (short) (value >>> 16);
    }

    public static short lowerShortOfInt(int value) {
        return (short) value;
    }

    /**
     * 将一个{@link #composeShortToInt(short, short)} 得到的int分解为原始的short
     */
    public static ShortPair decomposeIntToShortPair(int value) {
        return new ShortPair(higherShortOfInt(value), lowerShortOfInt(value));
    }

    /**
     * 帧间隔(毫秒)
     *
     * @param framePerSecond 每秒帧数 1 ~ 1000
     * @return timeMillis
     */
    public static long frameInterval(int framePerSecond) {
        if (framePerSecond < 1 || framePerSecond > 1000) {
            throw new IllegalArgumentException("framePerSecond " + framePerSecond + " must within 1~1000");
        }
        return 1000 / framePerSecond;
    }

    /**
     * 两个int安全相乘，返回一个long，避免越界；
     * 相乘之后再强转可能越界。
     *
     * @param a int
     * @param b int
     * @return long
     */
    public static long safeMultiplyToLong(int a, int b) {
        return (long) a * b;
    }

    /**
     * 两个short安全相乘，返回一个int，避免越界；
     * 相乘之后再强转可能越界。
     *
     * @param a short
     * @param b short
     * @return integer
     */
    public static int safeMultiplyToInt(short a, short b) {
        return (int) a * b;
    }

    /**
     * 两个int相除，如果余数大于0，则进一
     *
     * @param a int
     * @param b int
     * @return int
     */
    public static int divideIntCeil(int a, int b) {
        return IntMath.divide(a, b, RoundingMode.CEILING);
    }

    /**
     * 判断一个值是否是2的整次幂
     */
    public static boolean isPowerOfTwo(int val) {
        return IntMath.isPowerOfTwo(val);
    }

    // region 范围

    /**
     * 是否在区间段内，左闭右开，包含左边界，不包含右边界
     *
     * @param start 区间起始值 inclusive
     * @param end   区间结束值 exclusive
     * @param value 待检测的值
     */
    public static boolean withinRange(int start, int end, int value) {
        return value >= start && value < end;
    }

    /**
     * 是否在区间段内，闭区间，包含双端边界
     *
     * @param start 区间起始值 inclusive
     * @param end   区间结束值 inclusive
     * @param value 待检测的值
     */
    public static boolean withinRangeClosed(int start, int end, int value) {
        return value >= start && value <= end;
    }

    /**
     * 是否在区间段内，不包含双端边界值
     *
     * @param start 区间起始值 exclusive
     * @param end   区间结束值 exclusive
     * @param value 待检测的值
     */
    public static boolean betweenRange(int start, int end, int value) {
        return value > start && value < end;
    }
    // endregion

    // region 浮点数比较

    /**
     * 判断两个float是否近似相等
     *
     * @return 当两个float的差值在一定区间内时，我们认为其相等
     */
    public static boolean equals(float a, float b) {
        return Math.abs(a - b) < FLOAT_DEVIATION;
    }

    /**
     * 判断两个double是否近似相等
     *
     * @return 当两个double的差值在一定区间内时，我们认为其相等
     */
    public static boolean equals(double a, double b) {
        return Math.abs(a - b) < DOUBLE_DEVIATION;
    }
    // endregion

}
