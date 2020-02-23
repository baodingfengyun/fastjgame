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
import com.wjybxx.fastjgame.net.misc.MessageMapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/23
 */
class EntityOutputStreamImp implements EntityOutputStream {

    private final BinaryProtocolCodec binaryProtocolCodec;
    private final MessageMapper messageMapper;
    private final CodedOutputStream outputStream;

    EntityOutputStreamImp(BinaryProtocolCodec binaryProtocolCodec, MessageMapper messageMapper, CodedOutputStream outputStream) {
        this.binaryProtocolCodec = binaryProtocolCodec;
        this.messageMapper = messageMapper;
        this.outputStream = outputStream;
    }

    @Override
    public void writeInt(int value) throws Exception {
        BinaryProtocolCodec.writeTag(outputStream, WireType.INT);
        outputStream.writeInt32NoTag(value);
    }

    @Override
    public void writeLong(long value) throws Exception {
        BinaryProtocolCodec.writeTag(outputStream, WireType.LONG);
        outputStream.writeInt64NoTag(value);
    }

    @Override
    public void writeFloat(float value) throws Exception {
        BinaryProtocolCodec.writeTag(outputStream, WireType.FLOAT);
        outputStream.writeFloatNoTag(value);
    }

    @Override
    public void writeDouble(double value) throws Exception {
        BinaryProtocolCodec.writeTag(outputStream, WireType.DOUBLE);
        outputStream.writeDoubleNoTag(value);
    }

    @Override
    public void writeShort(short value) throws Exception {
        BinaryProtocolCodec.writeTag(outputStream, WireType.SHORT);
        outputStream.writeInt32NoTag(value);
    }

    @Override
    public void writeBoolean(boolean value) throws Exception {
        BinaryProtocolCodec.writeTag(outputStream, WireType.BOOLEAN);
        outputStream.writeBoolNoTag(value);
    }

    @Override
    public void writeByte(byte value) throws Exception {
        BinaryProtocolCodec.writeTag(outputStream, WireType.BYTE);
        outputStream.writeRawByte(value);
    }

    @Override
    public void writeChar(char value) throws Exception {
        BinaryProtocolCodec.writeTag(outputStream, WireType.CHAR);
        outputStream.writeUInt32NoTag(value);
    }

    @Override
    public void writeBytes(@Nonnull byte[] bytes, int offset, int length) throws Exception {
        BinaryProtocolCodec.writeTag(outputStream, WireType.ARRAY);
        ArrayCodec.writeByteArray(outputStream, bytes, offset, length);
    }

    @Override
    public <T> void writeField(byte wireType, @Nullable T fieldValue) throws Exception {
        // null也需要写入，因为新对象的属性不一定也是null
        if (fieldValue == null) {
            BinaryProtocolCodec.writeTag(outputStream, WireType.NULL);
            return;
        }

        // 索引为具体类型的字段
        if (wireType != WireType.RUN_TIME) {
            BinaryProtocolCodec.writeTag(outputStream, wireType);
            final BinaryCodec<T> codec = binaryProtocolCodec.getCodec(wireType);
            codec.writeDataNoTag(outputStream, fieldValue);
            return;
        }

        // 运行时才知道的类型 - 极少走到这里
        binaryProtocolCodec.writeRuntimeType(outputStream, fieldValue);
    }

    /**
     * 读写格式仍然要与{@link CustomEntityCodec}保持一致
     */
    @Override
    public <E> void writeEntity(@Nullable E entity, EntitySerializer<? super E> entitySerializer) throws Exception {
        if (null == entity) {
            BinaryProtocolCodec.writeTag(outputStream, WireType.NULL);
            return;
        }

        writeSuperClassMessageId(entitySerializer);

        // 这里是生成的代码走进来的，因此即使异常，也能定位
        entitySerializer.writeObject(entity, this);
    }

    private <E> void writeSuperClassMessageId(EntitySerializer<? super E> entitySerializer) throws IOException {
        final Class<?> messageClass = entitySerializer.getEntityClass();
        final int messageId = messageMapper.getMessageId(messageClass);
        outputStream.writeInt32NoTag(messageId);
    }

}
