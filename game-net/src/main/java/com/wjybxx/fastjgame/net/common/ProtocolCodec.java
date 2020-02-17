/*
 *  Copyright 2019 wjybxx
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to iBn writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.wjybxx.fastjgame.net.common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;

/**
 * 协议编解码器。<br>
 * 注意：子类实现必须是线程安全的！因为它可能在多个线程中调用。但能保证的是：{@link ProtocolCodec}发布到网络层一定 happens-before 任意编解码方法！<br>
 * 因此建议的实现方式：在传递给网络层之前完成所有的初始化工作，并且所有的编解码工作都不会修改对象的状态。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/18
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public interface ProtocolCodec {

    /**
     * 将一个对象序列化为字节数组，为了直接转发到玩家，该格式应当是兼容的。
     * (服务器之间、玩家与服务器之间对于protoBuf的编码格式应当是相同的)
     *
     * @param obj 待序列化的对象 - 当前不是字节数组
     * @return 字节数组
     */
    @Nonnull
    byte[] serializeToBytes(@Nullable Object obj) throws IOException;

    /**
     * 将一个对象反序列化。
     * (服务器之间、玩家与服务器之间对于protoBuf的编码格式应当是相同的)
     *
     * @param data 序列化后的数组
     * @return 反序列化的结果
     */
    Object deserializeFromBytes(@Nonnull byte[] data) throws IOException;

    /**
     * 克隆一个对象。
     * 该方法的主要目的是消除调用{@link #serializeToBytes(Object)}和{@link #deserializeFromBytes(byte[])}实现克隆产生的中间数组。
     *
     * @param obj 待克隆的对象
     * @return 深度克隆的对象
     * @throws IOException error
     */
    @Nullable
    Object cloneObject(@Nullable Object obj) throws IOException;

    /**
     * 写入一个对象
     *
     * @param bufAllocator buf分配器，为了减少中间数据创建
     * @param object       待编码的对象
     * @return 编码后的字节数组
     * @throws IOException error
     */
    @Nonnull
    ByteBuf writeObject(ByteBufAllocator bufAllocator, @Nullable Object object) throws IOException;

    /**
     * 读取一个对象
     *
     * @param data 字节数组
     * @return instance
     * @throws IOException error
     */
    Object readObject(ByteBuf data) throws IOException;

}
