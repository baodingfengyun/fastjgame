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
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
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

    private static final int CHILD_SIZE = 9;
    private static final IdentityHashMap<Class<?>, Byte> component2TypeMapping = new IdentityHashMap<>(CHILD_SIZE);
    private static final Byte2ObjectMap<Class<?>> type2ComponentMapping = new Byte2ObjectOpenHashMap<>(CHILD_SIZE, Hash.FAST_LOAD_FACTOR);

    static {
        register(byte.class, WireType.BYTE);
        register(char.class, WireType.CHAR);
        register(short.class, WireType.SHORT);
        register(int.class, WireType.INT);
        register(long.class, WireType.LONG);
        register(float.class, WireType.FLOAT);
        register(double.class, WireType.DOUBLE);
        register(boolean.class, WireType.BOOLEAN);

        register(String.class, WireType.STRING);
    }

    private static void register(Class<?> component, byte type) {
        component2TypeMapping.put(component, type);
        type2ComponentMapping.put(type, component);
    }

    private final BinaryProtocolCodec binaryProtocolCodec;

    ArrayCodec(BinaryProtocolCodec binaryProtocolCodec) {
        this.binaryProtocolCodec = binaryProtocolCodec;
    }

    @Override
    public boolean isSupport(Class<?> runtimeType) {
        return runtimeType.isArray();
    }

    @Override
    public void writeData(CodedOutputStream outputStream, @Nonnull Object instance) throws Exception {
        if (instance instanceof byte[]) {
            final byte[] bytes = (byte[]) instance;
            writeBytesArray(outputStream, bytes, 0, bytes.length);
        } else {
            writeOtherArray(outputStream, instance);
        }
    }

    static void writeBytesArray(CodedOutputStream outputStream, @Nonnull byte[] bytes, int offset, int length) throws Exception {
        writeChildTypeAndLength(outputStream, WireType.BYTE, length);
        outputStream.writeRawBytes(bytes, offset, length);
    }

    private static void writeChildTypeAndLength(CodedOutputStream outputStream, byte childType, int length) throws IOException {
        BinaryProtocolCodec.writeTag(outputStream, childType);
        outputStream.writeUInt32NoTag(length);
    }

    private void writeOtherArray(CodedOutputStream outputStream, @Nonnull Object instance) throws Exception {
        final byte childType = component2TypeMapping.getOrDefault(instance.getClass().getComponentType(), WireType.RUN_TIME);
        final int length = Array.getLength(instance);
        writeChildTypeAndLength(outputStream, childType, length);

        for (int index = 0; index < length; index++) {
            Object value = Array.get(instance, index);
            binaryProtocolCodec.writeRuntimeType(outputStream, value);
        }
    }

    @Nonnull
    @Override
    public Object readData(CodedInputStream inputStream) throws Exception {
        final byte childType = BinaryProtocolCodec.readTag(inputStream);
        // 默认使用Object数组
        final Class<?> componentType = type2ComponentMapping.getOrDefault(childType, Object.class);
        return readArray(inputStream, componentType);
    }

    Object readArray(CodedInputStream inputStream, Class<?> componentType) throws Exception {
        if (componentType == byte.class) {
            return readByteArray(inputStream);
        } else {
            return readOtherArray(inputStream, componentType);
        }
    }

    private static byte[] readByteArray(CodedInputStream inputStream) throws IOException {
        final int length = inputStream.readUInt32();
        if (length == 0) {
            return ArrayUtils.EMPTY_BYTE_ARRAY;
        }
        return inputStream.readRawBytes(length);
    }

    private Object readOtherArray(CodedInputStream inputStream, Class<?> componentType) throws Exception {
        final int length = inputStream.readUInt32();
        final Object array = Array.newInstance(componentType, length);
        for (int index = 0; index < length; index++) {
            Array.set(array, index, binaryProtocolCodec.readObject(inputStream));
        }
        return array;
    }

    @Override
    public byte getWireType() {
        return WireType.ARRAY;
    }

}
