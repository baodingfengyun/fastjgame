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
class IntegerCodec implements PrimitiveCodec<Integer, int[]> {

    @Override
    public boolean isSupport(Class<?> runtimeType) {
        return runtimeType == Integer.class;
    }

    @Nonnull
    @Override
    public Integer readData(CodedInputStream inputStream) throws Exception {
        return inputStream.readInt32();
    }

    @Override
    public byte getWireType() {
        return WireType.INT;
    }

    @Override
    public final void writeData(CodedOutputStream outputStream, @Nonnull Integer instance) throws Exception {
        outputStream.writeInt32NoTag(instance);
    }

    @Override
    public void writeArray(CodedOutputStream outputStream, @Nonnull int[] array) throws Exception {
        for (int value : array) {
            outputStream.writeInt32NoTag(value);
        }
    }

    @Override
    public int[] readArray(CodedInputStream inputStream, int length) throws Exception {
        int[] result = new int[length];
        for (int index = 0; index < length; index++) {
            result[index] = inputStream.readInt32();
        }
        return result;
    }

}
