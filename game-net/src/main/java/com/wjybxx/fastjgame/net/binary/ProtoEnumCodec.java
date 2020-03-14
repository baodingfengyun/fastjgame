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

import com.google.protobuf.Internal;
import com.google.protobuf.ProtocolMessageEnum;

import javax.annotation.Nonnull;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/17
 */
public class ProtoEnumCodec<T extends ProtocolMessageEnum> extends PojoCodec<T> {

    private final Class<T> enumClass;
    private final Internal.EnumLiteMap<T> mapper;

    public ProtoEnumCodec(byte providerId, int classId, Class<T> enumClass, Internal.EnumLiteMap<T> mapper) {
        super(providerId, classId);
        this.enumClass = enumClass;
        this.mapper = mapper;
    }

    @Override
    public void encodeBody(@Nonnull DataOutputStream outputStream, @Nonnull T value, CodecRegistry codecRegistry) throws Exception {
        outputStream.writeIndex(value.getNumber());
    }

    @Nonnull
    @Override
    public T decode(@Nonnull DataInputStream inputStream, CodecRegistry codecRegistry) throws Exception {
        return mapper.findValueByNumber(inputStream.readInt());
    }

    @Override
    public Class<T> getEncoderClass() {
        return enumClass;
    }
}
