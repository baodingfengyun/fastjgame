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
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/13
 * github - https://github.com/hl845740757
 */
public abstract class CodedDataOutputStream implements DataOutputStream {

    CodedOutputStream codedOutputStream;

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
    public DataOutputStream slice() {
        return slice(writerIndex());
    }

    @Override
    public void flush() throws IOException {
        codedOutputStream.flush();
    }

    public static CodedDataOutputStream newInstance(byte[] buffer) {
        return new ArrayCodedDataOutputStream(buffer);
    }

    public static CodedDataOutputStream newInstance(byte[] buffer, int offset, int length) {
        return new ArrayCodedDataOutputStream(buffer, offset, length);
    }

    public static CodedDataOutputStream newInstance(ByteBuf byteBuf) {
        return new NioDataOutputStream(byteBuf);
    }

    public static CodedDataOutputStream newInstance(ByteBuf byteBuf, int index, int length) {
        return new NioDataOutputStream(byteBuf, index, length);
    }

    private static class ArrayCodedDataOutputStream extends CodedDataOutputStream {

        private final byte[] buffer;
        private final int offset;
        private final int limit;
        private int codedOutputStreamOffset;

        ArrayCodedDataOutputStream(byte[] buffer) {
            this(buffer, 0, buffer.length);
        }

        ArrayCodedDataOutputStream(byte[] buffer, int offset, int length) {
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
        public void setFixedInt32(final int index, int value) throws IOException {
            int position = offset + index;
            buffer[position++] = (byte) (value >>> 24);
            buffer[position++] = (byte) (value >>> 16);
            buffer[position++] = (byte) (value >>> 8);
            buffer[position] = (byte) value;
        }

        @Override
        public int writerIndex() {
            return codedOutputStreamOffset - offset + codedOutputStream.getTotalBytesWritten();
        }

        @Override
        public void writerIndex(int newWriteIndex) {
            if (newWriteIndex == writerIndex()) {
                return;
            }
            codedOutputStreamOffset = offset + newWriteIndex;
            codedOutputStream = CodedOutputStream.newInstance(buffer, codedOutputStreamOffset, limit - codedOutputStreamOffset);
        }

        @Override
        public DataOutputStream slice(int index) {
            final int newOffset = offset + index;
            final int newLength = limit - newOffset;
            return new ArrayCodedDataOutputStream(buffer, newOffset, newLength);
        }

    }

    public static class NioDataOutputStream extends CodedDataOutputStream {

        private final ByteBuf byteBuf;
        private final int startWriterIndex;
        private final int limit;
        private int codedOutputStreamOffset;

        public NioDataOutputStream(ByteBuf byteBuf) {
            this(byteBuf, byteBuf.writerIndex(), byteBuf.capacity() - byteBuf.writerIndex());
        }

        public NioDataOutputStream(ByteBuf byteBuf, int startWriterIndex, int length) {
            validateByteBuf(byteBuf, startWriterIndex, length);

            this.byteBuf = byteBuf;
            this.startWriterIndex = startWriterIndex;
            this.limit = startWriterIndex + length;

            this.codedOutputStream = CodedOutputStream.newInstance(byteBuf.internalNioBuffer(startWriterIndex, length));
            this.codedOutputStreamOffset = startWriterIndex;
        }

        @Override
        public void setFixedInt32(int index, int value) throws IOException {
            final int byteBufWriterIndex = byteBufWriterIndex(index);
            byteBuf.setInt(byteBufWriterIndex, value);
        }

        @Override
        public int writerIndex() {
            return codedOutputStreamOffset - startWriterIndex + codedOutputStream.getTotalBytesWritten();
        }

        @Override
        public void writerIndex(int newWriteIndex) {
            validateWriterIndex(newWriteIndex);

            if (newWriteIndex == writerIndex()) {
                return;
            }
            codedOutputStreamOffset = startWriterIndex + newWriteIndex;
            codedOutputStream = CodedOutputStream.newInstance(byteBuf.internalNioBuffer(codedOutputStreamOffset, limit - codedOutputStreamOffset));
        }

        void validateWriterIndex(int writeIndex) {
            if (byteBufWriterIndex(writeIndex) > limit) {
                throw new IllegalArgumentException("writeIndex: " + writeIndex + ", limit: " + limit);
            }
        }

        @Override
        public DataOutputStream slice(int index) {
            validateWriterIndex(index);

            final int byteBufWriterIndex = byteBufWriterIndex(index);
            final int length = limit - byteBufWriterIndex;
            final ByteBuf slice = byteBuf.slice(byteBufWriterIndex, length);
            // 默认slice是用于读的，所以其写索引默认是其capacity
            slice.writerIndex(0);
            return new NioDataOutputStream(slice);
        }

        @Override
        public void flush() throws IOException {
            super.flush();
            // 更新写索引
            byteBuf.writerIndex(byteBufWriterIndex(codedOutputStream.getTotalBytesWritten()));
        }

        protected int byteBufWriterIndex(int index) {
            return codedOutputStreamOffset + index;
        }
    }

    static void validateByteBuf(ByteBuf byteBuf, int index, int length) {
        // 未来可能优化不可扩容限制
        if (byteBuf.nioBufferCount() != 1 || byteBuf.capacity() != byteBuf.maxCapacity()) {
            throw new UnsupportedOperationException("Only ByteBuf without capacity expansion is supported");
        }

        if (index < 0 || length <= 0 || index + length > byteBuf.capacity()) {
            throw new IllegalArgumentException(String.format(
                    "Buffer range is invalid. Buffer.length=%d, offset=%d, length=%d",
                    byteBuf.capacity(), index, length));
        }
    }
}
