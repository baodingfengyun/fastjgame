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
    public void writeBytes(@Nullable byte[] value) throws Exception {
        if (value == null) {
            outputStream.writeTag(BinaryTag.NULL);
            return;
        }

        ArrayCodec.writeByteArray(outputStream, value, 0, value.length);
    }

    @Override
    public void writeBytes(@Nonnull byte[] bytes, int offset, int length) throws Exception {
        ArrayCodec.writeByteArray(outputStream, bytes, offset, length);
    }

    @Override
    public <T> void writeObject(@Nullable T value) throws Exception {
        BinarySerializer.encodeObject(outputStream, value, codecRegistry);
    }

    @Override
    public <E> void writeCollection(@Nullable Collection<? extends E> collection) throws Exception {
        if (collection == null) {
            outputStream.writeTag(BinaryTag.NULL);
            return;
        }
        CollectionCodec.encodeCollection(outputStream, collection, codecRegistry);
    }

    @Override
    public <K, V> void writeMap(@Nullable Map<K, V> map) throws Exception {
        if (map == null) {
            outputStream.writeTag(BinaryTag.NULL);
            return;
        }
        MapCodec.encodeMap(outputStream, map, codecRegistry);
    }

    @Override
    public void writeArray(@Nullable Object array) throws Exception {
        if (array == null) {
            outputStream.writeTag(BinaryTag.NULL);
            return;
        }
        if (!array.getClass().isArray()) {
            throw new IOException();
        }
        ArrayCodec.encodeArray(outputStream, array, codecRegistry);
    }

    /**
     * 读写格式仍然要与{@link CustomPojoCodec}保持一致
     */
    @Override
    public <E> void writeObject(@Nullable E value, Class<? super E> superClass) throws Exception {
        if (null == value) {
            outputStream.writeTag(BinaryTag.NULL);
            return;
        }
        @SuppressWarnings("unchecked") final PojoCodec<? super E> codec = (PojoCodec<? super E>) codecRegistry.get(superClass);
        // 这里是生成的代码走进来的，因此即使异常，也能定位
        codec.encode(outputStream, value, codecRegistry);
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

        // 设置长度
        outputStream.writeTag(BinaryTag.ARRAY);
        outputStream.writeTag(BinaryTag.BYTE);
        outputStream.writeFixedInt32(childOutputStream.writeIndex());

        // 更新写索引
        outputStream.writeIndex(outputStream.writeIndex() + childOutputStream.writeIndex());
    }

}
