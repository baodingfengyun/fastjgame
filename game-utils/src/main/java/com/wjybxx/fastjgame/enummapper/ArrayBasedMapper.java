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

package com.wjybxx.fastjgame.enummapper;

import javax.annotation.Nullable;
import java.lang.reflect.Array;

/**
 * 基于数组的映射，对于数量少的枚举效果好；
 * (可能存在一定空间浪费，空间换时间，如果数字基本连续，那么空间利用率很好)
 * @param <T>
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/4 15:48
 * github - https://github.com/hl845740757
 */
public class ArrayBasedMapper<T extends NumberEnum> implements NumberEnumMapper<T>{

    /**
     * 最小空间资源利用率，小于该值空间浪费太大
     */
    private static final float THRESHOLD = 0.7f;

    private final T[] elements;

    private final int minNumber;
    private final int maxNumber;

    /**
     * new instance
     * 构造对象之前必须调用{@link #available(int, int, int)}
     * @param values 枚举的所有元素
     * @param minNumber 枚举中的最小number
     * @param maxNumber 枚举中的最大number
     */
    @SuppressWarnings("unchecked")
    public ArrayBasedMapper(T[] values,int minNumber,int maxNumber) {
        assert available(minNumber,maxNumber, values.length);

        this.minNumber = minNumber;
        this.maxNumber = maxNumber;

        // 数组真实长度
        int capacity = capacity(minNumber, maxNumber);
        this.elements = (T[]) Array.newInstance(values.getClass().getComponentType(),capacity);

        // 存入数组
        for (T e:values){
            this.elements[toIndex(e.getNumber())] = e;
        }
    }

    @Nullable
    @Override
    public T forNumber(int number) {
        if (number < minNumber || number > maxNumber){
            return null;
        }
        return elements[toIndex(number)];
    }

    private int toIndex(int number) {
        return number - minNumber;
    }

    /**
     * 是否可以使用基于数组的映射
     * @param minNumber num的最小值
     * @param maxNumber num的最大值
     * @param length 元素个数
     * @return 如果空间利用率能达到期望的话，返回true。
     */
    public static boolean available(int minNumber, int maxNumber, int length){
        return length >= Math.ceil(capacity(minNumber, maxNumber) * THRESHOLD);
    }

    /**
     * 计算需要的容量
     * @param minNumber num的最小值
     * @param maxNumber num的最大值
     * @return capacity
     */
    private static int capacity(int minNumber, int maxNumber) {
        return maxNumber - minNumber + 1;
    }
}
