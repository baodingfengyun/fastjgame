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

package com.wjybxx.fastjgame.net.misc;

import com.wjybxx.fastjgame.net.binary.BinaryProtocolCodec;
import com.wjybxx.fastjgame.net.rpc.ProtocolCodec;
import com.wjybxx.fastjgame.net.utils.NetUtils;
import com.wjybxx.fastjgame.utils.JsonUtils;
import io.netty.buffer.*;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.InputStream;
import java.util.Set;

/**
 * 基于Json的编解码器，必须使用简单对象来封装参数。-- POJO
 * 编码后的数据量较多，编解码效率也很低，建议只在测试期间使用。-- 因为json可读性很好，正式编解码时建议使用{@link BinaryProtocolCodec}。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/18
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class JsonProtocolCodec implements ProtocolCodec {

    private static final ThreadLocal<byte[]> LOCAL_BUFFER = ThreadLocal.withInitial(() -> new byte[NetUtils.MAX_BUFFER_SIZE]);

    private final MessageMapper messageMapper;

    private JsonProtocolCodec(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Nonnull
    @Override
    public byte[] serializeToBytes(@Nullable Object object) throws Exception {
        if (null == object) {
            return ArrayUtils.EMPTY_BYTE_ARRAY;
        }
        final byte[] localBuffer = LOCAL_BUFFER.get();
        ByteBuf cacheBuffer = Unpooled.wrappedBuffer(localBuffer);
        try {
            // wrap会认为bytes中的数据都是可读的，我们需要清空这些标记。
            cacheBuffer.clear();

            ByteBufOutputStream byteBufOutputStream = new ByteBufOutputStream(cacheBuffer);
            // 协议classId
            int messageId = messageMapper.getMessageId(object.getClass());
            byteBufOutputStream.writeInt(messageId);
            // 写入序列化的内容
            JsonUtils.writeToOutputStream(byteBufOutputStream, object);

            // 拷贝结果
            byte[] result = new byte[cacheBuffer.readableBytes()];
            System.arraycopy(localBuffer, 0, result, 0, result.length);
            return result;
        } finally {
            cacheBuffer.release();
        }
    }

    @Override
    public Object deserializeFromBytes(@Nonnull byte[] data) throws Exception {
        if (data.length == 0) {
            return null;
        }
        ByteBuf byteBuf = Unpooled.wrappedBuffer(data);
        try {
            return readObject(byteBuf);
        } finally {
            byteBuf.release();
        }
    }

    @Override
    public Object cloneObject(@Nullable Object object) throws Exception {
        if (null == object) {
            return null;
        }
        ByteBuf cacheBuffer = Unpooled.wrappedBuffer(LOCAL_BUFFER.get());
        try {
            // wrap会认为bytes中的数据都是可读的，我们需要清空这些标记。
            cacheBuffer.clear();

            // 写入序列化的内容
            ByteBufOutputStream byteBufOutputStream = new ByteBufOutputStream(cacheBuffer);
            JsonUtils.writeToOutputStream(byteBufOutputStream, object);

            // 再读出来
            ByteBufInputStream byteBufInputStream = new ByteBufInputStream(cacheBuffer);
            return JsonUtils.readFromInputStream((InputStream) byteBufInputStream, object.getClass());
        } finally {
            cacheBuffer.release();
        }
    }

    @Nonnull
    @Override
    public ByteBuf writeObject(ByteBufAllocator bufAllocator, @Nullable Object obj) throws Exception {
        if (null == obj) {
            return bufAllocator.buffer(0);
        }
        ByteBuf cacheBuffer = Unpooled.wrappedBuffer(LOCAL_BUFFER.get());
        // wrap会认为bytes中的数据都是可读的，我们需要清空这些标记。
        cacheBuffer.clear();

        try (ByteBufOutputStream byteBufOutputStream = new ByteBufOutputStream(cacheBuffer)) {
            // 协议classId
            int messageId = messageMapper.getMessageId(obj.getClass());
            byteBufOutputStream.writeInt(messageId);
            // 写入序列化的内容
            JsonUtils.writeToOutputStream(byteBufOutputStream, obj);
            // 写入byteBuf
            ByteBuf byteBuf = bufAllocator.buffer(cacheBuffer.readableBytes());
            byteBuf.writeBytes(cacheBuffer);
            return byteBuf;
        } finally {
            cacheBuffer.release();
        }
    }

    @Override
    public Object readObject(ByteBuf data) throws Exception {
        if (data.readableBytes() == 0) {
            return null;
        }
        final Class<?> messageClazz = messageMapper.getMessageClass(data.readInt());
        final ByteBufInputStream inputStream = new ByteBufInputStream(data);
        return JsonUtils.readFromInputStream((InputStream) inputStream, messageClazz);
    }

    public static JsonProtocolCodec newInstance(Set<Class<?>> entityClasses, MessageMappingStrategy mappingStrategy) {
        final MessageMapper messageMapper = MessageMapper.newInstance(entityClasses, mappingStrategy);
        return new JsonProtocolCodec(messageMapper);
    }
}
