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
import com.wjybxx.fastjgame.net.common.ProtocolCodec;
import com.wjybxx.fastjgame.net.misc.JsonProtocolCodec;
import com.wjybxx.fastjgame.net.misc.MessageMapper;
import com.wjybxx.fastjgame.net.utils.NetUtils;
import com.wjybxx.fastjgame.net.utils.ProtoUtils;
import com.wjybxx.fastjgame.utils.EnumUtils;
import com.wjybxx.fastjgame.utils.entity.NumericalEntityMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.IntFunction;

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
        codecMapper = EnumUtils.mapping(initValues(messageMapper, parserMap, protoEnumDescriptorMap, beanSerializerMap));
    }

    private BinaryCodec<?>[] initValues(MessageMapper messageMapper,
                                        Map<Class<?>, Parser<?>> parserMap,
                                        Map<Class<?>, ProtoEnumCodec.ProtoEnumDescriptor> protoEnumDescriptorMap,
                                        Map<Class<?>, EntitySerializer<?>> beanSerializerMap) {
        // 预估出现的频率排个序
        return new BinaryCodec[]{
                new IntegerCodec(),
                new LongCodec(),
                new FloatCodec(),
                new DoubleCodec(),
                new StringCodec(),

                new ProtoMessageCodec(messageMapper, parserMap),
                new ProtoEnumCodec(messageMapper, protoEnumDescriptorMap),
                new ChunkCodec(),

                // 带有Serializer的类
                new CustomEntityCodec(messageMapper, beanSerializerMap, this),

                // 默认集合支持
                new DefaultMapCodec(this),
                new DefaultCollectionCodec(this),

                // 字节数组比较常见
                new ByteArrayCodec(),

                new BooleanCodec(),
                new ByteCodec(),
                new ShortCodec(),
                new CharCodec(),

                // 其它数组使用较少
                new IntegerArrayCodec(),
                new LongArrayCodec(),
                new FloatArrayCodec(),
                new DoubleArrayCodec(),
                new CharArrayCodec(),
                new ShortArrayCodec(),
                new BooleanArrayCodec()
        };
    }

    @Nonnull
    @Override
    public ByteBuf writeObject(ByteBufAllocator bufAllocator, @Nullable Object object) throws IOException {
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

    private void writeObject(CodedOutputStream outputStream, @Nullable Object object) throws IOException {
        if (object == null) {
            writeTag(outputStream, WireType.NULL);
            return;
        }
        writeRuntimeType(outputStream, object);
    }

    @SuppressWarnings("unchecked")
    void writeRuntimeType(CodedOutputStream outputStream, @Nonnull Object object) throws IOException {
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
    public Object readObject(ByteBuf data) throws IOException {
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
    private Object readObject(CodedInputStream inputStream) throws IOException {
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
    public byte[] serializeToBytes(@Nullable Object obj) throws IOException {
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
    public Object deserializeFromBytes(@Nonnull byte[] data) throws IOException {
        return readObject(CodedInputStream.newInstance(data));
    }

    @Nullable
    @Override
    public Object cloneObject(@Nullable Object obj) throws IOException {
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

    // ------------------------------------------- map相关 ------------------------------------

    /**
     * 将map的所有键值对写入输出流
     */
    <K, V> void writeMapImp(@Nonnull CodedOutputStream outputStream, @Nonnull Map<K, V> map) throws IOException {
        outputStream.writeUInt32NoTag(map.size());
        if (map.size() == 0) {
            return;
        }
        for (Map.Entry<K, V> entry : map.entrySet()) {
            writeObject(outputStream, entry.getKey());
            writeObject(outputStream, entry.getValue());
        }
    }

    /**
     * 从输入流中读取指定个数元素到map中
     */
    @Nonnull
    <M extends Map<K, V>, K, V> M readMapImp(@Nonnull CodedInputStream inputStream, @Nonnull IntFunction<M> mapFactory) throws IOException {
        final int size = inputStream.readUInt32();
        if (size == 0) {
            return mapFactory.apply(0);
        }
        final M result = mapFactory.apply(size);
        for (int index = 0; index < size; index++) {
            @SuppressWarnings("unchecked") K key = (K) readObject(inputStream);
            @SuppressWarnings("unchecked") V value = (V) readObject(inputStream);
            result.put(key, value);
        }
        return result;
    }

    // ------------------------------------------- collection相关 ------------------------------------

    /**
     * 将collection的所有元素写入输出流
     */
    <E> void writeCollectionImp(@Nonnull CodedOutputStream outputStream, @Nonnull Collection<E> collection) throws IOException {
        outputStream.writeUInt32NoTag(collection.size());
        if (collection.size() == 0) {
            return;
        }
        for (E element : collection) {
            BinaryProtocolCodec.this.writeObject(outputStream, element);
        }
    }

    /**
     * 从输入流中读取指定元素到集合中
     */
    @Nonnull
    <C extends Collection<E>, E> C readCollectionImp(@Nonnull CodedInputStream inputStream, @Nonnull IntFunction<C> collectionFactory) throws IOException {
        final int size = inputStream.readUInt32();
        if (size == 0) {
            return collectionFactory.apply(0);
        }

        final C result = collectionFactory.apply(size);
        for (int index = 0; index < size; index++) {
            @SuppressWarnings("unchecked") final E e = (E) readObject(inputStream);
            result.add(e);
        }
        return result;
    }

    // ------------------------------------------------- 工厂方法 ------------------------------------------------------
    @Nonnull
    public static BinaryProtocolCodec newInstance(MessageMapper messageMapper) {
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
                final Class<? extends EntitySerializer<?>> serializerClass = WireType.getBeanSerializer(messageClazz);
                if (serializerClass != null) {
                    final Constructor<? extends EntitySerializer<?>> noArgsConstructor = serializerClass.getDeclaredConstructor();
                    noArgsConstructor.setAccessible(true);
                    beanSerializerMap.put(messageClazz, noArgsConstructor.newInstance());
                    continue;
                }

                // 其它不带注解的一律不支持
                throw new RuntimeException("Unsupported message " + messageClazz.getName());

            }
            return new BinaryProtocolCodec(messageMapper, parserMap, protoEnumDescriptorMap, beanSerializerMap);
        } catch (Exception e) {
            return ExceptionUtils.rethrow(e);
        }
    }

}
