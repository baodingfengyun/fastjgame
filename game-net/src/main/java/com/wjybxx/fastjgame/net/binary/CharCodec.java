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
class CharCodec implements PrimitiveCodec<Character, char[]> {

    @Override
    public boolean isSupport(Class<?> runtimeType) {
        return runtimeType == Character.class;
    }

    @Override
    public void writeData(CodedOutputStream outputStream, @Nonnull Character instance) throws Exception {
        outputStream.writeUInt32NoTag(instance);
    }

    @Nonnull
    @Override
    public Character readData(CodedInputStream inputStream) throws Exception {
        return (char) inputStream.readUInt32();
    }

    @Override
    public byte getWireType() {
        return WireType.CHAR;
    }

    @Override
    public void writeArray(CodedOutputStream outputStream, @Nonnull char[] array) throws Exception {
        for (char value : array) {
            outputStream.writeUInt32NoTag(value);
        }
    }

    @Override
    public char[] readArray(CodedInputStream inputStream, int length) throws Exception {
        char[] result = new char[length];
        for (int index = 0; index < length; index++) {
            result[index] = (char) inputStream.readUInt32();
        }
        return result;
    }

}
