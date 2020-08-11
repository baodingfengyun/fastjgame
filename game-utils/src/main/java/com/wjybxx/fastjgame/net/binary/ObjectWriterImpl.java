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

import com.google.protobuf.MessageLite;
import com.google.protobuf.ProtocolMessageEnum;
import com.wjybxx.fastjgame.net.serialization.TypeId;
import com.wjybxx.fastjgame.net.serialization.TypeIdMapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/8/4
 */
public class ObjectWriterImpl implements ObjectWriter {

    static final int DEFAULT_RECURSION_LIMIT = 64;

    private final BinarySerializer serializer;
    private final CodecRegistry codecRegistry;
    private final CodedDataOutputStream outputStream;

    private final int recursionLimit = DEFAULT_RECURSION_LIMIT;
    private int recursionDepth;

    ObjectWriterImpl(BinarySerializer serializer, CodecRegistry codecRegistry, CodedDataOutputStream outputStream) {
        this.serializer = serializer;
        this.codecRegistry = codecRegistry;
        this.outputStream = outputStream;
    }

    @Override
    public BinarySerializer serializer() {
        return serializer;
    }

    @Override
    public CodecRegistry codecRegistry() {
        return codecRegistry;
    }

    private TypeIdMapper getTypeIdMapper() {
        return serializer.typeIdMapper;
    }

    // -------------------------------------------- 基本值 --------------------------------------

    @Override
    public void writeInt(int value) throws Exception {
        outputStream.writeType(BinaryValueType.INT);
        outputStream.writeInt32(value);
    }

    @Override
    public void writeLong(long value) throws Exception {
        outputStream.writeType(BinaryValueType.LONG);
        outputStream.writeInt64(value);
    }

    @Override
    public void writeFloat(float value) throws Exception {
        outputStream.writeType(BinaryValueType.FLOAT);
        outputStream.writeFloat(value);
    }

    @Override
    public void writeDouble(double value) throws Exception {
        outputStream.writeType(BinaryValueType.DOUBLE);
        outputStream.writeDouble(value);
    }

    @Override
    public void writeShort(short value) throws Exception {
        outputStream.writeType(BinaryValueType.SHORT);
        outputStream.writeInt32(value);
    }

    @Override
    public void writeBoolean(boolean value) throws Exception {
        outputStream.writeType(BinaryValueType.BOOLEAN);
        outputStream.writeBool(value);
    }

    @Override
    public void writeByte(byte value) throws Exception {
        outputStream.writeType(BinaryValueType.BYTE);
        outputStream.writeRawByte(value);
    }

    @Override
    public void writeChar(char value) throws Exception {
        outputStream.writeType(BinaryValueType.CHAR);
        outputStream.writeInt32(value);
    }

    @Override
    public void writeString(@Nullable String value) throws Exception {
        if (value == null) {
            writeNull();
        } else {
            outputStream.writeType(BinaryValueType.STRING);
            outputStream.writeString(value);
        }
    }

    @Override
    public void writeBytes(@Nullable byte[] value) throws Exception {
        if (value == null) {
            writeNull();
        } else {
            writeBytes(value, 0, value.length);
        }
    }

    @Override
    public void writeBytes(@Nonnull byte[] bytes, int offset, int length) throws Exception {
        // type + length(fixed32) + value
        outputStream.writeType(BinaryValueType.BINARY);
        outputStream.writeFixed32(length);
        outputStream.writeRawBytes(bytes, offset, length);
    }

    @Override
    public void writeMessage(@Nullable MessageLite messageLite) throws Exception {
        if (messageLite == null) {
            writeNull();
            return;
        }

        final TypeId typeId = getCheckedTypeId(messageLite);
        outputStream.writeType(BinaryValueType.MESSAGE);
        // length (typeId + content)
        outputStream.writeFixed32(5 + messageLite.getSerializedSize());
        writeTypeId(typeId);
        outputStream.writeMessageNoSize(messageLite);
    }

    private TypeId getCheckedTypeId(Object value) {
        final TypeId typeId = getTypeIdMapper().ofType(value.getClass());
        if (typeId == null) {
            throw new IllegalStateException("typeModel expect not null, but null, type " + value.getClass().getName());
        }
        return typeId;
    }

    private void writeNull() throws IOException {
        outputStream.writeType(BinaryValueType.NULL);
    }

    // ------------------------------------------------- 主要处理容器对象 ------------------------------------------------

    @Override
    public void writeObject(@Nullable Object value) throws Exception {
        if (value == null) {
            writeNull();
            return;
        }
        writeObjectHelper(value);
    }

    /**
     * 该方法用于捕获类型
     */
    private <T> void writeObjectHelper(@Nonnull T value) throws Exception {
        @SuppressWarnings("unchecked") final Class<T> type = (Class<T>) value.getClass();
        final PojoCodec<? super T> pojoCodec = codecRegistry.get(type);
        if (pojoCodec != null) {
            writePojo(value, type, pojoCodec);
            return;
        }

        // 第一梯队
        if (type == Integer.class) {
            writeInt((Integer) value);
            return;
        }
        if (type == Long.class) {
            writeLong((Long) value);
            return;
        }
        if (type == String.class) {
            writeString((String) value);
            return;
        }
        if (type == Float.class) {
            writeFloat((Float) value);
            return;
        }
        if (type == Double.class) {
            writeDouble((Double) value);
            return;
        }
        if (type == Boolean.class) {
            writeBoolean((Boolean) value);
            return;
        }
        if (type == byte[].class) {
            writeBytes((byte[]) value);
            return;
        }

        // 第二梯队
        // 1. protoBuf消息
        // 2. 任意集合/Map/数组允许序列化，但不一定能精确反序列化
        if (value instanceof MessageLite) {
            writeMessage((MessageLite) value);
            return;
        }
        if (value instanceof Collection) {
            writeCollection((Collection<?>) value);
            return;
        }
        if (value instanceof Map) {
            writeMap((Map<?, ?>) value);
            return;
        }
        if (type.isArray()) {
            writeArray(value);
            return;
        }

        // 第三梯队
        if (type == Short.class) {
            writeShort((Short) value);
            return;
        }
        if (type == Byte.class) {
            writeByte((Byte) value);
            return;
        }
        if (type == Character.class) {
            writeChar((Character) value);
            return;
        }
        if (value instanceof ProtocolMessageEnum) {
            writeProtocolEnum((ProtocolMessageEnum) value);
            return;
        }

        throw new IOException("Unsupported type " + type.getName());
    }

    private <T> void writePojo(@Nonnull T value, @Nonnull Class<? super T> type, @Nonnull PojoCodec<? super T> pojoCodec) throws Exception {
        final TypeId typeId = getTypeIdMapper().ofType(type);
        if (typeId == null) {
            throw new IllegalStateException("typeId expect not null, but null, type " + type.getName());
        }

        if (++recursionDepth > recursionLimit) {
            throw new IOException("Object had too many levels of nesting");
        }

        // 预留4字节表示对象的长度
        outputStream.writeType(BinaryValueType.OBJECT);
        outputStream.writeFixed32(0);

        final int preIndex = outputStream.getTotalBytesWritten();

        // 类型信息
        writeTypeId(typeId);

        // 对象内容
        pojoCodec.writeObject(this, value, codecRegistry);

        // 回写size
        backpatchSize(preIndex);

        recursionDepth--;
    }

    private void writeTypeId(final TypeId typeId) throws Exception {
        // 固定5字节
        outputStream.writeRawByte(typeId.getNamespace());
        outputStream.writeFixed32(typeId.getClassId());
    }

    /**
     * @param preIndex 写完长度字段后的当前写索引（写正式内容之前的索引）
     */
    private void backpatchSize(int preIndex) throws IOException {
        final int size = outputStream.getTotalBytesWritten() - preIndex;
        outputStream.setFixedInt32(preIndex - 4, size);
    }

    @Override
    public <T> void writeObject(@Nullable T value, @Nonnull Class<? super T> superClass) throws Exception {
        if (value == null) {
            writeNull();
            return;
        }

        final PojoCodec<? super T> pojoCodec = codecRegistry.get(superClass);
        if (pojoCodec == null) {
            throw new IOException("Unsupported type " + superClass.getName());
        }

        writePojo(value, superClass, pojoCodec);
    }

    @Override
    public void writeLazySerializeObject(@Nullable Object value) throws Exception {
        if (value == null) {
            writeNull();
            return;
        }

        if (value instanceof byte[]) {
            writeBytes((byte[]) value);
            return;
        }

        // 字节数组前缀信息
        outputStream.writeType(BinaryValueType.BINARY);
        outputStream.writeFixed32(0);

        // 写入的数据都会被当做字节数组的内容部分
        final int preIndex = outputStream.getTotalBytesWritten();
        writeObject(value);
        backpatchSize(preIndex);
    }

    // ---------------------------------------- 通用容器类型 --------------------------------------
    private <T> void writeAsPojo(@Nonnull T value, TypeId typeId, ContainerWriter<? super T> writer) throws Exception {
        if (++recursionDepth > recursionLimit) {
            throw new IOException("Object had too many levels of nesting");
        }

        // 预留4字节表示对象的长度
        outputStream.writeType(BinaryValueType.OBJECT);
        outputStream.writeFixed32(0);

        final int preIndex = outputStream.getTotalBytesWritten();

        // 写入类型信息
        writeTypeId(typeId);

        // 写入对象内容
        writer.accept(value);

        // 回写内容
        backpatchSize(preIndex);

        recursionDepth--;
    }

    @FunctionalInterface
    interface ContainerWriter<T> {

        void accept(T value) throws Exception;
    }

    @Override
    public void writeArray(@Nullable Object array) throws Exception {
        if (array == null) {
            writeNull();
            return;
        }
        writeAsPojo(array, getArrayTypeId(array), this::writeArrayImpl);
    }

    private TypeId getArrayTypeId(Object array) {
        final TypeId typeId = getTypeIdMapper().ofType(array.getClass());
        return typeId == null ? TypeId.DEFAULT_ARRAY : typeId;
    }

    private void writeArrayImpl(@Nullable Object array) throws Exception {
        if (array instanceof Object[]) {
            // 对象数组，转为Object[]，使用普通api访问
            final Object[] objectArray = (Object[]) array;
            for (Object object : objectArray) {
                writeObject(object);
            }
        } else {
            // 需要使用反射api - 基本类型数组无法转为Object[]
            for (int index = 0, length = Array.getLength(array); index < length; index++) {
                writeObject(Array.get(array, index));
            }
        }
    }

    @Override
    public void writeCollection(@Nullable Collection<?> collection) throws Exception {
        if (collection == null) {
            writeNull();
            return;
        }
        writeAsPojo(collection, getCollectionTypeId(collection), this::writeCollectionImpl);
    }

    private TypeId getCollectionTypeId(@Nonnull Collection<?> collection) {
        final TypeId typeId = getTypeIdMapper().ofType(collection.getClass());
        if (typeId != null) {
            return typeId;
        }
        if (collection instanceof Set) {
            return TypeId.DEFAULT_SET;
        }
        // 默认List
        return TypeId.DEFAULT_LIST;
    }

    private void writeCollectionImpl(@Nonnull Collection<?> collection) throws Exception {
        for (Object e : collection) {
            writeObject(e);
        }
    }

    @Override
    public void writeMap(@Nullable Map<?, ?> map) throws Exception {
        if (map == null) {
            writeNull();
            return;
        }
        writeAsPojo(map, getMapTypeId(map), this::writeMapImpl);
    }

    private TypeId getMapTypeId(Map<?, ?> map) {
        final TypeId typeId = getTypeIdMapper().ofType(map.getClass());
        return typeId == null ? TypeId.DEFAULT_MAP : typeId;
    }

    private void writeMapImpl(@Nonnull Map<?, ?> map) throws Exception {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            writeObject(entry.getKey());
            writeObject(entry.getValue());
        }
    }

    private void writeProtocolEnum(@Nullable ProtocolMessageEnum protocolMessageEnum) throws Exception {
        if (protocolMessageEnum == null) {
            writeNull();
            return;
        }
        writeAsPojo(protocolMessageEnum, getCheckedTypeId(protocolMessageEnum), this::writeProtocolEnumImpl);
    }

    private void writeProtocolEnumImpl(ProtocolMessageEnum protocolMessageEnum) throws Exception {
        // 必须按照pojo的格式序列化，不能直接写入inputStream
        writeInt(protocolMessageEnum.getNumber());
    }

    // -------------------------------------------- 其它 ---------------------------------------

    @Override
    public void flush() throws IOException {
        outputStream.flush();
    }

    @Override
    public void close() throws Exception {

    }
}
