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

import com.wjybxx.fastjgame.utils.entity.NumericalEntity;
import com.wjybxx.fastjgame.utils.entity.NumericalEntityMapper;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

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

    /**
     * 查找指定数字的数字枚举
     *
     * @param values 数字枚举集合
     * @param number 要查找的数字
     * @param <T>    对象类型
     * @return T
     */
    @Nullable
    public static <T extends NumericalEntity> T forNumber(T[] values, int number) {
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

    /**
     * 根据枚举的values建立索引；
     * 该方法的开销相对小，代码量也能省下；
     *
     * @param values 枚举数组
     * @param <T>    枚举类型
     * @return unmodifiable
     */
    public static <T extends NumericalEntity> NumericalEntityMapper<T> mapping(T[] values) {
        if (values.length == 0) {
            @SuppressWarnings("unchecked") final NumericalEntityMapper<T> mapper = (NumericalEntityMapper<T>) EmptyMapper.INSTANCE;
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

        final int minNumber = Arrays.stream(values)
                .mapToInt(NumericalEntity::getNumber)
                .min()
                .getAsInt();

        final int maxNumber = Arrays.stream(values)
                .mapToInt(NumericalEntity::getNumber)
                .max()
                .getAsInt();

        if (ArrayBasedEnumMapper.available(minNumber, maxNumber, values.length)) {
            return new ArrayBasedEnumMapper<>(values, minNumber, maxNumber);
        } else {
            return new MapBasedMapper<>(values, result);
        }
    }

    private static class EmptyMapper<T extends NumericalEntity> implements NumericalEntityMapper<T> {

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
    private static class ArrayBasedEnumMapper<T extends NumericalEntity> implements NumericalEntityMapper<T> {

        /**
         * 最小空间资源利用率，小于该值空间浪费太大
         */
        private static final float THRESHOLD = 0.7f;

        private final T[] values;
        private final T[] elements;

        private final int minNumber;
        private final int maxNumber;

        /**
         * new instance
         * 构造对象之前必须调用{@link #available(int, int, int)}
         *
         * @param values    枚举的所有元素
         * @param minNumber 枚举中的最小number
         * @param maxNumber 枚举中的最大number
         */
        @SuppressWarnings("unchecked")
        private ArrayBasedEnumMapper(T[] values, int minNumber, int maxNumber) {
            assert available(minNumber, maxNumber, values.length);

            this.values = values;
            this.minNumber = minNumber;
            this.maxNumber = maxNumber;

            // 数组真实长度
            int capacity = capacity(minNumber, maxNumber);
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

        /**
         * 是否可以使用基于数组的映射
         *
         * @param minNumber num的最小值
         * @param maxNumber num的最大值
         * @param length    元素个数
         * @return 如果空间利用率能达到期望的话，返回true。
         */
        private static boolean available(int minNumber, int maxNumber, int length) {
            return length >= Math.ceil(capacity(minNumber, maxNumber) * THRESHOLD);
        }

        /**
         * 计算需要的容量
         *
         * @param minNumber num的最小值
         * @param maxNumber num的最大值
         * @return capacity
         */
        private static int capacity(int minNumber, int maxNumber) {
            return maxNumber - minNumber + 1;
        }
    }

    /**
     * 基于map的映射。
     * 对于枚举值较多或数字取值范围散乱的枚举适合；
     */
    private static class MapBasedMapper<T extends NumericalEntity> implements NumericalEntityMapper<T> {

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
