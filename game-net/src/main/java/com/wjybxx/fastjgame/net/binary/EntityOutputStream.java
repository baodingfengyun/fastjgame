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

import javax.annotation.Nonnull;
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

    void writeInt(int value) throws Exception;

    void writeLong(long value) throws Exception;

    void writeFloat(float value) throws Exception;

    void writeDouble(double value) throws Exception;

    void writeShort(short value) throws Exception;

    void writeBoolean(boolean value) throws Exception;

    void writeByte(byte value) throws Exception;

    void writeChar(char value) throws Exception;

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

    /**
     * 向输出流中写入一个字节数组，并可以指定偏移量和长度
     */
    void writeBytes(@Nonnull byte[] bytes, int offset, int length) throws Exception;

    // ----------------------------------------- 处理多态问题 ----------------------------------

    /**
     * 向输出流中写一个collection
     */
    default <E> void writeCollection(@Nullable Collection<? extends E> collection) throws Exception {
        writeField(WireType.COLLECTION, collection);
    }

    /**
     * 向输出流中写入一个map
     */
    default <K, V> void writeMap(@Nullable Map<K, V> map) throws Exception {
        writeField(WireType.MAP, map);
    }

    /**
     * 向输出流中写入一个数组
     *
     * @param array 要支持基本类型数组，因此为{@link Object}而不是泛型数组+
     */
    default void writeArray(@Nullable Object array) throws Exception {
        writeField(WireType.ARRAY, array);
    }

    /**
     * 向输出流中写一个多态实体对象（按照超类格式写入数据，并忽略子类字段）
     * 注意：必须保证和{@link EntityInputStream#readEntity(EntityFactory, AbstractEntitySerializer)} 使用相同的serializer
     */
    <E> void writeEntity(@Nullable E entity, EntitySerializer<? super E> entitySerializer) throws Exception;

    // ----------------------------------------- 生成代码调用 --------------------------------------

    /**
     * 向输出流中写入一个字段
     *
     * @param wireType   字段的缓存类型，如果该值为{@link WireType#RUN_TIME}，则需要动态解析。
     * @param fieldValue 字段的值
     */
    <T> void writeField(byte wireType, @Nullable T fieldValue) throws Exception;

}
