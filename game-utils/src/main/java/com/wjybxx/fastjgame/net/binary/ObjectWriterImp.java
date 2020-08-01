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

import com.google.protobuf.Message;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/23
 */
class ObjectWriterImp implements ObjectWriter {

    private final CodecRegistry codecRegistry;
    private final DataOutputStream outputStream;

    ObjectWriterImp(CodecRegistry codecRegistry, DataOutputStream outputStream) {
        this.codecRegistry = codecRegistry;
        this.outputStream = outputStream;
    }

    @Override
    public void writeInt(int value) throws Exception {
        outputStream.writeTag(BinaryTag.INT);
        outputStream.writeInt(value);
    }

    @Override
    public void writeLong(long value) throws Exception {
        outputStream.writeTag(BinaryTag.LONG);
        outputStream.writeLong(value);
    }

    @Override
    public void writeFloat(float value) throws Exception {
        outputStream.writeTag(BinaryTag.FLOAT);
        outputStream.writeFloat(value);
    }

    @Override
    public void writeDouble(double value) throws Exception {
        outputStream.writeTag(BinaryTag.DOUBLE);
        outputStream.writeDouble(value);
    }

    @Override
    public void writeShort(short value) throws Exception {
        outputStream.writeTag(BinaryTag.SHORT);
        outputStream.writeShort(value);
    }

    @Override
    public void writeBoolean(boolean value) throws Exception {
        outputStream.writeTag(BinaryTag.BOOLEAN);
        outputStream.writeBoolean(value);
    }

    @Override
    public void writeByte(byte value) throws Exception {
        outputStream.writeTag(BinaryTag.BYTE);
        outputStream.writeByte(value);
    }

    @Override
    public void writeChar(char value) throws Exception {
        outputStream.writeTag(BinaryTag.CHAR);
        outputStream.writeChar(value);
    }

    @Override
    public void writeString(@Nullable String value) throws Exception {
        if (value == null) {
            outputStream.writeTag(BinaryTag.NULL);
            return;
        }
        outputStream.writeTag(BinaryTag.STRING);
        outputStream.writeString(value);
    }

    @Override
    public void writeMessage(@Nullable Message message) throws Exception {
        if (message == null) {
            outputStream.writeTag(BinaryTag.NULL);
            return;
        }

        writePojo(message, message.getClass());
    }

    private void writePojo(@Nonnull Object pojo, @Nonnull Class<?> type) throws Exception {
        outputStream.writeTag(BinaryTag.POJO);
        PojoCodec.writePojoImp(outputStream, pojo, type, this, codecRegistry);
    }

    @Override
    public void writeBytes(@Nullable byte[] value) throws Exception {
        if (value == null) {
            outputStream.writeTag(BinaryTag.NULL);
            return;
        }

        outputStream.writeTag(BinaryTag.ARRAY);
        ArrayCodec.writeByteArray(outputStream, value, 0, value.length);
    }

    @Override
    public void writeBytes(@Nonnull byte[] bytes, int offset, int length) throws Exception {
        outputStream.writeTag(BinaryTag.ARRAY);
        ArrayCodec.writeByteArray(outputStream, bytes, offset, length);
    }

    @Override
    public <E> void writeCollection(@Nullable Collection<? extends E> collection) throws Exception {
        if (collection == null) {
            outputStream.writeTag(BinaryTag.NULL);
            return;
        }
        outputStream.writeTag(BinaryTag.COLLECTION);
        CollectionCodec.writeCollectionImpl(outputStream, collection, this);
    }

    @Override
    public <K, V> void writeMap(@Nullable Map<K, V> map) throws Exception {
        if (map == null) {
            outputStream.writeTag(BinaryTag.NULL);
            return;
        }

        outputStream.writeTag(BinaryTag.MAP);
        MapCodec.writeMapImpl(outputStream, map, this);
    }

    @Override
    public void writeArray(@Nullable Object array) throws Exception {
        if (array == null) {
            outputStream.writeTag(BinaryTag.NULL);
            return;
        }
        if (!array.getClass().isArray()) {
            throw new IOException("Array expeceted");
        }

        outputStream.writeTag(BinaryTag.ARRAY);
        ArrayCodec.writeArrayImpl(outputStream, array, this);
    }

    @Override
    public <E> void writeObject(@Nullable E value, Class<? super E> superClass) throws Exception {
        if (null == value) {
            outputStream.writeTag(BinaryTag.NULL);
            return;
        }

        writePojo(value, superClass);
    }

    @Override
    public void writeLazySerializeObject(@Nullable Object value) throws Exception {
        if (null == value) {
            outputStream.writeTag(BinaryTag.NULL);
            return;
        }

        if (value instanceof byte[]) {
            writeBytes((byte[]) value);
            return;
        }

        // 占位，用于后面填充tag和长度字段
        final DataOutputStream childOutputStream = outputStream.slice(outputStream.writeIndex() + 1 + 1 + 4);
        final ObjectWriter childObjectWriter = new ObjectWriterImp(codecRegistry, childOutputStream);
        childObjectWriter.writeObject(value);

        outputStream.writeTag(BinaryTag.ARRAY);
        ArrayCodec.writeChildTypeAndLength(outputStream, BinaryTag.BYTE, childOutputStream.writeIndex());

        // 更新写索引
        outputStream.writeIndex(outputStream.writeIndex() + childOutputStream.writeIndex());
    }

    @Override
    public void flush() throws IOException {
        outputStream.flush();
    }

    // ---------------------------------------------------------------------------
    @Override
    public <T> void writeObject(@Nullable T value) throws Exception {
        if (value == null) {
            outputStream.writeTag(BinaryTag.NULL);
            return;
        }

        final Class<?> type = value.getClass();
        // POJO优先，可能性最高，且如果命中，可以节省诸多消耗
        final PojoCodecImpl<?> pojoCodec = codecRegistry.get(type);
        if (pojoCodec != null) {
            writePojo(value, type);
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

        // 第二梯队
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
            outputStream.writeShort((Short) value);
            return;
        }
        if (type == Byte.class) {
            outputStream.writeByte((Byte) value);
            return;
        }
        if (type == Character.class) {
            outputStream.writeChar((Character) value);
            return;
        }

        throw new IOException("Unsupported type " + type.getName());
    }
}
