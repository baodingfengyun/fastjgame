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

import com.wjybxx.fastjgame.db.core.TypeId;
import com.wjybxx.fastjgame.db.core.TypeModel;

import java.io.IOException;

/**
 * 简单对象编解码器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/25
 */
final class PojoCodec {

    static <T> void writePojoImp(DataOutputStream outputStream, T value, Class<?> type,
                                 ObjectWriter writer, CodecRegistry codecRegistry) throws Exception {
        final TypeModel typeModel = codecRegistry.typeModelMapper().ofType(type);

        if (typeModel == null) {
            throw new IOException("Unsupported type " + type.getName());
        }

        // 写入类型信息
        outputStream.writeByte(typeModel.typeId().getNamespace());
        outputStream.writeFixedInt32(typeModel.typeId().getClassId());

        @SuppressWarnings("unchecked") final PojoCodecImpl<T> pojoCodec = (PojoCodecImpl<T>) codecRegistry.get(type);
        if (null == pojoCodec) {
            throw new IOException("Unsupported type " + type.getName());
        }

        pojoCodec.writeObject(value, writer);
    }

    static Object readPojoImp(DataInputStream inputStream, CodecRegistry codecRegistry) throws Exception {
        final TypeId typeId = new TypeId(inputStream.readByte(), inputStream.readFixedInt32());
        final PojoCodecImpl pojoCodec = codecRegistry.get(typeId);
        if (null == pojoCodec) {
            throw new IOException("Unsupported type " + typeId);
        }
        final ObjectReader reader = new ObjectReaderImp(codecRegistry, inputStream);
        return pojoCodec.readObject(reader);
    }

    static <E> E readPolymorphicPojoImpl(DataInputStream inputStream, EntityFactory<E> factory, Class<? super E> superClass,
                                         ObjectReader reader, CodecRegistry codecRegistry) throws Exception {
        final TypeId typeId = new TypeId(inputStream.readByte(), inputStream.readFixedInt32());
        final PojoCodecImpl<E> pojoCodec = codecRegistry.get(typeId);

        if (null == pojoCodec) {
            throw new IOException("Unsupported type " + typeId);
        }

        checkSuperClass(superClass, pojoCodec);
        checkSupportReadFields(pojoCodec);

        final E instance = factory.newInstance();
        readFields(reader, pojoCodec, instance);
        return instance;
    }

    private static void checkSuperClass(Class<?> entitySuperClass, PojoCodecImpl<?> pojoCodec) throws IOException {
        if (entitySuperClass != pojoCodec.getEncoderClass()) {
            throw new IOException(String.format("Incompatible class, expected: %s, but read %s ",
                    entitySuperClass.getName(),
                    pojoCodec.getEncoderClass().getName()));
        }
    }

    private static void checkSupportReadFields(PojoCodecImpl<?> pojoCodec) throws IOException {
        if (pojoCodec instanceof CustomPojoCodec) {
            PojoCodecImpl<?> delegate = ((CustomPojoCodec<?>) pojoCodec).getDelegate();
            if (delegate instanceof AbstractPojoCodecImpl) {
                return;
            }
        }
        throw new IOException("Unsupported codec, superClass serializer must implements " +
                AbstractPojoCodecImpl.class.getName());
    }

    private static <E> void readFields(ObjectReader reader, PojoCodecImpl<E> pojoCodec, E instance) throws Exception {
        final PojoCodecImpl<E> delegate = ((CustomPojoCodec<E>) pojoCodec).getDelegate();
        final AbstractPojoCodecImpl<E> abstractPojoCodec = (AbstractPojoCodecImpl<E>) delegate;
        abstractPojoCodec.readFields(instance, reader);
    }
}
