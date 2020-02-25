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

/**
 * 封装{@link EntitySerializer}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/24
 */
public class SerializerBasedCodec<T> extends PojoCodec<T> {

    private final EntitySerializer<T> serializer;

    public SerializerBasedCodec(int providerId, int classId, EntitySerializer<T> serializer) {
        super(providerId, classId);
        this.serializer = serializer;
    }

    @Override
    public void encodeBody(@Nonnull CodedOutputStream outputStream, @Nonnull T value, CodecRegistry codecRegistry) throws Exception {
        final EntityOutputStream entityOutputStream = new EntityOutputStreamImp(codecRegistry, outputStream);
        serializer.writeObject(value, entityOutputStream);
    }

    @Nonnull
    @Override
    public T decode(@Nonnull CodedInputStream inputStream, CodecRegistry codecRegistry) throws Exception {
        final EntityInputStream entityInputStream = new EntityInputStreamImp(codecRegistry, inputStream);
        return serializer.readObject(entityInputStream);
    }

    @Override
    public Class<T> getEncoderClass() {
        return serializer.getEntityClass();
    }

    public void tyrDecodeBody(EntityFactory<? extends T> eEntityFactory, @Nonnull CodedInputStream inputStream, CodecRegistry codecRegistry) throws Exception {
        if (serializer instanceof AbstractEntitySerializer) {
            final EntityInputStream entityInputStream = new EntityInputStreamImp(codecRegistry, inputStream);
            ((AbstractEntitySerializer<T>) serializer).readFields(eEntityFactory.newInstance(), entityInputStream);
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
