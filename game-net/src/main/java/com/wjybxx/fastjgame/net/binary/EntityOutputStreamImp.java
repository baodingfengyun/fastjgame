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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/23
 */
class EntityOutputStreamImp implements EntityOutputStream {

    private final CodecRegistry codecRegistry;
    private final CodedOutputStream outputStream;

    EntityOutputStreamImp(CodecRegistry codecRegistry, CodedOutputStream outputStream) {
        this.codecRegistry = codecRegistry;
        this.outputStream = outputStream;
    }

    @Override
    public void writeInt(int value) throws Exception {
        BinaryProtocolCodec.writeTag(outputStream, Tag.INT);
        outputStream.writeInt32NoTag(value);
    }

    @Override
    public void writeLong(long value) throws Exception {
        BinaryProtocolCodec.writeTag(outputStream, Tag.LONG);
        outputStream.writeInt64NoTag(value);
    }

    @Override
    public void writeFloat(float value) throws Exception {
        BinaryProtocolCodec.writeTag(outputStream, Tag.FLOAT);
        outputStream.writeFloatNoTag(value);
    }

    @Override
    public void writeDouble(double value) throws Exception {
        BinaryProtocolCodec.writeTag(outputStream, Tag.DOUBLE);
        outputStream.writeDoubleNoTag(value);
    }

    @Override
    public void writeShort(short value) throws Exception {
        BinaryProtocolCodec.writeTag(outputStream, Tag.SHORT);
        outputStream.writeInt32NoTag(value);
    }

    @Override
    public void writeBoolean(boolean value) throws Exception {
        BinaryProtocolCodec.writeTag(outputStream, Tag.BOOLEAN);
        outputStream.writeBoolNoTag(value);
    }

    @Override
    public void writeByte(byte value) throws Exception {
        BinaryProtocolCodec.writeTag(outputStream, Tag.BYTE);
        outputStream.writeRawByte(value);
    }

    @Override
    public void writeChar(char value) throws Exception {
        BinaryProtocolCodec.writeTag(outputStream, Tag.CHAR);
        outputStream.writeUInt32NoTag(value);
    }

    @Override
    public <T> void writeObject(@Nullable T value) throws Exception {
        BinaryProtocolCodec.encodeObject(outputStream, value, codecRegistry);
    }

    @Override
    public void writeBytes(@Nonnull byte[] bytes, int offset, int length) throws Exception {
        BinaryProtocolCodec.writeTag(outputStream, Tag.ARRAY);
        ArrayCodec.writeByteArray(outputStream, bytes, offset, length);
    }

    /**
     * 读写格式仍然要与{@link SerializerBasedCodec}保持一致
     */
    @Override
    public <E> void writeEntity(@Nullable E entity, EntitySerializer<? super E> entitySerializer) throws Exception {
        if (null == entity) {
            BinaryProtocolCodec.writeTag(outputStream, Tag.NULL);
            return;
        }

        writeSuperClassMessageId(entitySerializer);

        // 这里是生成的代码走进来的，因此即使异常，也能定位
        entitySerializer.writeObject(entity, this);
    }

    private <E> void writeSuperClassMessageId(EntitySerializer<? super E> entitySerializer) throws IOException {
//        final Class<?> messageClass = entitySerializer.getEntityClass();
//        final int messageId = messageMapper.getMessageId(messageClass);
//        outputStream.writeInt32NoTag(messageId);
    }

}
