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

package com.wjybxx.fastjgame.utils.config;


import com.wjybxx.fastjgame.utils.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 配置工具类。提供一些特定的方法。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/7 23:09
 * github - https://github.com/hl845740757
 */
public class ConfigUtils {

    /**
     * 默认数组分隔符 '|'
     * 逗号在某些场合下效果不好，逗号使用面太广。
     */
    public static final String DEFAULT_ARRAY_DELIMITER = "\\|";
    /**
     * 默认键值对分隔符, '=' 与 ':' 都是不错的选择， ':'更贴近于json
     */
    public static final String DEFAULT_KEY_VALUE_DELIMITER = "=";

    private ConfigUtils() {

    }

    public static String getAsString(String content, String defaultValue) {
        return null != content ? content : defaultValue;
    }
    // ------------------------------------------------------- 基本类型支持 ------------------------------------------

    /**
     * 解析一个int值
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
        if (null == value) {
            throw new NullPointerException("value");
        }
        return value.split(DEFAULT_ARRAY_DELIMITER);
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
     * @param keyMapper   键类型映射
     * @param valueMapper 值类型映射
     * @param <K>         键类型
     * @param <V>         值类型
     * @return map 保持有序
     */
    public static <K, V> Map<K, V> parseToMap(String content, Function<String, K> keyMapper, Function<String, V> valueMapper) {
        return parseToMap(content, DEFAULT_ARRAY_DELIMITER, DEFAULT_KEY_VALUE_DELIMITER,
                keyMapper, valueMapper);
    }

    /**
     * 使用指定的数组分隔符和键值对分隔符解析字符串;
     * 配合{@link #toString(Map, String, String, Function, Function)}
     *
     * @param content        字符串内容
     * @param arrayDelimiter 数组分隔符
     * @param kvDelimiter    键值对分隔符
     * @param keyMapper      键类型映射
     * @param valueMapper    值类型映射
     * @param <K>            键类型
     * @param <V>            值类型
     * @return Map NonNull 保持有序
     */
    public static <K, V> Map<K, V> parseToMap(String content, String arrayDelimiter, String kvDelimiter,
                                              Function<String, K> keyMapper, Function<String, V> valueMapper) {
        if (StringUtils.isBlank(content)) {
            return new LinkedHashMap<>();
        }
        String[] kvPairArray = content.split(arrayDelimiter);
        Map<K, V> result = CollectionUtils.newLinkedHashMapWithExpectedSize(kvPairArray.length);
        for (String kvPairStr : kvPairArray) {
            String[] kvPair = kvPairStr.split(kvDelimiter, 2);
            K key = keyMapper.apply(kvPair[0]);
            // 校验重复
            if (result.containsKey(key)) {
                throw new RuntimeException(content + " find duplicate key " + kvPair[0]);
            }
            // 解析成功
            result.put(key, valueMapper.apply(kvPair[1]));
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
     * @param keyEncoder     键类型编码器
     * @param valueEncoder   值类型编码器
     * @param <K>            键类型
     * @param <V>            值类型
     * @return String NonNull
     */
    public static <K, V> String toString(Map<K, V> map, String arrayDelimiter, String kvDelimiter,
                                         Function<K, String> keyEncoder, Function<V, String> valueEncoder) {
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
            sb.append(keyEncoder.apply(entry.getKey()));
            sb.append(kvDelimiter);
            sb.append(valueEncoder.apply(entry.getValue()));
        }
        return sb.toString();
    }

}

