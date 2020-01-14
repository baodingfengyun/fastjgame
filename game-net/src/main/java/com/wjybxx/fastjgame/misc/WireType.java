/*
 *    Copyright 2019 wjybxx
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.wjybxx.fastjgame.misc;


import com.google.protobuf.AbstractMessage;
import com.google.protobuf.ProtocolMessageEnum;
import com.wjybxx.fastjgame.annotation.SerializableClass;
import com.wjybxx.fastjgame.annotationprocessor.SerializableNumberProcessor2;
import com.wjybxx.fastjgame.enummapper.NumericalEnum;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 序列化的数据类型。
 * <p>
 * 1. 数组类型只进行了部分支持，完全的支持的话比较难受，请使用list代替。
 * 2. 取消了Queue类型支持，因为{@link java.util.LinkedList}这种实现多接口的类会造成解析混乱。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/17
 * github - https://github.com/hl845740757
 */
public class WireType {

    /**
     * rawByte
     */
    public static final byte BYTE = 1;
    /**
     * uInt
     */
    public static final byte CHAR = 2;
    /**
     * varInt, sInt
     */
    public static final byte SHORT = 3;
    /**
     * varInt, sInt
     */
    public static final byte INT = 4;
    /**
     * varInt64, sInt64
     */
    public static final byte LONG = 5;
    /**
     * fixed32
     */
    public static final byte FLOAT = 6;
    /**
     * fixed64
     */
    public static final byte DOUBLE = 7;
    /**
     * uInt
     */
    public static final byte BOOLEAN = 8;

    /**
     * NULL
     */
    public static final byte NULL = 9;

    /**
     * 字符串 LENGTH_DELIMITED
     */
    public static final byte STRING = 10;

    /**
     * protobuf LENGTH_DELIMITED
     */
    public static final byte MESSAGE = 11;

    /**
     * protoBuf的枚举
     */
    public static final byte PROTO_ENUM = 12;
    /**
     * 枚举支持，自定义枚举必须实现{@link NumericalEnum}接口，
     * 且必须定义 forNumber(int) 获取枚举值的静态方法，以使得和protoBuf一样解析。
     * 拆分为两个枚举是为了避免编码时的反射调用。
     */
    public static final byte NUMBER_ENUM = 13;

    // -- 基本集合，注意：在rpc方法中时不要使用具体类型，请使用顶层接口类型 List/Set/Map，否则可能调用失败。
    /**
     * List，解析时使用{@link java.util.ArrayList}，保持顺序
     */
    public static final byte LIST = 14;
    /**
     * Set，解析时使用{@link java.util.LinkedHashSet}，保持顺序
     */
    public static final byte SET = 15;
    /**
     * Map，解析时使用{@link java.util.LinkedHashMap}，保持顺序
     */
    public static final byte MAP = 16;

    /**
     * 带有{@link SerializableClass}注解，且有非private无参构造方法和对应的getter setter
     */
    public static final byte NORMAL_BEAN = 17;
    /**
     * 带有{@link SerializableClass}注解，但是无参构造方法是private，或没有对应的getter setter方法。
     */
    public static final byte REFLECT_BEAN = 18;

    /**
     * 动态类型 - 运行时才能确定的类型（它是标记类型）
     */
    public static final byte RUN_TIME = 19;

    // -- 常用数组
    /**
     * 字节数组
     */
    public static final byte BYTE_ARRAY = 20;
    /**
     * short数组
     */
    public static final byte SHORT_ARRAY = 21;
    /**
     * int数组
     */
    public static final byte INT_ARRAY = 22;
    /**
     * long数组
     */
    public static final byte LONG_ARRAY = 23;
    /**
     * float数组
     */
    public static final byte FLOAT_ARRAY = 24;
    /**
     * double数组
     */
    public static final byte DOUBLE_ARRAY = 25;
    /**
     * char数组
     */
    public static final byte CHAR_ARRAY = 26;

    /**
     * 查找一个class对应的wireType
     *
     * @param type class
     * @return wireType
     */
    public static byte findType(@Nonnull final Class<?> type) {
        // --- 基本类型
        if (type == byte.class || type == Byte.class) {
            return WireType.BYTE;
        }
        if (type == char.class || type == Character.class) {
            return WireType.CHAR;
        }
        if (type == short.class || type == Short.class) {
            return WireType.SHORT;
        }
        if (type == int.class || type == Integer.class) {
            return WireType.INT;
        }
        if (type == long.class || type == Long.class) {
            return WireType.LONG;
        }
        if (type == float.class || type == Float.class) {
            return WireType.FLOAT;
        }
        if (type == double.class || type == Double.class) {
            return WireType.DOUBLE;
        }
        if (type == boolean.class || type == Boolean.class) {
            return WireType.BOOLEAN;
        }

        // 字符串
        if (type == String.class) {
            return WireType.STRING;
        }
        // protoBuf
        if (AbstractMessage.class.isAssignableFrom(type)) {
            return WireType.MESSAGE;
        }

        // NumericalEnum枚举 -- 不一定真的是枚举
        if (NumericalEnum.class.isAssignableFrom(type)) {
            return WireType.NUMBER_ENUM;
        }
        // protoBuf的枚举
        if (ProtocolMessageEnum.class.isAssignableFrom(type)) {
            return WireType.PROTO_ENUM;
        }

        // 常用集合支持
        if (List.class.isAssignableFrom(type)) {
            return WireType.LIST;
        }
        // Set
        if (Set.class.isAssignableFrom(type)) {
            return WireType.SET;
        }
        // Map
        if (Map.class.isAssignableFrom(type)) {
            return WireType.MAP;
        }

        // 自定义类型
        if (type.isAnnotationPresent(SerializableClass.class)) {
            try {
                loadBeanSerializer(type);
                return WireType.NORMAL_BEAN;
            } catch (ClassNotFoundException ignore) {
                return WireType.REFLECT_BEAN;
            }
        }

        // ----数组类型
        if (type == byte[].class) {
            return WireType.BYTE_ARRAY;
        }
        if (type == short[].class) {
            return WireType.SHORT_ARRAY;
        }
        if (type == int[].class) {
            return WireType.INT_ARRAY;
        }
        if (type == long[].class) {
            return WireType.LONG_ARRAY;
        }
        if (type == float[].class) {
            return WireType.FLOAT_ARRAY;
        }
        if (type == double[].class) {
            return WireType.DOUBLE_ARRAY;
        }
        if (type == char[].class) {
            return WireType.CHAR_ARRAY;
        }

        // 动态类型
        return WireType.RUN_TIME;
    }

    /**
     * 加载消息类对应的序列化辅助类
     *
     * @param messageClazz 消息类
     * @return 序列化辅助类
     * @throws ClassNotFoundException 不存在对应的辅助类时抛出该异常
     */
    public static Class<?> loadBeanSerializer(Class<?> messageClazz) throws ClassNotFoundException {
        return Class.forName(messageClazz.getPackageName() + "." + SerializableNumberProcessor2.getSerializerName(messageClazz.getSimpleName()));
    }

}
