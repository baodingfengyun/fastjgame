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

import com.google.protobuf.*;
import com.wjybxx.fastjgame.net.misc.BufferPool;
import com.wjybxx.fastjgame.net.serialization.*;
import com.wjybxx.fastjgame.net.utils.ProtoUtils;
import com.wjybxx.fastjgame.util.CollectionUtils;
import io.netty.buffer.ByteBuf;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

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
    final TypeIdMapper typeIdMapper;
    final Map<Class<?>, Supplier<? extends Collection<?>>> collectionFactoryMap;
    final Map<Class<?>, Supplier<? extends Map<?, ?>>> mapFactoryMap;

    final Map<Class<?>, Parser<? extends MessageLite>> parserMap;
    final Map<Class<?>, Internal.EnumLiteMap<? extends ProtocolMessageEnum>> protocolEnumMap;

    private final int defaultByteBufCapacity;

    private BinarySerializer(TypeIdMapper typeIdMapper, CodecRegistry codecRegistry,
                             Map<Class<?>, Supplier<? extends Collection<?>>> collectionFactoryMap,
                             Map<Class<?>, Supplier<? extends Map<?, ?>>> mapFactoryMap,
                             Map<Class<?>, Parser<? extends MessageLite>> parserMap,
                             Map<Class<?>, Internal.EnumLiteMap<? extends ProtocolMessageEnum>> protocolEnumMap) {
        this(typeIdMapper, codecRegistry, collectionFactoryMap, mapFactoryMap, parserMap, protocolEnumMap, 256);
    }

    public BinarySerializer(TypeIdMapper typeIdMapper, CodecRegistry codecRegistry,
                            Map<Class<?>, Supplier<? extends Collection<?>>> collectionFactoryMap,
                            Map<Class<?>, Supplier<? extends Map<?, ?>>> mapFactoryMap,
                            Map<Class<?>, Parser<? extends MessageLite>> parserMap,
                            Map<Class<?>, Internal.EnumLiteMap<? extends ProtocolMessageEnum>> protocolEnumMap,
                            int defaultByteBufCapacity) {
        this.codecRegistry = codecRegistry;
        this.typeIdMapper = typeIdMapper;
        this.parserMap = parserMap;
        this.protocolEnumMap = protocolEnumMap;
        this.collectionFactoryMap = collectionFactoryMap;
        this.mapFactoryMap = mapFactoryMap;
        this.defaultByteBufCapacity = defaultByteBufCapacity;
    }

    @Override
    public int estimateSerializedSize(@Nullable Object object) {
        if (object == null) {
            return 1;
        }

        if (object instanceof MessageLite) {
            // 对protoBuf协议的优化
            // tag + length + typeId + content
            return 1 + 4 + 5 + ((MessageLite) object).getSerializedSize();
        }

        if (object instanceof byte[]) {
            // tag  + length + content
            return 1 + 4 + ((byte[]) object).length;
        }

        return defaultByteBufCapacity;
    }

    @Override
    public void writeObject(ByteBuf byteBuf, @Nullable Object object) throws Exception {
        final CodedDataOutputStream outputStream = CodedDataOutputStream.newInstance(byteBuf);
        encodeObject(outputStream, object);
    }

    @Override
    public Object readObject(ByteBuf data) throws Exception {
        if (data.nioBufferCount() == 1) {
            final CodedDataInputStream codedDataInputStream = CodedDataInputStream.newInstance(data);
            return decodeObject(codedDataInputStream);
        } else {
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
    }

    @Nonnull
    @Override
    public byte[] toBytes(@Nullable Object object) throws Exception {
        final byte[] localBuffer = BufferPool.allocateBuffer();
        try {
            final CodedDataOutputStream outputStream = CodedDataOutputStream.newInstance(localBuffer);
            encodeObject(outputStream, object);

            // 拷贝序列化结果
            final byte[] resultBytes = new byte[outputStream.getTotalBytesWritten()];
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
            final CodedDataInputStream inputStream = CodedDataInputStream.newInstance(localBuffer, 0, outputStream.getTotalBytesWritten());
            return decodeObject(inputStream);
        } finally {
            BufferPool.releaseBuffer(localBuffer);
        }
    }

    private void encodeObject(CodedDataOutputStream outputStream, @Nullable Object value) throws Exception {
        final ObjectWriter writer = new ObjectWriterImpl(this, codecRegistry, outputStream);
        writer.writeObject(value);
        writer.flush();
    }

    private Object decodeObject(CodedDataInputStream inputStream) throws Exception {
        final ObjectReader reader = new ObjectReaderImpl(this, codecRegistry, inputStream);
        return reader.readObject();
    }

    // ------------------------------------------------- 工厂方法 ------------------------------------------------------

    public static BinarySerializer newInstance(final TypeIdMappingStrategy typeIdMappingStrategy,
                                               final CollectionScanner.ScanResult scanResult) {
        return newInstance(typeIdMappingStrategy,
                scanResult.collectionFactories,
                scanResult.mapFactories,
                scanResult.arrayTypes);
    }

    /**
     * @param collectionFactories 所有精确解析的集合
     * @param mapFactories        所有支精确解析的map
     * @param arrayTypes          所有精确解析的数组
     */
    @SuppressWarnings("unchecked")
    public static BinarySerializer newInstance(final TypeIdMappingStrategy typeIdMappingStrategy,
                                               final Collection<Supplier<? extends Collection<?>>> collectionFactories,
                                               final Collection<Supplier<? extends Map<?, ?>>> mapFactories,
                                               final Collection<Class<?>> arrayTypes) {
        final Map<Class<?>, Supplier<? extends Collection<?>>> collectionFactoryMap = indexCollectionFactories(collectionFactories);
        final Map<Class<?>, Supplier<? extends Map<?, ?>>> mapFactoryMap = indexMapFactories(mapFactories);

        // protoBuf支持
        final Set<Class<?>> allProtoBufferClasses = ProtoBufScanner.scan();
        final Map<Class<?>, Parser<? extends MessageLite>> parserMap = new IdentityHashMap<>();
        final Map<Class<?>, Internal.EnumLiteMap<? extends ProtocolMessageEnum>> protocolEnumMap = new IdentityHashMap<>();

        for (Class<?> messageClazz : allProtoBufferClasses) {
            // protoBuf消息
            if (Message.class.isAssignableFrom(messageClazz)) {
                Parser<? extends MessageLite> parser = ProtoUtils.findParser((Class<? extends Message>) messageClazz);
                parserMap.put(messageClazz, parser);
                continue;
            }
            // protoBufEnum
            if (ProtocolMessageEnum.class.isAssignableFrom(messageClazz)) {
                final Internal.EnumLiteMap<? extends ProtocolMessageEnum> mapper = ProtoUtils.findMapper((Class<? extends ProtocolMessageEnum>) messageClazz);
                protocolEnumMap.put(messageClazz, mapper);
                continue;
            }
            throw new IllegalArgumentException("Unsupported class " + messageClazz.getName());
        }

        try {
            // 定义有PojoCodec的类
            final Map<Class<?>, PojoCodec<?>> pojoCodecMap = indexPojoCodcMap();

            final Set<Class<?>> allClass = CollectionUtils.newHashSetWithExpectedSize(collectionFactoryMap.size() + mapFactoryMap.size() + arrayTypes.size()
                    + allProtoBufferClasses.size() + pojoCodecMap.size());

            allClass.addAll(collectionFactoryMap.keySet());
            allClass.addAll(mapFactoryMap.keySet());
            allClass.addAll(arrayTypes);
            allClass.addAll(allProtoBufferClasses);
            allClass.addAll(pojoCodecMap.keySet());

            final DefaultTypeIdMapper typeIdMapper = DefaultTypeIdMapper.newInstance(allClass, typeIdMappingStrategy);
            final CodecRegistry codecRegistry = CodecRegistries.fromPojoCodecs(pojoCodecMap);

            return new BinarySerializer(typeIdMapper, codecRegistry, collectionFactoryMap, mapFactoryMap, parserMap, protocolEnumMap);
        } catch (Exception e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    private static Map<Class<?>, Supplier<? extends Collection<?>>> indexCollectionFactories(Collection<Supplier<? extends Collection<?>>> collectionFactories) {
        final Map<Class<?>, Supplier<? extends Collection<?>>> result = new IdentityHashMap<>(collectionFactories.size());
        for (Supplier<? extends Collection<?>> supplier : collectionFactories) {
            // 创建空的集合实现索引
            final Class<? extends Collection> type = supplier.get().getClass();
            CollectionUtils.requireNotContains(result, type, type.getName());
            result.put(type, supplier);
        }
        return result;
    }

    private static Map<Class<?>, Supplier<? extends Map<?, ?>>> indexMapFactories(Collection<Supplier<? extends Map<?, ?>>> mapFactories) {
        final Map<Class<?>, Supplier<? extends Map<?, ?>>> result = new IdentityHashMap<>(mapFactories.size());
        for (Supplier<? extends Map<?, ?>> supplier : mapFactories) {
            // 创建空的Map实现索引
            final Class<? extends Map> type = supplier.get().getClass();
            CollectionUtils.requireNotContains(result, type, type.getName());
            result.put(type, supplier);
        }
        return result;
    }

    private static Map<Class<?>, PojoCodec<?>> indexPojoCodcMap() throws Exception {
        final Map<Class<?>, Class<? extends PojoCodecImpl<?>>> scanResult = CodecScanner.scan();
        final Map<Class<?>, PojoCodec<?>> pojoCodecMap = new IdentityHashMap<>(scanResult.size());
        for (Map.Entry<Class<?>, Class<? extends PojoCodecImpl<?>>> entry : scanResult.entrySet()) {
            final Class<?> type = entry.getKey();
            CollectionUtils.requireNotContains(pojoCodecMap, type, type.getName());

            final Class<? extends PojoCodecImpl<?>> codecClass = entry.getValue();
            final PojoCodecImpl<?> codec = createCodecInstance(codecClass);

            pojoCodecMap.put(type, new PojoCodec<>(codec));
        }
        return pojoCodecMap;
    }

    private static PojoCodecImpl<?> createCodecInstance(Class<? extends PojoCodecImpl<?>> codecClass) throws Exception {
        final Constructor<? extends PojoCodecImpl<?>> noArgsConstructor = codecClass.getDeclaredConstructor(ArrayUtils.EMPTY_CLASS_ARRAY);
        noArgsConstructor.setAccessible(true);
        return noArgsConstructor.newInstance(ArrayUtils.EMPTY_OBJECT_ARRAY);
    }

}
