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

import com.wjybxx.fastjgame.enummapper.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import javax.annotation.Nullable;
import java.util.function.ToIntFunction;

/**
 * 枚举辅助类
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
     * @param values 数字枚举集合
     * @param number 要查找的数字
     * @param <T> 对象类型
     * @return T
     */
    @Nullable
    public static <T extends NumberEnum> T forNumber(T[] values, int number) {
        return forNumber(values, NumberEnum::getNumber, number);
    }

    /**
     * 查找对应数字的对象
     * @param values 对象集合
     * @param func 类型到数字的映射
     * @param number 要查找的数字
     * @param <T> 对象类型
     * @return T
     */
    @Nullable
    public static <T> T forNumber(T[] values, ToIntFunction<T> func, int number) {
        for (T t : values){
            if (func.applyAsInt(t) == number){
                return t;
            }
        }
        return null;
    }

    /**
     * 通过名字查找枚举。
     * 与{@link Enum#valueOf(Class, String)}区别在于返回null代替抛出异常。
     * @param values 枚举集合
     * @param name 要查找的枚举名字
     * @param <T> 枚举类型
     * @return T
     */
    @Nullable
    public static <T extends Enum<T>> T forName(T[] values, String name) {
        for (T t : values){
            if (t.name().equals(name)){
                return t;
            }
        }
        return null;
    }

    /**
     * 通过名字查找枚举(忽略名字的大小写)。
     * 与{@link Enum#valueOf(Class, String)}区别在于返回null代替抛出异常。
     * @param values 枚举集合
     * @param name 要查找的枚举名字
     * @param <T> 枚举类型
     * @return T
     */
    public static <T extends Enum<T>> T forNameIgnoreCase(T[] values, String name) {
        for (T t : values){
            if (t.name().equalsIgnoreCase(name)){
                return t;
            }
        }
        return null;
    }

    /**
     * 根据枚举的values建立索引；
     * 该方法的开销相对小，代码量也能省下；
     * @param values 枚举数组
     * @param <T> 枚举类型
     * @return unmodifiable
     */
    @SuppressWarnings("unchecked")
    public static <T extends NumberEnum> NumberEnumMapper<T> indexNumberEnum(T[] values){
        if (values.length == 0){
            return (NumberEnumMapper<T>) EmptyMapper.INSTANCE;
        }

        // 存在一定的浪费，判定重复用
        Int2ObjectMap<T> result = FastCollectionsUtils.newEnoughCapacityIntMap(values.length);
        int minNumber = values[0].getNumber();
        int maxNumber = values[0].getNumber();

        for (T t : values){
            FastCollectionsUtils.requireNotContains(result,t.getNumber(),"number");
            result.put(t.getNumber(),t);

            minNumber = Math.min(minNumber,t.getNumber());
            maxNumber = Math.max(maxNumber,t.getNumber());
        }

        if (ArrayBasedMapper.available(minNumber, maxNumber, values.length)){
            return new ArrayBasedMapper<>(values, minNumber, maxNumber);
        }else {
            return new MapBasedMapper<>(values, result);
        }
    }
}
