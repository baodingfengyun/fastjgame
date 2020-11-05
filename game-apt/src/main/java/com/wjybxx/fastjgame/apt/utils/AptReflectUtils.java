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

package com.wjybxx.fastjgame.apt.utils;

import com.google.common.primitives.Ints;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.function.Supplier;

/**
 * 为生成代码服务器的注解处理器工具类 - 手写代码最好不要使用该类。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/18
 * github - https://github.com/hl845740757
 */
@SuppressWarnings("unused")
public class AptReflectUtils {

    /**
     * An empty immutable {@code Object} array.
     */
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    /**
     * 获取对的无参构造方法
     * 生成的代码调用
     */
    public static <T> Constructor<T> getNoArgsConstructor(Class<T> clazz) {
        try {
            final Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException e) {
            return rethrow(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <R, T extends Throwable> R rethrow(final Throwable throwable) throws T {
        throw (T) throwable;
    }

    /**
     * 生成的代码调用
     */
    public static Field getDeclaredField(Class<?> clazz, String fieldName) {
        try {
            final Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (Throwable e) {
            return rethrow(e);
        }
    }

    /**
     * Returns a capacity that is sufficient to keep the map from being resized as long as it grows no
     * larger than expectedSize and the load factor is ≥ its default (0.75).
     */
    public static int capacity(int expectedSize) {
        if (expectedSize < 3) {
            return 3;
        }

        if (expectedSize < Ints.MAX_POWER_OF_TWO) {
            // This is the calculation used in JDK8 to resize when a putAll
            // happens; it seems to be the most conservative calculation we
            // can make.  0.75 is the default load factor.
            return (int) ((float) expectedSize / 0.75F + 1.0F);
        }

        return Integer.MAX_VALUE; // any large value
    }

    public static <E extends Enum<E>> Supplier<EnumSet<E>> enumSetFactory(Class<E> type) {
        return () -> EnumSet.noneOf(type);
    }

    public static <K extends Enum<K>, V> Supplier<EnumMap<K, V>> enumMapFactory(Class<K> type) {
        return () -> new EnumMap<K, V>(type);
    }
}
