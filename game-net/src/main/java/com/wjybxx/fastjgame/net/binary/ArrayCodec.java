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

    private static final int CHILD_SIZE = 8;
    private static final IdentityHashMap<Class<?>, Byte> primitiveType2TagMapping = new IdentityHashMap<>(CHILD_SIZE);

    static {
        register(byte.class, WireType.BYTE);
        register(char.class, WireType.CHAR);
        register(short.class, WireType.SHORT);
        register(int.class, WireType.INT);
        register(long.class, WireType.LONG);
        register(float.class, WireType.FLOAT);
        register(double.class, WireType.DOUBLE);
        register(boolean.class, WireType.BOOLEAN);
    }

    private static void register(Class<?> component, byte type) {
        primitiveType2TagMapping.put(component, type);
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
        final Byte childType = primitiveType2TagMapping.get(instance.getClass().getComponentType());
        final int length = Array.getLength(instance);

        if (null != childType) {
            writeChildTypeAndLength(outputStream, childType, length);
            final PrimitiveCodec<?, Object> primitiveCodec = (PrimitiveCodec<?, Object>) binaryProtocolCodec.getCodec(childType);
            primitiveCodec.writeArray(outputStream, instance);
        } else {
            writeChildTypeAndLength(outputStream, WireType.RUN_TIME, length);
            writeObjectArray(outputStream, instance, length);
        }
    }

    private static void writeChildTypeAndLength(CodedOutputStream outputStream, byte childType, int length) throws IOException {
        BinaryProtocolCodec.writeTag(outputStream, childType);
        outputStream.writeUInt32NoTag(length);
    }

    private void writeObjectArray(CodedOutputStream outputStream, @Nonnull Object instance, int length) throws Exception {
        for (int index = 0; index < length; index++) {
            Object value = Array.get(instance, index);
            binaryProtocolCodec.writeObject(outputStream, value);
        }
    }

    @Nonnull
    @Override
    public Object readData(CodedInputStream inputStream) throws Exception {
        return readArray(inputStream, Object.class);
    }

    Object readArray(CodedInputStream inputStream, Class<?> objectArrayComponentType) throws Exception {
        final byte childType = BinaryProtocolCodec.readTag(inputStream);
        final int length = inputStream.readUInt32();

        if (childType != WireType.RUN_TIME) {
            final PrimitiveCodec<?, ?> primitiveCodec = (PrimitiveCodec<?, ?>) binaryProtocolCodec.getCodec(childType);
            return primitiveCodec.readArray(inputStream, length);
        } else {
            // 默认使用Object数组
            return readObjectArray(inputStream, objectArrayComponentType, length);
        }
    }

    private Object readObjectArray(CodedInputStream inputStream, Class<?> componentType, int length) throws Exception {
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

    static void writeByteArray(CodedOutputStream outputStream, @Nonnull byte[] bytes, int offset, int length) throws Exception {
        writeChildTypeAndLength(outputStream, WireType.BYTE, length);
        ByteCodec.writeArray(outputStream, bytes, offset, length);
    }

}
