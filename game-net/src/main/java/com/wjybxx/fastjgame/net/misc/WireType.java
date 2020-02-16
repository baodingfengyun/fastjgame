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

package com.wjybxx.fastjgame.net.misc;


import com.google.protobuf.AbstractMessage;
import com.google.protobuf.ProtocolMessageEnum;
import com.wjybxx.fastjgame.net.annotation.SerializableClass;
import com.wjybxx.fastjgame.net.serializer.BeanSerializer;
import com.wjybxx.fastjgame.utils.ClassScanner;
import com.wjybxx.fastjgame.utils.entity.IndexableEntity;
import com.wjybxx.fastjgame.utils.entity.NumericalEntity;
import com.wjybxx.fastjgame.utils.reflect.TypeParameterFinder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 数据类型
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/17
 * github - https://github.com/hl845740757
 */
public class WireType {

    /**
     * bean -> beanSerializer (自动生成的beanSerializer 或 手动实现的)
     * 缓存起来，避免大量查找。
     */
    private static final Map<Class<?>, Class<? extends BeanSerializer<?>>> classBeanSerializerMap;

    static {
        final Set<Class<?>> allSerializerClass = ClassScanner.findClasses("com.wjybxx.fastjgame", name -> true, WireType::isBeanSerializer);
        classBeanSerializerMap = new IdentityHashMap<>(allSerializerClass.size());

        for (Class<?> clazz : allSerializerClass) {
            @SuppressWarnings("unchecked") final Class<? extends BeanSerializer<?>> serializerClass = (Class<? extends BeanSerializer<?>>) clazz;
            final Class<?> beanClass = TypeParameterFinder.findTypeParameterUnsafe(serializerClass, BeanSerializer.class, "T");

            if (beanClass == Object.class) {
                throw new UnsupportedOperationException("BeanSerializer must declare type parameter");
            }

            if (classBeanSerializerMap.containsKey(beanClass)) {
                throw new UnsupportedOperationException(beanClass.getSimpleName() + " has more than one serializer");
            }

            classBeanSerializerMap.put(beanClass, serializerClass);
        }
    }

    private static boolean isBeanSerializer(Class<?> c) {
        return c != BeanSerializer.class && BeanSerializer.class.isAssignableFrom(c);
    }

    /**
     * NULL
     */
    public static final byte NULL = 0;

    // ------------------------------------------- 基本类型 -----------------------------
    /**
     * rawByte
     */
    public static final byte BYTE = 1;
    /**
     * uInt
     */
    public static final byte CHAR = 2;
    /**
     * varInt
     */
    public static final byte SHORT = 3;
    /**
     * varInt
     */
    public static final byte INT = 4;
    /**
     * varInt64
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
     * rawByte
     */
    public static final byte BOOLEAN = 8;

    // ------------------------------------------- 基本类型数组 -----------------------------
    /**
     * 字节数组
     */
    public static final byte BYTE_ARRAY = 9;
    /**
     * char数组
     */
    public static final byte CHAR_ARRAY = 10;
    /**
     * short数组
     */
    public static final byte SHORT_ARRAY = 11;
    /**
     * int数组
     */
    public static final byte INT_ARRAY = 12;
    /**
     * long数组
     */
    public static final byte LONG_ARRAY = 13;
    /**
     * float数组
     */
    public static final byte FLOAT_ARRAY = 14;
    /**
     * double数组
     */
    public static final byte DOUBLE_ARRAY = 15;
    /**
     * boolean数组
     */
    public static final byte BOOLEAN_ARRAY = 16;

    // --------------------------------------------- 对象 ------------------------------
    /**
     * 字符串 LENGTH_DELIMITED
     */
    public static final byte STRING = 17;

    /**
     * protobuf的Message LENGTH_DELIMITED
     */
    public static final byte PROTO_MESSAGE = 18;
    /**
     * protoBuf的枚举
     */
    public static final byte PROTO_ENUM = 19;
    /**
     * 自定义数据块
     */
    public static final byte CHUNK = 20;

    // -------------------------------------------- 带有注解的类 -----------------------------

    /**
     * {@link com.wjybxx.fastjgame.utils.entity.NumericalEntity}的子类
     */
    public static final byte NUMERICAL_ENTITY = 21;
    /**
     * {@link com.wjybxx.fastjgame.utils.entity.IndexableEntity}的子类
     */
    public static final byte INDEXABLE_ENTITY = 22;
    /**
     * 其它对象
     */
    public static final byte CUSTOM_ENTITY = 23;

    /**
     * 动态类型 - 运行时才能确定的类型（它是标记类型）
     */
    public static final byte RUN_TIME = 24;

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

        // ---- 基本类型数组

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

        // 字符串
        if (type == String.class) {
            return WireType.STRING;
        }

        // protoBuf
        if (AbstractMessage.class.isAssignableFrom(type)) {
            return WireType.PROTO_MESSAGE;
        }

        // protoBuf的枚举
        if (ProtocolMessageEnum.class.isAssignableFrom(type)) {
            return WireType.PROTO_ENUM;
        }

        // CHUNK
        if (type == Chunk.class) {
            return WireType.CHUNK;
        }

        // Object
        if (type == Object.class) {
            return WireType.RUN_TIME;
        }

        if (type.isAnnotationPresent(SerializableClass.class)) {
            if (NumericalEntity.class.isAssignableFrom(type)) {
                return WireType.NUMERICAL_ENTITY;
            }
            if (IndexableEntity.class.isAssignableFrom(type)) {
                return WireType.INDEXABLE_ENTITY;
            }
        }

        // 需要处理手写的实现，因此放在这里
        if (classBeanSerializerMap.containsKey(type)) {
            return WireType.CUSTOM_ENTITY;
        }

        // 一些接口，超类等等
        return WireType.RUN_TIME;
    }

    /**
     * 获取消息类对应的序列化辅助类
     *
     * @param messageClazz 消息类
     * @return 序列化辅助类
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> Class<? extends BeanSerializer<T>> getBeanSerializer(Class<T> messageClazz) {
        return (Class<? extends BeanSerializer<T>>) classBeanSerializerMap.get(messageClazz);
    }

    /**
     * 判断一个类是否可以被序列化
     */
    public static boolean isSerializable(Class<?> messageClazz) {
        return classBeanSerializerMap.containsKey(messageClazz)
                || messageClazz.isAnnotationPresent(SerializableClass.class)
                || AbstractMessage.class.isAssignableFrom(messageClazz)
                || ProtocolMessageEnum.class.isAssignableFrom(messageClazz);
    }
}
