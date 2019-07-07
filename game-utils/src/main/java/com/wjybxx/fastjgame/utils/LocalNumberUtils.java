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

package com.wjybxx.fastjgame.utils;


import org.apache.commons.lang3.math.NumberUtils;

import java.util.Objects;
import java.util.function.Function;

/**
 * 本地的NumberUtils。提供一些特定的方法。
 *
 * 我们或策划在配置文件的时候，很容易多个空格什么的。我们可能需要自动的去除这些空格。
 *
 * 如果保持代码的整洁性，可能会产生一些拆装箱。
 * 或者说需要包装类型的时候{@link #parseNumber(String, Function)}可能很有帮助。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/7 23:09
 * github - https://github.com/hl845740757
 */
public class LocalNumberUtils {

    private LocalNumberUtils() {

    }

    /**
     * 解析一个数字类型的字符串，当字符串为null 或 格式错误时，会抛出异常。
     * @param content 字符串
     * @param parser 字符串解析方法
     * @param <T> 数字类型
     * @return number
     */
    public static <T> T parseNumber(String content, Function<String,T> parser) {
        String value = Objects.requireNonNull(content).trim();
        return parser.apply(value);
    }

    /**
     * 解析一个数字类型的字符串，当字符串为null 或 格式错误时，返回默认值。
     * @param content 字符串
     * @param parser 字符串解析方法
     * @param defaultValue 当字符串为null 或 格式异常的时候返回该默认值
     * @param <T> 数字类型
     * @return number
     */
    public static <T extends Number> T parseNumber(String content, Function<String,T> parser, T defaultValue) {
        if (null == content){
            return defaultValue;
        } else {
            try {
                return parser.apply(content.trim());
            } catch (NumberFormatException e){
                // 出现异常，返回默认值
                return defaultValue;
            }
        }
    }

    /**
     * 获取真正的内容，如果字符串不为null，则调用一次trim
     * @param content 待解析的字符串
     * @return 校验后的字符串
     */
    private static String tryTrim(String content){
        return null == content? null : content.trim();
    }

    /**
     * 解析一个int值，会自动调用{@link String#trim()}
     * {@link org.apache.commons.lang3.math.NumberUtils#toInt(String)}的区别在于不会默认返回0。
     * 在不期望返回默认值的时候返回默认值，可能导致潜在的错误！
     *
     * @param content 待解析的字符串
     * @return int
     */
    public static int toInt(String content) {
        return Integer.parseInt(tryTrim(content));
    }

    /**
     * 解析一个int值，会自动调用{@link String#trim()}，当解析失败时，会返回默认值。
     * @param content 待解析的字符串
     * @param defaultValue 当字符串为null 或 格式异常的时候返回该默认值
     * @return int
     */
    public static int toInt(String content, int defaultValue) {
        return NumberUtils.toInt(tryTrim(content), defaultValue);
    }

    public static long toLong(String str) {
        return Long.parseLong(tryTrim(str));
    }

    public static long toLong(String str, long defaultValue) {
        return NumberUtils.toLong(tryTrim(str), defaultValue);
    }

    public static float toFloat(final String str) {
        return Float.parseFloat(tryTrim(str));
    }

    public static float toFloat(final String str, final float defaultValue) {
        return NumberUtils.toFloat(tryTrim(str), defaultValue);
    }

    public static double toDouble(final String str) {
        return Double.parseDouble(tryTrim(str));
    }

    public static double toDouble(final String str, final double defaultValue) {
        return NumberUtils.toDouble(tryTrim(str), defaultValue);
    }

    public static byte toByte(final String str) {
        return Byte.parseByte(tryTrim(str));
    }

    public static byte toByte(final String str, final byte defaultValue) {
       return NumberUtils.toByte(tryTrim(str), defaultValue);
    }

    public static short toShort(final String str) {
        return Short.parseShort(tryTrim(str));
    }

    public static short toShort(final String str, final short defaultValue) {
        return NumberUtils.toShort(tryTrim(str), defaultValue);
    }

}

