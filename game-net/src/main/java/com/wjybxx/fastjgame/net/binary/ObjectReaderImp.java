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
import java.util.function.IntFunction;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/23
 */
class ObjectReaderImp implements ObjectReader {

    private final CodecRegistry codecRegistry;
    private final DataInputStream inputStream;

    ObjectReaderImp(CodecRegistry codecRegistry, DataInputStream inputStream) {
        this.codecRegistry = codecRegistry;
        this.inputStream = inputStream;
    }

    @Override
    public int readInt() throws Exception {
        readTagAndCheck(BinaryTag.INT);
        return inputStream.readInt();
    }

    @Override
    public long readLong() throws Exception {
        readTagAndCheck(BinaryTag.LONG);
        return inputStream.readLong();
    }

    @Override
    public float readFloat() throws Exception {
        readTagAndCheck(BinaryTag.FLOAT);
        return inputStream.readFloat();
    }

    @Override
    public double readDouble() throws Exception {
        readTagAndCheck(BinaryTag.DOUBLE);
        return inputStream.readDouble();
    }

    @Override
    public short readShort() throws Exception {
        readTagAndCheck(BinaryTag.SHORT);
        return inputStream.readShort();
    }

    @Override
    public boolean readBoolean() throws Exception {
        readTagAndCheck(BinaryTag.BOOLEAN);
        return inputStream.readBoolean();
    }

    @Override
    public byte readByte() throws Exception {
        readTagAndCheck(BinaryTag.BYTE);
        return inputStream.readByte();
    }

    @Override
    public char readChar() throws Exception {
        readTagAndCheck(BinaryTag.CHAR);
        return inputStream.readChar();
    }

    @Override
    public String readString() throws Exception {
        final BinaryTag tag = inputStream.readTag();
        if (tag == BinaryTag.NULL) {
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
        return BinarySerializer.decodeObject(inputStream, codecRegistry);
    }

    private void readTagAndCheck(BinaryTag expectedTag) throws Exception {
        final BinaryTag tag = inputStream.readTag();
        if (tag != expectedTag) {
            throw new IOException("Incompatible wireType, expected: " + expectedTag + ", but read: " + tag);
        }
    }

    @Nullable
    @Override
    public <C extends Collection<E>, E> C readCollection(@Nonnull IntFunction<C> collectionFactory) throws Exception {
        final BinaryTag tag = inputStream.readTag();
        if (tag == BinaryTag.NULL) {
            return null;
        }

        checkTag(tag, BinaryTag.COLLECTION);

        return CollectionCodec.readCollectionImp(inputStream, collectionFactory, codecRegistry);
    }

    @Nullable
    @Override
    public <M extends Map<K, V>, K, V> M readMap(@Nonnull IntFunction<M> mapFactory) throws Exception {
        final BinaryTag tag = inputStream.readTag();
        if (tag == BinaryTag.NULL) {
            return null;
        }

        checkTag(tag, BinaryTag.MAP);

        return MapCodec.readMapImp(inputStream, mapFactory, codecRegistry);
    }

    @Nullable
    @Override
    public <T> T readArray(@Nonnull Class<?> componentType) throws Exception {
        final BinaryTag tag = inputStream.readTag();
        if (tag == BinaryTag.NULL) {
            return null;
        }

        checkTag(tag, BinaryTag.ARRAY);

        @SuppressWarnings("unchecked") final T array = (T) ArrayCodec.readArray(inputStream, componentType, codecRegistry);
        return array;
    }

    public <E> E readEntity(EntityFactory<E> entityFactory, Class<? super E> entitySuperClass) throws Exception {
        final BinaryTag tag = inputStream.readTag();
        if (tag == BinaryTag.NULL) {
            return null;
        }

        checkTag(tag, BinaryTag.POJO);

        final PojoCodec<?> pojoCodec = PojoCodec.getPojoCodec(inputStream, codecRegistry);

        checkEntitySuperClass(entitySuperClass, pojoCodec);

        checkSupportReadFields(pojoCodec);

        @SuppressWarnings("unchecked") final CustomPojoCodec<E> customPojoCodec = (CustomPojoCodec<E>) pojoCodec;
        final E instance = entityFactory.newInstance();
        customPojoCodec.decodeBody(instance, inputStream, codecRegistry);
        return instance;
    }

    private void checkEntitySuperClass(Class<?> entitySuperClass, ObjectCodec<?> pojoCodec) throws IOException {
        if (entitySuperClass != pojoCodec.getEncoderClass()) {
            throw new IOException(String.format("Incompatible class, expected: %s, but read %s ",
                    entitySuperClass.getName(),
                    pojoCodec.getEncoderClass().getName()));
        }
    }

    private void checkSupportReadFields(PojoCodec<?> pojoCodec) throws IOException {
        if (!(pojoCodec instanceof CustomPojoCodec) || !((CustomPojoCodec<?>) pojoCodec).isSupportReadFields()) {
            throw new IOException("Unsupported codec, entitySuperClass serializer must implements " +
                    AbstractPojoCodecImpl.class.getName());
        }
    }

    private void checkTag(final BinaryTag readTag, final BinaryTag expectedTag) throws Exception {
        if (readTag != expectedTag) {
            throw new IOException("Incompatible wireType, expected: " + expectedTag + ", but read: " + readTag);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T readPreDeserializeObject() throws Exception {
        final BinaryTag tag = inputStream.readTag();
        if (tag == BinaryTag.NULL) {
            return null;
        }

        if (tag != BinaryTag.ARRAY) {
            return (T) BinarySerializer.decodeObjectImp(tag, inputStream, codecRegistry);
        }

        final BinaryTag childTag = inputStream.readTag();
        final int length = inputStream.readFixedInt32();

        if (childTag != BinaryTag.BYTE) {
            return (T) ArrayCodec.readArrayImp(inputStream, null, codecRegistry, childTag, length);
        }

        final DataInputStream childInputStream = inputStream.slice(length);
        final ObjectReaderImp childEntityInputStream = new ObjectReaderImp(codecRegistry, childInputStream);
        final T value = childEntityInputStream.readObject();

        // 更新当前输入流的读索引
        inputStream.readIndex(inputStream.readIndex() + childInputStream.readIndex());

        return value;
    }
}
