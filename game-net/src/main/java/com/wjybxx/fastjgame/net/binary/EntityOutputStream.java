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

package com.wjybxx.fastjgame.net.binary;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

/**
 * 普通JavaBean对象输出流。
 * Q: 为什么必须使用包装类型？
 * A: 某些时刻需要使用null表示未赋值状态，使用特殊值是不好的。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/13
 * github - https://github.com/hl845740757
 */
public interface EntityOutputStream {

    /**
     * 向输出流中写入一个字段
     *
     * @param wireType   字段的缓存类型，如果该值为{@link WireType#RUN_TIME}，则需要动态解析。
     * @param fieldValue 字段的值
     */
    <T> void writeField(byte wireType, @Nullable T fieldValue) throws Exception;

    // ----------------------------------------- 处理多态问题 ----------------------------------

    /**
     * 向输出流中写一个多态实体对象
     * （按照超类格式写入数据，并忽略子类字段）
     */
    <E> void writeEntity(@Nullable E entity, EntitySerializer<? super E> entitySerializer) throws Exception;

    /**
     * 向输出流中写入一个map
     */
    <K, V> void writeMap(@Nullable Map<K, V> map) throws Exception;

    /**
     * 向输出流中写一个collection
     */
    <E> void writeCollection(@Nullable Collection<? extends E> collection) throws Exception;

    /**
     * 向输出流中写入一个数组
     *
     * @param array 要支持基本类型数组，因此为{@link Object}而不是泛型数组+
     */
    void writeArray(@Nullable Object array) throws Exception;
    // ---------------------------------------- 字节数组特殊写入需求 ----------------------------------

    /**
     * 向输出流中写入一个字节数组，并可以指定偏移量和长度
     */
    void writeBytes(@Nullable byte[] bytes, int offset, int length) throws Exception;

    // ----------------------------------------- 方便手动实现扩展 --------------------------------------

    default void writeInt(@Nullable Integer value) throws Exception {
        writeField(WireType.INT, value);
    }

    default void writeLong(@Nullable Long value) throws Exception {
        writeField(WireType.LONG, value);
    }

    default void writeFloat(@Nullable Float value) throws Exception {
        writeField(WireType.FLOAT, value);
    }

    default void writeDouble(@Nullable Double value) throws Exception {
        writeField(WireType.DOUBLE, value);
    }

    default void writeShort(@Nullable Short value) throws Exception {
        writeField(WireType.SHORT, value);
    }

    default void writeBoolean(@Nullable Boolean value) throws Exception {
        writeField(WireType.BOOLEAN, value);
    }

    default void writeByte(@Nullable Byte value) throws Exception {
        writeField(WireType.BYTE, value);
    }

    default void writeChar(@Nullable Character value) throws Exception {
        writeField(WireType.CHAR, value);
    }

    default void writeString(@Nullable String value) throws Exception {
        writeField(WireType.STRING, value);
    }

    default void writeBytes(@Nullable byte[] value) throws Exception {
        writeField(WireType.ARRAY, value);
    }

    /**
     * 向输出流中写入一个字段，如果没有对应的简便方法，可以使用该方法
     *
     * @param value 字段的值
     */
    default <T> void writeObject(@Nullable T value) throws Exception {
        writeField(WireType.RUN_TIME, value);
    }
}
