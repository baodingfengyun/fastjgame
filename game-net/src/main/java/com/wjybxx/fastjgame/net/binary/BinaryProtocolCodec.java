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
import com.wjybxx.fastjgame.utils.EnumUtils;
import com.wjybxx.fastjgame.utils.entity.NumericalEntityMapper;
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
import java.util.IdentityHashMap;
import java.util.Map;
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
    /**
     * 所有codec映射
     */
    private final NumericalEntityMapper<BinaryCodec<?>> codecMapper;

    private BinaryProtocolCodec(MessageMapper messageMapper,
                                Map<Class<?>, Parser<?>> parserMap,
                                Map<Class<?>, ProtoEnumCodec.ProtoEnumDescriptor> protoEnumDescriptorMap,
                                Map<Class<?>, EntitySerializer<?>> beanSerializerMap) {
        codecMapper = EnumUtils.mapping(initValues(messageMapper, parserMap, protoEnumDescriptorMap, beanSerializerMap), true);
    }

    private BinaryCodec<?>[] initValues(MessageMapper messageMapper,
                                        Map<Class<?>, Parser<?>> parserMap,
                                        Map<Class<?>, ProtoEnumCodec.ProtoEnumDescriptor> protoEnumDescriptorMap,
                                        Map<Class<?>, EntitySerializer<?>> beanSerializerMap) {
        // 预估出现的频率排个序
        return new BinaryCodec[]{
                // 存在Serializer的类
                // 它为什么放最前面？
                // 服务器内部通信时，一定是自定义实体开始，基本是有索引的，它放前面收益较大。
                new CustomEntityCodec(messageMapper, beanSerializerMap, this),

                new IntegerCodec(),
                new LongCodec(),
                new StringCodec(),

                new CollectionCodec(this),
                new MapCodec(this),

                new ProtoMessageCodec(messageMapper, parserMap),

                new BooleanCodec(),
                new FloatCodec(),
                new DoubleCodec(),
                new ShortCodec(),

                new ArrayCodec(this),

                new ProtoEnumCodec(messageMapper, protoEnumDescriptorMap),

                new ByteCodec(),
                new CharCodec(),
        };
    }

    @Nonnull
    @Override
    public ByteBuf writeObject(ByteBufAllocator bufAllocator, @Nullable Object object) throws Exception {
        // 这里的测试结果是：拷贝字节数组，比先计算一次大小，再写入ByteBuf快，而且快很多。
        // 此外，即使使用输入输出流构造，其内部还是做了缓存(创建了字节数组)，因此一定要有自己的缓冲字节数组
        final byte[] localBuffer = LOCAL_BUFFER.get();

        // 写入字节数组缓存
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(localBuffer);
        writeObject(codedOutputStream, object);

        // 写入byteBuf
        final ByteBuf buffer = bufAllocator.buffer(codedOutputStream.getTotalBytesWritten());
        buffer.writeBytes(localBuffer, 0, codedOutputStream.getTotalBytesWritten());
        return buffer;
    }

    void writeObject(CodedOutputStream outputStream, @Nullable Object object) throws Exception {
        if (object == null) {
            writeTag(outputStream, WireType.NULL);
            return;
        }
        writeRuntimeType(outputStream, object);
    }

    @SuppressWarnings("unchecked")
    void writeRuntimeType(CodedOutputStream outputStream, @Nonnull Object object) throws Exception {
        final Class<?> type = object.getClass();
        for (BinaryCodec codec : codecMapper.values()) {
            if (codec.isSupport(type)) {
                writeTag(outputStream, codec.getWireType());
                codec.writeData(outputStream, object);
                return;
            }
        }
        throw new IOException("unsupported class " + object.getClass().getName());
    }

    @Override
    public Object readObject(ByteBuf data) throws Exception {
        final byte[] localBuffer = LOCAL_BUFFER.get();

        // 读入缓存数组
        final int readableBytes = data.readableBytes();
        data.readBytes(localBuffer, 0, readableBytes);

        // 解析对象
        CodedInputStream codedInputStream = CodedInputStream.newInstance(localBuffer, 0, readableBytes);
        return readObject(codedInputStream);
    }

    /**
     * 读取一个对象，先读取一个标记，在读取数据
     *
     * @param inputStream 输入流
     * @return obj
     * @throws IOException error
     */
    @Nullable
    Object readObject(CodedInputStream inputStream) throws Exception {
        final byte wireType = readTag(inputStream);
        if (wireType == WireType.NULL) {
            return null;
        }
        return getCodec(wireType).readData(inputStream);
    }

    @Nonnull
    <T> BinaryCodec<T> getCodec(int wireType) throws IOException {
        @SuppressWarnings("unchecked") BinaryCodec<T> codec = (BinaryCodec<T>) codecMapper.forNumber(wireType);
        if (null == codec) {
            throw new IOException("unsupported wireType " + wireType);
        }
        return codec;
    }

    @Nonnull
    @Override
    public byte[] serializeToBytes(@Nullable Object obj) throws Exception {
        // 这里测试也是拷贝字节数组快于先计算大小（两轮反射）
        final byte[] localBuffer = LOCAL_BUFFER.get();
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(localBuffer);
        writeObject(codedOutputStream, obj);

        // 拷贝序列化结果
        final byte[] resultBytes = new byte[codedOutputStream.getTotalBytesWritten()];
        System.arraycopy(localBuffer, 0, resultBytes, 0, resultBytes.length);
        return resultBytes;
    }

    @Override
    public Object deserializeFromBytes(@Nonnull byte[] data) throws Exception {
        return readObject(CodedInputStream.newInstance(data));
    }

    @Nullable
    @Override
    public Object cloneObject(@Nullable Object obj) throws Exception {
        if (obj == null) {
            return null;
        }
        final byte[] localBuffer = LOCAL_BUFFER.get();
        // 写入缓冲区
        final CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(localBuffer);
        writeObject(codedOutputStream, obj);

        // 读出
        final CodedInputStream codedInputStream = CodedInputStream.newInstance(localBuffer, 0, codedOutputStream.getTotalBytesWritten());
        return readObject(codedInputStream);
    }

    // ------------------------------------------- tag相关 ------------------------------------

    /**
     * 写入一个tag
     *
     * @param outputStream 输出流
     * @param wireType     tag
     * @throws IOException error
     */
    static void writeTag(CodedOutputStream outputStream, byte wireType) throws IOException {
        outputStream.writeRawByte(wireType);
    }

    /**
     * 读取一个tag
     *
     * @param inputStream 输入流
     * @return tag
     * @throws IOException error
     */
    static byte readTag(CodedInputStream inputStream) throws IOException {
        return inputStream.readRawByte();
    }

    // ------------------------------------------------- 工厂方法 ------------------------------------------------------

    public static BinaryProtocolCodec newInstance(MessageMappingStrategy mappingStrategy) {
        return newInstance(mappingStrategy, c -> true);
    }

    /**
     * @param filter 由于{@link BinaryProtocolCodec}支持的消息类是确定的，不能加入，但是允许过滤删除
     */
    public static BinaryProtocolCodec newInstance(MessageMappingStrategy mappingStrategy, Predicate<Class<?>> filter) {
        final Set<Class<?>> supportedClassSet = getFilteredSupportedClasses(filter);
        final MessageMapper messageMapper = MessageMapper.newInstance(supportedClassSet, mappingStrategy);

        final Map<Class<?>, Parser<?>> parserMap = new IdentityHashMap<>();
        final Map<Class<?>, ProtoEnumCodec.ProtoEnumDescriptor> protoEnumDescriptorMap = new IdentityHashMap<>();
        final Map<Class<?>, EntitySerializer<?>> beanSerializerMap = new IdentityHashMap<>();
        try {
            for (Class<?> messageClazz : messageMapper.getAllMessageClasses()) {
                // protoBuf消息
                if (Message.class.isAssignableFrom(messageClazz)) {
                    @SuppressWarnings("unchecked")
                    Parser<?> parser = ProtoUtils.findParser((Class<? extends Message>) messageClazz);
                    parserMap.put(messageClazz, parser);
                    continue;
                }

                // protoBufEnum
                if (ProtocolMessageEnum.class.isAssignableFrom(messageClazz)) {
                    @SuppressWarnings("unchecked") final Internal.EnumLiteMap<?> mapper = ProtoUtils.findMapper((Class<? extends ProtocolMessageEnum>) messageClazz);
                    protoEnumDescriptorMap.put(messageClazz, new ProtoEnumCodec.ProtoEnumDescriptor(mapper));
                    continue;
                }

                // 带有DBEntity和SerializableClass注解的所有类，和手写Serializer的类
                final Class<? extends EntitySerializer<?>> serializerClass = EntitySerializerScanner.getSerializerClass(messageClazz);
                if (serializerClass != null) {
                    final EntitySerializer<?> serializer = createSerializerInstance(serializerClass);
                    beanSerializerMap.put(messageClazz, serializer);
                }
            }
            return new BinaryProtocolCodec(messageMapper, parserMap, protoEnumDescriptorMap, beanSerializerMap);
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
