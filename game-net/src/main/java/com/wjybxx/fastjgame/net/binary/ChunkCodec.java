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
import com.wjybxx.fastjgame.utils.misc.Chunk;

import javax.annotation.Nonnull;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/17
 */
class ChunkCodec implements BinaryCodec<Chunk> {

    @Override
    public boolean isSupport(Class<?> runtimeType) {
        return runtimeType == Chunk.class;
    }

    @Override
    public void writeData(CodedOutputStream outputStream, @Nonnull Chunk instance) throws Exception {
        outputStream.writeUInt32NoTag(instance.getLength());
        if (instance.getLength() > 0) {
            outputStream.writeRawBytes(instance.getBuffer(), instance.getOffset(), instance.getLength());
        }
    }

    @Nonnull
    @Override
    public Chunk readData(CodedInputStream inputStream) throws Exception {
        final int length = inputStream.readUInt32();
        if (length == 0) {
            return Chunk.EMPTY_CHUNK;
        }
        final byte[] buffer = inputStream.readRawBytes(length);
        return Chunk.newInstance(buffer);
    }

    @Override
    public byte getWireType() {
        return WireType.CHUNK;
    }

}
