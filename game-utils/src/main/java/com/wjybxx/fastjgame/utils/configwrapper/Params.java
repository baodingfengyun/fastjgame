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
package com.wjybxx.fastjgame.utils.configwrapper;

import com.wjybxx.fastjgame.utils.ConfigUtils;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * 基于字符串键值对配置的帮助类。
 * <p>
 * 注意：数组分隔为{@link ConfigUtils#DEFAULT_ARRAY_DELIMITER} 即'|'
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/11 16:05
 * github - https://github.com/hl845740757
 */
public abstract class Params {

    /**
     * 如果属性名获取属性的值，如果不存在则返回null
     * 子类可以有不同的存储结构，这里需要自己实现。
     *
     * @param key 键
     * @return value
     */
    @Nullable
    public abstract String getAsString(String key);

    /**
     * 判断一个键是否存在
     */
    public boolean containsKey(String key) {
        return null != getAsString(key);
    }

    /**
     * 返回所有的键的集合。
     */
    public abstract Set<String> keys();

    public String getAsString(String key, String defaultValue) {
        return ConfigUtils.getAsString(getAsString(key), defaultValue);
    }

    // region 对基本类型的支持
    public int getAsInt(String key) {
        return ConfigUtils.getAsInt(getAsString(key));
    }

    public int getAsInt(String key, int defaultValue) {
        return ConfigUtils.getAsInt(getAsString(key), defaultValue);
    }

    public long getAsLong(String key) {
        return ConfigUtils.getAsLong(getAsString(key));
    }

    public long getAsLong(String key, long defaultValue) {
        return ConfigUtils.getAsLong(getAsString(key), defaultValue);
    }

    public double getAsDouble(String key) {
        return ConfigUtils.getAsDouble(getAsString(key));
    }

    public double getAsDouble(String key, double defaultValue) {
        return ConfigUtils.getAsDouble(getAsString(key), defaultValue);
    }

    public byte getAsByte(String key) {
        return ConfigUtils.getAsByte(getAsString(key));
    }

    public byte getAsByte(String key, byte defaultValue) {
        return ConfigUtils.getAsByte(getAsString(key), defaultValue);
    }

    public short getAsShort(String key) {
        return ConfigUtils.getAsShort(getAsString(key));
    }

    public short getAsShort(String key, short defaultValue) {
        return ConfigUtils.getAsShort(getAsString(key), defaultValue);
    }

    public float getAsFloat(String key) {
        return ConfigUtils.getAsFloat(getAsString(key));
    }

    public float getAsFloat(String key, float defaultValue) {
        return ConfigUtils.getAsFloat(getAsString(key), defaultValue);
    }

    /**
     * 字符串是否表示true
     *
     * @param key name
     * @return true, yes, 1, y表示为真，其余为假。
     */
    public boolean getAsBool(String key) {
        return ConfigUtils.getAsBool(getAsString(key));
    }

    /**
     * 字符串是否表示true
     *
     * @param key          name
     * @param defaultValue 如果不存在指定名字的属性，则返回默认值
     * @return true, yes, 1, y表示为真，其余为假。
     */
    public boolean getAsBool(String key, boolean defaultValue) {
        return ConfigUtils.getAsBool(getAsString(key), defaultValue);
    }
    // endregion

    // region 获取为数组类型
    public String[] getAsStringArray(String key) {
        return ConfigUtils.getAsStringArray(getAsString(key));
    }

    public int[] getAsIntArray(String key) {
        return ConfigUtils.getAsIntArray(getAsString(key));
    }

    public long[] getAsLongArray(String key) {
        return ConfigUtils.getAsLongArray(getAsString(key));
    }

    public double[] getAsDoubleArray(String key) {
        return ConfigUtils.getAsDoubleArray(getAsString(key));
    }
    // endregion
}
