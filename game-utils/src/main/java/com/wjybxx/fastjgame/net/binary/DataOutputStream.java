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

import com.google.protobuf.Message;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * 数据输出流。
 * 和JDK的{@code DataOutput}不同，我们允许对写入的数据进行压缩等处理，因此不要求short必须是2个字节，也不要求int必须是4个字节等，但仍要满足一些约定:
 *
 * <h3>实现约定</h3>
 * <li>1. byte必须固定一个字节。</li>
 * <li>2. 命名包含{@code Fixed}的方法必定固定字节数，如：int4字节，long8字节，且按照<b>大端模式</b>写入</li>
 * <li>3. 命名包含{@code set}的方法不修改写{@link #writerIndex()}</li>
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/14
 * github - https://github.com/hl845740757
 */
public interface DataOutputStream {

    void writeByte(byte value) throws IOException;

    void writeByte(int value) throws IOException;

    void writeShort(short value) throws IOException;

    void writeChar(char value) throws IOException;

    void writeInt(int value) throws IOException;

    void writeLong(long value) throws IOException;

    void writeFloat(float value) throws IOException;

    void writeDouble(double value) throws IOException;

    void writeBoolean(boolean value) throws IOException;

    void writeBytes(byte[] bytes) throws IOException;

    void writeBytes(byte[] bytes, int off, int len) throws IOException;

    void writeString(@Nonnull String value) throws IOException;

    /**
     * 针对protoBuffer消息的特定支持
     */
    void writeMessage(@Nonnull Message message) throws IOException;

    /**
     * {@link #writeByte(int)}和{@link BinaryTag#getNumber()}的快捷调用
     */
    void writeTag(BinaryTag tag) throws IOException;

    /**
     * 以固定4个字节大端模式写入一个int
     * {@link #writerIndex()}应该加4
     */
    void writeFixedInt32(int value) throws IOException;

    /**
     * {@link #writerIndex()}保持不变
     */
    void setFixedInt32(int index, int value) throws IOException;

    /**
     * 获取当前的写索引
     */
    int writerIndex();

    /**
     * 修改写索引
     */
    void writerIndex(int newWriteIndex);

    /**
     * {@link #slice(int)}和{@link #writerIndex()}的快捷调用。
     */
    DataOutputStream slice();

    /**
     * 从指定位置返回一个数据切片(视图)，该切片拥有独立的索引，但是它们仍然共享底层的数据结构，
     * 任何在切片上的操作都将反应在底层数据结构上。
     */
    DataOutputStream slice(int index);

    /**
     * 刷新缓冲区
     */
    void flush() throws IOException;
}
