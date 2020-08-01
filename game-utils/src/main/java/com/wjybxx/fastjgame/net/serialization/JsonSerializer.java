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

import com.wjybxx.fastjgame.net.binary.BinarySerializer;
import com.wjybxx.fastjgame.net.misc.BufferPool;
import com.wjybxx.fastjgame.net.type.TypeId;
import com.wjybxx.fastjgame.net.type.TypeModel;
import com.wjybxx.fastjgame.net.type.TypeModelMapper;
import com.wjybxx.fastjgame.util.JsonUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.InputStream;

/**
 * 基于Json的编解码器，必须使用简单对象来封装参数。-- POJO
 * 编码后的数据量较多，编解码效率也很低，建议只在测试期间使用。-- 因为json可读性很好，正式编解码时建议使用{@link BinarySerializer}。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/18
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class JsonSerializer implements Serializer {

    private final TypeModelMapper typeModelMapper;
    private final int defaultByteBufSize;

    private JsonSerializer(TypeModelMapper typeModelMapper) {
        this(typeModelMapper, 256);
    }

    private JsonSerializer(TypeModelMapper typeModelMapper, int defaultByteBufSize) {
        this.typeModelMapper = typeModelMapper;
        this.defaultByteBufSize = defaultByteBufSize;
    }

    @Nonnull
    @Override
    public byte[] toBytes(@Nullable Object object) throws Exception {
        if (null == object) {
            return ArrayUtils.EMPTY_BYTE_ARRAY;
        }

        final byte[] localBuffer = BufferPool.allocateBuffer();
        final ByteBuf cacheBuffer = Unpooled.wrappedBuffer(localBuffer);
        try {
            // wrap会认为bytes中的数据都是可读的，我们需要清空这些标记。
            cacheBuffer.clear();

            ByteBufOutputStream byteBufOutputStream = new ByteBufOutputStream(cacheBuffer);
            // 类型信息
            writeTypeId(byteBufOutputStream, object.getClass());
            // 写入序列化的内容
            JsonUtils.writeToOutputStream(byteBufOutputStream, object);

            // 拷贝结果
            byte[] result = new byte[cacheBuffer.readableBytes()];
            System.arraycopy(localBuffer, 0, result, 0, result.length);
            return result;
        } finally {
            cacheBuffer.release();
            BufferPool.releaseBuffer(localBuffer);
        }
    }

    @Override
    public Object fromBytes(@Nonnull byte[] data) throws Exception {
        if (data.length == 0) {
            return null;
        }

        final ByteBuf byteBuf = Unpooled.wrappedBuffer(data);
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

        final byte[] localBuffer = BufferPool.allocateBuffer();
        final ByteBuf cacheBuffer = Unpooled.wrappedBuffer(localBuffer);
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
            BufferPool.releaseBuffer(localBuffer);
        }
    }

    @Override
    public int estimatedSerializedSize(@Nullable Object object) {
        // 无法估算
        if (object == null) {
            return 0;
        }
        return defaultByteBufSize;
    }

    @Override
    public void writeObject(final ByteBuf byteBuf, @Nullable Object object) throws Exception {
        if (object == null) {
            return;
        }

        final byte[] localBuffer = BufferPool.allocateBuffer();
        final ByteBuf cacheBuffer = Unpooled.wrappedBuffer(localBuffer);
        // wrap会认为bytes中的数据都是可读的，我们需要清空这些标记。
        cacheBuffer.clear();

        try (ByteBufOutputStream byteBufOutputStream = new ByteBufOutputStream(cacheBuffer)) {
            // 类型信息
            writeTypeId(byteBufOutputStream, object.getClass());
            // 写入序列化的内容
            JsonUtils.writeToOutputStream(byteBufOutputStream, object);
            // 写入byteBuf
            byteBuf.writeBytes(cacheBuffer);
        } finally {
            cacheBuffer.release();
            BufferPool.releaseBuffer(localBuffer);
        }
    }

    private void writeTypeId(ByteBufOutputStream byteBufOutputStream, Class<?> type) throws IOException {
        final TypeModel typeModel = typeModelMapper.ofType(type);
        if (null == typeModel) {
            throw new IOException("Unsupported type " + type.getName());
        }
        byteBufOutputStream.writeByte(typeModel.typeId().getNamespace());
        byteBufOutputStream.writeByte(typeModel.typeId().getClassId());
    }

    @Override
    public Object readObject(ByteBuf data) throws Exception {
        if (data.readableBytes() == 0) {
            return null;
        }
        final Class<?> messageClazz = readType(data);
        final ByteBufInputStream inputStream = new ByteBufInputStream(data);
        return JsonUtils.readFromInputStream((InputStream) inputStream, messageClazz);
    }

    private Class<?> readType(ByteBuf data) throws IOException {
        final byte nameSpace = data.readByte();
        final int classId = data.readInt();
        final TypeId typeId = new TypeId(nameSpace, classId);
        final TypeModel typeModel = typeModelMapper.ofId(typeId);
        if (null == typeModel) {
            throw new IOException("Unknown typeId " + typeId);
        }
        return typeModel.type();
    }

    public static JsonSerializer newInstance(TypeModelMapper typeModelMapper) {
        return new JsonSerializer(typeModelMapper);
    }
}
