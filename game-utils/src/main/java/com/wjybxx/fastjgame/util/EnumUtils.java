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

package com.wjybxx.fastjgame.util;

import com.wjybxx.fastjgame.util.dsl.IndexableEnum;
import com.wjybxx.fastjgame.util.dsl.IndexableEnumMapper;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.function.ToIntFunction;

/**
 * 枚举辅助类
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/22 17:51
 * github - https://github.com/hl845740757
 */
public class EnumUtils {

    private EnumUtils() {
        // close
    }

    public static <T extends IndexableEnum> void checkNumberDuplicate(T[] values) {
        checkNumberDuplicate(values, IndexableEnum::getNumber);
    }

    public static <T> void checkNumberDuplicate(T[] values, ToIntFunction<T> func) {
        final IntSet numberSet = new IntOpenHashSet(values.length);
        for (T t : values) {
            final int number = func.applyAsInt(t);
            if (!numberSet.add(number)) {
                final String msg = String.format("The number %d of %s is duplicate", number, t.toString());
                throw new IllegalArgumentException(msg);
            }
        }
    }

    /**
     * 查找指定数字的枚举
     *
     * @param values 数字枚举集合
     * @param number 要查找的数字
     * @param <T>    对象类型
     * @return T
     */
    @Nullable
    public static <T extends IndexableEnum> T forNumber(T[] values, int number) {
        for (T t : values) {
            if (t.getNumber() == number) {
                return t;
            }
        }
        return null;
    }

    /**
     * 查找对应数字的对象
     *
     * @param values 对象集合
     * @param func   类型到数字的映射
     * @param number 要查找的数字
     * @param <T>    对象类型
     * @return T
     */
    @Nullable
    public static <T> T forNumber(T[] values, ToIntFunction<T> func, int number) {
        for (T t : values) {
            if (func.applyAsInt(t) == number) {
                return t;
            }
        }
        return null;
    }

    /**
     * 通过名字查找枚举。
     * 与{@link Enum#valueOf(Class, String)}区别在于返回null代替抛出异常。
     *
     * @param values 枚举集合
     * @param name   要查找的枚举名字
     * @param <T>    枚举类型
     * @return T
     */
    @Nullable
    public static <T extends Enum<T>> T forName(T[] values, String name) {
        for (T t : values) {
            if (t.name().equals(name)) {
                return t;
            }
        }
        return null;
    }

    /**
     * 通过名字查找枚举(忽略名字的大小写)。
     * 与{@link Enum#valueOf(Class, String)}区别在于返回null代替抛出异常。
     *
     * @param values 枚举集合
     * @param name   要查找的枚举名字
     * @param <T>    枚举类型
     * @return T
     */
    public static <T extends Enum<T>> T forNameIgnoreCase(T[] values, String name) {
        for (T t : values) {
            if (t.name().equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null;
    }

    public static <T extends IndexableEnum> T[] lookup(final T[] values) {
        final int minNumber = minNumber(values);
        final int maxNumber = maxNumber(values);

        if (maxNumber - minNumber - values.length > 256) {
            throw new IllegalArgumentException("lookup is not a good idea");
        }

        final IntOpenHashSet numberSet = new IntOpenHashSet(values.length);
        for (T t : values) {
            if (t.getNumber() < 0) {
                throw new IllegalArgumentException(t.getClass().getSimpleName() + " number:" + t.getNumber() + " is negative");
            }
            if (!numberSet.add(t.getNumber())) {
                throw new IllegalArgumentException(t.getClass().getSimpleName() + " number:" + t.getNumber() + " is duplicate");
            }
        }

        // 必定相同的类型
        @SuppressWarnings("unchecked") final T[] result = (T[]) Array.newInstance(values.getClass().getComponentType(), maxNumber + 1);
        for (T t : values) {
            result[t.getNumber()] = t;
        }
        return result;
    }


    /**
     * 根据枚举的values建立索引；
     *
     * @param values 枚举数组
     * @param <T>    枚举类型
     * @return unmodifiable
     */
    public static <T extends IndexableEnum> IndexableEnumMapper<T> mapping(final T[] values) {
        return mapping(values, false);
    }

    /**
     * 根据枚举的values建立索引；
     *
     * @param values    枚举数组
     * @param fastQuery 是否追求极致的查询性能
     * @param <T>       枚举类型
     * @return unmodifiable
     */
    public static <T extends IndexableEnum> IndexableEnumMapper<T> mapping(final T[] values, final boolean fastQuery) {
        if (values.length == 0) {
            @SuppressWarnings("unchecked") final IndexableEnumMapper<T> mapper = (IndexableEnumMapper<T>) EmptyMapper.INSTANCE;
            return mapper;
        }

        // 结果不一定用得上，存在一定的浪费，但必须检测重复
        final Int2ObjectMap<T> result = new Int2ObjectOpenHashMap<>(values.length);
        for (T t : values) {
            if (result.containsKey(t.getNumber())) {
                throw new IllegalArgumentException(t.getClass().getSimpleName() + " number:" + t.getNumber() + " is duplicate");
            }
            result.put(t.getNumber(), t);
        }

        final int minNumber = minNumber(values);
        final int maxNumber = maxNumber(values);

        // 保护性拷贝，避免出现并发问题 - 不确定values()是否会被修改
        final T[] copiedValues = Arrays.copyOf(values, values.length);
        if (isArrayAvailable(minNumber, maxNumber, values.length, fastQuery)) {
            return new ArrayBasedMapper<>(copiedValues, minNumber, maxNumber);
        } else {
            return new MapBasedMapper<>(copiedValues, result);
        }
    }

    public static <T extends IndexableEnum> int minNumber(T[] values) {
        return Arrays.stream(values)
                .mapToInt(IndexableEnum::getNumber)
                .min()
                .orElseThrow();
    }

    public static <T extends IndexableEnum> int maxNumber(T[] values) {
        return Arrays.stream(values)
                .mapToInt(IndexableEnum::getNumber)
                .max()
                .orElseThrow();
    }

    private static boolean isArrayAvailable(int minNumber, int maxNumber, int length, boolean fastQuery) {
        if (ArrayBasedMapper.matchDefaultFactor(minNumber, maxNumber, length)) {
            return true;
        }
        if (fastQuery && ArrayBasedMapper.matchMinFactor(minNumber, maxNumber, length)) {
            return true;
        }
        return false;
    }

    private static class EmptyMapper<T extends IndexableEnum> implements IndexableEnumMapper<T> {

        private static final EmptyMapper<?> INSTANCE = new EmptyMapper<>();
        private static final Object[] EMPTY_ARRAY = new Object[0];

        private EmptyMapper() {
        }

        @Nullable
        @Override
        public T forNumber(int number) {
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public T[] values() {
            return (T[]) EMPTY_ARRAY;
        }
    }

    /**
     * 基于数组的映射，对于数量少的枚举效果好；
     * (可能存在一定空间浪费，空间换时间，如果数字基本连续，那么空间利用率很好)
     */
    private static class ArrayBasedMapper<T extends IndexableEnum> implements IndexableEnumMapper<T> {

        private static final float DEFAULT_FACTOR = 0.5f;
        private static final float MIN_FACTOR = 0.25f;

        private final T[] values;
        private final T[] elements;

        private final int minNumber;
        private final int maxNumber;

        /**
         * @param values    枚举的所有元素
         * @param minNumber 枚举中的最小number
         * @param maxNumber 枚举中的最大number
         */
        @SuppressWarnings("unchecked")
        private ArrayBasedMapper(T[] values, int minNumber, int maxNumber) {
            this.values = values;
            this.minNumber = minNumber;
            this.maxNumber = maxNumber;

            // 数组真实长度
            final int capacity = capacity(minNumber, maxNumber);
            this.elements = (T[]) Array.newInstance(values.getClass().getComponentType(), capacity);

            // 存入数组
            for (T e : values) {
                this.elements[toIndex(e.getNumber())] = e;
            }
        }

        @Nullable
        @Override
        public T forNumber(int number) {
            if (number < minNumber || number > maxNumber) {
                return null;
            }
            return elements[toIndex(number)];
        }

        @Override
        public T[] values() {
            return values;
        }

        private int toIndex(int number) {
            return number - minNumber;
        }

        private static boolean matchDefaultFactor(int minNumber, int maxNumber, int length) {
            return length >= Math.ceil(capacity(minNumber, maxNumber) * DEFAULT_FACTOR);
        }

        private static boolean matchMinFactor(int minNumber, int maxNumber, int length) {
            return length >= Math.ceil(capacity(minNumber, maxNumber) * MIN_FACTOR);
        }

        private static int capacity(int minNumber, int maxNumber) {
            return maxNumber - minNumber + 1;
        }
    }

    /**
     * 基于map的映射。
     * 对于枚举值较多或数字取值范围散乱的枚举适合。
     */
    private static class MapBasedMapper<T extends IndexableEnum> implements IndexableEnumMapper<T> {

        private final T[] values;
        private final Int2ObjectMap<T> mapping;

        private MapBasedMapper(T[] values, Int2ObjectMap<T> mapping) {
            this.values = values;
            this.mapping = mapping;
        }

        @Nullable
        @Override
        public T forNumber(int number) {
            return mapping.get(number);
        }

        @Override
        public T[] values() {
            return values;
        }
    }
}
