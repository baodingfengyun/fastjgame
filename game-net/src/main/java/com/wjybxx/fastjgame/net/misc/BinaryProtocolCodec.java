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

import com.google.protobuf.*;
import com.wjybxx.fastjgame.net.annotation.SerializableClass;
import com.wjybxx.fastjgame.net.common.ProtocolCodec;
import com.wjybxx.fastjgame.net.serializer.EntityInputStream;
import com.wjybxx.fastjgame.net.serializer.EntityOutputStream;
import com.wjybxx.fastjgame.net.serializer.EntitySerializer;
import com.wjybxx.fastjgame.net.utils.NetUtils;
import com.wjybxx.fastjgame.net.utils.ProtoUtils;
import com.wjybxx.fastjgame.utils.EnumUtils;
import com.wjybxx.fastjgame.utils.entity.IndexableEntity;
import com.wjybxx.fastjgame.utils.entity.NumericalEntity;
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
import java.util.function.Function;
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

    private final MessageMapper messageMapper;
    /**
     * proto message 解析方法
     */
    private final Map<Class<?>, Parser<?>> parserMap;
    /**
     * proto enum 解析方法
     */
    private final Map<Class<?>, ProtoEnumDescriptor> protoEnumDescriptorMap;

    /**
     * {@link NumericalEntity}类型解析器
     */
    private final Map<Class<?>, IntFunction<? extends NumericalEntity>> forNumberMethodMap;
    /**
     * {@link IndexableEntity}类型解析器
     */
    private final Map<Class<?>, Function<?, ? extends IndexableEntity<?>>> forIndexMethodMap;
    /**
     * 其它普通类型bean
     */
    private final Map<Class<?>, EntitySerializer<?>> beanSerializerMap;
    /**
     * 所有codec映射
     */
    private final NumericalEntityMapper<Codec<?>> codecMapper;

    private BinaryProtocolCodec(MessageMapper messageMapper,
                                Map<Class<?>, Parser<?>> parserMap,
                                Map<Class<?>, ProtoEnumDescriptor> protoEnumDescriptorMap,
                                Map<Class<?>, IntFunction<? extends NumericalEntity>> forNumberMethodMap,
                                Map<Class<?>, Function<?, ? extends IndexableEntity<?>>> forIndexMethodMap,
                                Map<Class<?>, EntitySerializer<?>> beanSerializerMap
    ) {
        this.messageMapper = messageMapper;
        this.parserMap = parserMap;
        this.protoEnumDescriptorMap = protoEnumDescriptorMap;
        this.forNumberMethodMap = forNumberMethodMap;
        this.forIndexMethodMap = forIndexMethodMap;
        this.beanSerializerMap = beanSerializerMap;

        codecMapper = EnumUtils.mapping(initValues());
    }

    private Codec<?>[] initValues() {
        // 预估出现的频率排个序
        return new Codec[]{
                new IntegerCodec(),
                new LongCodec(),
                new FloatCodec(),
                new DoubleCodec(),
                new StringCodec(),

                new MessageCodec(),
                new ProtoEnumCodec(),
                new ChunkCodec(),

                new NormalBeanCodec(),

                // 字节数组比较常见
                new ByteArrayCodec(),

                new BoolCodec(),
                new ByteCodec(),
                new ShortCodec(),
                new CharCodec(),

                // 其它数组使用较少
                new IntArrayCodec(),
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
        final byte[] localBuffer = LOCAL_BUFFER.get();
        // 减少字节数组创建，即使使用输出流构造，其内部还是做了缓存。
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(localBuffer);
        writeObject(codedOutputStream, object);
        // 真正写入到byteBuf
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
    private void writeRuntimeType(CodedOutputStream outputStream, @Nonnull Object object) throws IOException {
        final Class<?> type = object.getClass();
        for (Codec codec : codecMapper.values()) {
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
        final int readableBytes = data.readableBytes();
        data.readBytes(localBuffer, 0, readableBytes);
        // 减少字节数组创建，即使使用输入流构造，其内部还是做了缓存(创建了字节数组)
        CodedInputStream codedInputStream = CodedInputStream.newInstance(localBuffer, 0, readableBytes);
        // 真正读取数据
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
    private Codec getCodec(int wireType) throws IOException {
        Codec codec = codecMapper.forNumber(wireType);
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

        final byte[] resultBytes = new byte[codedOutputStream.getTotalBytesWritten()];
        System.arraycopy(localBuffer, 0, resultBytes, 0, resultBytes.length);
        return resultBytes;
    }

    @Override
    public Object deserializeFromBytes(@Nonnull byte[] data) throws IOException {
        return readObject(CodedInputStream.newInstance(data));
    }

    // ------------------------------------------- tag相关 ------------------------------------

    /**
     * 写入一个tag
     *
     * @param outputStream 输出流
     * @param wireType     tag
     * @throws IOException error
     */
    private static void writeTag(CodedOutputStream outputStream, byte wireType) throws IOException {
        outputStream.writeRawByte(wireType);
    }

    /**
     * 读取一个tag
     *
     * @param inputStream 输入流
     * @return tag
     * @throws IOException error
     */
    private static byte readTag(CodedInputStream inputStream) throws IOException {
        return inputStream.readRawByte();
    }

    /**
     * protobuf枚举描述符
     */
    private static class ProtoEnumDescriptor {

        private final Internal.EnumLiteMap<?> mapper;

        private ProtoEnumDescriptor(Internal.EnumLiteMap<?> mapper) {
            this.mapper = mapper;
        }
    }

    private interface Codec<T> extends NumericalEntity {

        /**
         * 是否支持编码该对象
         *
         * @param type 数据类型
         * @return 如果支持则返回true
         */
        boolean isSupport(Class<?> type);

        /**
         * 编码协议内容，不包含wireType
         *
         * @param outputStream 输出流
         * @param obj          待编码的对象
         * @throws IOException error，
         */
        void writeData(CodedOutputStream outputStream, @Nonnull T obj) throws IOException;

        /**
         * 解码字段协议内容，不包含wireType
         *
         * @param inputStream 输入流
         * @return data
         * @throws IOException error
         */
        T readData(CodedInputStream inputStream) throws IOException;

        @Override
        default int getNumber() {
            return getWireType();
        }

        /**
         * 该codec对应的类型
         *
         * @return wireType
         */
        byte getWireType();

    }

    /**
     * 一个字节就占用一个字节 - 不需要使用int32的格式
     */
    private static class ByteCodec implements Codec<Byte> {

        @Override
        public boolean isSupport(Class<?> type) {
            return type == Byte.class;
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull Byte obj) throws IOException {
            outputStream.writeRawByte(obj);
        }

        @Override
        public Byte readData(CodedInputStream inputStream) throws IOException {
            return inputStream.readRawByte();
        }

        @Override
        public byte getWireType() {
            return WireType.BYTE;
        }

    }

    private static abstract class Int32Codec<T extends Number> implements Codec<T> {

        @Override
        public final void writeData(CodedOutputStream outputStream, @Nonnull T obj) throws IOException {
            outputStream.writeInt32NoTag(obj.intValue());
        }

    }

    private static class IntegerCodec extends Int32Codec<Integer> {

        @Override
        public boolean isSupport(Class<?> type) {
            return type == Integer.class;
        }

        @Override
        public Integer readData(CodedInputStream inputStream) throws IOException {
            return inputStream.readInt32();
        }

        @Override
        public byte getWireType() {
            return WireType.INT;
        }

    }

    private static class ShortCodec extends Int32Codec<Short> {

        @Override
        public boolean isSupport(Class<?> type) {
            return type == Short.class;
        }

        @Override
        public Short readData(CodedInputStream inputStream) throws IOException {
            return (short) inputStream.readInt32();
        }

        @Override
        public byte getWireType() {
            return WireType.SHORT;
        }

    }

    private static class CharCodec implements Codec<Character> {

        @Override
        public boolean isSupport(Class<?> type) {
            return type == Character.class;
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull Character obj) throws IOException {
            outputStream.writeUInt32NoTag(obj);
        }

        @Override
        public Character readData(CodedInputStream inputStream) throws IOException {
            return (char) inputStream.readUInt32();
        }

        @Override
        public byte getWireType() {
            return WireType.CHAR;
        }

    }

    private static class LongCodec implements Codec<Long> {

        @Override
        public boolean isSupport(Class<?> type) {
            return type == Long.class;
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull Long obj) throws IOException {
            outputStream.writeInt64NoTag(obj);
        }

        @Override
        public Long readData(CodedInputStream inputStream) throws IOException {
            return inputStream.readInt64();
        }

        @Override
        public byte getWireType() {
            return WireType.LONG;
        }

    }

    private static class FloatCodec implements Codec<Float> {

        @Override
        public boolean isSupport(Class<?> type) {
            return type == Float.class;
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull Float obj) throws IOException {
            outputStream.writeFloatNoTag(obj);
        }

        @Override
        public Float readData(CodedInputStream inputStream) throws IOException {
            return inputStream.readFloat();
        }

        @Override
        public byte getWireType() {
            return WireType.FLOAT;
        }

    }

    private static class DoubleCodec implements Codec<Double> {

        @Override
        public boolean isSupport(Class<?> type) {
            return type == Double.class;
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull Double obj) throws IOException {
            outputStream.writeDoubleNoTag(obj);
        }

        @Override
        public Double readData(CodedInputStream inputStream) throws IOException {
            return inputStream.readDouble();
        }

        @Override
        public byte getWireType() {
            return WireType.DOUBLE;
        }

    }

    private static class BoolCodec implements Codec<Boolean> {

        @Override
        public boolean isSupport(Class<?> type) {
            return type == Boolean.class;
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull Boolean obj) throws IOException {
            outputStream.writeBoolNoTag(obj);
        }

        @Override
        public Boolean readData(CodedInputStream inputStream) throws IOException {
            return inputStream.readBool();
        }

        @Override
        public byte getWireType() {
            return WireType.BOOLEAN;
        }

    }

    private static class StringCodec implements Codec<String> {

        @Override
        public boolean isSupport(Class<?> type) {
            return String.class == type;
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull String obj) throws IOException {
            outputStream.writeStringNoTag(obj);
        }

        @Override
        public String readData(CodedInputStream inputStream) throws IOException {
            return inputStream.readString();
        }

        @Override
        public byte getWireType() {
            return WireType.STRING;
        }

    }

    // protoBuf消息编解码支持
    // messageId 使用大端模式写入，和json序列化方式一致，也方便客户端解析
    private class MessageCodec implements Codec<AbstractMessage> {

        @Override
        public boolean isSupport(Class<?> type) {
            return AbstractMessage.class.isAssignableFrom(type);
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull AbstractMessage obj) throws IOException {
            int messageId = messageMapper.getMessageId(obj.getClass());

            // 大端模式写入一个int
            outputStream.writeRawByte((messageId >>> 24));
            outputStream.writeRawByte((messageId >>> 16));
            outputStream.writeRawByte((messageId >>> 8));
            outputStream.writeRawByte(messageId);

            outputStream.writeMessageNoTag(obj);
        }

        @SuppressWarnings("unchecked")
        @Override
        public AbstractMessage readData(CodedInputStream inputStream) throws IOException {
            // 大端模式读取一个int
            final int messageId = (inputStream.readRawByte() & 0xFF) << 24
                    | (inputStream.readRawByte() & 0xFF) << 16
                    | (inputStream.readRawByte() & 0xFF) << 8
                    | inputStream.readRawByte() & 0xFF;

            final Class<?> messageClazz = messageMapper.getMessageClazz(messageId);
            final Parser<AbstractMessage> parser = (Parser<AbstractMessage>) parserMap.get(messageClazz);
            return inputStream.readMessage(parser, ExtensionRegistryLite.getEmptyRegistry());
        }

        @Override
        public byte getWireType() {
            return WireType.PROTO_MESSAGE;
        }

    }


    private class ProtoEnumCodec implements Codec<ProtocolMessageEnum> {

        @Override
        public boolean isSupport(Class<?> type) {
            return ProtocolMessageEnum.class.isAssignableFrom(type);
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull ProtocolMessageEnum obj) throws IOException {
            outputStream.writeSInt32NoTag(messageMapper.getMessageId(obj.getClass()));
            outputStream.writeEnumNoTag(obj.getNumber());
        }

        @Override
        public ProtocolMessageEnum readData(CodedInputStream inputStream) throws IOException {
            final int messageId = inputStream.readSInt32();
            final int number = inputStream.readEnum();
            final Class<?> enumClass = messageMapper.getMessageClazz(messageId);
            try {
                return (ProtocolMessageEnum) protoEnumDescriptorMap.get(enumClass).mapper.findValueByNumber(number);
            } catch (Exception e) {
                throw new IOException(enumClass.getName(), e);
            }
        }

        @Override
        public byte getWireType() {
            return WireType.PROTO_ENUM;
        }

    }

    /**
     * 普通javabean对象 - 通过生成的辅助类进行编解码
     */
    private class NormalBeanCodec implements Codec<Object> {

        @Override
        public boolean isSupport(Class<?> type) {
            return beanSerializerMap.containsKey(type);
        }

        @SuppressWarnings({"unchecked", "rawTypes"})
        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull Object obj) throws IOException {
            final Class<?> messageClass = obj.getClass();
            outputStream.writeSInt32NoTag(messageMapper.getMessageId(messageClass));

            final EntitySerializer entitySerializer = beanSerializerMap.get(messageClass);
            final EntityOutputStreamImp beanOutputStreamImp = new EntityOutputStreamImp(outputStream);

            // 先写入自身定义的字段
            entitySerializer.writeFields(obj, beanOutputStreamImp);

            // 递归写入父类定义字段
            for (Class<?> parent = messageClass.getSuperclass(); parent != Object.class; parent = parent.getSuperclass()) {
                final EntitySerializer parentSerializer = beanSerializerMap.get(parent);
                if (parentSerializer != null) {
                    parentSerializer.writeFields(obj, beanOutputStreamImp);
                }
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object readData(CodedInputStream inputStream) throws IOException {
            final int messageId = inputStream.readSInt32();
            final Class<?> messageClass = messageMapper.getMessageClazz(messageId);

            final EntitySerializer entitySerializer = beanSerializerMap.get(messageClass);
            final Object instance = entitySerializer.newInstance();
            final EntityInputStreamImp beanInputStreamImp = new EntityInputStreamImp(inputStream);

            // 先读取当前类定义的字段
            entitySerializer.readFields(instance, beanInputStreamImp);

            // 递归读取父类字段
            for (Class<?> parent = messageClass.getSuperclass(); parent != Object.class; parent = parent.getSuperclass()) {
                final EntitySerializer parentSerializer = beanSerializerMap.get(parent);
                if (parentSerializer != null) {
                    parentSerializer.readFields(instance, beanInputStreamImp);
                }
            }
            return instance;
        }

        @Override
        public byte getWireType() {
            return WireType.CUSTOM_ENTITY;
        }

    }

    private class EntityOutputStreamImp implements EntityOutputStream {

        private final CodedOutputStream outputStream;

        private EntityOutputStreamImp(CodedOutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public void writeField(byte wireType, @Nullable Object fieldValue) throws IOException {
            writeFieldValue(wireType, outputStream, fieldValue);
        }

        @Override
        public <K, V> void writeMap(@Nonnull Map<K, V> map) throws IOException {
            outputStream.writeUInt32NoTag(map.size());
            if (map.size() == 0) {
                return;
            }
            for (Map.Entry<K, V> entry : map.entrySet()) {
                BinaryProtocolCodec.this.writeObject(outputStream, entry.getKey());
                BinaryProtocolCodec.this.writeObject(outputStream, entry.getValue());
            }
        }

        @Override
        public <E> void writeCollection(@Nonnull Collection<E> collection) throws IOException {
            outputStream.writeUInt32NoTag(collection.size());
            if (collection.size() == 0) {
                return;
            }
            for (E element : collection) {
                BinaryProtocolCodec.this.writeObject(outputStream, element);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void writeFieldValue(byte wireType, @Nonnull CodedOutputStream outputStream, @Nullable Object fieldValue) throws IOException {
        // null也需要写入，因为新对象的属性不一定也是null
        if (fieldValue == null) {
            writeTag(outputStream, WireType.NULL);
            return;
        }

        // 索引为具体类型的字段
        if (wireType != WireType.RUN_TIME) {
            writeTag(outputStream, wireType);
            getCodec(wireType).writeData(outputStream, fieldValue);
            return;
        }

        // 运行时才知道的类型 - 极少走到这里
        writeRuntimeType(outputStream, fieldValue);
    }

    private class EntityInputStreamImp implements EntityInputStream {

        private final CodedInputStream inputStream;

        private EntityInputStreamImp(CodedInputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public <T> T readField(byte wireType) throws IOException {
            final byte tag = readTag(inputStream);
            if (tag == WireType.NULL) {
                return null;
            }

            // 类型校验
            if (wireType != WireType.RUN_TIME && wireType != tag) {
                throw new IOException("Incompatible wireType, expected: " + wireType + ", but read: " + tag);
            }

            @SuppressWarnings("unchecked") final T value = (T) getCodec(tag).readData(inputStream);
            return value;
        }

        @Override
        public <K, V> void readMap(@Nonnull Map<K, V> map) throws IOException {
            int size = inputStream.readUInt32();
            if (size == 0) {
                return;
            }
            for (int index = 0; index < size; index++) {
                @SuppressWarnings("unchecked") K key = (K) BinaryProtocolCodec.this.readObject(inputStream);
                @SuppressWarnings("unchecked") V value = (V) BinaryProtocolCodec.this.readObject(inputStream);
                map.put(key, value);
            }
        }

        @Override
        public <E> void readCollection(@Nonnull Collection<E> collection) throws IOException {
            int size = inputStream.readUInt32();
            if (size == 0) {
                return;
            }
            for (int index = 0; index < size; index++) {
                @SuppressWarnings("unchecked") final E e = (E) BinaryProtocolCodec.this.readObject(inputStream);
                collection.add(e);
            }
        }
    }


    // ------------------------------------------------- 基本数组支持 ---------------------------------------------

    private static class ByteArrayCodec implements Codec<byte[]> {
        @Override
        public boolean isSupport(Class<?> type) {
            return type == byte[].class;
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull byte[] obj) throws IOException {
            outputStream.writeUInt32NoTag(obj.length);
            if (obj.length > 0) {
                outputStream.writeRawBytes(obj);
            }
        }

        @Override
        public byte[] readData(CodedInputStream inputStream) throws IOException {
            final int length = inputStream.readUInt32();
            if (length == 0) {
                return new byte[0];
            }
            return inputStream.readRawBytes(length);
        }

        @Override
        public byte getWireType() {
            return WireType.BYTE_ARRAY;
        }

    }

    private static class CharArrayCodec implements Codec<char[]> {

        @Override
        public boolean isSupport(Class<?> type) {
            return type == char[].class;
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull char[] obj) throws IOException {
            outputStream.writeUInt32NoTag(obj.length);
            if (obj.length == 0) {
                return;
            }
            for (char value : obj) {
                outputStream.writeUInt32NoTag(value);
            }
        }

        @Override
        public char[] readData(CodedInputStream inputStream) throws IOException {
            final int length = inputStream.readUInt32();
            if (length == 0) {
                return new char[0];
            }
            char[] result = new char[length];
            for (int index = 0; index < length; index++) {
                result[index] = (char) inputStream.readUInt32();
            }
            return result;
        }

        @Override
        public byte getWireType() {
            return WireType.CHAR_ARRAY;
        }

    }

    private static class ShortArrayCodec implements Codec<short[]> {

        @Override
        public boolean isSupport(Class<?> type) {
            return type == short[].class;
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull short[] obj) throws IOException {
            outputStream.writeUInt32NoTag(obj.length);
            if (obj.length == 0) {
                return;
            }
            for (short value : obj) {
                outputStream.writeSInt32NoTag(value);
            }
        }

        @Override
        public short[] readData(CodedInputStream inputStream) throws IOException {
            final int length = inputStream.readUInt32();
            if (length == 0) {
                return new short[0];
            }
            short[] result = new short[length];
            for (int index = 0; index < length; index++) {
                result[index] = (short) inputStream.readSInt32();
            }
            return result;
        }

        @Override
        public byte getWireType() {
            return WireType.SHORT_ARRAY;
        }

    }

    private static class IntArrayCodec implements Codec<int[]> {

        @Override
        public boolean isSupport(Class<?> type) {
            return type == int[].class;
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull int[] obj) throws IOException {
            outputStream.writeUInt32NoTag(obj.length);
            if (obj.length == 0) {
                return;
            }
            for (int value : obj) {
                outputStream.writeSInt32NoTag(value);
            }
        }

        @Override
        public int[] readData(CodedInputStream inputStream) throws IOException {
            final int length = inputStream.readUInt32();
            if (length == 0) {
                return new int[0];
            }
            int[] result = new int[length];
            for (int index = 0; index < length; index++) {
                result[index] = inputStream.readSInt32();
            }
            return result;
        }

        @Override
        public byte getWireType() {
            return WireType.INT_ARRAY;
        }

    }

    private static class LongArrayCodec implements Codec<long[]> {
        @Override
        public boolean isSupport(Class<?> type) {
            return type == long[].class;
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull long[] obj) throws IOException {
            outputStream.writeUInt32NoTag(obj.length);
            if (obj.length == 0) {
                return;
            }
            for (long value : obj) {
                outputStream.writeSInt64NoTag(value);
            }
        }

        @Override
        public long[] readData(CodedInputStream inputStream) throws IOException {
            final int length = inputStream.readUInt32();
            if (length == 0) {
                return new long[0];
            }
            long[] result = new long[length];
            for (int index = 0; index < length; index++) {
                result[index] = inputStream.readSInt64();
            }
            return result;
        }

        @Override
        public byte getWireType() {
            return WireType.LONG_ARRAY;
        }

    }

    private static class FloatArrayCodec implements Codec<float[]> {

        @Override
        public boolean isSupport(Class<?> type) {
            return type == float[].class;
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull float[] obj) throws IOException {
            outputStream.writeUInt32NoTag(obj.length);
            if (obj.length == 0) {
                return;
            }
            for (float value : obj) {
                outputStream.writeFloatNoTag(value);
            }
        }

        @Override
        public float[] readData(CodedInputStream inputStream) throws IOException {
            final int length = inputStream.readUInt32();
            if (length == 0) {
                return new float[0];
            }
            float[] result = new float[length];
            for (int index = 0; index < length; index++) {
                result[index] = inputStream.readFloat();
            }
            return result;
        }

        @Override
        public byte getWireType() {
            return WireType.FLOAT_ARRAY;
        }

    }

    private static class DoubleArrayCodec implements Codec<double[]> {
        @Override
        public boolean isSupport(Class<?> type) {
            return type == double[].class;
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull double[] obj) throws IOException {
            outputStream.writeUInt32NoTag(obj.length);
            if (obj.length == 0) {
                return;
            }
            for (double value : obj) {
                outputStream.writeDoubleNoTag(value);
            }
        }

        @Override
        public double[] readData(CodedInputStream inputStream) throws IOException {
            final int length = inputStream.readUInt32();
            if (length == 0) {
                return new double[0];
            }
            double[] result = new double[length];
            for (int index = 0; index < length; index++) {
                result[index] = inputStream.readDouble();
            }
            return result;
        }

        @Override
        public byte getWireType() {
            return WireType.DOUBLE_ARRAY;
        }

    }

    private static class BooleanArrayCodec implements Codec<boolean[]> {

        @Override
        public boolean isSupport(Class<?> type) {
            return type == boolean[].class;
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull boolean[] obj) throws IOException {
            outputStream.writeUInt32NoTag(obj.length);
            if (obj.length == 0) {
                return;
            }
            for (boolean value : obj) {
                outputStream.writeBoolNoTag(value);
            }
        }

        @Override
        public boolean[] readData(CodedInputStream inputStream) throws IOException {
            final int length = inputStream.readUInt32();
            if (length == 0) {
                return new boolean[0];
            }
            boolean[] result = new boolean[length];
            for (int index = 0; index < length; index++) {
                result[index] = inputStream.readBool();
            }
            return result;
        }

        @Override
        public byte getWireType() {
            return WireType.BOOLEAN_ARRAY;
        }

    }


    private static class ChunkCodec implements Codec<Chunk> {

        @Override
        public boolean isSupport(Class<?> type) {
            return type == Chunk.class;
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull Chunk obj) throws IOException {
            outputStream.writeUInt32NoTag(obj.getLength());
            if (obj.getLength() > 0) {
                outputStream.writeRawBytes(obj.getBuffer(), obj.getOffset(), obj.getLength());
            }
        }

        @Override
        public Chunk readData(CodedInputStream inputStream) throws IOException {
            final int length = inputStream.readUInt32();
            if (length == 0) {
                return Chunk.EMPTY_CHUNK;
            }
            final byte[] buffer = inputStream.readRawBytes(length);
            return Chunk.newInstance(buffer);
        }

        @Override
        public byte getWireType() {
            return WireType.CHUNK;
        }

    }

    // ------------------------------------------------- 工厂方法 ------------------------------------------------------
    @Nonnull
    public static BinaryProtocolCodec newInstance(MessageMapper messageMapper) {
        final Map<Class<?>, Parser<?>> parserMap = new IdentityHashMap<>();
        final Map<Class<?>, ProtoEnumDescriptor> protoEnumDescriptorMap = new IdentityHashMap<>();

        final Map<Class<?>, IntFunction<? extends NumericalEntity>> forNumberMethodMap = new IdentityHashMap<>();
        final Map<Class<?>, Function<?, ? extends IndexableEntity<?>>> forIndexMethodMap = new IdentityHashMap<>();
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
                    protoEnumDescriptorMap.put(messageClazz, new ProtoEnumDescriptor(mapper));
                    continue;
                }

                // 尝试加载 BeanSerializer - 包括生成的和手写的代码
                if (indexClassBySerializer(messageClazz, beanSerializerMap)) {
                    continue;
                }

                // 其它不带注解的一律不支持
                if (!messageClazz.isAnnotationPresent(SerializableClass.class)) {
                    throw new RuntimeException("unsupportted message " + messageClazz.getName());
                }

            }
            return new BinaryProtocolCodec(messageMapper, parserMap, protoEnumDescriptorMap, forNumberMethodMap, forIndexMethodMap, beanSerializerMap);
        } catch (Exception e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    private static boolean indexClassBySerializer(Class<?> messageClazz, Map<Class<?>, EntitySerializer<?>> beanSerializerMap) throws Exception {
        final Class<? extends EntitySerializer<?>> serializerClass = WireType.getBeanSerializer(messageClazz);
        if (serializerClass != null) {
            final Constructor<? extends EntitySerializer<?>> noArgsConstructor = serializerClass.getDeclaredConstructor();
            noArgsConstructor.setAccessible(true);
            beanSerializerMap.put(messageClazz, noArgsConstructor.newInstance());
            return true;
        } else {
            return false;
        }
    }

}
