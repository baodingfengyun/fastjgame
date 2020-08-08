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
import java.util.List;
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

    int recursionDepth;
    int recursionLimit = DEFAULT_RECURSION_LIMIT;

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
        // type + length + value
        // 未来可能fix32方式写length
        outputStream.writeType(BinaryValueType.BINARY);
        outputStream.writeInt32(length);
        outputStream.writeRawBytes(bytes, offset, length);
    }

    private void writeNull() throws IOException {
        outputStream.writeType(BinaryValueType.NULL);
    }

    // ------------------------------------------ 写自定义对象的真正实现 --------------------------------------

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
        // 1. protoBuf消息可以序列化
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

    private <T> void writePojo(@Nonnull T value,
                               @Nonnull Class<? super T> type,
                               @Nonnull PojoCodec<? super T> pojoCodec) throws Exception {
        final TypeId typeId = getTypeIdMapper().ofType(type);
        if (typeId == null) {
            throw new IllegalStateException("typeModel expect not null, but null, type " + type.getName());
        }

        if (++recursionDepth > recursionLimit) {
            throw new IOException("Object had too many levels of nesting");
        }

        // 对象类型
        outputStream.writeType(BinaryValueType.OBJECT);

        // 预留4字节表示对象的长度
        final int preIndex = outputStream.getTotalBytesWritten();
        outputStream.writeFixed32(0);

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

    private void backpatchSize(int preIndex) throws IOException {
        // 需要去除size自身
        final int size = outputStream.getTotalBytesWritten() - preIndex - 4;
        outputStream.setFixedInt32(preIndex, size);
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

        final byte[] bytes = serializer.toBytes(value);
        writeBytes(bytes);
    }

    // ---------------------------------------- 没有PojoCodec，但有typeId，可以序列化的对象类型 --------------------------------------
    private <T> void writeAsPojo(@Nonnull T value, TypeId typeId, PojoWriter<? super T> writer) throws Exception {
        if (++recursionDepth > recursionLimit) {
            throw new IOException("Object had too many levels of nesting");
        }

        outputStream.writeType(BinaryValueType.OBJECT);

        // 预留4字节表示对象的长度
        final int preIndex = outputStream.getTotalBytesWritten();
        outputStream.writeFixed32(0);

        // 写入默认类型信息
        writeTypeId(typeId);

        writer.accept(value);

        // 回写内容
        backpatchSize(preIndex);

        recursionDepth--;
    }

    @FunctionalInterface
    interface PojoWriter<T> {

        void accept(T value) throws Exception;
    }

    @Override
    public void writeMessage(@Nullable MessageLite messageLite) throws Exception {
        if (messageLite == null) {
            writeNull();
            return;
        }
        writeAsPojo(messageLite, getCheckedTypeId(messageLite), this::writeMessageImpl);
    }

    private TypeId getCheckedTypeId(Object value) {
        final TypeId typeId = getTypeIdMapper().ofType(value.getClass());
        if (typeId == null) {
            throw new IllegalStateException("typeModel expect not null, but null, type " + value.getClass().getName());
        }
        return typeId;
    }

    private void writeMessageImpl(@Nonnull MessageLite messageLite) throws Exception {
        // length(未来可能fix32方式写length) + size
        outputStream.writeInt32(messageLite.getSerializedSize());
        outputStream.writeMessageNoSize(messageLite);
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

        if (collection instanceof List) {
            return TypeId.DEFAULT_LIST;
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

    private void writeProtocolEnumImpl(ProtocolMessageEnum protocolMessageEnum) throws IOException {
        outputStream.writeInt32(protocolMessageEnum.getNumber());
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
