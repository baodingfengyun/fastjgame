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

import com.google.protobuf.CodedOutputStream;
import com.wjybxx.fastjgame.net.utils.NetUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/23
 */
class EntityOutputStreamImp implements EntityOutputStream {

    private static final ThreadLocal<Queue<byte[]>> LOCAL_BUFFER_QUEUE = ThreadLocal.withInitial(ArrayDeque::new);

    private final CodecRegistry codecRegistry;
    private final CodedOutputStream outputStream;

    EntityOutputStreamImp(CodecRegistry codecRegistry, CodedOutputStream outputStream) {
        this.codecRegistry = codecRegistry;
        this.outputStream = outputStream;
    }

    @Override
    public void writeInt(int value) throws Exception {
        BinarySerializer.writeTag(outputStream, Tag.INT);
        outputStream.writeInt32NoTag(value);
    }

    @Override
    public void writeLong(long value) throws Exception {
        BinarySerializer.writeTag(outputStream, Tag.LONG);
        outputStream.writeInt64NoTag(value);
    }

    @Override
    public void writeFloat(float value) throws Exception {
        BinarySerializer.writeTag(outputStream, Tag.FLOAT);
        outputStream.writeFloatNoTag(value);
    }

    @Override
    public void writeDouble(double value) throws Exception {
        BinarySerializer.writeTag(outputStream, Tag.DOUBLE);
        outputStream.writeDoubleNoTag(value);
    }

    @Override
    public void writeShort(short value) throws Exception {
        BinarySerializer.writeTag(outputStream, Tag.SHORT);
        outputStream.writeInt32NoTag(value);
    }

    @Override
    public void writeBoolean(boolean value) throws Exception {
        BinarySerializer.writeTag(outputStream, Tag.BOOLEAN);
        outputStream.writeBoolNoTag(value);
    }

    @Override
    public void writeByte(byte value) throws Exception {
        BinarySerializer.writeTag(outputStream, Tag.BYTE);
        outputStream.writeRawByte(value);
    }

    @Override
    public void writeChar(char value) throws Exception {
        BinarySerializer.writeTag(outputStream, Tag.CHAR);
        outputStream.writeUInt32NoTag(value);
    }

    @Override
    public void writeString(@Nullable String value) throws Exception {
        if (value == null) {
            BinarySerializer.writeTag(outputStream, Tag.NULL);
            return;
        }
        BinarySerializer.writeTag(outputStream, Tag.STRING);
        outputStream.writeStringNoTag(value);
    }

    @Override
    public void writeBytes(@Nullable byte[] value) throws Exception {
        if (value == null) {
            BinarySerializer.writeTag(outputStream, Tag.NULL);
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
            BinarySerializer.writeTag(outputStream, Tag.NULL);
            return;
        }
        CollectionCodec.encodeCollection(outputStream, collection, codecRegistry);
    }

    @Override
    public <K, V> void writeMap(@Nullable Map<K, V> map) throws Exception {
        if (map == null) {
            BinarySerializer.writeTag(outputStream, Tag.NULL);
            return;
        }
        MapCodec.encodeMap(outputStream, map, codecRegistry);
    }

    @Override
    public void writeArray(@Nullable Object array) throws Exception {
        if (array == null) {
            BinarySerializer.writeTag(outputStream, Tag.NULL);
            return;
        }
        if (!array.getClass().isArray()) {
            throw new IOException();
        }
        ArrayCodec.encodeArray(outputStream, array, codecRegistry);
    }

    /**
     * 读写格式仍然要与{@link EntitySerializerBasedCodec}保持一致
     */
    @Override
    public <E> void writeEntity(@Nullable E entity, Class<? super E> entitySuperClass) throws Exception {
        if (null == entity) {
            BinarySerializer.writeTag(outputStream, Tag.NULL);
            return;
        }
        @SuppressWarnings("unchecked") final PojoCodec<? super E> codec = (PojoCodec<? super E>) codecRegistry.get(entitySuperClass);
        // 这里是生成的代码走进来的，因此即使异常，也能定位
        codec.encode(outputStream, entity, codecRegistry);
    }

    /**
     * 临时方案
     * TODO 优化，最好能直接使用当前的{@link #outputStream}
     */
    @Override
    public void writeLazySerializeObject(@Nullable Object value) throws Exception {
        if (null == value) {
            BinarySerializer.writeTag(outputStream, Tag.NULL);
            return;
        }

        if (value instanceof byte[]) {
            writeBytes((byte[]) value);
            return;
        }

        byte[] buffer = allocateBuffer();
        try {
            CodedOutputStream outputStream = CodedOutputStream.newInstance(buffer);
            EntityOutputStream outputStreamImp = new EntityOutputStreamImp(codecRegistry, outputStream);
            outputStreamImp.writeObject(value);

            writeBytes(buffer, 0, outputStream.getTotalBytesWritten());
        } finally {
            releaseBuffer(buffer);
        }
    }

    @Nonnull
    private static byte[] allocateBuffer() {
        final Queue<byte[]> bufferQueue = LOCAL_BUFFER_QUEUE.get();
        byte[] buffer = bufferQueue.poll();
        if (buffer != null) {
            return buffer;
        }
        return new byte[NetUtils.MAX_BUFFER_SIZE];
    }

    private static void releaseBuffer(@Nonnull byte[] buffer) {
        final Queue<byte[]> bufferQueue = LOCAL_BUFFER_QUEUE.get();
        bufferQueue.offer(buffer);
    }
}
