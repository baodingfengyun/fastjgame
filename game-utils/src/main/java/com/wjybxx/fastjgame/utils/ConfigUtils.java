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


import com.wjybxx.fastjgame.constants.UtilConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 配置工具类。提供一些特定的方法。
 * <p>
 * 如果保持代码的整洁性，可能会产生一些拆装箱。
 * 或者说需要包装类型的时候{@link #parseString(String, Function)}可能很有帮助。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/7 23:09
 * github - https://github.com/hl845740757
 */
public class ConfigUtils {

    private ConfigUtils() {

    }

    /**
     * 解析一个数字类型的字符串，当字符串为null 或 格式错误时，会抛出异常。
     *
     * @param content 字符串
     * @param parser  字符串解析方法
     * @param <T>     数字类型
     * @return number
     */
    public static <T> T parseString(String content, Function<String, T> parser) {
        String value = Objects.requireNonNull(content).trim();
        return parser.apply(value);
    }

    /**
     * 解析一个数字类型的字符串，当字符串为null 或 格式错误时，返回默认值。
     *
     * @param content      字符串
     * @param parser       字符串解析方法
     * @param defaultValue 当字符串为null 或 格式异常的时候返回该默认值
     * @param <T>          数字类型
     * @return number
     */
    public static <T extends Number> T parseNumber(String content, Function<String, T> parser, T defaultValue) {
        if (null == content) {
            return defaultValue;
        } else {
            try {
                return parser.apply(content.trim());
            } catch (NumberFormatException e) {
                // 出现异常，返回默认值
                return defaultValue;
            }
        }
    }

    public static String getAsString(String content, String defaultValue) {
        return null != content ? content : defaultValue;
    }

    // ------------------------------------------------------- 基本类型支持 ------------------------------------------

    /**
     * 解析一个int值，会自动调用{@link String#trim()}
     * {@link org.apache.commons.lang3.math.NumberUtils#toInt(String)}的区别在于不会默认返回0。
     * 在不期望返回默认值的时候返回默认值，可能导致潜在的错误！
     *
     * @param content 待解析的字符串
     * @return int
     */
    public static int getAsInt(String content) {
        return Integer.parseInt(content);
    }

    /**
     * 解析一个int值，会自动调用{@link String#trim()}，当解析失败时，会返回默认值。
     *
     * @param content      待解析的字符串
     * @param defaultValue 当字符串为null 或 格式异常的时候返回该默认值
     * @return int
     */
    public static int getAsInt(String content, int defaultValue) {
        return NumberUtils.toInt(content, defaultValue);
    }

    public static long getAsLong(String str) {
        return Long.parseLong(str);
    }

    public static long getAsLong(String str, long defaultValue) {
        return NumberUtils.toLong(str, defaultValue);
    }

    public static float getAsFloat(final String str) {
        return Float.parseFloat(str);
    }

    public static float getAsFloat(final String str, final float defaultValue) {
        return NumberUtils.toFloat(str, defaultValue);
    }

    public static double getAsDouble(final String str) {
        return Double.parseDouble(str);
    }

    public static double getAsDouble(final String str, final double defaultValue) {
        return NumberUtils.toDouble(str, defaultValue);
    }

    public static byte getAsByte(final String str) {
        return Byte.parseByte(str);
    }

    public static byte getAsByte(final String str, final byte defaultValue) {
        return NumberUtils.toByte(str, defaultValue);
    }

    public static short getAsShort(final String str) {
        return Short.parseShort(str);
    }

    public static short getAsShort(final String str, final short defaultValue) {
        return NumberUtils.toShort(str, defaultValue);
    }

    public static boolean getAsBool(String str) {
        String value = Objects.requireNonNull(str).trim();
        return value.equalsIgnoreCase("true")
                || value.equalsIgnoreCase("yes")
                || value.equalsIgnoreCase("1")
                || value.equalsIgnoreCase("y");
    }

    public static boolean getAsBool(String value, final boolean defaultValue) {
        if (null == value) {
            return defaultValue;
        }
        try {
            return getAsBool(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }
    // ----------------------------------------------- 基本类型数组支持 -------------------------------------------

    public static String[] getAsStringArray(String value) {
        return value.split(UtilConstants.DEFAULT_ARRAY_DELIMITER);
    }

    /**
     * @see it.unimi.dsi.fastutil.ints.IntArrayList#wrap(int[])
     */
    public static int[] getAsIntArray(String value) {
        String[] stringArray = getAsStringArray(value);
        int[] intArray = new int[stringArray.length];
        for (int index = 0; index < stringArray.length; index++) {
            intArray[index] = getAsInt(stringArray[index]);
        }
        return intArray;
    }

    /**
     * @see it.unimi.dsi.fastutil.longs.LongArrayList#wrap(long[])
     */
    public static long[] getAsLongArray(String value) {
        String[] stringArray = getAsStringArray(value);
        long[] longArray = new long[stringArray.length];
        for (int index = 0; index < stringArray.length; index++) {
            longArray[index] = getAsLong(stringArray[index]);
        }
        return longArray;
    }

    /**
     * @see it.unimi.dsi.fastutil.doubles.DoubleArrayList#wrap(double[])
     */
    public static double[] getAsDoubleArray(String value) {
        String[] stringArray = getAsStringArray(value);
        double[] doubleArray = new double[stringArray.length];
        for (int index = 0; index < stringArray.length; index++) {
            doubleArray[index] = getAsDouble(stringArray[index]);
        }
        return doubleArray;
    }

    // ------------------------------------------------ map支持 -------------------------------------------

    /**
     * 使用默认的数组分隔符和键值对分隔符解析字符串。
     *
     * @param content     字符串内容
     * @param keyParser   键解析器
     * @param valueParser 值解析器
     * @param <K>         键类型
     * @param <V>         值类型
     * @return map 保持有序
     */
    public static <K, V> Map<K, V> parseToMap(String content, Function<String, K> keyParser, Function<String, V> valueParser) {
        return parseToMap(content, UtilConstants.DEFAULT_ARRAY_DELIMITER, UtilConstants.DEFAULT_KEY_VALUE_DELIMITER,
                keyParser, valueParser);
    }

    /**
     * 使用指定的数组分隔符和键值对分隔符解析字符串;
     * 配合{@link #toString(Map, String, String, Function, Function)}
     *
     * @param content        字符串内容
     * @param arrayDelimiter 数组分隔符
     * @param kvDelimiter    键值对分隔符
     * @param keyParser      键类型映射
     * @param valueParser    值类型映射
     * @param <K>            键类型
     * @param <V>            值类型
     * @return Map NonNull 保持有序
     */
    public static <K, V> Map<K, V> parseToMap(String content, String arrayDelimiter, String kvDelimiter,
                                              Function<String, K> keyParser, Function<String, V> valueParser) {
        if (StringUtils.isBlank(content)) {
            return new LinkedHashMap<>();
        }
        String[] kvPairArray = content.split(arrayDelimiter);
        Map<K, V> result = CollectionUtils.newEnoughCapacityLinkedHashMap(kvPairArray.length);
        for (String kvPairStr : kvPairArray) {
            String[] kvPair = kvPairStr.split(kvDelimiter, 2);
            K key = keyParser.apply(kvPair[0]);
            // 校验重复
            if (result.containsKey(key)) {
                throw new RuntimeException(content + " find duplicate key " + kvPair[0]);
            }
            // 解析成功
            result.put(key, valueParser.apply(kvPair[1]));
        }
        return result;
    }

    /**
     * 将键值对数组格式化为键值对的数组字符串；
     * 配合{@link #parseToMap(String, String, String, Function, Function)}
     *
     * @param map            键值对
     * @param arrayDelimiter 数组分隔符
     * @param kvDelimiter    键值对分隔符
     * @param keyMapper      键类型映射
     * @param valueMapper    值类型映射
     * @param <K>            键类型
     * @param <V>            值类型
     * @return String NonNull
     */
    public static <K, V> String toString(Map<K, V> map, String arrayDelimiter, String kvDelimiter,
                                         Function<K, String> keyMapper, Function<V, String> valueMapper) {
        if (null == map || map.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            // eg: k=v;
            if (sb.length() > 0) {
                sb.append(arrayDelimiter);
            }
            // eg: k=v 这里其实校验是否产生了重复的key字符串会更好
            sb.append(keyMapper.apply(entry.getKey()));
            sb.append(kvDelimiter);
            sb.append(valueMapper.apply(entry.getValue()));
        }
        return sb.toString();
    }

}

