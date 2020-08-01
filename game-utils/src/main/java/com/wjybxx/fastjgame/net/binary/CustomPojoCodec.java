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

import java.io.IOException;

/**
 * 用户自定义Codec实现，通过桥连接
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/5/11
 */
public class CustomPojoCodec<T> implements PojoCodec<T> {

    private PojoCodecImpl<T> codec;

    CustomPojoCodec(PojoCodecImpl<T> codec) {
        this.codec = codec;
    }

    @Override
    public Class<T> getEncoderClass() {
        return codec.getEncoderClass();
    }

    @Override
    public T readObject(DataInputStream dataInputStream, CodecRegistry codecRegistry, ObjectReader reader) throws Exception {
        return codec.readObject(reader);
    }

    @Override
    public void writeObject(T instance, DataOutputStream dataOutputStream, CodecRegistry codecRegistry, ObjectWriter writer) throws Exception {
        codec.writeObject(instance, writer);
    }

    T readPolymorphicPojoImpl(DataInputStream inputStream, EntityFactory<? extends T> factory, CodecRegistry codecRegistry, ObjectReader reader) throws Exception {
        if (!(codec instanceof AbstractPojoCodecImpl)) {
            throw new IOException("Unsupported type " + getEncoderClass().getName());
        }
        final AbstractPojoCodecImpl<T> abstractPojoCodec = (AbstractPojoCodecImpl<T>) codec;
        // 这里使用给定工厂创建对象，而不是codec自己创建的对象，从而实现多态
        final T instance = factory.newInstance();
        abstractPojoCodec.readFields(instance, reader);
        return instance;
    }
}