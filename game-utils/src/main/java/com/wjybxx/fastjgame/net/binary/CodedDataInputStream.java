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
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/14
 * github - https://github.com/hl845740757
 */
public abstract class CodedDataInputStream implements DataInputStream {

    CodedInputStream codedInputStream;

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
    public BinaryTag readTag() throws IOException {
        return BinaryTag.forNumber(codedInputStream.readRawByte());
    }

    @Override
    public DataInputStream slice(int length) {
        return slice(readerIndex(), length);
    }

    @Override
    public int readFixedInt32() throws IOException {
        return (codedInputStream.readRawByte() & 0xff) << 24 |
                (codedInputStream.readRawByte() & 0xff) << 16 |
                (codedInputStream.readRawByte() & 0xff) << 8 |
                codedInputStream.readRawByte() & 0xff;
    }

    public static CodedDataInputStream newInstance(byte[] buffer) {
        return new ArrayCodedDataInputStream(buffer);
    }

    public static CodedDataInputStream newInstance(byte[] buffer, int offset, int length) {
        return new ArrayCodedDataInputStream(buffer, offset, length);
    }

    public static CodedDataInputStream newInstance(ByteBuf byteBuf) {
        return new NioCodedDataInputStream(byteBuf);
    }

    public static CodedDataInputStream newInstance(ByteBuf byteBuf, int readerIndex, int length) {
        return new NioCodedDataInputStream(byteBuf, readerIndex, length);
    }

    private static class ArrayCodedDataInputStream extends CodedDataInputStream {

        private final byte[] buffer;
        private final int offset;
        private final int limit;
        private int codedInputStreamOffset;

        private ArrayCodedDataInputStream(byte[] buffer) {
            this(buffer, 0, buffer.length);
        }

        private ArrayCodedDataInputStream(byte[] buffer, int offset, int length) {
            this.buffer = buffer;
            this.offset = offset;
            this.limit = offset + length;

            codedInputStreamOffset = offset;
            codedInputStream = CodedInputStream.newInstance(buffer, offset, length);
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
        public int readerIndex() {
            return codedInputStreamOffset - offset + codedInputStream.getTotalBytesRead();
        }

        @Override
        public void readerIndex(int newReaderIndex) {
            if (newReaderIndex == readerIndex()) {
                return;
            }
            codedInputStreamOffset = offset + newReaderIndex;
            codedInputStream = CodedInputStream.newInstance(buffer, codedInputStreamOffset, limit - codedInputStreamOffset);
        }

        @Override
        public DataInputStream slice(int index, int length) {
            final int newOffset = offset + index;
            return newInstance(buffer, newOffset, length);
        }

        @Override
        public String toString() {
            return "ArrayCodedDataInputStream{" +
                    "arrayLength=" + buffer.length +
                    ", offset=" + offset +
                    ", limit=" + limit +
                    ", codedInputStreamOffset=" + codedInputStreamOffset +
                    ", codedInputStreamTotalBytesRead=" + codedInputStream.getTotalBytesRead() +
                    ", readerIndex=" + readerIndex() +
                    '}';
        }
    }

    private static class NioCodedDataInputStream extends CodedDataInputStream {

        private final ByteBuf byteBuf;
        private final int startReaderIndex;
        private final int limit;
        private int codedInputStreamOffset;

        NioCodedDataInputStream(ByteBuf byteBuf) {
            this(byteBuf, byteBuf.readerIndex(), byteBuf.readableBytes());
        }

        private NioCodedDataInputStream(ByteBuf byteBuf, int startReaderIndex, int length) {
            validateByteBuf(byteBuf, startReaderIndex, length);

            this.byteBuf = byteBuf;
            this.startReaderIndex = startReaderIndex;
            this.limit = startReaderIndex + length;

            this.codedInputStreamOffset = startReaderIndex;
            this.codedInputStream = CodedInputStream.newInstance(byteBuf.internalNioBuffer(startReaderIndex, length));
        }

        private static void validateByteBuf(ByteBuf byteBuf, int index, int length) {
            if (byteBuf.nioBufferCount() != 1) {
                throw new IllegalArgumentException("nioBufferCount: " + byteBuf.nioBufferCount() + " (expected: == 1)");
            }

            if (index < 0 || length <= 0 || index + length > byteBuf.capacity()) {
                throw new IllegalArgumentException(String.format(
                        "Buffer range is invalid. Buffer.length=%d, offset=%d, length=%d",
                        byteBuf.capacity(), index, length));
            }
        }

        @Override
        public int getFixedInt32(int index) throws IOException {
            final int byteBufReaderIndex = byteBufReaderIndex(index);
            return byteBuf.getInt(byteBufReaderIndex);
        }

        @Override
        public void readerIndex(int newReaderIndex) {
            validateReaderIndex(newReaderIndex);

            if (newReaderIndex == readerIndex()) {
                return;
            }

            codedInputStreamOffset = startReaderIndex + newReaderIndex;
            codedInputStream = CodedInputStream.newInstance(byteBuf.internalNioBuffer(codedInputStreamOffset, limit - codedInputStreamOffset));
        }

        private void validateReaderIndex(int readerIndex) {
            if (byteBufReaderIndex(readerIndex) > limit) {
                throw new IllegalArgumentException("readerIndex: " + readerIndex + ", limit: " + limit);
            }
        }

        @Override
        public DataInputStream slice(int index, int length) {
            validateReaderIndex(index);

            final int byteBufReaderIndex = byteBufReaderIndex(index);
            // slice返回一段数据的视图，默认用于读，不可以扩容
            final ByteBuf slice = byteBuf.slice(byteBufReaderIndex, length);
            return new NioCodedDataInputStream(slice);
        }

        @Override
        public int readerIndex() {
            return codedInputStreamOffset - startReaderIndex + codedInputStream.getTotalBytesRead();
        }

        private int byteBufReaderIndex(int readerIndex) {
            return startReaderIndex + readerIndex;
        }

        @Override
        public String toString() {
            return "NioCodedDataInputStream{" +
                    "byteBuf=" + byteBuf +
                    ", startReaderIndex=" + startReaderIndex +
                    ", limit=" + limit +
                    ", codedInputStreamOffset=" + codedInputStreamOffset +
                    ", codedInputStreamTotalBytesRead=" + codedInputStream.getTotalBytesRead() +
                    ", readerIndex=" + readerIndex() +
                    '}';
        }
    }
}
