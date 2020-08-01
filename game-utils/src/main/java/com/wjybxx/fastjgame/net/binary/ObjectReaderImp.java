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

import com.wjybxx.fastjgame.util.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
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

    private void readTagAndCheck(BinaryTag expectedTag) throws Exception {
        final BinaryTag tag = inputStream.readTag();
        if (tag != expectedTag) {
            throw new IOException("Incompatible wireType, expected: " + expectedTag + ", but read: " + tag);
        }
    }

    @Override
    public String readString() throws Exception {
        final BinaryTag tag = inputStream.readTag();
        if (tag == BinaryTag.NULL) {
            return null;
        }
        checkTag(tag, BinaryTag.STRING);

        return inputStream.readString();
    }

    @Override
    public <T> T readMessage() throws Exception {
        final BinaryTag tag = inputStream.readTag();
        if (tag == BinaryTag.NULL) {
            return null;
        }
        checkTag(tag, BinaryTag.POJO);

        @SuppressWarnings("unchecked") final T message = (T) PojoCodecUtils.readPojoImp(inputStream, codecRegistry, this);
        return message;
    }

    @Override
    public byte[] readBytes() throws Exception {
        final BinaryTag tag = inputStream.readTag();
        if (tag == BinaryTag.NULL) {
            return null;
        }

        checkTag(tag, BinaryTag.ARRAY);

        return ArrayCodec.readByteArray(inputStream);
    }

    @Nullable
    @Override
    public <C extends Collection<E>, E> C readCollection(@Nonnull IntFunction<C> collectionFactory) throws Exception {
        final BinaryTag tag = inputStream.readTag();
        if (tag == BinaryTag.NULL) {
            return null;
        }

        checkTag(tag, BinaryTag.COLLECTION);

        return CollectionCodec.readCollectionImp(inputStream, collectionFactory, this);
    }

    @Nullable
    @Override
    public <M extends Map<K, V>, K, V> M readMap(@Nonnull IntFunction<M> mapFactory) throws Exception {
        final BinaryTag tag = inputStream.readTag();
        if (tag == BinaryTag.NULL) {
            return null;
        }

        checkTag(tag, BinaryTag.MAP);

        return MapCodec.readMapImpl(inputStream, mapFactory, this);
    }

    @Nullable
    @Override
    public <T> T readArray(@Nonnull Class<?> componentType) throws Exception {
        final BinaryTag tag = inputStream.readTag();
        if (tag == BinaryTag.NULL) {
            return null;
        }

        checkTag(tag, BinaryTag.ARRAY);

        @SuppressWarnings("unchecked") final T array = (T) ArrayCodec.readArrayImpl(inputStream, componentType, this);
        return array;
    }

    public <E> E readEntity(EntityFactory<E> factory, Class<? super E> superClass) throws Exception {
        final BinaryTag tag = inputStream.readTag();
        if (tag == BinaryTag.NULL) {
            return null;
        }

        checkTag(tag, BinaryTag.POJO);

        return PojoCodecUtils.readPolymorphicPojoImpl(inputStream, factory, superClass, codecRegistry, this);
    }

    @Override
    public <T> T readPreDeserializeObject() throws Exception {
        final BinaryTag tag = inputStream.readTag();
        if (tag == BinaryTag.NULL) {
            return null;
        }

        checkTag(tag, BinaryTag.ARRAY);

        final BinaryTag childType = inputStream.readTag();
        checkTag(childType, BinaryTag.BYTE);

        final int length = inputStream.readFixedInt32();
        final DataInputStream childInputStream = inputStream.slice(length);
        final ObjectReaderImp childReader = new ObjectReaderImp(codecRegistry, childInputStream);
        final T value = childReader.readObject();

        // 更新当前输入流的读索引
        inputStream.readerIndex(inputStream.readerIndex() + length);

        return value;
    }

    private void checkTag(final BinaryTag readTag, final BinaryTag expectedTag) throws Exception {
        if (readTag != expectedTag) {
            throw new IOException("Incompatible wireType, expected: " + expectedTag + ", but read: " + readTag);
        }
    }

    // -----------------------------------------------------------------------------------------------------------

    @Override
    public <T> T readObject() throws Exception {
        final BinaryTag tag = inputStream.readTag();
        @SuppressWarnings("unchecked") final T result = (T) readObjectImpl(tag);
        return result;
    }

    private Object readObjectImpl(BinaryTag tag) throws Exception {
        switch (tag) {
            case NULL:
                return null;

            case BYTE:
                return inputStream.readByte();
            case CHAR:
                return inputStream.readChar();
            case SHORT:
                return inputStream.readShort();
            case INT:
                return inputStream.readInt();
            case BOOLEAN:
                return inputStream.readBoolean();
            case LONG:
                return inputStream.readLong();
            case FLOAT:
                return inputStream.readFloat();
            case DOUBLE:
                return inputStream.readDouble();
            case STRING:
                return inputStream.readString();

            case POJO:
                return PojoCodecUtils.readPojoImp(inputStream, codecRegistry, this);
            case ARRAY:
                return ArrayCodec.readArrayImpl(inputStream, null, this);
            case MAP:
                return MapCodec.readMapImpl(inputStream, CollectionUtils::newLinkedHashMapWithExpectedSize, this);
            case COLLECTION:
                return CollectionCodec.readCollectionImp(inputStream, ArrayList::new, this);
            default:
                throw new IOException("unexpected tag : " + tag);
        }
    }
}
