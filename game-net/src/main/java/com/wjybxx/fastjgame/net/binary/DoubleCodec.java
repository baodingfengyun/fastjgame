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
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/17
 */
public class DoubleCodec implements Codec<Double> {

    DoubleCodec() {
    }

    @Override
    public void encode(@Nonnull DataOutputStream outputStream, @Nonnull Double value, CodecRegistry codecRegistry) throws Exception {
        outputStream.writeTag(Tag.DOUBLE);
        outputStream.writeDouble(value);
    }

    @Nonnull
    @Override
    public Double decode(@Nonnull DataInputStream inputStream, CodecRegistry codecRegistry) throws Exception {
        return inputStream.readDouble();
    }

    @Override
    public Class<?> getEncoderClass() {
        return Double.class;
    }
}
