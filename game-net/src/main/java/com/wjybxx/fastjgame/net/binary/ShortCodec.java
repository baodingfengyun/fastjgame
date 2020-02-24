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
class ShortCodec extends JDKObjectCodec<Short> {

    ShortCodec(int classId) {
        super(classId);
    }

    @Override
    public void encode(@Nonnull CodedOutputStream outputStream, @Nonnull Short value, CodecRegistry codecRegistry) throws Exception {
        outputStream.writeInt32NoTag(value);
    }

    @Nonnull
    @Override
    public Short decode(@Nonnull CodedInputStream inputStream, CodecRegistry codecRegistry) throws Exception {
        return (short) inputStream.readInt32();
    }

    @Override
    public Class<Short> getEncoderClass() {
        return Short.class;
    }
}
