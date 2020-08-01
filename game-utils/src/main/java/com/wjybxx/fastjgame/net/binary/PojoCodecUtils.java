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

import com.wjybxx.fastjgame.net.type.TypeId;
import com.wjybxx.fastjgame.net.type.TypeModel;

import java.io.IOException;

/**
 * 简单对象编解码器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/25
 */
final class PojoCodecUtils {

    static <T> void writePojoImp(T value, Class<?> type, DataOutputStream outputStream,
                                 CodecRegistry codecRegistry, ObjectWriter writer) throws Exception {

        final TypeModel typeModel = codecRegistry.typeModelMapper().ofType(type);
        @SuppressWarnings("unchecked") final PojoCodec<T> pojoCodec = (PojoCodec<T>) codecRegistry.get(type);

        if (typeModel == null || null == pojoCodec) {
            throw new IOException("Unsupported type " + type.getName());
        }

        // 写入类型信息
        writeTypeId(outputStream, typeModel.typeId());
        // 交给实现类
        pojoCodec.writeObject(value, outputStream, codecRegistry, writer);
    }

    private static void writeTypeId(DataOutputStream outputStream, TypeId typeId) throws Exception {
        outputStream.writeByte(typeId.getNamespace());
        outputStream.writeFixedInt32(typeId.getClassId());
    }

    static Object readPojoImp(DataInputStream inputStream, CodecRegistry codecRegistry, ObjectReader objectReader) throws Exception {
        final TypeId typeId = readTypeId(inputStream);
        final PojoCodec pojoCodec = codecRegistry.get(typeId);
        if (null == pojoCodec) {
            throw new IOException("Unsupported type " + typeId);
        }
        return pojoCodec.readObject(inputStream, codecRegistry, objectReader);
    }

    private static TypeId readTypeId(DataInputStream inputStream) throws Exception {
        return new TypeId(inputStream.readByte(), inputStream.readFixedInt32());
    }

    /**
     * 读取多态实例
     * 要求对方也是按照{@code superClass}格式写入的
     */
    static <E> E readPolymorphicPojoImpl(DataInputStream inputStream, EntityFactory<E> factory, Class<? super E> superClass,
                                         CodecRegistry codecRegistry, ObjectReader reader) throws Exception {
        final TypeId typeId = readTypeId(inputStream);
        final PojoCodec<E> pojoCodec = codecRegistry.get(typeId);

        if (null == pojoCodec) {
            throw new IOException("Unsupported type " + typeId);
        }

        checkSuperClass(superClass, pojoCodec);

        checkPojoCodec(pojoCodec);

        final CustomPojoCodec<E> customPojoCodec = (CustomPojoCodec<E>) pojoCodec;
        return customPojoCodec.readPolymorphicPojoImpl(inputStream, factory, codecRegistry, reader);
    }

    private static void checkSuperClass(Class<?> entitySuperClass, PojoCodec<?> pojoCodec) throws IOException {
        if (entitySuperClass != pojoCodec.getEncoderClass()) {
            throw new IOException(String.format("Incompatible class, expected: %s, but read %s ",
                    entitySuperClass.getName(),
                    pojoCodec.getEncoderClass().getName()));
        }
    }

    private static void checkPojoCodec(PojoCodec<?> pojoCodec) throws IOException {
        if (!(pojoCodec instanceof CustomPojoCodec)) {
            throw new IOException("Unsupported codec, superClass codec must implements " + CustomPojoCodec.class.getName());
        }
    }
}
