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

import javax.annotation.Nonnull;

/**
 * 封装{@link PojoCodecImpl}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/24
 */
public class CustomPojoCodec<T> extends PojoCodec<T> {

    private final PojoCodecImpl<T> codec;

    public CustomPojoCodec(byte providerId, int classId, PojoCodecImpl<T> codec) {
        super(providerId, classId);
        this.codec = codec;
    }

    @Override
    protected void encodeBody(@Nonnull DataOutputStream outputStream, @Nonnull T value, CodecRegistry codecRegistry) throws Exception {
        final ObjectWriter objectWriter = new ObjectWriterImp(codecRegistry, outputStream);
        codec.writeObject(value, objectWriter);
    }

    @Nonnull
    @Override
    public T decode(@Nonnull DataInputStream inputStream, CodecRegistry codecRegistry) throws Exception {
        final ObjectReader objectReader = new ObjectReaderImp(codecRegistry, inputStream);
        return codec.readObject(objectReader);
    }

    @Override
    public Class<T> getEncoderClass() {
        return codec.getEncoderClass();
    }

    boolean isSupportReadFields() {
        return codec instanceof AbstractPojoCodecImpl;
    }

    void decodeBody(T instance, @Nonnull DataInputStream inputStream, CodecRegistry codecRegistry) throws Exception {
        final ObjectReader objectReader = new ObjectReaderImp(codecRegistry, inputStream);
        ((AbstractPojoCodecImpl<T>) codec).readFields(instance, objectReader);
    }
}
