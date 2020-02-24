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
import com.wjybxx.fastjgame.net.serializer.ClassSerializer;
import com.wjybxx.fastjgame.utils.EnumUtils;
import com.wjybxx.fastjgame.utils.entity.NumericalEntity;
import com.wjybxx.fastjgame.utils.entity.NumericalEntityMapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.IdentityHashMap;

/**
 * 数组编解码器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/17
 */
class ArrayCodec implements BinaryCodec<Object> {

    private static final int CHILD_SIZE = 8;

    private static final IdentityHashMap<Class<?>, PrimitiveTypeArrayCodec<?>> primitiveType2CodecMapping = new IdentityHashMap<>(CHILD_SIZE);
    private static final NumericalEntityMapper<PrimitiveTypeArrayCodec<?>> tag2CodecMapping;

    static {
        register(byte.class, new ByteArrayCodec());
        register(char.class, new CharArrayCodec());
        register(short.class, new ShortArrayCodec());
        register(int.class, new IntArrayCodec());
        register(long.class, new LongArrayCodec());
        register(float.class, new FloatArrayCodec());
        register(double.class, new DoubleArrayCodec());
        register(boolean.class, new BooleanArrayCodec());

        PrimitiveTypeArrayCodec<?>[] codecs = primitiveType2CodecMapping.values().toArray(PrimitiveTypeArrayCodec<?>[]::new);
        tag2CodecMapping = EnumUtils.mapping(codecs, true);
    }

    private static void register(Class<?> component, PrimitiveTypeArrayCodec<?> codec) {
        primitiveType2CodecMapping.put(component, codec);
    }

    private final BinaryProtocolCodec binaryProtocolCodec;

    ArrayCodec(BinaryProtocolCodec binaryProtocolCodec) {
        this.binaryProtocolCodec = binaryProtocolCodec;
    }

    @Override
    public boolean isSupport(Class<?> runtimeType) {
        return runtimeType.isArray();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void writeDataNoTag(CodedOutputStream outputStream, @Nonnull Object instance) throws Exception {
        final PrimitiveTypeArrayCodec codec = primitiveType2CodecMapping.get(instance.getClass().getComponentType());
        final int length = Array.getLength(instance);

        if (null != codec) {
            writeChildTypeAndLength(outputStream, codec.childType(), length);
            codec.writeArray(outputStream, instance);
        } else {
            writeChildTypeAndLength(outputStream, WireType.RUN_TIME, length);
            writeObjectArray(outputStream, instance, length);
        }
    }

    private static void writeChildTypeAndLength(CodedOutputStream outputStream, byte childType, int length) throws IOException {
        BinaryProtocolCodec.writeTag(outputStream, childType);
        outputStream.writeUInt32NoTag(length);
    }

    /**
     * 写对象数组，对象数组主要存在Null等处理
     */
    private void writeObjectArray(CodedOutputStream outputStream, @Nonnull Object instance, int length) throws Exception {
        outputStream.writeStringNoTag(instance.getClass().getComponentType().getName());
        for (int index = 0; index < length; index++) {
            Object value = Array.get(instance, index);
            binaryProtocolCodec.writeObject(outputStream, value);
        }
    }

    @Nonnull
    @Override
    public Object readData(CodedInputStream inputStream) throws Exception {
        return readArray(inputStream, null);
    }

    Object readArray(CodedInputStream inputStream, Class<?> objectArrayComponentType) throws Exception {
        final byte childType = BinaryProtocolCodec.readTag(inputStream);
        final int length = inputStream.readUInt32();

        if (childType != WireType.RUN_TIME) {
            final PrimitiveTypeArrayCodec<?> codec = tag2CodecMapping.forNumber(childType);
            assert null != codec;
            return codec.readArray(inputStream, length);
        } else {
            return readObjectArray(inputStream, objectArrayComponentType, length);
        }
    }

    private Object readObjectArray(CodedInputStream inputStream,
                                   @Nullable Class<?> objectArrayComponentType,
                                   int length) throws Exception {
        final String componentClassName = inputStream.readString();
        if (null == objectArrayComponentType) {
            objectArrayComponentType = ClassSerializer.findClass(componentClassName);
        }

        final Object array = Array.newInstance(objectArrayComponentType, length);
        for (int index = 0; index < length; index++) {
            Array.set(array, index, binaryProtocolCodec.readObject(inputStream));
        }
        return array;
    }

    @Override
    public byte getWireType() {
        return WireType.ARRAY;
    }

    static void writeByteArray(CodedOutputStream outputStream, @Nonnull byte[] bytes, int offset, int length) throws Exception {
        writeChildTypeAndLength(outputStream, WireType.BYTE, length);
        ByteArrayCodec.writeArray(outputStream, bytes, offset, length);
    }

    private interface PrimitiveTypeArrayCodec<U> extends NumericalEntity {

        /**
         * 子类标识
         */
        byte childType();

        /**
         * 写入数组的内容
         * 注意：数组的长度已经写入
         */
        void writeArray(CodedOutputStream outputStream, @Nonnull U array) throws Exception;

        /**
         * 读取指定长度的数组
         */
        U readArray(CodedInputStream inputStream, int length) throws Exception;

        @Override
        default int getNumber() {
            return childType();
        }
    }

    private static class ByteArrayCodec implements PrimitiveTypeArrayCodec<byte[]> {

        @Override
        public byte childType() {
            return WireType.BYTE;
        }

        @Override
        public void writeArray(CodedOutputStream outputStream, @Nonnull byte[] array) throws Exception {
            outputStream.writeRawBytes(array, 0, array.length);
        }

        @Override
        public byte[] readArray(CodedInputStream inputStream, int length) throws Exception {
            return inputStream.readRawBytes(length);
        }

        private static void writeArray(CodedOutputStream outputStream, @Nonnull byte[] array, int offset, int length) throws Exception {
            outputStream.writeRawBytes(array, offset, length);
        }
    }

    private static class IntArrayCodec implements PrimitiveTypeArrayCodec<int[]> {

        @Override
        public byte childType() {
            return WireType.INT;
        }

        @Override
        public void writeArray(CodedOutputStream outputStream, @Nonnull int[] array) throws Exception {
            for (int value : array) {
                outputStream.writeInt32NoTag(value);
            }
        }

        @Override
        public int[] readArray(CodedInputStream inputStream, int length) throws Exception {
            int[] result = new int[length];
            for (int index = 0; index < length; index++) {
                result[index] = inputStream.readInt32();
            }
            return result;
        }

    }

    private static class FloatArrayCodec implements PrimitiveTypeArrayCodec<float[]> {

        @Override
        public byte childType() {
            return WireType.FLOAT;
        }

        @Override
        public void writeArray(CodedOutputStream outputStream, @Nonnull float[] array) throws Exception {
            for (float value : array) {
                outputStream.writeFloatNoTag(value);
            }
        }

        @Override
        public float[] readArray(CodedInputStream inputStream, int length) throws Exception {
            float[] result = new float[length];
            for (int index = 0; index < length; index++) {
                result[index] = inputStream.readFloat();
            }
            return result;
        }
    }

    private static class DoubleArrayCodec implements PrimitiveTypeArrayCodec<double[]> {

        @Override
        public byte childType() {
            return WireType.DOUBLE;
        }

        @Override
        public void writeArray(CodedOutputStream outputStream, @Nonnull double[] array) throws Exception {
            for (double value : array) {
                outputStream.writeDoubleNoTag(value);
            }
        }

        @Override
        public double[] readArray(CodedInputStream inputStream, int length) throws Exception {
            double[] result = new double[length];
            for (int index = 0; index < length; index++) {
                result[index] = inputStream.readDouble();
            }
            return result;
        }
    }

    private static class LongArrayCodec implements PrimitiveTypeArrayCodec<long[]> {

        @Override
        public byte childType() {
            return WireType.LONG;
        }

        @Override
        public void writeArray(CodedOutputStream outputStream, @Nonnull long[] array) throws Exception {
            for (long value : array) {
                outputStream.writeInt64NoTag(value);
            }
        }

        @Override
        public long[] readArray(CodedInputStream inputStream, int length) throws Exception {
            long[] result = new long[length];
            for (int index = 0; index < length; index++) {
                result[index] = inputStream.readInt64();
            }
            return result;
        }
    }

    private static class ShortArrayCodec implements PrimitiveTypeArrayCodec<short[]> {

        @Override
        public byte childType() {
            return WireType.SHORT;
        }

        @Override
        public void writeArray(CodedOutputStream outputStream, @Nonnull short[] array) throws Exception {
            for (short value : array) {
                outputStream.writeInt32NoTag(value);
            }
        }

        @Override
        public short[] readArray(CodedInputStream inputStream, int length) throws Exception {
            short[] result = new short[length];
            for (int index = 0; index < length; index++) {
                result[index] = (short) inputStream.readInt32();
            }
            return result;
        }
    }

    private static class CharArrayCodec implements PrimitiveTypeArrayCodec<char[]> {

        @Override
        public byte childType() {
            return WireType.CHAR;
        }

        @Override
        public void writeArray(CodedOutputStream outputStream, @Nonnull char[] array) throws Exception {
            for (char value : array) {
                outputStream.writeUInt32NoTag(value);
            }
        }

        @Override
        public char[] readArray(CodedInputStream inputStream, int length) throws Exception {
            char[] result = new char[length];
            for (int index = 0; index < length; index++) {
                result[index] = (char) inputStream.readUInt32();
            }
            return result;
        }
    }

    private static class BooleanArrayCodec implements PrimitiveTypeArrayCodec<boolean[]> {

        @Override
        public byte childType() {
            return WireType.BOOLEAN;
        }

        @Override
        public void writeArray(CodedOutputStream outputStream, @Nonnull boolean[] array) throws Exception {
            for (boolean value : array) {
                outputStream.writeBoolNoTag(value);
            }
        }

        @Override
        public boolean[] readArray(CodedInputStream inputStream, int length) throws Exception {
            boolean[] result = new boolean[length];
            for (int index = 0; index < length; index++) {
                result[index] = inputStream.readBool();
            }
            return result;
        }
    }
}
