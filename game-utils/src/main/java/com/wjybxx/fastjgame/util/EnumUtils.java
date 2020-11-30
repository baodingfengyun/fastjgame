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

import com.google.common.collect.Sets;
import com.wjybxx.fastjgame.util.dsl.IndexableEnum;
import com.wjybxx.fastjgame.util.dsl.IndexableEnumMapper;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;
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
     * @param <T>    对象类型
     * @param values 对象集合
     * @param number 要查找的数字
     * @param func   类型到数字的映射
     * @return T
     */
    @Nullable
    public static <T> T forNumber(T[] values, int number, ToIntFunction<T> func) {
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

    /**
     * 查找指定元素
     *
     * @param values   枚举集合
     * @param expected 期望的比较值
     * @param func     映射函数
     * @return T
     */
    public static <T extends Enum<T>, E> T find(T[] values, @Nonnull E expected, Function<T, E> func) {
        for (T t : values) {
            if (expected.equals(func.apply(t))) {
                return t;
            }
        }
        return null;
    }

    /**
     * 检查枚举中的number是否存在重复
     */
    public static <T> void checkNumberDuplicate(T[] values, ToIntFunction<T> func) {
        final IntSet numberSet = new IntOpenHashSet(values.length);
        for (T t : values) {
            final int number = func.applyAsInt(t);
            if (!numberSet.add(number)) {
                final String msg = String.format("The number is duplicate, num: %d, enum: %s", number, t.toString());
                throw new IllegalArgumentException(msg);
            }
        }
    }

    /**
     * 检查枚举中的某个值是否重复
     */
    public static <T, R> void checkFieldDuplicate(T[] values, Function<T, R> func) {
        final Set<R> fieldSet = Sets.newHashSetWithExpectedSize(values.length);
        for (T t : values) {
            final R field = func.apply(t);
            if (field == null) {
                throw new NullPointerException(t.toString());
            }

            if (!fieldSet.add(field)) {
                final String msg = String.format("Field is duplicate, num: %s, enum: %s", field, t.toString());
                throw new IllegalArgumentException(msg);
            }
        }
    }

    /**
     * 检查枚举中的number是否连续
     */
    public static <T> void checkNumberContinuity(final T[] originValues, ToIntFunction<T> func) {
        if (originValues.length == 0) {
            return;
        }
        // 避免修改原数组
        final T[] clone = originValues.clone();
        Arrays.sort(clone, Comparator.comparingInt(func));
        for (int index = 0; index < clone.length - 1; index++) {
            if (func.applyAsInt(clone[index]) + 1 != func.applyAsInt(clone[index + 1])) {
                throw new IllegalArgumentException("the number or values is not Continuity, value: " + clone[index]);
            }
        }
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

    private static <T extends IndexableEnum> int minNumber(T[] values) {
        return Arrays.stream(values)
                .mapToInt(IndexableEnum::getNumber)
                .min()
                .orElseThrow();
    }

    private static <T extends IndexableEnum> int maxNumber(T[] values) {
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

    /**
     * @return 用枚举的number作为下标的数组
     */
    public static <T extends IndexableEnum> T[] lookup(final T[] values) {
        final int minNumber = minNumber(values);
        final int maxNumber = maxNumber(values);

        if (maxNumber - minNumber - values.length > 256) {
            throw new IllegalArgumentException("lookup is not a good idea, the elements are sparse");
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

    private static class EmptyMapper<T extends IndexableEnum> implements IndexableEnumMapper<T> {

        private static final EmptyMapper<?> INSTANCE = new EmptyMapper<>();

        private EmptyMapper() {
        }

        @Nullable
        @Override
        public T forNumber(int number) {
            return null;
        }

        @Override
        public List<T> values() {
            return Collections.emptyList();
        }
    }

    /**
     * 基于数组的映射，对于数量少的枚举效果好；
     * (可能存在一定空间浪费，空间换时间，如果数字基本连续，那么空间利用率很好)
     */
    private static class ArrayBasedMapper<T extends IndexableEnum> implements IndexableEnumMapper<T> {

        private static final float DEFAULT_FACTOR = 0.5f;
        private static final float MIN_FACTOR = 0.25f;

        private final List<T> values;
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
            this.values = List.of(values);
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
        public List<T> values() {
            return values;
        }

        private int toIndex(int number) {
            return number - minNumber;
        }

        private static boolean matchDefaultFactor(int minNumber, int maxNumber, int length) {
            return matchFactor(minNumber, maxNumber, length, DEFAULT_FACTOR);
        }

        private static boolean matchMinFactor(int minNumber, int maxNumber, int length) {
            return matchFactor(minNumber, maxNumber, length, MIN_FACTOR);
        }

        private static boolean matchFactor(int minNumber, int maxNumber, int length, float factor) {
            return length >= Math.ceil(capacity(minNumber, maxNumber) * factor);
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

        private final List<T> values;
        private final Int2ObjectMap<T> mapping;

        private MapBasedMapper(T[] values, Int2ObjectMap<T> mapping) {
            this.values = List.of(values);
            this.mapping = mapping;
        }

        @Nullable
        @Override
        public T forNumber(int number) {
            return mapping.get(number);
        }

        @Override
        public List<T> values() {
            return values;
        }
    }
}
