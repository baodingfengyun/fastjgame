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
import com.wjybxx.fastjgame.util.annotation.UnstableApi;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/13
 * github - https://github.com/hl845740757
 */
public abstract class CodedDataOutputStream implements DataOutputStream {

    @Override
    public DataOutputStream duplicate() {
        return duplicate(writerIndex());
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

    public static CodedDataOutputStream newInstance(ByteBuf byteBuf, int index) {
        return new NioDataOutputStream(byteBuf, index);
    }

    private static class ArrayCodedDataOutputStream extends CodedDataOutputStream {

        private final byte[] buffer;
        private final int offset;
        private final int limit;

        private int codedOutputStreamOffset;
        private CodedOutputStream codedOutputStream;

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
        public void writeByte(byte value) throws Exception {
            codedOutputStream.writeRawByte(value);
        }

        @Override
        public void writeByte(int value) throws Exception {
            codedOutputStream.writeRawByte(value);
        }

        @Override
        public void writeShort(short value) throws Exception {
            codedOutputStream.writeInt32NoTag(value);
        }

        @Override
        public void writeChar(char value) throws Exception {
            codedOutputStream.writeInt32NoTag(value);
        }

        @Override
        public void writeInt(int value) throws Exception {
            codedOutputStream.writeInt32NoTag(value);
        }

        @Override
        public void writeLong(long value) throws Exception {
            codedOutputStream.writeInt64NoTag(value);
        }

        @Override
        public void writeFloat(float value) throws Exception {
            codedOutputStream.writeFloatNoTag(value);
        }

        @Override
        public void writeDouble(double value) throws Exception {
            codedOutputStream.writeDoubleNoTag(value);
        }

        @Override
        public void writeBoolean(boolean value) throws Exception {
            codedOutputStream.writeBoolNoTag(value);
        }

        @Override
        public void writeBytes(byte[] bytes) throws Exception {
            codedOutputStream.writeRawBytes(bytes);
        }

        @Override
        public void writeBytes(byte[] bytes, int off, int len) throws Exception {
            codedOutputStream.writeRawBytes(bytes, off, len);
        }

        @Override
        public void writeString(@Nonnull String value) throws Exception {
            codedOutputStream.writeStringNoTag(value);
        }

        @Override
        public void writeMessage(@Nonnull Message message) throws Exception {
            message.writeTo(codedOutputStream);
        }

        @Override
        public void writeTag(BinaryTag tag) throws Exception {
            codedOutputStream.writeRawByte(tag.getNumber());
        }

        @Override
        public void writeFixedInt32(int value) throws Exception {
            codedOutputStream.write((byte) (value >>> 24));
            codedOutputStream.write((byte) (value >>> 16));
            codedOutputStream.write((byte) (value >>> 8));
            codedOutputStream.write((byte) value);
        }

        @Override
        public void setFixedInt32(final int index, int value) throws Exception {
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
        public void writerIndex(int newWriterIndex) {
            if (newWriterIndex == writerIndex()) {
                return;
            }
            codedOutputStreamOffset = offset + newWriterIndex;
            codedOutputStream = CodedOutputStream.newInstance(buffer, codedOutputStreamOffset, limit - codedOutputStreamOffset);
        }

        @Override
        public DataOutputStream duplicate(int index) {
            final int newOffset = offset + index;
            final int newLength = limit - newOffset;
            return new ArrayCodedDataOutputStream(buffer, newOffset, newLength);
        }

        @Override
        public void flush() throws Exception {
            codedOutputStream.flush();
        }

        @Override
        public String toString() {
            return "ArrayCodedDataOutputStream{" +
                    "arrayLength=" + buffer.length +
                    ", offset=" + offset +
                    ", limit=" + limit +
                    ", codedOutputStreamOffset=" + codedOutputStreamOffset +
                    ", codedOutputStreamTotalBytesWritten=" + codedOutputStream.getTotalBytesWritten() +
                    ", writerIndex=" + writerIndex() +
                    '}';
        }
    }

    /**
     * 扩容有两种方式：
     * 1. 外部写失败后，扩容，然后重写。
     * 2. 内部写失败后，扩容，继续写。
     * 这个选择还是不容易，继续写的话开销可能小一点，但未测试。
     */
    @UnstableApi
    private static class NioDataOutputStream extends CodedDataOutputStream {

        private static final Logger logger = LoggerFactory.getLogger(NioDataOutputStream.class);

        private final ByteBuf byteBuf;
        private final int startWriterIndex;

        private int codedOutputStreamOffset;
        private CodedOutputStream codedOutputStream;

        NioDataOutputStream(ByteBuf byteBuf) {
            this(byteBuf, byteBuf.writerIndex());
        }

        NioDataOutputStream(ByteBuf byteBuf, int startWriterIndex) {
            validateByteBuf(byteBuf, startWriterIndex);

            this.byteBuf = byteBuf;
            this.startWriterIndex = startWriterIndex;

            this.codedOutputStream = CodedOutputStream.newInstance(byteBuf.internalNioBuffer(startWriterIndex, byteBuf.capacity() - startWriterIndex));
            this.codedOutputStreamOffset = startWriterIndex;
        }

        private static void validateByteBuf(ByteBuf byteBuf, int index) {
            if (byteBuf.nioBufferCount() != 1) {
                throw new IllegalArgumentException("nioBufferCount: " + byteBuf.nioBufferCount() + " (expected: == 1)");
            }

            // 必须显示指定最大容量
            if (byteBuf.maxCapacity() < 512) {
                throw new IllegalArgumentException("maxCapacity: " + byteBuf.maxCapacity() + " (expected: >= 512 )");
            }

            if (index < 0 || index > byteBuf.capacity()) {
                throw new IllegalArgumentException(String.format(
                        "Buffer range is invalid. Buffer.length=%d, offset=%d",
                        byteBuf.capacity(), index));
            }
        }

        @Override
        public void writeByte(byte value) throws Exception {
            int preWriterIndex = writerIndex();
            while (true) {
                try {
                    codedOutputStream.writeRawByte(value);
                    return;
                } catch (CodedOutputStream.OutOfSpaceException | IndexOutOfBoundsException exception) {
                    ensureCapacity(preWriterIndex, exception);
                }
                preWriterIndex = writerIndex();
            }
        }

        @Override
        public void writeByte(int value) throws Exception {
            int preWriterIndex = writerIndex();
            while (true) {
                try {
                    codedOutputStream.writeRawByte(value);
                    return;
                } catch (CodedOutputStream.OutOfSpaceException | IndexOutOfBoundsException exception) {
                    ensureCapacity(preWriterIndex, exception);
                }
                preWriterIndex = writerIndex();
            }
        }

        @Override
        public void writeShort(short value) throws Exception {
            int preWriterIndex = writerIndex();
            while (true) {
                try {
                    codedOutputStream.writeInt32NoTag(value);
                    return;
                } catch (CodedOutputStream.OutOfSpaceException | IndexOutOfBoundsException exception) {
                    ensureCapacity(preWriterIndex, exception);
                }
                preWriterIndex = writerIndex();
            }
        }

        @Override
        public void writeChar(char value) throws Exception {
            int preWriterIndex = writerIndex();
            while (true) {
                try {
                    codedOutputStream.writeInt32NoTag(value);
                    return;
                } catch (CodedOutputStream.OutOfSpaceException | IndexOutOfBoundsException exception) {
                    ensureCapacity(preWriterIndex, exception);
                }
                preWriterIndex = writerIndex();
            }
        }

        @Override
        public void writeInt(int value) throws Exception {
            int preWriterIndex = writerIndex();
            while (true) {
                try {
                    codedOutputStream.writeInt32NoTag(value);
                    return;
                } catch (CodedOutputStream.OutOfSpaceException | IndexOutOfBoundsException exception) {
                    ensureCapacity(preWriterIndex, exception);
                }
                preWriterIndex = writerIndex();
            }
        }

        @Override
        public void writeLong(long value) throws Exception {
            int preWriterIndex = writerIndex();
            while (true) {
                try {
                    codedOutputStream.writeInt64NoTag(value);
                    return;
                } catch (CodedOutputStream.OutOfSpaceException | IndexOutOfBoundsException exception) {
                    ensureCapacity(preWriterIndex, exception);
                }
                preWriterIndex = writerIndex();
            }
        }

        @Override
        public void writeFloat(float value) throws Exception {
            int preWriterIndex = writerIndex();
            while (true) {
                try {
                    codedOutputStream.writeFloatNoTag(value);
                    return;
                } catch (CodedOutputStream.OutOfSpaceException | IndexOutOfBoundsException exception) {
                    ensureCapacity(preWriterIndex, exception);
                }
                preWriterIndex = writerIndex();
            }
        }

        @Override
        public void writeDouble(double value) throws Exception {
            int preWriterIndex = writerIndex();
            while (true) {
                try {
                    codedOutputStream.writeDoubleNoTag(value);
                    return;
                } catch (CodedOutputStream.OutOfSpaceException | IndexOutOfBoundsException exception) {
                    ensureCapacity(preWriterIndex, exception);
                }
                preWriterIndex = writerIndex();
            }
        }

        @Override
        public void writeBoolean(boolean value) throws Exception {
            int preWriterIndex = writerIndex();
            while (true) {
                try {
                    codedOutputStream.writeBoolNoTag(value);
                    return;
                } catch (CodedOutputStream.OutOfSpaceException | IndexOutOfBoundsException exception) {
                    ensureCapacity(preWriterIndex, exception);
                }
                preWriterIndex = writerIndex();
            }
        }

        @Override
        public void writeBytes(byte[] bytes) throws Exception {
            int preWriterIndex = writerIndex();
            while (true) {
                try {
                    codedOutputStream.writeRawBytes(bytes);
                    return;
                } catch (CodedOutputStream.OutOfSpaceException | IndexOutOfBoundsException exception) {
                    ensureCapacity(preWriterIndex, exception);
                }
                preWriterIndex = writerIndex();
            }
        }

        @Override
        public void writeBytes(byte[] bytes, int off, int len) throws Exception {
            int preWriterIndex = writerIndex();
            while (true) {
                try {
                    codedOutputStream.writeRawBytes(bytes, off, len);
                    return;
                } catch (CodedOutputStream.OutOfSpaceException | IndexOutOfBoundsException exception) {
                    ensureCapacity(preWriterIndex, exception);
                }
                preWriterIndex = writerIndex();
            }
        }

        @Override
        public void writeString(@Nonnull String value) throws Exception {
            int preWriterIndex = writerIndex();
            while (true) {
                try {
                    codedOutputStream.writeStringNoTag(value);
                    return;
                } catch (CodedOutputStream.OutOfSpaceException | IndexOutOfBoundsException exception) {
                    ensureCapacity(preWriterIndex, exception);
                }
                preWriterIndex = writerIndex();
            }
        }

        @Override
        public void writeMessage(@Nonnull Message message) throws Exception {
            int preWriterIndex = writerIndex();
            while (true) {
                try {
                    message.writeTo(codedOutputStream);
                    return;
                } catch (CodedOutputStream.OutOfSpaceException | IndexOutOfBoundsException exception) {
                    ensureCapacity(preWriterIndex, exception);
                }
                preWriterIndex = writerIndex();
            }
        }

        @Override
        public void writeTag(BinaryTag tag) throws Exception {
            int preWriterIndex = writerIndex();
            while (true) {
                try {
                    codedOutputStream.writeRawByte(tag.getNumber());
                    return;
                } catch (CodedOutputStream.OutOfSpaceException | IndexOutOfBoundsException exception) {
                    ensureCapacity(preWriterIndex, exception);
                }
                preWriterIndex = writerIndex();
            }
        }

        @Override
        public void writeFixedInt32(int value) throws Exception {
            int preWriterIndex = writerIndex();
            while (true) {
                try {
                    codedOutputStream.write((byte) (value >>> 24));
                    codedOutputStream.write((byte) (value >>> 16));
                    codedOutputStream.write((byte) (value >>> 8));
                    codedOutputStream.write((byte) value);
                    return;
                } catch (CodedOutputStream.OutOfSpaceException | IndexOutOfBoundsException exception) {
                    ensureCapacity(preWriterIndex, exception);
                }
                preWriterIndex = writerIndex();
            }
        }

        @Override
        public void setFixedInt32(int index, int value) throws Exception {
            final int byteBufWriterIndex = byteBufWriterIndex(index);
            byteBuf.setInt(byteBufWriterIndex, value);
        }

        private void ensureCapacity(int preWriterIndex, Exception exception) {
            if (byteBuf.capacity() >= byteBuf.maxCapacity()) {
                // 无法继续扩容
                throw new RuntimeException("reach maxCapacity " + byteBuf.maxCapacity(), exception);
            }

            // 2倍扩容
            final int lastCapacity = byteBuf.capacity();
            final int targetCapacity = Math.min(lastCapacity * 2, byteBuf.maxCapacity());

            if (targetCapacity <= lastCapacity) {
                // 溢出
                throw new RuntimeException(
                        String.format("targetCapacity %d <= lastCapacity %d ", targetCapacity, lastCapacity),
                        exception);
            }

            byteBuf.capacity(targetCapacity);

            final int newCapacity = byteBuf.capacity();

            if (newCapacity <= lastCapacity) {
                // 扩容失败
                throw new RuntimeException(
                        String.format("newCapacity %d <= lastCapacity %d ", newCapacity, lastCapacity),
                        exception);
            }

            this.codedOutputStreamOffset = startWriterIndex + preWriterIndex;
            this.codedOutputStream = CodedOutputStream.newInstance(byteBuf.internalNioBuffer(codedOutputStreamOffset, newCapacity - codedOutputStreamOffset));

            if (logger.isDebugEnabled()) {
                logger.debug("adjust capacity, lastCapacity {}, newCapacity {}", lastCapacity, newCapacity);
            }
        }

        @Override
        public void writerIndex(int newWriterIndex) {
            validateWriterIndex(newWriterIndex);

            if (newWriterIndex == writerIndex()) {
                return;
            }
            codedOutputStreamOffset = startWriterIndex + newWriterIndex;
            codedOutputStream = CodedOutputStream.newInstance(byteBuf.internalNioBuffer(codedOutputStreamOffset, byteBuf.capacity() - codedOutputStreamOffset));
        }

        void validateWriterIndex(int writeIndex) {
            if (byteBufWriterIndex(writeIndex) > byteBuf.capacity()) {
                throw new IllegalArgumentException("writeIndex: " + writeIndex + ", capacity: " + byteBuf.capacity());
            }
        }

        @Override
        public DataOutputStream duplicate(int index) {
            validateWriterIndex(index);

            // 这里需要使用duplicate，需要共享底层所有数据
            final int byteBufWriterIndex = byteBufWriterIndex(index);
            final ByteBuf duplicateByteBuf = byteBuf.duplicate().writerIndex(byteBufWriterIndex);
            return new NioDataOutputStream(duplicateByteBuf);
        }

        @Override
        public void flush() throws Exception {
            codedOutputStream.flush();
            // 更新写索引
            byteBuf.writerIndex(byteBufWriterIndex(writerIndex()));
        }

        @Override
        public int writerIndex() {
            return codedOutputStreamOffset - startWriterIndex + codedOutputStream.getTotalBytesWritten();
        }

        private int byteBufWriterIndex(int writerIndex) {
            return startWriterIndex + writerIndex;
        }

        @Override
        public String toString() {
            return "NioDataOutputStream{" +
                    "byteBuf=" + byteBuf +
                    ", startWriterIndex=" + startWriterIndex +
                    ", codedOutputStreamOffset=" + codedOutputStreamOffset +
                    ", codedOutputStreamTotalBytesWritten=" + codedOutputStream.getTotalBytesWritten() +
                    ", writerIndex=" + writerIndex() +
                    '}';
        }
    }
}
