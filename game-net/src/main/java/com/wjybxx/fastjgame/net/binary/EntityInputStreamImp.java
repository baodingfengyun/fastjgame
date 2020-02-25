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
        readTagAndCheck(Tag.INT);
        return inputStream.readInt32();
    }

    @Override
    public long readLong() throws Exception {
        readTagAndCheck(Tag.LONG);
        return inputStream.readInt64();
    }

    @Override
    public float readFloat() throws Exception {
        readTagAndCheck(Tag.FLOAT);
        return inputStream.readFloat();
    }

    @Override
    public double readDouble() throws Exception {
        readTagAndCheck(Tag.DOUBLE);
        return inputStream.readDouble();
    }

    @Override
    public short readShort() throws Exception {
        readTagAndCheck(Tag.SHORT);
        return (short) inputStream.readInt32();
    }

    @Override
    public boolean readBoolean() throws Exception {
        readTagAndCheck(Tag.BOOLEAN);
        return inputStream.readBool();
    }

    @Override
    public byte readByte() throws Exception {
        readTagAndCheck(Tag.BYTE);
        return inputStream.readRawByte();
    }

    @Override
    public char readChar() throws Exception {
        readTagAndCheck(Tag.CHAR);
        return (char) inputStream.readUInt32();
    }

    @Override
    public String readString() throws Exception {
        final Tag tag = BinaryProtocolCodec.readTag(inputStream);
        if (tag == Tag.NULL) {
            return null;
        }
        return inputStream.readString();
    }

    @Override
    public byte[] readBytes() throws Exception {
        return readArray(byte.class);
    }

    @Override
    public <T> T readObject() throws Exception {
        return BinaryProtocolCodec.decodeObject(inputStream, codecRegistry);
    }

    private void readTagAndCheck(Tag expectedTag) throws Exception {
        final Tag tag = BinaryProtocolCodec.readTag(inputStream);
        if (tag != expectedTag) {
            throw new IOException("Incompatible wireType, expected: " + expectedTag + ", but read: " + tag);
        }
    }

    @Nullable
    @Override
    public <C extends Collection<E>, E> C readCollection(@Nonnull IntFunction<C> collectionFactory) throws Exception {
        final Tag tag = BinaryProtocolCodec.readTag(inputStream);
        if (tag == Tag.NULL) {
            return null;
        }

        checkTag(tag, Tag.COLLECTION);

        return CollectionCodec.readCollectionImp(inputStream, collectionFactory, codecRegistry);
    }

    @Nullable
    @Override
    public <M extends Map<K, V>, K, V> M readMap(@Nonnull IntFunction<M> mapFactory) throws Exception {
        final Tag tag = BinaryProtocolCodec.readTag(inputStream);
        if (tag == Tag.NULL) {
            return null;
        }

        checkTag(tag, Tag.MAP);

        return MapCodec.readMapImp(inputStream, mapFactory, codecRegistry);
    }

    @Nullable
    @Override
    public <T> T readArray(@Nonnull Class<?> componentType) throws Exception {
        final Tag tag = BinaryProtocolCodec.readTag(inputStream);
        if (tag == Tag.NULL) {
            return null;
        }

        checkTag(tag, Tag.ARRAY);

        @SuppressWarnings("unchecked") final T array = (T) ArrayCodec.readArray(inputStream, componentType, codecRegistry);
        return array;
    }

    public <E> E readEntity(EntityFactory<E> entityFactory, AbstractEntitySerializer<? super E> serializer) throws Exception {
        final Tag tag = BinaryProtocolCodec.readTag(inputStream);
        if (tag == Tag.NULL) {
            return null;
        }

        checkTag(tag, Tag.POJO);

        checkMessageId(serializer);

        final E instance = entityFactory.newInstance();
        serializer.readFields(instance, this);
        return instance;
    }

    private void checkMessageId(AbstractEntitySerializer<?> entitySerializer) throws IOException {
        final int providerId = inputStream.readInt32();
        final int classId = inputStream.readInt32();
        final Codec<?> pojoCodec = codecRegistry.getPojoCodec(providerId, classId);

        if (entitySerializer.getEntityClass() != pojoCodec.getEncoderClass()) {
            throw new IOException(String.format("Incompatible class, expected: %s, but read %s ",
                    entitySerializer.getEntityClass().getName(),
                    pojoCodec.getEncoderClass().getName()));
        }
    }

    private void checkTag(final Tag readTag, final Tag expectedTag) throws Exception {
        if (readTag != expectedTag) {
            throw new IOException("Incompatible wireType, expected: " + expectedTag + ", but read: " + readTag);
        }
    }
}
