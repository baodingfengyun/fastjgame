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
package com.wjybxx.fastjgame.util.config;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import org.apache.commons.lang3.math.NumberUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 基于字符串键值对配置的帮助类。
 * 注意：该层面的API均不修改对象的状态。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/11 16:05
 * github - https://github.com/hl845740757
 */
public abstract class Params {

    @Nullable
    public abstract String getAsString(String key);

    /**
     * 返回所有的键的集合。
     */
    public abstract Set<String> keys();

    /**
     * 判断一个键是否存在
     */
    public boolean containsKey(String key) {
        return null != getAsString(key);
    }

    // region 基本值类型
    public String getAsString(String key, String defaultValue) {
        final String value = getAsString(key);
        return null != value ? value : defaultValue;
    }

    public int getAsInt(String key) {
        final String value = getAsString(key);
        Objects.requireNonNull(value);
        return Integer.parseInt(value);
    }

    public int getAsInt(String key, int defaultValue) {
        return NumberUtils.toInt(getAsString(key), defaultValue);
    }

    public long getAsLong(String key) {
        final String value = getAsString(key);
        Objects.requireNonNull(value);
        return Long.parseLong(value);
    }

    public long getAsLong(String key, long defaultValue) {
        return NumberUtils.toLong(getAsString(key), defaultValue);
    }

    public float getAsFloat(String key) {
        final String value = getAsString(key);
        Objects.requireNonNull(value);
        return Float.parseFloat(value);
    }

    public float getAsFloat(String key, float defaultValue) {
        return NumberUtils.toFloat(getAsString(key), defaultValue);
    }

    public double getAsDouble(String key) {
        final String value = getAsString(key);
        Objects.requireNonNull(value);
        return Double.parseDouble(value);
    }

    public double getAsDouble(String key, double defaultValue) {
        return NumberUtils.toDouble(getAsString(key), defaultValue);
    }

    public boolean getAsBool(String key) {
        final String value = getAsString(key);
        Objects.requireNonNull(value);
        return parseBool(value);
    }

    public boolean getAsBool(String key, final boolean defaultValue) {
        final String value = getAsString(key);
        return null == value ? defaultValue : parseBool(value);
    }

    public short getAsShort(String key) {
        final String value = getAsString(key);
        Objects.requireNonNull(value);
        return Short.parseShort(value);
    }

    public short getAsShort(String key, short defaultValue) {
        return NumberUtils.toShort(getAsString(key), defaultValue);
    }

    public byte getAsByte(String key) {
        final String value = getAsString(key);
        Objects.requireNonNull(value);
        return Byte.parseByte(value);
    }

    public byte getAsByte(String key, byte defaultValue) {
        return NumberUtils.toByte(getAsString(key), defaultValue);
    }
    // endregion

    // region 获取为数组类型

    public IntList getAsIntArray(String key) {
        final String value = Objects.requireNonNull(getAsString(key));
        final List<String> stringArray = parseList(value);
        final IntList result = new IntArrayList(stringArray.size());
        for (String e : stringArray) {
            result.add(Integer.parseInt(e));
        }
        return result;
    }

    public LongList getAsLongArray(String key) {
        final String value = Objects.requireNonNull(getAsString(key));
        final List<String> stringArray = parseList(value);
        final LongList result = new LongArrayList(stringArray.size());
        for (String e : stringArray) {
            result.add(Long.parseLong(e));
        }
        return result;
    }

    public FloatList getAsFloatArray(String key) {
        final String value = Objects.requireNonNull(getAsString(key));
        final List<String> stringArray = parseList(value);
        final FloatList result = new FloatArrayList(stringArray.size());
        for (String e : stringArray) {
            result.add(Float.parseFloat(e));
        }
        return result;
    }

    public DoubleList getAsDoubleArray(String key) {
        final String value = Objects.requireNonNull(getAsString(key));
        final List<String> stringArray = parseList(value);
        final DoubleList result = new DoubleArrayList(stringArray.size());
        for (String e : stringArray) {
            result.add(Double.parseDouble(e));
        }
        return result;
    }

    // endregion

    public Map<String, String> getAsMap(String key) {
        final String value = Objects.requireNonNull(getAsString(key));
        return parseMap(value);
    }

    /**
     * 将拥有的key-value转为一个map
     */
    public Map<String, String> toMap() {
        final Set<String> keys = keys();
        final Map<String, String> result = Maps.newLinkedHashMapWithExpectedSize(keys.size());
        for (String key : keys) {
            result.put(key, getAsString(key));
        }
        return result;
    }

    // 内部实现
    protected abstract boolean parseBool(@Nonnull String value);

    @Nonnull
    protected abstract List<String> parseList(@Nonnull String value);

    @Nonnull
    protected abstract Map<String, String> parseMap(@Nonnull String value);

}
