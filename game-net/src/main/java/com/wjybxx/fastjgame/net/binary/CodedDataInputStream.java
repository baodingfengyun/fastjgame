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
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.Parser;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/14
 * github - https://github.com/hl845740757
 */
public class CodedDataInputStream implements DataInputStream {

    private final byte[] buffer;
    private final int offset;
    private final int limit;

    private CodedInputStream codedInputStream;
    private int codedInputStreamOffset;

    public CodedDataInputStream(byte[] buffer) {
        this(buffer, 0, buffer.length);
    }

    public CodedDataInputStream(byte[] buffer, int offset, int length) {
        this.buffer = buffer;
        this.offset = offset;
        this.limit = offset + length;

        codedInputStreamOffset = offset;
        codedInputStream = CodedInputStream.newInstance(buffer, offset, length);
    }

    @Override
    public byte readByte() throws IOException {
        return codedInputStream.readRawByte();
    }

    @Override
    public short readShort() throws IOException {
        return (short) codedInputStream.readInt32();
    }

    @Override
    public char readChar() throws IOException {
        return (char) codedInputStream.readInt32();
    }

    @Override
    public int readInt() throws IOException {
        return codedInputStream.readInt32();
    }

    @Override
    public long readLong() throws IOException {
        return codedInputStream.readInt64();
    }

    @Override
    public float readFloat() throws IOException {
        return codedInputStream.readFloat();
    }

    @Override
    public double readDouble() throws IOException {
        return codedInputStream.readDouble();
    }

    @Override
    public boolean readBoolean() throws IOException {
        return codedInputStream.readBool();
    }

    @Override
    public byte[] readBytes(int size) throws IOException {
        return codedInputStream.readRawBytes(size);
    }

    @Override
    public String readString() throws IOException {
        return codedInputStream.readString();
    }

    @Override
    public <T> T readMessage(@Nonnull Parser<T> parser) throws IOException {
        return parser.parseFrom(codedInputStream, ExtensionRegistryLite.getEmptyRegistry());
    }

    @Override
    public Tag readTag() throws IOException {
        return Tag.forNumber(codedInputStream.readRawByte());
    }

    @Override
    public int readFixedInt32() throws IOException {
        return (codedInputStream.readRawByte() & 0xff) << 24 |
                (codedInputStream.readRawByte() & 0xff) << 16 |
                (codedInputStream.readRawByte() & 0xff) << 8 |
                codedInputStream.readRawByte() & 0xff;
    }

    @Override
    public int getFixedInt32(final int index) throws IOException {
        int position = offset + index;
        return (buffer[position++] & 0xff) << 24 |
                (buffer[position++] & 0xff) << 16 |
                (buffer[position++] & 0xff) << 8 |
                buffer[position] & 0xff;
    }

    @Override
    public int readIndex() {
        return codedInputStreamOffset - offset + codedInputStream.getTotalBytesRead();
    }

    @Override
    public void readIndex(int newReadIndex) {
        if (newReadIndex == readIndex()) {
            return;
        }
        codedInputStreamOffset = offset + newReadIndex;
        codedInputStream = CodedInputStream.newInstance(buffer, codedInputStreamOffset, limit - codedInputStreamOffset);

    }

    @Override
    public DataInputStream slice(int length) {
        return slice(readIndex(), length);
    }

    @Override
    public DataInputStream slice(int index, int length) {
        final int newOffset = offset + index;
        return new CodedDataInputStream(buffer, newOffset, length);
    }
}
