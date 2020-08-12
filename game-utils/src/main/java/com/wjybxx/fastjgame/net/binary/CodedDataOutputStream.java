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
import com.google.protobuf.MessageLite;
import com.wjybxx.fastjgame.util.annotation.UnstableApi;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * 对{@link CodedOutputStream}的封装，屏蔽转义一些接口，以及扩展功能。
 * Q: 它为什么是个抽象类？？？
 * A: {@link CodedOutputStream}是不支持扩容的，这在有时候是个问题。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/13
 * github - https://github.com/hl845740757
 */
public abstract class CodedDataOutputStream {

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

    public final void writeType(BinaryValueType valueType) throws IOException {
        writeRawByte((byte) valueType.getNumber());
    }

    public final void writeRawByte(int value) throws IOException {
        writeRawByte((byte) value);
    }

    public final void writeRawBytes(byte[] value) throws IOException {
        writeRawBytes(value, 0, value.length);
    }

    public abstract void writeRawByte(byte value) throws IOException;

    public abstract void writeInt32(int value) throws IOException;

    public abstract void writeFixed32(int value) throws IOException;

    public abstract void writeInt64(long value) throws IOException;

    public abstract void writeFixed64(long value) throws IOException;

    public abstract void writeFloat(float value) throws IOException;

    public abstract void writeDouble(double value) throws IOException;

    public abstract void writeBool(boolean value) throws IOException;

    public abstract void writeString(String value) throws IOException;

    public abstract void writeRawBytes(byte[] value, int offset, int length) throws IOException;

    public abstract void flush() throws IOException;

    /**
     * @return 写入的字节数
     */
    public abstract int getTotalBytesWritten();

    /**
     * 只写入消息的内容，不写入消息的长度（外部已有size）
     */
    public abstract void writeMessageNoSize(MessageLite value) throws IOException;

    /**
     * 在指定写索引处写如给定值。
     * PS: 这是扩展API。
     *
     * @param index 写索引
     * @param value 要写入的值
     * @throws IOException error
     */
    public abstract void setFixedInt32(final int index, int value) throws IOException;

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
        public void writeRawByte(byte value) throws IOException {
            codedOutputStream.writeRawByte(value);
        }

        @Override
        public void writeInt32(int value) throws IOException {
            codedOutputStream.writeInt32NoTag(value);
        }

        @Override
        public void writeFixed32(int value) throws IOException {
            codedOutputStream.writeFixed32NoTag(value);
        }

        @Override
        public void writeInt64(long value) throws IOException {
            codedOutputStream.writeInt64NoTag(value);
        }

        @Override
        public void writeFixed64(long value) throws IOException {
            codedOutputStream.writeFixed64NoTag(value);
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
        public void writeBool(boolean value) throws IOException {
            codedOutputStream.writeBoolNoTag(value);
        }

        @Override
        public void writeString(String value) throws IOException {
            codedOutputStream.writeStringNoTag(value);
        }

        @Override
        public void writeRawBytes(byte[] value, int offset, int length) throws IOException {
            codedOutputStream.writeRawBytes(value, offset, length);
        }

        @Override
        public void writeMessageNoSize(MessageLite value) throws IOException {
            value.writeTo(codedOutputStream);
        }

        @Override
        public void flush() throws IOException {
            codedOutputStream.flush();
        }

        @Override
        public int getTotalBytesWritten() {
            return codedOutputStreamOffset - offset + codedOutputStream.getTotalBytesWritten();
        }

        @Override
        public void setFixedInt32(final int index, int value) throws IOException {
            final int position = offset + index;
            buffer[position] = (byte) value;
            buffer[position + 1] = (byte) (value >>> 8);
            buffer[position + 2] = (byte) (value >>> 16);
            buffer[position + 3] = (byte) (value >>> 24);
        }

        @Override
        public String toString() {
            return "ArrayCodedDataOutputStream{" +
                    "arrayLength=" + buffer.length +
                    ", offset=" + offset +
                    ", limit=" + limit +
                    ", codedOutputStreamOffset=" + codedOutputStreamOffset +
                    ", codedOutputStreamTotalBytesWritten=" + codedOutputStream.getTotalBytesWritten() +
                    ", totalBytesWritten=" + getTotalBytesWritten() +
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
                throw new IllegalArgumentException("nioBufferCount: " + byteBuf.nioBufferCount() + " (expected: 1)");
            }

            // 必须显示指定最大容量，且不能太小
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
        public void writeRawByte(byte value) throws IOException {
            final int preWriterIndex = getTotalBytesWritten();
            while (true) {
                try {
                    codedOutputStream.writeRawByte(value);
                    return;
                } catch (CodedOutputStream.OutOfSpaceException | IndexOutOfBoundsException exception) {
                    ensureCapacity(preWriterIndex, exception);
                }
            }
        }

        @Override
        public void writeInt32(int value) throws IOException {
            final int preWriterIndex = getTotalBytesWritten();
            while (true) {
                try {
                    codedOutputStream.writeInt32NoTag(value);
                    return;
                } catch (CodedOutputStream.OutOfSpaceException | IndexOutOfBoundsException exception) {
                    ensureCapacity(preWriterIndex, exception);
                }
            }
        }

        @Override
        public void writeFixed32(int value) throws IOException {
            final int preWriterIndex = getTotalBytesWritten();
            while (true) {
                try {
                    codedOutputStream.writeFixed32NoTag(value);
                    return;
                } catch (CodedOutputStream.OutOfSpaceException | IndexOutOfBoundsException exception) {
                    ensureCapacity(preWriterIndex, exception);
                }
            }
        }

        @Override
        public void writeInt64(long value) throws IOException {
            final int preWriterIndex = getTotalBytesWritten();
            while (true) {
                try {
                    codedOutputStream.writeInt64NoTag(value);
                    return;
                } catch (CodedOutputStream.OutOfSpaceException | IndexOutOfBoundsException exception) {
                    ensureCapacity(preWriterIndex, exception);
                }
            }
        }

        @Override
        public void writeFixed64(long value) throws IOException {
            final int preWriterIndex = getTotalBytesWritten();
            while (true) {
                try {
                    codedOutputStream.writeFixed64NoTag(value);
                    return;
                } catch (CodedOutputStream.OutOfSpaceException | IndexOutOfBoundsException exception) {
                    ensureCapacity(preWriterIndex, exception);
                }
            }
        }

        @Override
        public void writeFloat(float value) throws IOException {
            final int preWriterIndex = getTotalBytesWritten();
            while (true) {
                try {
                    codedOutputStream.writeFloatNoTag(value);
                    return;
                } catch (CodedOutputStream.OutOfSpaceException | IndexOutOfBoundsException exception) {
                    ensureCapacity(preWriterIndex, exception);
                }
            }
        }

        @Override
        public void writeDouble(double value) throws IOException {
            final int preWriterIndex = getTotalBytesWritten();
            while (true) {
                try {
                    codedOutputStream.writeDoubleNoTag(value);
                    return;
                } catch (CodedOutputStream.OutOfSpaceException | IndexOutOfBoundsException exception) {
                    ensureCapacity(preWriterIndex, exception);
                }
            }
        }

        @Override
        public void writeBool(boolean value) throws IOException {
            final int preWriterIndex = getTotalBytesWritten();
            while (true) {
                try {
                    codedOutputStream.writeBoolNoTag(value);
                    return;
                } catch (CodedOutputStream.OutOfSpaceException | IndexOutOfBoundsException exception) {
                    ensureCapacity(preWriterIndex, exception);
                }
            }
        }

        @Override
        public void writeString(@Nonnull String value) throws IOException {
            final int preWriterIndex = getTotalBytesWritten();
            while (true) {
                try {
                    codedOutputStream.writeStringNoTag(value);
                    return;
                } catch (CodedOutputStream.OutOfSpaceException | IndexOutOfBoundsException exception) {
                    ensureCapacity(preWriterIndex, exception);
                }
            }
        }

        @Override
        public void writeRawBytes(byte[] bytes, int off, int len) throws IOException {
            final int preWriterIndex = getTotalBytesWritten();
            while (true) {
                try {
                    codedOutputStream.writeRawBytes(bytes, off, len);
                    return;
                } catch (CodedOutputStream.OutOfSpaceException | IndexOutOfBoundsException exception) {
                    ensureCapacity(preWriterIndex, exception);
                }
            }
        }

        @Override
        public void writeMessageNoSize(@Nonnull MessageLite message) throws IOException {
            final int preWriterIndex = getTotalBytesWritten();
            while (true) {
                try {
                    message.writeTo(codedOutputStream);
                    return;
                } catch (CodedOutputStream.OutOfSpaceException | IndexOutOfBoundsException exception) {
                    ensureCapacity(preWriterIndex, exception);
                }
            }
        }

        @Override
        public void flush() throws IOException {
            codedOutputStream.flush();
            // 更新写索引
            byteBuf.writerIndex(byteBufWriterIndex(getTotalBytesWritten()));
        }

        private int byteBufWriterIndex(int writerIndex) {
            return startWriterIndex + writerIndex;
        }

        @Override
        public int getTotalBytesWritten() {
            return codedOutputStreamOffset - startWriterIndex + codedOutputStream.getTotalBytesWritten();
        }

        @Override
        public void setFixedInt32(int index, int value) throws IOException {
            final int byteBufWriterIndex = byteBufWriterIndex(index);
            final int preWriterIndex = getTotalBytesWritten();

            while (true) {
                try {
                    byteBuf.setIntLE(byteBufWriterIndex, value);
                    return;
                } catch (IndexOutOfBoundsException exception) {
                    ensureCapacity(preWriterIndex, exception);
                }
            }
        }

        private void ensureCapacity(int preWriterIndex, Exception exception) throws IOException {
            if (byteBuf.capacity() >= byteBuf.maxCapacity()) {
                // 无法继续扩容
                throw new IOException("reach maxCapacity " + byteBuf.maxCapacity(), exception);
            }

            // 2倍扩容
            final int lastCapacity = byteBuf.capacity();
            final int targetCapacity = Math.min(lastCapacity * 2, byteBuf.maxCapacity());

            if (targetCapacity <= lastCapacity) {
                // 溢出
                throw new IOException(
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
        public String toString() {
            return "NioDataOutputStream{" +
                    "byteBuf=" + byteBuf +
                    ", startWriterIndex=" + startWriterIndex +
                    ", codedOutputStreamOffset=" + codedOutputStreamOffset +
                    ", codedOutputStreamTotalBytesWritten=" + codedOutputStream.getTotalBytesWritten() +
                    ", getTotalBytesWritten=" + getTotalBytesWritten() +
                    '}';
        }
    }
}
