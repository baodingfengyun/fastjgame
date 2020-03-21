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

import com.wjybxx.fastjgame.net.binaryextend.ClassCodec;
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
public class ArrayCodec implements Codec<Object> {

    private static final Class<?> ARRAY_ENCODER_CLASS = Object[].class;

    private static final int CHILD_SIZE = 9;

    /**
     * 针对基本类型数组的优化
     */
    private static final IdentityHashMap<Class<?>, ValueArrayCodec<?>> componentType2CodecMapping = new IdentityHashMap<>(CHILD_SIZE);
    private static final EnumMap<Tag, ValueArrayCodec<?>> tag2CodecMapping = new EnumMap<>(Tag.class);

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

    private static void register(Class<?> component, ValueArrayCodec<?> codec) {
        componentType2CodecMapping.put(component, codec);
        tag2CodecMapping.put(codec.childType(), codec);
    }

    ArrayCodec() {

    }

    @Override
    public void encode(@Nonnull DataOutputStream outputStream, @Nonnull Object value, CodecRegistry codecRegistry) throws Exception {
        encodeArray(outputStream, value, codecRegistry);
    }

    @SuppressWarnings("unchecked")
    static void encodeArray(@Nonnull DataOutputStream outputStream, @Nonnull Object value, CodecRegistry codecRegistry) throws Exception {
        final ValueArrayCodec codec = componentType2CodecMapping.get(value.getClass().getComponentType());
        final int length = Array.getLength(value);

        if (null != codec) {
            writeTagAndChildTypeAndLength(outputStream, codec.childType(), length);
            codec.writeValueArray(outputStream, value);
        } else {
            writeTagAndChildTypeAndLength(outputStream, Tag.UNKNOWN, length);
            writeObjectArray(outputStream, value, length, codecRegistry);
        }
    }

    private static void writeTagAndChildTypeAndLength(DataOutputStream outputStream, Tag childType, int length) throws IOException {
        outputStream.writeTag(Tag.ARRAY);
        outputStream.writeTag(childType);
        // 大端模式固定长度写入主要为了兼容字节数组的扩展
        outputStream.writeFixedInt32(length);
    }

    /**
     * 写对象数组，对象数组主要存在Null等处理
     */
    private static void writeObjectArray(DataOutputStream outputStream, @Nonnull Object instance, int length,
                                         CodecRegistry codecRegistry) throws Exception {
        // Q: 为什么要将component的类型信息编码？
        // A: 因为component类型可能不在编解码范围内，比如接口。
        // 因此编码类信息才能完整解码，它会导致传输量增加，性能降低
        ClassCodec.encodeClass(outputStream, instance.getClass().getComponentType());

        for (int index = 0; index < length; index++) {
            Object value = Array.get(instance, index);
            BinarySerializer.encodeObject(outputStream, value, codecRegistry);
        }
    }

    @Nonnull
    @Override
    public Object decode(@Nonnull DataInputStream inputStream, CodecRegistry codecRegistry) throws Exception {
        return readArray(inputStream, null, codecRegistry);
    }

    static Object readArray(DataInputStream inputStream, @Nullable Class<?> objectArrayComponentType, CodecRegistry codecRegistry) throws Exception {
        final Tag childType = inputStream.readTag();
        final int length = inputStream.readFixedInt32();

        return readArrayImp(inputStream, objectArrayComponentType, codecRegistry, childType, length);
    }

    static Object readArrayImp(DataInputStream inputStream, @Nullable Class<?> objectArrayComponentType, CodecRegistry codecRegistry, Tag childType, int length) throws Exception {
        if (childType != Tag.UNKNOWN) {
            final ValueArrayCodec<?> codec = tag2CodecMapping.get(childType);
            assert null != codec;
            return codec.readValueArray(inputStream, length);
        } else {
            return readObjectArray(inputStream, objectArrayComponentType, length, codecRegistry);
        }
    }

    private static Object readObjectArray(DataInputStream inputStream,
                                          @Nullable Class<?> objectArrayComponentType, int length,
                                          CodecRegistry codecRegistry) throws Exception {
        final String componentClassName = inputStream.readString();
        if (null == objectArrayComponentType) {
            // 查找类性能不好
            objectArrayComponentType = ClassCodec.decodeClass(componentClassName);
        }

        final Object array = Array.newInstance(objectArrayComponentType, length);
        for (int index = 0; index < length; index++) {
            Array.set(array, index, BinarySerializer.decodeObject(inputStream, codecRegistry));
        }

        return array;
    }

    static void writeByteArray(DataOutputStream outputStream, @Nonnull byte[] bytes, int offset, int length) throws Exception {
        writeTagAndChildTypeAndLength(outputStream, Tag.BYTE, length);
        outputStream.writeBytes(bytes, offset, length);
    }

    @Override
    public Class<?> getEncoderClass() {
        return ARRAY_ENCODER_CLASS;
    }

    private interface ValueArrayCodec<U> extends NumericalEntity {

        /**
         * 子类标识
         */
        Tag childType();

        /**
         * 写入数组的内容
         * 注意：数组的长度已经写入
         */
        void writeValueArray(DataOutputStream outputStream, @Nonnull U array) throws Exception;

        /**
         * 读取指定长度的数组
         */
        U readValueArray(DataInputStream inputStream, int length) throws Exception;

        @Override
        default int getNumber() {
            return childType().getNumber();
        }
    }

    private static class ByteArrayCodec implements ValueArrayCodec<byte[]> {

        @Override
        public Tag childType() {
            return Tag.BYTE;
        }

        @Override
        public void writeValueArray(DataOutputStream outputStream, @Nonnull byte[] array) throws Exception {
            outputStream.writeBytes(array, 0, array.length);
        }

        @Override
        public byte[] readValueArray(DataInputStream inputStream, int length) throws Exception {
            return inputStream.readBytes(length);
        }

    }

    private static class IntArrayCodec implements ValueArrayCodec<int[]> {

        @Override
        public Tag childType() {
            return Tag.INT;
        }

        @Override
        public void writeValueArray(DataOutputStream outputStream, @Nonnull int[] array) throws Exception {
            for (int value : array) {
                outputStream.writeInt(value);
            }
        }

        @Override
        public int[] readValueArray(DataInputStream inputStream, int length) throws Exception {
            int[] result = new int[length];
            for (int index = 0; index < length; index++) {
                result[index] = inputStream.readInt();
            }
            return result;
        }

    }

    private static class FloatArrayCodec implements ValueArrayCodec<float[]> {

        @Override
        public Tag childType() {
            return Tag.FLOAT;
        }

        @Override
        public void writeValueArray(DataOutputStream outputStream, @Nonnull float[] array) throws Exception {
            for (float value : array) {
                outputStream.writeFloat(value);
            }
        }

        @Override
        public float[] readValueArray(DataInputStream inputStream, int length) throws Exception {
            float[] result = new float[length];
            for (int index = 0; index < length; index++) {
                result[index] = inputStream.readFloat();
            }
            return result;
        }
    }

    private static class DoubleArrayCodec implements ValueArrayCodec<double[]> {

        @Override
        public Tag childType() {
            return Tag.DOUBLE;
        }

        @Override
        public void writeValueArray(DataOutputStream outputStream, @Nonnull double[] array) throws Exception {
            for (double value : array) {
                outputStream.writeDouble(value);
            }
        }

        @Override
        public double[] readValueArray(DataInputStream inputStream, int length) throws Exception {
            double[] result = new double[length];
            for (int index = 0; index < length; index++) {
                result[index] = inputStream.readDouble();
            }
            return result;
        }
    }

    private static class LongArrayCodec implements ValueArrayCodec<long[]> {

        @Override
        public Tag childType() {
            return Tag.LONG;
        }

        @Override
        public void writeValueArray(DataOutputStream outputStream, @Nonnull long[] array) throws Exception {
            for (long value : array) {
                outputStream.writeLong(value);
            }
        }

        @Override
        public long[] readValueArray(DataInputStream inputStream, int length) throws Exception {
            long[] result = new long[length];
            for (int index = 0; index < length; index++) {
                result[index] = inputStream.readLong();
            }
            return result;
        }
    }

    private static class ShortArrayCodec implements ValueArrayCodec<short[]> {

        @Override
        public Tag childType() {
            return Tag.SHORT;
        }

        @Override
        public void writeValueArray(DataOutputStream outputStream, @Nonnull short[] array) throws Exception {
            for (short value : array) {
                outputStream.writeShort(value);
            }
        }

        @Override
        public short[] readValueArray(DataInputStream inputStream, int length) throws Exception {
            short[] result = new short[length];
            for (int index = 0; index < length; index++) {
                result[index] = inputStream.readShort();
            }
            return result;
        }
    }

    private static class CharArrayCodec implements ValueArrayCodec<char[]> {

        @Override
        public Tag childType() {
            return Tag.CHAR;
        }

        @Override
        public void writeValueArray(DataOutputStream outputStream, @Nonnull char[] array) throws Exception {
            for (char value : array) {
                outputStream.writeChar(value);
            }
        }

        @Override
        public char[] readValueArray(DataInputStream inputStream, int length) throws Exception {
            char[] result = new char[length];
            for (int index = 0; index < length; index++) {
                result[index] = inputStream.readChar();
            }
            return result;
        }
    }

    private static class BooleanArrayCodec implements ValueArrayCodec<boolean[]> {

        @Override
        public Tag childType() {
            return Tag.BOOLEAN;
        }

        @Override
        public void writeValueArray(DataOutputStream outputStream, @Nonnull boolean[] array) throws Exception {
            for (boolean value : array) {
                outputStream.writeBoolean(value);
            }
        }

        @Override
        public boolean[] readValueArray(DataInputStream inputStream, int length) throws Exception {
            boolean[] result = new boolean[length];
            for (int index = 0; index < length; index++) {
                result[index] = inputStream.readBoolean();
            }
            return result;
        }
    }

    private static class StringArrayCodec implements ValueArrayCodec<String[]> {

        @Override
        public Tag childType() {
            return Tag.STRING;
        }

        @Override
        public void writeValueArray(DataOutputStream outputStream, @Nonnull String[] array) throws Exception {
            for (String value : array) {
                if (value == null) {
                    outputStream.writeTag(Tag.NULL);
                } else {
                    outputStream.writeTag(Tag.STRING);
                    outputStream.writeString(value);
                }
            }
        }

        @Override
        public String[] readValueArray(DataInputStream inputStream, int length) throws Exception {
            final String[] result = new String[length];
            for (int index = 0; index < length; index++) {
                if (inputStream.readTag() == Tag.NULL) {
                    result[index] = null;
                } else {
                    result[index] = inputStream.readString();
                }
            }
            return result;
        }
    }
}
