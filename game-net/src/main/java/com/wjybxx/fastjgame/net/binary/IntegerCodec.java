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
public class IntegerCodec implements Codec<Integer> {

    IntegerCodec() {

    }

    @Override
    public void encode(@Nonnull DataOutputStream outputStream, @Nonnull Integer value, CodecRegistry codecRegistry) throws Exception {
        outputStream.writeTag(Tag.INT);
        outputStream.writeInt(value);
    }

    @Nonnull
    @Override
    public Integer decode(@Nonnull DataInputStream inputStream, CodecRegistry codecRegistry) throws Exception {
        return inputStream.readInt();
    }

    @Override
    public Class<?> getEncoderClass() {
        return Integer.class;
    }
}
