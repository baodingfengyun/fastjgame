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
import com.wjybxx.fastjgame.net.serialization.JsonSerializer;
import com.wjybxx.fastjgame.net.serialization.MessageMapper;
import com.wjybxx.fastjgame.net.serialization.MessageMappingStrategy;
import com.wjybxx.fastjgame.net.serialization.Serializer;
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

    private static final ThreadLocal<byte[]> LOCAL_BUFFER = ThreadLocal.withInitial(() -> new byte[NetUtils.MAX_BUFFER_SIZE]);

    private final CodecRegistry codecRegistry;

    public BinarySerializer(CodecRegistry codecRegistry) {
        this.codecRegistry = codecRegistry;
    }

    @Nonnull
    @Override
    public ByteBuf writeObject(ByteBufAllocator bufAllocator, @Nullable Object object) throws Exception {
        // 这里的测试结果是：拷贝字节数组，比先计算一次大小，再写入ByteBuf快，而且快很多。
        // 此外，即使使用输入输出流构造，其内部还是做了缓存(创建了字节数组)，因此一定要有自己的缓冲字节数组
        final byte[] localBuffer = LOCAL_BUFFER.get();

        // 写入字节数组缓存
        final CodedDataOutputStream outputStream = new CodedDataOutputStream(localBuffer);
        encodeObject(outputStream, object, codecRegistry);

        // 写入byteBuf
        final ByteBuf buffer = bufAllocator.buffer(outputStream.writeIndex());
        buffer.writeBytes(localBuffer, 0, outputStream.writeIndex());
        return buffer;
    }

    @Override
    public Object readObject(ByteBuf data) throws Exception {
        final byte[] localBuffer = LOCAL_BUFFER.get();

        // 读入缓存数组
        final int readableBytes = data.readableBytes();
        data.readBytes(localBuffer, 0, readableBytes);

        // 解析对象
        final CodedDataInputStream inputStream = new CodedDataInputStream(localBuffer, 0, readableBytes);
        return decodeObject(inputStream, codecRegistry);
    }

    @Nonnull
    @Override
    public byte[] toBytes(@Nullable Object object) throws Exception {
        // 这里测试也是拷贝字节数组快于先计算大小（两轮反射）
        final byte[] localBuffer = LOCAL_BUFFER.get();
        final CodedDataOutputStream outputStream = new CodedDataOutputStream(localBuffer);
        encodeObject(outputStream, object, codecRegistry);

        // 拷贝序列化结果
        final byte[] resultBytes = new byte[outputStream.writeIndex()];
        System.arraycopy(localBuffer, 0, resultBytes, 0, resultBytes.length);
        return resultBytes;
    }

    @Override
    public Object fromBytes(@Nonnull byte[] data) throws Exception {
        return decodeObject(new CodedDataInputStream(data), codecRegistry);
    }

    @Override
    public Object cloneObject(@Nullable Object object) throws Exception {
        if (object == null) {
            return null;
        }
        final byte[] localBuffer = LOCAL_BUFFER.get();

        // 写入缓冲区
        final CodedDataOutputStream outputStream = new CodedDataOutputStream(localBuffer);
        encodeObject(outputStream, object, codecRegistry);

        // 读出
        final CodedDataInputStream inputStream = new CodedDataInputStream(localBuffer, 0, outputStream.writeIndex());
        return decodeObject(inputStream, codecRegistry);
    }

    // ------------------------------------------- tag相关 ------------------------------------

    static <T> void encodeObject(DataOutputStream outputStream, @Nullable T value, CodecRegistry codecRegistry) throws Exception {
        if (null == value) {
            outputStream.writeTag(Tag.NULL);
        } else {
            @SuppressWarnings("unchecked") final Codec<T> codec = (Codec<T>) codecRegistry.get(value.getClass());
            codec.encode(outputStream, value, codecRegistry);
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T decodeObject(DataInputStream inputStream, CodecRegistry codecRegistry) throws Exception {
        final Tag tag = inputStream.readTag();
        return (T) decodeObjectImp(tag, inputStream, codecRegistry);
    }

    static Object decodeObjectImp(Tag tag, DataInputStream inputStream, CodecRegistry codecRegistry) throws Exception {
        switch (tag) {
            case NULL:
                return null;

            case BYTE:
                return inputStream.readByte();
            case CHAR:
                return inputStream.readChar();
            case SHORT:
                return inputStream.readShort();
            case INT:
                return inputStream.readInt();
            case BOOLEAN:
                return inputStream.readBoolean();
            case LONG:
                return inputStream.readLong();
            case FLOAT:
                return inputStream.readFloat();
            case DOUBLE:
                return inputStream.readDouble();
            case STRING:
                return inputStream.readString();

            case POJO:
                return PojoCodec.getPojoCodec(inputStream, codecRegistry).decode(inputStream, codecRegistry);

            case ARRAY:
                return codecRegistry.getArrayCodec().decode(inputStream, codecRegistry);
            case MAP:
                return codecRegistry.getMapCodec().decode(inputStream, codecRegistry);
            case COLLECTION:
                return codecRegistry.getCollectionCodec().decode(inputStream, codecRegistry);

            default:
                throw new IOException("unexpected tag : " + tag);
        }
    }

    // ------------------------------------------------- 工厂方法 ------------------------------------------------------

    public static BinarySerializer newInstance(MessageMappingStrategy mappingStrategy) {
        return newInstance(mappingStrategy, c -> true);
    }

    /**
     * @param mappingStrategy 未来会改为不同的消息来源使用不同的映射策略，以减少冲突。
     * @param filter          由于{@link BinarySerializer}支持的消息类是确定的，不能加入，但是允许过滤删除
     */
    @SuppressWarnings("unchecked")
    public static BinarySerializer newInstance(MessageMappingStrategy mappingStrategy, Predicate<Class<?>> filter) {
        final Set<Class<?>> supportedClassSet = getFilteredSupportedClasses(filter);
        final MessageMapper messageMapper = MessageMapper.newInstance(supportedClassSet, mappingStrategy);
        final List<PojoCodec<?>> codecList = new ArrayList<>(supportedClassSet.size());
        final byte providerId = 11;

        try {
            for (Class<?> messageClazz : messageMapper.getAllMessageClasses()) {
                // protoBuf消息
                if (Message.class.isAssignableFrom(messageClazz)) {
                    Parser<?> parser = ProtoUtils.findParser((Class<? extends Message>) messageClazz);
                    codecList.add(new ProtoMessageCodec(providerId, messageMapper.getMessageId(messageClazz), messageClazz, parser));
                    continue;
                }

                // protoBufEnum
                if (ProtocolMessageEnum.class.isAssignableFrom(messageClazz)) {
                    final Internal.EnumLiteMap<?> mapper = ProtoUtils.findMapper((Class<? extends ProtocolMessageEnum>) messageClazz);
                    codecList.add(new ProtoEnumCodec(providerId, messageMapper.getMessageId(messageClazz), messageClazz, mapper));
                    continue;
                }

                // 带有DBEntity和SerializableClass注解的所有类，和手写Serializer的类
                final Class<? extends EntitySerializer<?>> serializerClass = EntitySerializerScanner.getSerializerClass(messageClazz);
                if (serializerClass != null) {
                    final EntitySerializer<?> serializer = createSerializerInstance(serializerClass);
                    codecList.add(new EntitySerializerBasedCodec(providerId, messageMapper.getMessageId(messageClazz), serializer));
                    continue;
                }

                throw new IllegalArgumentException("Unsupported class " + messageClazz.getName());
            }

            final CodecRegistry codecRegistry = CodecRegistrys.fromAppPojoCodecs(codecList);
            return new BinarySerializer(codecRegistry);
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
