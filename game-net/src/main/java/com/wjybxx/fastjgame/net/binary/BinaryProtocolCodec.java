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
import com.google.protobuf.*;
import com.wjybxx.fastjgame.net.common.ProtocolCodec;
import com.wjybxx.fastjgame.net.misc.JsonProtocolCodec;
import com.wjybxx.fastjgame.net.misc.MessageMapper;
import com.wjybxx.fastjgame.net.misc.MessageMappingStrategy;
import com.wjybxx.fastjgame.net.utils.NetUtils;
import com.wjybxx.fastjgame.net.utils.ProtoUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * 基于protoBuf的二进制格式编解码器。
 * 相对于{@link JsonProtocolCodec}传输的数据量要少得多(大致1/2)，更少的数据量当然伴随着更快编码速度(大致4倍)。
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
public class BinaryProtocolCodec implements ProtocolCodec {

    private static final ThreadLocal<byte[]> LOCAL_BUFFER = ThreadLocal.withInitial(() -> new byte[NetUtils.MAX_BUFFER_SIZE]);

    private final CodecRegistry codecRegistry;

    public BinaryProtocolCodec(CodecRegistry codecRegistry) {
        this.codecRegistry = codecRegistry;
    }

    @Nonnull
    @Override
    public ByteBuf writeObject(ByteBufAllocator bufAllocator, @Nullable Object object) throws Exception {
        // 这里的测试结果是：拷贝字节数组，比先计算一次大小，再写入ByteBuf快，而且快很多。
        // 此外，即使使用输入输出流构造，其内部还是做了缓存(创建了字节数组)，因此一定要有自己的缓冲字节数组
        final byte[] localBuffer = LOCAL_BUFFER.get();

        // 写入字节数组缓存
        final CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(localBuffer);
        encodeObject(codedOutputStream, object, codecRegistry);

        // 写入byteBuf
        final ByteBuf buffer = bufAllocator.buffer(codedOutputStream.getTotalBytesWritten());
        buffer.writeBytes(localBuffer, 0, codedOutputStream.getTotalBytesWritten());
        return buffer;
    }

    @Override
    public Object readObject(ByteBuf data) throws Exception {
        final byte[] localBuffer = LOCAL_BUFFER.get();

        // 读入缓存数组
        final int readableBytes = data.readableBytes();
        data.readBytes(localBuffer, 0, readableBytes);

        // 解析对象
        final CodedInputStream codedInputStream = CodedInputStream.newInstance(localBuffer, 0, readableBytes);
        return decodeObject(codedInputStream, codecRegistry);
    }

    @Nonnull
    @Override
    public byte[] serializeToBytes(@Nullable Object object) throws Exception {
        // 这里测试也是拷贝字节数组快于先计算大小（两轮反射）
        final byte[] localBuffer = LOCAL_BUFFER.get();
        final CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(localBuffer);
        encodeObject(codedOutputStream, object, codecRegistry);

        // 拷贝序列化结果
        final byte[] resultBytes = new byte[codedOutputStream.getTotalBytesWritten()];
        System.arraycopy(localBuffer, 0, resultBytes, 0, resultBytes.length);
        return resultBytes;
    }

    @Override
    public Object deserializeFromBytes(@Nonnull byte[] data) throws Exception {
        return decodeObject(CodedInputStream.newInstance(data), codecRegistry);
    }

    @Nullable
    @Override
    public Object cloneObject(@Nullable Object object) throws Exception {
        if (object == null) {
            return null;
        }
        final byte[] localBuffer = LOCAL_BUFFER.get();
        // 写入缓冲区
        final CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(localBuffer);
        encodeObject(codedOutputStream, object, codecRegistry);

        // 读出
        final CodedInputStream codedInputStream = CodedInputStream.newInstance(localBuffer, 0, codedOutputStream.getTotalBytesWritten());
        return decodeObject(codedInputStream, codecRegistry);
    }

    // ------------------------------------------- tag相关 ------------------------------------

    /**
     * 写入一个tag
     *
     * @param outputStream 输出流
     * @param wireType     tag
     * @throws IOException error
     */
    static void writeTag(CodedOutputStream outputStream, WireType wireType) throws IOException {
        outputStream.writeRawByte(wireType.getNumber());
    }

    /**
     * 读取一个tag
     *
     * @param inputStream 输入流
     * @return tag
     * @throws IOException error
     */
    static WireType readTag(CodedInputStream inputStream) throws IOException {
        return WireType.forNumber(inputStream.readRawByte());
    }

    static <T> void encodeObject(CodedOutputStream outputStream, @Nullable T value, CodecRegistry codecRegistry) throws Exception {
        if (null == value) {
            writeTag(outputStream, WireType.NULL);
        } else {
            @SuppressWarnings("unchecked") final Codec<T> codec = (Codec<T>) codecRegistry.get(value.getClass());
            writeTag(outputStream, codec.wireType());
            outputStream.writeInt32NoTag(codec.getProviderId());
            outputStream.writeInt32NoTag(codec.getClassId());
            codec.encode(outputStream, value, codecRegistry);
        }
    }

    static <T> T decodeObject(CodedInputStream inputStream, CodecRegistry codecRegistry) throws Exception {
        final WireType tag = readTag(inputStream);
        if (tag == WireType.NULL) {
            return null;
        }
        final int providerId = inputStream.readInt32();
        final int classId = inputStream.readInt32();
        @SuppressWarnings("unchecked") final Codec<T> codec = (Codec<T>) codecRegistry.get(providerId, classId);
        return codec.decode(inputStream, codecRegistry);
    }

    // ------------------------------------------------- 工厂方法 ------------------------------------------------------

    public static BinaryProtocolCodec newInstance(MessageMappingStrategy mappingStrategy) {
        return newInstance(mappingStrategy, c -> true);
    }

    /**
     * @param filter 由于{@link BinaryProtocolCodec}支持的消息类是确定的，不能加入，但是允许过滤删除
     */
    @SuppressWarnings("unchecked")
    public static BinaryProtocolCodec newInstance(MessageMappingStrategy mappingStrategy, Predicate<Class<?>> filter) {
        final Set<Class<?>> supportedClassSet = getFilteredSupportedClasses(filter);
        final MessageMapper messageMapper = MessageMapper.newInstance(supportedClassSet, mappingStrategy);
        final List<AppObjectCodec<?>> codecList = new ArrayList<>(supportedClassSet.size());

        try {
            for (Class<?> messageClazz : messageMapper.getAllMessageClasses()) {
                // protoBuf消息
                if (Message.class.isAssignableFrom(messageClazz)) {
                    Parser<?> parser = ProtoUtils.findParser((Class<? extends Message>) messageClazz);
                    codecList.add(new ProtoMessageCodec(messageMapper.getMessageId(messageClazz), messageClazz, parser));
                    continue;
                }

                // protoBufEnum
                if (ProtocolMessageEnum.class.isAssignableFrom(messageClazz)) {
                    final Internal.EnumLiteMap<?> mapper = ProtoUtils.findMapper((Class<? extends ProtocolMessageEnum>) messageClazz);
                    codecList.add(new ProtoEnumCodec(messageMapper.getMessageId(messageClazz), messageClazz, mapper));
                    continue;
                }

                // 带有DBEntity和SerializableClass注解的所有类，和手写Serializer的类
                final Class<? extends EntitySerializer<?>> serializerClass = EntitySerializerScanner.getSerializerClass(messageClazz);
                if (serializerClass != null) {
                    final EntitySerializer<?> serializer = createSerializerInstance(serializerClass);
                    codecList.add(new SerializerBasedCodec(messageMapper.getMessageId(messageClazz), serializer));
                    continue;
                }

                throw new IllegalArgumentException("Unsupported class " + messageClazz.getName());
            }

            final CodecRegistry codecRegistry = new CodecRegistryImp(AppCodecProvider.newInstance(codecList));
            return new BinaryProtocolCodec(codecRegistry);
        } catch (Exception e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    private static Set<Class<?>> getFilteredSupportedClasses(Predicate<Class<?>> filter) {
        final Set<Class<?>> allCustomEntityClasses = EntitySerializerScanner.getAllCustomEntityClasses();
        final Set<Class<?>> allProtoBufferClasses = ProtoBufferScanner.getAllProtoBufferClasses();
        final Set<Class<?>> supportedClassSet = Sets.newHashSetWithExpectedSize(allCustomEntityClasses.size() + allProtoBufferClasses.size());

        Stream.concat(allCustomEntityClasses.stream(), allProtoBufferClasses.stream())
                .filter(filter)
                .forEach(supportedClassSet::add);
        return supportedClassSet;
    }

    private static EntitySerializer<?> createSerializerInstance(Class<? extends EntitySerializer<?>> serializerClass) throws Exception {
        final Constructor<? extends EntitySerializer<?>> noArgsConstructor = serializerClass.getDeclaredConstructor(ArrayUtils.EMPTY_CLASS_ARRAY);
        noArgsConstructor.setAccessible(true);
        return noArgsConstructor.newInstance(ArrayUtils.EMPTY_OBJECT_ARRAY);
    }

}
