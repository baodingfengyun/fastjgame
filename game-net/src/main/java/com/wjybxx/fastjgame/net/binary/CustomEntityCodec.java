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

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.wjybxx.fastjgame.net.misc.MessageMapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * 自定义实体对象 - 通过生成的辅助类或手写的{@link EntitySerializer}进行编解码
 */
class CustomEntityCodec implements BinaryCodec<Object> {

    private final MessageMapper messageMapper;
    private final Map<Class<?>, EntitySerializer<?>> beanSerializerMap;
    private final BinaryProtocolCodec binaryProtocolCodec;

    CustomEntityCodec(MessageMapper messageMapper,
                      Map<Class<?>, EntitySerializer<?>> beanSerializerMap,
                      BinaryProtocolCodec binaryProtocolCodec) {
        this.messageMapper = messageMapper;
        this.beanSerializerMap = beanSerializerMap;
        this.binaryProtocolCodec = binaryProtocolCodec;
    }

    @Override
    public boolean isSupport(Class<?> runtimeType) {
        return beanSerializerMap.containsKey(runtimeType);
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public void writeData(CodedOutputStream outputStream, @Nonnull Object instance) throws Exception {
        final Class<?> messageClass = instance.getClass();
        outputStream.writeInt32NoTag(messageMapper.getMessageId(messageClass));

        final EntitySerializer entitySerializer = beanSerializerMap.get(messageClass);
        final EntityOutputStreamImp beanOutputStreamImp = new EntityOutputStreamImp(outputStream);

        try {
            entitySerializer.writeObject(instance, beanOutputStreamImp);
        } catch (Exception e) {
            throw new IOException(messageClass.getName(), e);
        }
    }

    @Nonnull
    @Override
    public Object readData(CodedInputStream inputStream) throws Exception {
        final int messageId = inputStream.readInt32();
        final Class<?> messageClass = messageMapper.getMessageClazz(messageId);

        if (null == messageClass) {
            throw new IOException("unknown message id " + messageId);
        }

        final EntitySerializer<?> entitySerializer = beanSerializerMap.get(messageClass);
        final EntityInputStreamImp beanInputStreamImp = new EntityInputStreamImp(inputStream);

        try {
            return entitySerializer.readObject(beanInputStreamImp);
        } catch (Exception e) {
            throw new IOException(messageClass.getName(), e);
        }
    }

    @Override
    public byte getWireType() {
        return WireType.CUSTOM_ENTITY;
    }

    class EntityOutputStreamImp implements EntityOutputStream {

        private final CodedOutputStream outputStream;

        private EntityOutputStreamImp(CodedOutputStream outputStream) {
            this.outputStream = outputStream;
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
                codec.writeData(outputStream, fieldValue);
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

        @Override
        public void writeBytes(@Nullable byte[] bytes, int offset, int length) throws Exception {
            if (null == bytes) {
                BinaryProtocolCodec.writeTag(outputStream, WireType.NULL);
                return;
            }

            BinaryProtocolCodec.writeTag(outputStream, WireType.ARRAY);
            ArrayCodec.writeByteArray(outputStream, bytes, offset, length);
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
    }

    class EntityInputStreamImp implements EntityInputStream {

        private final CodedInputStream inputStream;

        private EntityInputStreamImp(CodedInputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public <T> T readField(byte wireType) throws Exception {
            final byte tag = BinaryProtocolCodec.readTag(inputStream);
            if (tag == WireType.NULL) {
                return null;
            }

            // 类型校验
            if (wireType != WireType.RUN_TIME && wireType != tag) {
                throw new IOException("Incompatible wireType, expected: " + wireType + ", but read: " + tag);
            }

            final BinaryCodec<T> codec = binaryProtocolCodec.getCodec(tag);
            return codec.readData(inputStream);
        }

        /**
         * 读写格式仍然要与{@link CustomEntityCodec}保持一致
         */
        public <E> E readEntity(EntityFactory<E> entityFactory, AbstractEntitySerializer<? super E> entitySerializer) throws Exception {
            final byte tag = BinaryProtocolCodec.readTag(inputStream);
            if (tag == WireType.NULL) {
                return null;
            }

            if (tag != WireType.CUSTOM_ENTITY) {
                throw new IOException("Incompatible wireType, expected: " + WireType.CUSTOM_ENTITY + ", but read: " + tag);
            }

            checkMessageId(entitySerializer);

            final E instance = entityFactory.newInstance();
            entitySerializer.readFields(instance, this);
            return instance;
        }

        private void checkMessageId(AbstractEntitySerializer<?> entitySerializer) throws IOException {
            final int messageIdExpected = messageMapper.getMessageId(entitySerializer.getEntityClass());
            final int messageId = inputStream.readInt32();
            if (messageId != messageIdExpected) {
                throw new IOException("Incompatible message, expected: " + messageIdExpected + ", but read: " + messageId);
            }
        }

        @Nullable
        @Override
        public <M extends Map<K, V>, K, V> M readMap(@Nonnull IntFunction<M> mapFactory) throws Exception {
            final byte tag = BinaryProtocolCodec.readTag(inputStream);
            if (tag == WireType.NULL) {
                return null;
            }

            if (tag != WireType.MAP) {
                throw new IOException("Incompatible wireType, expected: " + WireType.MAP + ", but read: " + tag);
            }

            return MapCodec.readMapImp(binaryProtocolCodec, inputStream, mapFactory);
        }

        @Nullable
        @Override
        public <C extends Collection<E>, E> C readCollection(@Nonnull IntFunction<C> collectionFactory) throws Exception {
            final byte tag = BinaryProtocolCodec.readTag(inputStream);
            if (tag == WireType.NULL) {
                return null;
            }

            if (tag != WireType.COLLECTION) {
                throw new IOException("Incompatible wireType, expected: " + WireType.COLLECTION + ", but read: " + tag);
            }

            return CollectionCodec.readCollectionImp(binaryProtocolCodec, inputStream, collectionFactory);
        }

        @Nullable
        @Override
        public <T> T readArray(@Nonnull Class<?> componentType) throws Exception {
            final byte tag = BinaryProtocolCodec.readTag(inputStream);
            if (tag == WireType.NULL) {
                return null;
            }

            if (tag != WireType.ARRAY) {
                throw new IOException("Incompatible wireType, expected: " + WireType.ARRAY + ", but read: " + tag);
            }

            final ArrayCodec arrayCodec = (ArrayCodec) binaryProtocolCodec.getCodec(WireType.ARRAY);
            @SuppressWarnings("unchecked") final T array = (T) arrayCodec.readArray(inputStream, componentType);
            return array;
        }

        private void readTagAndCheck(byte expectedTag) throws Exception {
            final byte tag = BinaryProtocolCodec.readTag(inputStream);
            if (tag != expectedTag) {
                throw new IOException("Incompatible wireType, expected: " + expectedTag + ", but read: " + tag);
            }
        }

        @Override
        public int readInt() throws Exception {
            readTagAndCheck(WireType.INT);
            return inputStream.readInt32();
        }

        @Override
        public long readLong() throws Exception {
            readTagAndCheck(WireType.LONG);
            return inputStream.readInt64();
        }

        @Override
        public float readFloat() throws Exception {
            readTagAndCheck(WireType.FLOAT);
            return inputStream.readFloat();
        }

        @Override
        public double readDouble() throws Exception {
            readTagAndCheck(WireType.DOUBLE);
            return inputStream.readDouble();
        }

        @Override
        public short readShort() throws Exception {
            readTagAndCheck(WireType.SHORT);
            return (short) inputStream.readInt32();
        }

        @Override
        public boolean readBoolean() throws Exception {
            readTagAndCheck(WireType.BOOLEAN);
            return inputStream.readBool();
        }

        @Override
        public byte readByte() throws Exception {
            readTagAndCheck(WireType.BYTE);
            return inputStream.readRawByte();
        }

        @Override
        public char readChar() throws Exception {
            readTagAndCheck(WireType.CHAR);
            return (char) inputStream.readUInt32();
        }
    }
}
