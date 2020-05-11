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

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Message;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/13
 * github - https://github.com/hl845740757
 */
public class CodedDataOutputStream implements DataOutputStream {

    private final byte[] buffer;
    private final int offset;
    private final int limit;

    private CodedOutputStream codedOutputStream;
    private int codedOutputStreamOffset;

    public CodedDataOutputStream(byte[] buffer) {
        this(buffer, 0, buffer.length);
    }

    public CodedDataOutputStream(byte[] buffer, int offset, int length) {
        if (offset >= buffer.length) {
            throw new IllegalArgumentException();
        }
        this.buffer = buffer;
        this.offset = offset;
        this.limit = offset + length;

        this.codedOutputStreamOffset = offset;
        this.codedOutputStream = CodedOutputStream.newInstance(buffer, offset, length);
    }

    @Override
    public void writeByte(byte value) throws IOException {
        codedOutputStream.writeRawByte(value);
    }

    @Override
    public void writeByte(int value) throws IOException {
        codedOutputStream.writeRawByte(value);
    }

    @Override
    public void writeShort(short value) throws IOException {
        codedOutputStream.writeInt32NoTag(value);
    }

    @Override
    public void writeChar(char value) throws IOException {
        codedOutputStream.writeInt32NoTag(value);
    }

    @Override
    public void writeInt(int value) throws IOException {
        codedOutputStream.writeInt32NoTag(value);
    }

    @Override
    public void writeLong(long value) throws IOException {
        codedOutputStream.writeInt64NoTag(value);
    }

    @Override
    public void writeFloat(float value) throws IOException {
        codedOutputStream.writeFloatNoTag(value);
    }

    @Override
    public void writeDouble(double value) throws IOException {
        codedOutputStream.writeDoubleNoTag(value);
    }

    @Override
    public void writeBoolean(boolean value) throws IOException {
        codedOutputStream.writeBoolNoTag(value);
    }

    @Override
    public void writeBytes(byte[] bytes) throws IOException {
        codedOutputStream.writeRawBytes(bytes);
    }

    @Override
    public void writeBytes(byte[] bytes, int off, int len) throws IOException {
        codedOutputStream.writeRawBytes(bytes, off, len);
    }

    @Override
    public void writeString(@Nonnull String value) throws IOException {
        codedOutputStream.writeStringNoTag(value);
    }

    @Override
    public void writeMessage(@Nonnull Message message) throws IOException {
        message.writeTo(codedOutputStream);
    }

    @Override
    public void writeTag(BinaryTag tag) throws IOException {
        codedOutputStream.writeRawByte(tag.getNumber());
    }

    @Override
    public void writeFixedInt32(int value) throws IOException {
        codedOutputStream.write((byte) (value >>> 24));
        codedOutputStream.write((byte) (value >>> 16));
        codedOutputStream.write((byte) (value >>> 8));
        codedOutputStream.write((byte) value);
    }

    @Override
    public void setFixedInt32(final int index, int value) throws IOException {
        int position = offset + index;
        buffer[position++] = (byte) (value >>> 24);
        buffer[position++] = (byte) (value >>> 16);
        buffer[position++] = (byte) (value >>> 8);
        buffer[position] = (byte) value;
    }

    @Override
    public int writeIndex() {
        return codedOutputStreamOffset - offset + codedOutputStream.getTotalBytesWritten();
    }

    @Override
    public void writeIndex(int newWriteIndex) {
        if (newWriteIndex == writeIndex()) {
            return;
        }
        codedOutputStreamOffset = offset + newWriteIndex;
        codedOutputStream = CodedOutputStream.newInstance(buffer, codedOutputStreamOffset, limit - codedOutputStreamOffset);
    }

    @Override
    public DataOutputStream slice() {
        return slice(writeIndex());
    }

    @Override
    public DataOutputStream slice(int index) {
        final int newOffset = offset + index;
        final int newLength = limit - newOffset;
        return new CodedDataOutputStream(buffer, newOffset, newLength);
    }

    @Override
    public void flush() throws IOException {
        codedOutputStream.flush();
    }
}
