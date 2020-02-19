/*
 * Copyright 2019 wjybxx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wjybxx.fastjgame.net.binary;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/18
 * github - https://github.com/hl845740757
 */
public class ClassArrayCodec implements BinaryCodec<Class[]> {

    @Override
    public boolean isSupport(Class<?> runtimeType) {
        return runtimeType == Class[].class;
    }

    @Override
    public void writeData(CodedOutputStream outputStream, @Nonnull Class[] instance) throws Exception {
        outputStream.writeUInt32NoTag(instance.length);
        for (Class clazz : instance) {
            ClassCodec.writeClass(outputStream, clazz);
        }
    }

    @Nonnull
    @Override
    public Class[] readData(CodedInputStream inputStream) throws Exception {
        final int length = inputStream.readUInt32();
        if (length == 0) {
            return ArrayUtils.EMPTY_CLASS_ARRAY;
        }
        final Class[] result = new Class[length];
        for (int index = 0; index < length; index++) {
            result[index] = ClassCodec.readClass(inputStream);
        }
        return result;
    }

    @Override
    public byte getWireType() {
        return WireType.CLASS_ARRAY;
    }
}
