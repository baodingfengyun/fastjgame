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

package com.wjybxx.fastjgame.net.binary;

import com.google.common.collect.Sets;
import com.google.protobuf.Internal;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import com.google.protobuf.ProtocolMessageEnum;
import com.wjybxx.fastjgame.net.misc.BufferPool;
import com.wjybxx.fastjgame.net.serialization.JsonSerializer;
import com.wjybxx.fastjgame.net.serialization.Serializer;
import com.wjybxx.fastjgame.net.type.TypeModelMapper;
import com.wjybxx.fastjgame.net.utils.ProtoUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * 基于protoBuf的二进制格式编解码器。
 * 相对于{@link JsonSerializer}传输的数据量要少得多(大致1/2)，更少的数据量当然伴随着更快编码速度(大致4倍)。
 * 加上网络传输的影响，差距会被放大。
 * <p>
 * 建议能单例就单例，能减少内存占用。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/19
 * github - https://github.com/hl845740757
 */
@Immutable
@ThreadSafe
public class BinarySerializer implements Serializer {

    private final CodecRegistry codecRegistry;

    private BinarySerializer(CodecRegistry codecRegistry) {
        this.codecRegistry = codecRegistry;
    }

    @Nonnull
    @Override
    public ByteBuf writeObject(ByteBufAllocator bufAllocator, @Nullable Object object) throws Exception {
        // 这里的测试结果是：拷贝字节数组，比先计算一次大小，再写入ByteBuf快，而且快很多。
        // 此外，即使使用输入输出流构造，其内部还是做了缓存(创建了字节数组)，因此一定要有自己的缓冲字节数组
        final byte[] localBuffer = BufferPool.allocateBuffer();
        try {
            // 写入字节数组缓存
            final CodedDataOutputStream outputStream = CodedDataOutputStream.newInstance(localBuffer);
            encodeObject(outputStream, object);

            // 写入byteBuf
            final ByteBuf buffer = bufAllocator.buffer(outputStream.writerIndex());
            buffer.writeBytes(localBuffer, 0, outputStream.writerIndex());
            return buffer;
        } finally {
            BufferPool.releaseBuffer(localBuffer);
        }
    }

    @Override
    public Object readObject(ByteBuf data) throws Exception {
        final byte[] localBuffer = BufferPool.allocateBuffer();
        try {
            // 读入缓存数组
            final int readableBytes = data.readableBytes();
            data.readBytes(localBuffer, 0, readableBytes);

            // 解析对象
            final CodedDataInputStream inputStream = CodedDataInputStream.newInstance(localBuffer, 0, readableBytes);
            return decodeObject(inputStream);
        } finally {
            BufferPool.releaseBuffer(localBuffer);
        }
    }

    @Nonnull
    @Override
    public byte[] toBytes(@Nullable Object object) throws Exception {
        // 这里测试也是拷贝字节数组快于先计算大小（两轮反射）
        final byte[] localBuffer = BufferPool.allocateBuffer();
        try {
            final CodedDataOutputStream outputStream = CodedDataOutputStream.newInstance(localBuffer);
            encodeObject(outputStream, object);

            // 拷贝序列化结果
            final byte[] resultBytes = new byte[outputStream.writerIndex()];
            System.arraycopy(localBuffer, 0, resultBytes, 0, resultBytes.length);
            return resultBytes;
        } finally {
            BufferPool.releaseBuffer(localBuffer);
        }
    }

    @Override
    public Object fromBytes(@Nonnull byte[] data) throws Exception {
        return decodeObject(CodedDataInputStream.newInstance(data));
    }

    @Override
    public Object cloneObject(@Nullable Object object) throws Exception {
        if (object == null) {
            return null;
        }
        final byte[] localBuffer = BufferPool.allocateBuffer();
        try {
            // 写入缓冲区
            final CodedDataOutputStream outputStream = CodedDataOutputStream.newInstance(localBuffer);
            encodeObject(outputStream, object);

            // 读出
            final CodedDataInputStream inputStream = CodedDataInputStream.newInstance(localBuffer, 0, outputStream.writerIndex());
            return decodeObject(inputStream);
        } finally {
            BufferPool.releaseBuffer(localBuffer);
        }
    }

    private void encodeObject(DataOutputStream outputStream, @Nullable Object value) throws Exception {
        final ObjectWriter writer = new ObjectWriterImp(codecRegistry, outputStream);
        writer.writeObject(value);
        writer.flush();
    }

    private Object decodeObject(DataInputStream inputStream) throws Exception {
        final ObjectReader reader = new ObjectReaderImp(codecRegistry, inputStream);
        return reader.readObject();
    }

    // ------------------------------------------------- 工厂方法 ------------------------------------------------------

    public static BinarySerializer newInstance(TypeModelMapper typeModelMapper) {
        return newInstance(typeModelMapper, c -> true);
    }

    /**
     * @param typeModelMapper 类型映射信息
     * @param filter          由于{@link BinarySerializer}支持的消息类是确定的，不能加入，但是允许过滤删除
     */
    @SuppressWarnings("unchecked")
    public static BinarySerializer newInstance(TypeModelMapper typeModelMapper, Predicate<Class<?>> filter) {
        final Set<Class<?>> supportedClassSet = getFilteredSupportedClasses(filter);
        final List<PojoCodecImpl<?>> codecList = new ArrayList<>(supportedClassSet.size());
        try {
            for (Class<?> messageClazz : supportedClassSet) {
                // protoBuf消息
                if (Message.class.isAssignableFrom(messageClazz)) {
                    Parser<?> parser = ProtoUtils.findParser((Class<? extends Message>) messageClazz);
                    codecList.add(new ProtoMessageCodec(messageClazz, parser));
                    continue;
                }

                // protoBufEnum
                if (ProtocolMessageEnum.class.isAssignableFrom(messageClazz)) {
                    final Internal.EnumLiteMap<?> mapper = ProtoUtils.findMapper((Class<? extends ProtocolMessageEnum>) messageClazz);
                    codecList.add(new ProtoEnumCodec(messageClazz, mapper));
                    continue;
                }

                // 带有DBEntity和SerializableClass注解的所有类，和手写Serializer的类
                final Class<? extends PojoCodecImpl<?>> serializerClass = CodecScanner.getCodecClass(messageClazz);
                if (serializerClass != null) {
                    final PojoCodecImpl<?> codec = createCodecInstance(serializerClass);
                    codecList.add(new CustomPojoCodec(codec));
                    continue;
                }

                throw new IllegalArgumentException("Unsupported class " + messageClazz.getName());
            }

            final CodecRegistry codecRegistry = CodecRegistrys.fromAppPojoCodecs(typeModelMapper, codecList);
            return new BinarySerializer(codecRegistry);
        } catch (Exception e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    private static Set<Class<?>> getFilteredSupportedClasses(Predicate<Class<?>> filter) {
        final Set<Class<?>> allCustomCodecClass = CodecScanner.getAllCustomCodecClass();
        final Set<Class<?>> allProtoBufferClasses = ProtoBufferScanner.getAllProtoBufferClasses();
        final Set<Class<?>> supportedClassSet = Sets.newHashSetWithExpectedSize(allCustomCodecClass.size() + allProtoBufferClasses.size());

        Stream.concat(allCustomCodecClass.stream(), allProtoBufferClasses.stream())
                .filter(filter)
                .forEach(supportedClassSet::add);
        return supportedClassSet;
    }

    private static PojoCodecImpl<?> createCodecInstance(Class<? extends PojoCodecImpl<?>> codecClass) throws Exception {
        final Constructor<? extends PojoCodecImpl<?>> noArgsConstructor = codecClass.getDeclaredConstructor(ArrayUtils.EMPTY_CLASS_ARRAY);
        noArgsConstructor.setAccessible(true);
        return noArgsConstructor.newInstance(ArrayUtils.EMPTY_OBJECT_ARRAY);
    }

}
