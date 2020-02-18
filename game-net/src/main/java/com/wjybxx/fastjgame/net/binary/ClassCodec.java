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

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/18
 * github - https://github.com/hl845740757
 */
public class ClassCodec implements BinaryCodec<Class> {

    @Override
    public boolean isSupport(Class<?> runtimeType) {
        return false;
    }

    @Override
    public void writeData(CodedOutputStream outputStream, @Nonnull Class instance) throws Exception {
        writeClass(outputStream, instance);
    }

    static void writeClass(CodedOutputStream outputStream, @Nonnull Class instance) throws IOException {
        outputStream.writeStringNoTag(instance.getCanonicalName());
    }

    @Nonnull
    @Override
    public Class readData(CodedInputStream inputStream) throws Exception {
        return readClass(inputStream);
    }

    static Class readClass(CodedInputStream inputStream) throws IOException, ClassNotFoundException {
        final String canonicalName = inputStream.readString();
        return BinaryCodec.class.getClassLoader().loadClass(canonicalName);
    }

    @Override
    public byte getWireType() {
        return WireType.CLASS;
    }
}
