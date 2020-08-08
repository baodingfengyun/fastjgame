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

import com.google.protobuf.*;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * {@link CodedInputStream}的封装，屏蔽转义一些接口。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/14
 * github - https://github.com/hl845740757
 */
public class CodedDataInputStream {

    /**
     * 缓存有助于性能
     */
    private static final ExtensionRegistryLite EMPTY_REGISTRY = ExtensionRegistryLite.getEmptyRegistry();

    private final CodedInputStream codedInputStream;

    private CodedDataInputStream(CodedInputStream codedInputStream) {
        this.codedInputStream = codedInputStream;
    }

    public static CodedDataInputStream newInstance(@Nonnull byte[] buffer) {
        return newInstance(buffer, 0, buffer.length);
    }

    public static CodedDataInputStream newInstance(byte[] buffer, int offset, int length) {
        return newInstance(CodedInputStream.newInstance(buffer, offset, length));
    }

    private static CodedDataInputStream newInstance(CodedInputStream codedInputStream) {
        return new CodedDataInputStream(codedInputStream);
    }

    public static CodedDataInputStream newInstance(@Nonnull ByteBuf byteBuf) {
        return newInstance(byteBuf, byteBuf.readerIndex(), byteBuf.readableBytes());
    }

    public static CodedDataInputStream newInstance(ByteBuf byteBuf, int index, int length) {
        validateByteBuf(byteBuf, index, length);
        final ByteBuffer byteBuffer = byteBuf.internalNioBuffer(index, length);
        return newInstance(CodedInputStream.newInstance(byteBuffer));
    }

    private static void validateByteBuf(ByteBuf byteBuf, int index, int length) {
        if (byteBuf.nioBufferCount() != 1) {
            throw new IllegalArgumentException("nioBufferCount: " + byteBuf.nioBufferCount() + " (expected: 1)");
        }

        if (index < 0 || length < 0 || index + length > byteBuf.capacity()) {
            final String msg = String.format("Buffer range is invalid. Buffer.length=%d, offset=%d, length=%d",
                    byteBuf.capacity(), index, length);
            throw new IllegalArgumentException(msg);
        }
    }

    public final BinaryValueType readType() throws IOException {
        final byte number = codedInputStream.readRawByte();
        return BinaryValueType.forNumber(number);
    }

    public byte readRawByte() throws IOException {
        return codedInputStream.readRawByte();
    }

    public int readInt32() throws IOException {
        return codedInputStream.readInt32();
    }

    public int readFixed32() throws IOException {
        return codedInputStream.readFixed32();
    }

    public long readFixed64() throws IOException {
        return codedInputStream.readFixed64();
    }

    public long readInt64() throws IOException {
        return codedInputStream.readInt64();
    }

    public float readFloat() throws IOException {
        return codedInputStream.readFloat();
    }

    public double readDouble() throws IOException {
        return codedInputStream.readDouble();
    }

    public boolean readBool() throws IOException {
        return codedInputStream.readBool();
    }

    public String readString() throws IOException {
        return codedInputStream.readString();
    }

    public byte[] readRawBytes(int size) throws IOException {
        return codedInputStream.readRawBytes(size);
    }

    public <T extends MessageLite> T readMessageNoSize(@Nonnull Parser<T> parser) throws IOException {
        return parser.parseFrom(codedInputStream, EMPTY_REGISTRY);
    }

    public int pushLimit(int byteLimit) throws InvalidProtocolBufferException {
        return codedInputStream.pushLimit(byteLimit);
    }

    public boolean isAtEnd() throws IOException {
        return codedInputStream.isAtEnd();
    }

    public void popLimit(int oldLimit) {
        codedInputStream.popLimit(oldLimit);
    }

    public int getTotalBytesRead() {
        return codedInputStream.getTotalBytesRead();
    }

}