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

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/5/11
 */
public class CustomPojoCodec<T> implements PojoCodecImpl<T> {

    private PojoCodecImpl<T> codec;

    public CustomPojoCodec(PojoCodecImpl<T> codec) {
        this.codec = codec;
    }

    public PojoCodecImpl<T> getDelegate() {
        return codec;
    }

    @Override
    public Class<T> getEncoderClass() {
        return codec.getEncoderClass();
    }

    @Override
    public T readObject(ObjectReader reader) throws Exception {
        return codec.readObject(reader);
    }

    @Override
    public void writeObject(T instance, ObjectWriter writer) throws Exception {
        codec.writeObject(instance, writer);
    }
}
