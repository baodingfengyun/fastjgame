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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/23
 */
class EntityInputStreamImp implements EntityInputStream {

    private final CodecRegistry codecRegistry;
    private final CodedInputStream inputStream;

    EntityInputStreamImp(CodecRegistry codecRegistry, CodedInputStream inputStream) {
        this.codecRegistry = codecRegistry;
        this.inputStream = inputStream;
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

    @Override
    public String readString() throws Exception {
        return BinaryProtocolCodec.decodeObject(inputStream, codecRegistry);
    }

    @Override
    public byte[] readBytes() throws Exception {
        return readArray(byte.class);
    }

    @Override
    public <T> T readObject() throws Exception {
        return BinaryProtocolCodec.decodeObject(inputStream, codecRegistry);
    }

    private void readTagAndCheck(WireType expectedTag) throws Exception {
        final WireType tag = BinaryProtocolCodec.readTag(inputStream);
        if (tag != expectedTag) {
            throw new IOException("Incompatible wireType, expected: " + expectedTag + ", but read: " + tag);
        }
    }

    @Nullable
    @Override
    public <C extends Collection<E>, E> C readCollection(@Nonnull IntFunction<C> collectionFactory) throws Exception {
        final WireType tag = BinaryProtocolCodec.readTag(inputStream);
        if (tag == WireType.NULL) {
            return null;
        }

        checkTag(tag, WireType.COLLECTION);

        return CollectionCodec.readCollectionImp(inputStream, collectionFactory, codecRegistry);
    }

    @Nullable
    @Override
    public <M extends Map<K, V>, K, V> M readMap(@Nonnull IntFunction<M> mapFactory) throws Exception {
        final WireType tag = BinaryProtocolCodec.readTag(inputStream);
        if (tag == WireType.NULL) {
            return null;
        }

        checkTag(tag, WireType.MAP);

        return MapCodec.readMapImp(inputStream, mapFactory, codecRegistry);
    }

    @Nullable
    @Override
    public <T> T readArray(@Nonnull Class<?> componentType) throws Exception {
        final WireType tag = BinaryProtocolCodec.readTag(inputStream);
        if (tag == WireType.NULL) {
            return null;
        }

        checkTag(tag, WireType.ARRAY);

        final ArrayCodec arrayCodec = (ArrayCodec) codecRegistry.get(ArrayCodec.ARRAY_CLASS_KEY);
        @SuppressWarnings("unchecked") final T array = (T) arrayCodec.readArray(inputStream, componentType, codecRegistry);
        return array;
    }

    public <E> E readEntity(EntityFactory<E> entityFactory, AbstractEntitySerializer<? super E> entitySerializer) throws Exception {
        final WireType tag = BinaryProtocolCodec.readTag(inputStream);
        if (tag == WireType.NULL) {
            return null;
        }

        checkTag(tag, WireType.POJO);

        checkMessageId(entitySerializer);

        final E instance = entityFactory.newInstance();
        entitySerializer.readFields(instance, this);
        return instance;
    }

    private void checkMessageId(AbstractEntitySerializer<?> entitySerializer) throws IOException {
//        final int messageIdExpected = messageMapper.getMessageId(entitySerializer.getEntityClass());
//        final int messageId = inputStream.readInt32();
//        if (messageId != messageIdExpected) {
//            throw new IOException("Incompatible message, expected: " + messageIdExpected + ", but read: " + messageId);
//        }
    }

    private void checkTag(final WireType readTag, final WireType expectedTag) throws Exception {
        if (readTag != expectedTag) {
            throw new IOException("Incompatible wireType, expected: " + expectedTag + ", but read: " + readTag);
        }
    }
}
