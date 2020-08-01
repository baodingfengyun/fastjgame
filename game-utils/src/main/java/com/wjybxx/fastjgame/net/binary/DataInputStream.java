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

import com.google.protobuf.Parser;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * 和JDK的{@code DataInput}不同，我们允许对写入的数据进行压缩等处理，因此不要求short必须是2个字节，也不要求int必须是4个字节等，但仍要满足一些约定:
 *
 * <h3>实现约定</h3>
 * <li>1. byte必须固定一个字节。</li>
 * <li>2. 命名包含{@code Fixed}的方法必定固定字节数，如：int4字节，long8字节，且必须大端模式读取。</li>
 * <li>3. 命名包含{@code get}的方法不修改写{@link #readerIndex()} </li>*
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/14
 * github - https://github.com/hl845740757
 */
public interface DataInputStream {

    byte readByte() throws IOException;

    short readShort() throws IOException;

    char readChar() throws IOException;

    int readInt() throws IOException;

    long readLong() throws IOException;

    float readFloat() throws IOException;

    double readDouble() throws IOException;

    boolean readBoolean() throws IOException;

    byte[] readBytes(int size) throws IOException;

    String readString() throws IOException;

    /**
     * 针对protoBuffer消息的特定支持
     */
    <T> T readMessage(@Nonnull Parser<T> parser) throws IOException;

    /**
     * {@link BinaryTag#forNumber(int)}和{@link #readByte()}的快捷调用。
     */
    BinaryTag readTag() throws IOException;

    /**
     * 读取4个字节，并以大端模式读取为int
     * {@link #readerIndex()}应该加4
     */
    int readFixedInt32() throws IOException;

    /**
     * {@link #readerIndex()}应该保持不变
     */
    int getFixedInt32(int index) throws IOException;

    /**
     * 获取当前的读索引
     */
    int readerIndex();

    /**
     * 修改读索引
     */
    void readerIndex(int newReadIndex);

    /**
     * {@link #slice(int, int)} 和 {@link #readerIndex()}的快捷调用。
     */
    DataInputStream slice(int length);

    /**
     * 从指定位置返回一个数据切片(视图)，该切片拥有独立的索引，但是它们仍然共享底层的数据结构。
     */
    DataInputStream slice(int index, int length);
}
