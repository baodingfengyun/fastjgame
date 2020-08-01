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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.EnumMap;
import java.util.IdentityHashMap;

/**
 * 数组编解码器。
 * 多维数组和非值类型数组，性能不好，传输量也大 - 未来可能不支持任意的数组，只支持值类型和String类型，其它一律作为Object数组处理
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/17
 */
class ArrayCodec {

    private static final int CHILD_SIZE = 9;

    /**
     * 针对基本类型数组的优化 - 主要是拆装箱问题
     */
    private static final IdentityHashMap<Class<?>, ValueArrayCodec<?>> componentType2CodecMap = new IdentityHashMap<>(CHILD_SIZE);
    private static final EnumMap<BinaryTag, ValueArrayCodec<?>> tag2CodecMap = new EnumMap<>(BinaryTag.class);

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
        componentType2CodecMap.put(component, codec);
        tag2CodecMap.put(codec.getTag(), codec);
    }

    ArrayCodec() {

    }

    @SuppressWarnings("unchecked")
    static void writeArrayImpl(@Nonnull DataOutputStream outputStream, @Nonnull Object array, ObjectWriter writer) throws Exception {
        final int length = Array.getLength(array);

        final ValueArrayCodec codec = componentType2CodecMap.get(array.getClass().getComponentType());
        if (null != codec) {
            writeChildTypeAndLength(outputStream, codec.getTag(), length);
            codec.writeValueArray(outputStream, array);
        } else {
            writeChildTypeAndLength(outputStream, BinaryTag.UNKNOWN, length);
            writeObjectArray(outputStream, array, writer, length);
        }
    }

    static void writeChildTypeAndLength(@Nonnull DataOutputStream outputStream, BinaryTag childType, int length) throws Exception {
        outputStream.writeByte(childType.getNumber());
        // 固定长度写入主要为了兼容字节数组的扩展
        outputStream.writeFixedInt32(length);
    }

    private static void writeObjectArray(DataOutputStream outputStream, @Nonnull Object array, ObjectWriter writer, int length) throws Exception {
        // 写入类型信息
        outputStream.writeString(array.getClass().getComponentType().getName());

        for (int index = 0; index < length; index++) {
            Object value = Array.get(array, index);
            writer.writeObject(value);
        }
    }

    static Object readArrayImpl(DataInputStream inputStream, @Nullable Class<?> componentType, ObjectReader reader) throws Exception {
        final byte tagValue = inputStream.readByte();
        final BinaryTag childType = BinaryTag.forNumber(tagValue);

        if (childType == null) {
            throw new Exception("Unknown child type " + tagValue);
        }

        final int length = inputStream.readFixedInt32();

        if (childType != BinaryTag.UNKNOWN) {
            final ValueArrayCodec<?> codec = tag2CodecMap.get(childType);
            return codec.readValueArray(inputStream, length);
        } else {
            return readObjectArray(inputStream, reader, componentType, length);
        }
    }

    private static Object readObjectArray(DataInputStream inputStream, ObjectReader reader, @Nullable Class<?> componentType, int length) throws Exception {
        // 不管用不用，都需要先读取
        final String className = inputStream.readString();

        if (componentType == null) {
            // 如果外部没有传入，则需要加载类
            componentType = ClassCodec.findClass(className);
        }

        final Object array = Array.newInstance(componentType, length);
        for (int index = 0; index < length; index++) {
            Array.set(array, index, reader.readObject());
        }
        return array;
    }

    static void writeByteArray(DataOutputStream outputStream, @Nonnull byte[] bytes, int offset, int length) throws Exception {
        writeChildTypeAndLength(outputStream, BinaryTag.BYTE, bytes.length);
        outputStream.writeBytes(bytes, offset, length);
    }

    static byte[] readByteArray(DataInputStream inputStream) throws Exception {
        final BinaryTag childType = inputStream.readTag();
        if (childType != BinaryTag.BYTE) {
            throw new Exception("Expected byteArray, but read " + childType);
        }
        final int length = inputStream.readFixedInt32();
        return inputStream.readBytes(length);
    }

    private interface ValueArrayCodec<U> {

        BinaryTag getTag();

        /**
         * 写入数组的内容
         * 注意：数组的长度已经写入
         */
        void writeValueArray(DataOutputStream outputStream, @Nonnull U array) throws Exception;

        /**
         * 读取指定长度的数组
         */
        U readValueArray(DataInputStream inputStream, int length) throws Exception;

    }

    private static class ByteArrayCodec implements ValueArrayCodec<byte[]> {

        @Override
        public BinaryTag getTag() {
            return BinaryTag.BYTE;
        }

        @Override
        public void writeValueArray(DataOutputStream outputStream, @Nonnull byte[] array) throws Exception {
            outputStream.writeBytes(array);
        }

        @Override
        public byte[] readValueArray(DataInputStream inputStream, int length) throws Exception {
            return inputStream.readBytes(length);
        }
    }


    private static class IntArrayCodec implements ValueArrayCodec<int[]> {

        @Override
        public BinaryTag getTag() {
            return BinaryTag.INT;
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
        public BinaryTag getTag() {
            return BinaryTag.FLOAT;
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
        public BinaryTag getTag() {
            return BinaryTag.DOUBLE;
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
        public BinaryTag getTag() {
            return BinaryTag.LONG;
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
        public BinaryTag getTag() {
            return BinaryTag.SHORT;
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
        public BinaryTag getTag() {
            return BinaryTag.CHAR;
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
        public BinaryTag getTag() {
            return BinaryTag.BOOLEAN;
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
        public BinaryTag getTag() {
            return BinaryTag.STRING;
        }

        @Override
        public void writeValueArray(DataOutputStream outputStream, @Nonnull String[] array) throws Exception {
            for (String value : array) {
                if (value == null) {
                    outputStream.writeTag(BinaryTag.NULL);
                } else {
                    outputStream.writeTag(BinaryTag.STRING);
                    outputStream.writeString(value);
                }
            }
        }

        @Override
        public String[] readValueArray(DataInputStream inputStream, int length) throws Exception {
            final String[] result = new String[length];
            for (int index = 0; index < length; index++) {
                if (inputStream.readTag() == BinaryTag.NULL) {
                    result[index] = null;
                } else {
                    result[index] = inputStream.readString();
                }
            }
            return result;
        }
    }
}
