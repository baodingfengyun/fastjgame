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
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/17
 */
public class FloatCodec implements Codec<Float> {

    FloatCodec() {

    }

    @Override
    public void encode(@Nonnull CodedOutputStream outputStream, @Nonnull Float value, CodecRegistry codecRegistry) throws Exception {
        BinarySerializer.writeTag(outputStream, Tag.FLOAT);
        outputStream.writeFloatNoTag(value);
    }

    @Nonnull
    @Override
    public Float decode(@Nonnull CodedInputStream inputStream, CodecRegistry codecRegistry) throws Exception {
        return inputStream.readFloat();
    }

    @Override
    public Class<Float> getEncoderClass() {
        return Float.class;
    }
}
