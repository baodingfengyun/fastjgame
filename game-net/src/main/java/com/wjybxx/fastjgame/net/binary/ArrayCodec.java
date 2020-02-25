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
import com.wjybxx.fastjgame.utils.entity.NumericalEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.EnumMap;
import java.util.IdentityHashMap;

/**
 * 数组编解码器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/17
 */
class ArrayCodec implements Codec<Object> {

    static final Class<Object[]> ARRAY_ENCODER_CLASS = Object[].class;

    private static final int CHILD_SIZE = 9;

    private static final IdentityHashMap<Class<?>, BasicArrayCodec<?>> componentType2CodecMapping = new IdentityHashMap<>(CHILD_SIZE);
    private static final EnumMap<Tag, BasicArrayCodec<?>> tag2CodecMapping = new EnumMap<>(Tag.class);

    static {
        register(byte.class, new ByteArrayCodec());
        register(char.class, new CharArrayCodec());
        register(short.class, new ShortArrayCodec());
        register(int.class, new IntArrayCodec());
        register(long.class, new LongArrayCodec());
        register(float.class, new FloatArrayCodec());
        register(double.class, new DoubleArrayCodec());
        register(boolean.class, new BooleanArrayCodec());
        register(String.class, new StringArrayCodec());
    }

    private static void register(Class<?> component, BasicArrayCodec<?> codec) {
        componentType2CodecMapping.put(component, codec);
        tag2CodecMapping.put(codec.childType(), codec);
    }

    ArrayCodec() {

    }

    @SuppressWarnings("unchecked")
    @Override
    public void encode(@Nonnull CodedOutputStream outputStream, @Nonnull Object value, CodecRegistry codecRegistry) throws Exception {
        final BasicArrayCodec codec = componentType2CodecMapping.get(value.getClass().getComponentType());
        final int length = Array.getLength(value);

        if (null != codec) {
            writeTagAndChildTypeAndLength(outputStream, codec.childType(), length);
            codec.writeArray(outputStream, value);
        } else {
            writeTagAndChildTypeAndLength(outputStream, Tag.UNKNOWN, length);
            writeObjectArray(outputStream, value, length, codecRegistry);
        }
    }

    private static void writeTagAndChildTypeAndLength(CodedOutputStream outputStream, Tag childType, int length) throws IOException {
        BinaryProtocolCodec.writeTag(outputStream, Tag.ARRAY);
        BinaryProtocolCodec.writeTag(outputStream, childType);
        outputStream.writeUInt32NoTag(length);
    }

    /**
     * 写对象数组，对象数组主要存在Null等处理
     */
    private void writeObjectArray(CodedOutputStream outputStream, @Nonnull Object instance, int length,
                                  CodecRegistry codecRegistry) throws Exception {
        ClassCodec.encodeClass(outputStream, instance.getClass().getComponentType());

        for (int index = 0; index < length; index++) {
            Object value = Array.get(instance, index);
            BinaryProtocolCodec.encodeObject(outputStream, value, codecRegistry);
        }
    }

    @Nonnull
    @Override
    public Object decode(@Nonnull CodedInputStream inputStream, CodecRegistry codecRegistry) throws Exception {
        return readArray(inputStream, null, codecRegistry);
    }

    Object readArray(CodedInputStream inputStream, Class<?> objectArrayComponentType, CodecRegistry codecRegistry) throws Exception {
        final Tag childType = BinaryProtocolCodec.readTag(inputStream);
        final int length = inputStream.readUInt32();

        if (childType != Tag.UNKNOWN) {
            final BasicArrayCodec<?> codec = tag2CodecMapping.get(childType);
            assert null != codec;
            return codec.readArray(inputStream, length);
        } else {
            return readObjectArray(inputStream, objectArrayComponentType, length, codecRegistry);
        }
    }

    private Object readObjectArray(CodedInputStream inputStream,
                                   @Nullable Class<?> objectArrayComponentType, int length,
                                   CodecRegistry codecRegistry) throws Exception {
        final String componentClassName = inputStream.readString();
        if (null == objectArrayComponentType) {
            // 查找类性能不好
            objectArrayComponentType = ClassCodec.decodeClass(componentClassName);
        }

        final Object array = Array.newInstance(objectArrayComponentType, length);
        for (int index = 0; index < length; index++) {
            Array.set(array, index, BinaryProtocolCodec.decodeObject(inputStream, codecRegistry));
        }

        return array;
    }

    static void writeByteArray(CodedOutputStream outputStream, @Nonnull byte[] bytes, int offset, int length) throws Exception {
        writeTagAndChildTypeAndLength(outputStream, Tag.BYTE, length);
        ByteArrayCodec.writeArray(outputStream, bytes, offset, length);
    }

    @Override
    public Class<?> getEncoderClass() {
        return ARRAY_ENCODER_CLASS;
    }

    private interface BasicArrayCodec<U> extends NumericalEntity {

        /**
         * 子类标识
         */
        Tag childType();

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
            return childType().getNumber();
        }
    }

    private static class ByteArrayCodec implements BasicArrayCodec<byte[]> {

        @Override
        public Tag childType() {
            return Tag.BOOLEAN;
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

    private static class IntArrayCodec implements BasicArrayCodec<int[]> {

        @Override
        public Tag childType() {
            return Tag.INT;
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

    private static class FloatArrayCodec implements BasicArrayCodec<float[]> {

        @Override
        public Tag childType() {
            return Tag.FLOAT;
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

    private static class DoubleArrayCodec implements BasicArrayCodec<double[]> {

        @Override
        public Tag childType() {
            return Tag.DOUBLE;
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

    private static class LongArrayCodec implements BasicArrayCodec<long[]> {

        @Override
        public Tag childType() {
            return Tag.LONG;
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

    private static class ShortArrayCodec implements BasicArrayCodec<short[]> {

        @Override
        public Tag childType() {
            return Tag.SHORT;
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

    private static class CharArrayCodec implements BasicArrayCodec<char[]> {

        @Override
        public Tag childType() {
            return Tag.CHAR;
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

    private static class BooleanArrayCodec implements BasicArrayCodec<boolean[]> {

        @Override
        public Tag childType() {
            return Tag.BOOLEAN;
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

    private static class StringArrayCodec implements BasicArrayCodec<String[]> {

        @Override
        public Tag childType() {
            return Tag.STRING;
        }

        @Override
        public void writeArray(CodedOutputStream outputStream, @Nonnull String[] array) throws Exception {
            for (String value : array) {
                if (value == null) {
                    BinaryProtocolCodec.writeTag(outputStream, Tag.NULL);
                } else {
                    BinaryProtocolCodec.writeTag(outputStream, Tag.STRING);
                    outputStream.writeStringNoTag(value);
                }
            }
        }

        @Override
        public String[] readArray(CodedInputStream inputStream, int length) throws Exception {
            final String[] result = new String[length];
            for (int index = 0; index < length; index++) {
                if (BinaryProtocolCodec.readTag(inputStream) == Tag.NULL) {
                    result[index] = null;
                } else {
                    result[index] = inputStream.readString();
                }
            }
            return result;
        }
    }
}
