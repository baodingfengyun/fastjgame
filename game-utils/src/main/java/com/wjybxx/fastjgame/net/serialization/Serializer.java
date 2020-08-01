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

package com.wjybxx.fastjgame.net.serialization;

import com.google.protobuf.Message;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * 对象编解码器。<br>
 * 注意：子类实现必须是线程安全的！因为它可能在多个线程中调用。
 * <p>
 * 这里提供的保证的是：{@link Serializer}发布到网络层一定 happens-before 网络层调用任意编解码方法！<br>
 * 因此建议的实现方式：在传递给网络层之前完成所有的初始化工作，并且所有的编解码工作都不会修改对象的状态。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/18
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public interface Serializer {

    /**
     * 将一个对象序列化为字节数组，为了直接转发到玩家，该格式应当是兼容的。
     * (服务器之间、玩家与服务器之间对于protoBuf的编码格式应当是相同的)
     *
     * @param object 待序列化的对象 - 当前不是字节数组
     * @return 字节数组
     */
    @Nonnull
    byte[] toBytes(@Nullable Object object) throws Exception;

    /**
     * 将一个对象反序列化。
     * (服务器之间、玩家与服务器之间对于protoBuf的编码格式应当是相同的)
     *
     * @param data 序列化后的数组
     * @return 反序列化的结果
     */
    Object fromBytes(@Nonnull byte[] data) throws Exception;

    /**
     * 克隆一个对象。
     * 该方法的主要目的是消除调用{@link #toBytes(Object)}和{@link #fromBytes(byte[])}实现克隆产生的中间数组。
     *
     * @param object 待克隆的对象
     * @return Q: 为什么不是泛型的？A:对于多态对象，如果缺少相应信息，可能返回不兼容的对象（map和集合）。
     * @throws Exception error
     */
    Object cloneObject(@Nullable Object object) throws Exception;

    /**
     * 写入一个对象
     *
     * @param bufAllocator buf分配器，为了减少中间数据创建
     * @param object       待编码的对象
     * @return 编码后的字节数组
     * @throws Exception error
     */
    @Nonnull
    ByteBuf writeObject(ByteBufAllocator bufAllocator, @Nullable Object object) throws Exception;

    /**
     * 写入一个对象到给的的byteBuf。
     * 用途：
     * 1. 当大致知道序列化后的长度的时候，分配容量合适的ByteBuf有助于性能，比如写protoBuf消息，{@link Message#getSerializedSize()}
     * 2. 当知道要序列化的内容长度较大时，分配一个容量较大的ByteBuf有助于性能，拿游戏来讲，角色进入场景/登录协议往往比较大，采用扩容机制的话，可能触发多次扩容。
     *
     * @param byteBuf 指定写入的byteBuf
     * @param object  待编码的对象
     * @return 编码后的字节数组
     * @throws Exception error
     */
    @Nonnull
    ByteBuf writeObject(ByteBuf byteBuf, @Nullable Object object) throws Exception;

    /**
     * 读取一个对象
     *
     * @param data 字节数组
     * @return instance
     * @throws Exception error
     */
    Object readObject(ByteBuf data) throws Exception;

}
