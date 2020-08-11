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

import com.google.protobuf.Internal;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import com.google.protobuf.ProtocolMessageEnum;
import com.wjybxx.fastjgame.net.serialization.TypeId;
import com.wjybxx.fastjgame.net.serialization.TypeIdMapper;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Supplier;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/8/4
 */
public class ObjectReaderImpl implements ObjectReader {

    private final BinarySerializer serializer;
    private final CodecRegistry codecRegistry;
    private final CodedDataInputStream inputStream;

    private final int recursionLimit = ObjectWriterImpl.DEFAULT_RECURSION_LIMIT;
    private int recursionDepth;

    ObjectReaderImpl(BinarySerializer serializer, CodecRegistry codecRegistry, CodedDataInputStream inputStream) {
        this.serializer = serializer;
        this.codecRegistry = codecRegistry;
        this.inputStream = inputStream;
    }

    // -------------------------------------------- 基本值 --------------------------------------

    private void readTypeAndCheck(BinaryValueType expected) throws IOException {
        final BinaryValueType type = inputStream.readType();
        if (type != expected) {
            throw new IOException("expected type " + expected + ", but found " + type);
        }
    }

    @Override
    public int readInt() throws Exception {
        readTypeAndCheck(BinaryValueType.INT);
        return inputStream.readInt32();
    }

    @Override
    public long readLong() throws Exception {
        readTypeAndCheck(BinaryValueType.LONG);
        return inputStream.readInt64();
    }

    @Override
    public float readFloat() throws Exception {
        readTypeAndCheck(BinaryValueType.FLOAT);
        return inputStream.readFloat();
    }

    @Override
    public double readDouble() throws Exception {
        readTypeAndCheck(BinaryValueType.DOUBLE);
        return inputStream.readDouble();
    }

    @Override
    public short readShort() throws Exception {
        readTypeAndCheck(BinaryValueType.SHORT);
        return (short) inputStream.readInt32();
    }

    @Override
    public boolean readBoolean() throws Exception {
        readTypeAndCheck(BinaryValueType.BOOLEAN);
        return inputStream.readBool();
    }

    @Override
    public byte readByte() throws Exception {
        readTypeAndCheck(BinaryValueType.BYTE);
        return inputStream.readRawByte();
    }

    @Override
    public char readChar() throws Exception {
        readTypeAndCheck(BinaryValueType.CHAR);
        return (char) inputStream.readInt32();
    }

    private void checkValueType(BinaryValueType expected, BinaryValueType type) throws IOException {
        if (type != expected) {
            throw new IOException("expected type " + expected + ", but found " + type);
        }
    }

    @Override
    public String readString() throws Exception {
        final BinaryValueType currentValueType = inputStream.readType();
        if (currentValueType == BinaryValueType.NULL) {
            return null;
        }

        checkValueType(BinaryValueType.STRING, currentValueType);

        return inputStream.readString();
    }

    @Override
    public byte[] readBytes() throws IOException {
        final BinaryValueType currentValueType = inputStream.readType();
        if (currentValueType == BinaryValueType.NULL) {
            return null;
        }

        checkValueType(BinaryValueType.BINARY, currentValueType);

        return readBytesImpl();
    }

    private byte[] readBytesImpl() throws IOException {
        final int size = inputStream.readFixed32();
        return inputStream.readRawBytes(size);
    }

    @Override
    public <T extends MessageLite> T readMessage() throws Exception {
        final BinaryValueType currentValueType = inputStream.readType();
        if (currentValueType == BinaryValueType.NULL) {
            return null;
        }

        checkValueType(BinaryValueType.MESSAGE, currentValueType);

        return readMessageImpl();
    }

    private <T extends MessageLite> T readMessageImpl() throws Exception {
        final int size = inputStream.readFixed32();

        final TypeId typeId = readTypeId();
        final Class<?> type = getTypeIdMapper().ofId(typeId);
        if (type == null) {
            throw new IOException("Unknown typeId " + typeId);
        }

        @SuppressWarnings("unchecked") final Parser<T> parser = (Parser<T>) serializer.parserMap.get(type);
        if (parser == null) {
            throw new IOException("parser is null, type " + type);
        }

        final int oldLimit = inputStream.pushLimit(size - 5);
        final T result = inputStream.readMessageNoSize(parser);
        inputStream.popLimit(oldLimit);
        return result;
    }

    private TypeIdMapper getTypeIdMapper() {
        return serializer.typeIdMapper;
    }
    // -----------------------------------------------  读取对象（核心） --------------------------------------

    @Override
    public <T> T readObject() throws Exception {
        final BinaryValueType currentValueType = inputStream.readType();
        if (currentValueType == BinaryValueType.NULL) {
            return null;
        }
        @SuppressWarnings("unchecked") final T result = (T) readObjectImpl(currentValueType);
        return result;
    }

    private Object readObjectImpl(BinaryValueType valueType) throws Exception {
        switch (valueType) {
            case NULL:
                return null;

            case BYTE:
                return inputStream.readRawByte();
            case CHAR:
                return (char) inputStream.readInt32();
            case SHORT:
                return (short) inputStream.readInt32();
            case INT:
                return inputStream.readInt32();
            case LONG:
                return inputStream.readInt64();
            case FLOAT:
                return inputStream.readFloat();
            case DOUBLE:
                return inputStream.readDouble();
            case BOOLEAN:
                return inputStream.readBool();

            case STRING:
                return inputStream.readString();
            case BINARY:
                return readBytesImpl();
            case MESSAGE:
                return readMessageImpl();

            case OBJECT:
                return readPojo(this::readAnyPojo);
            default:
                throw new IOException("unexpected valueType : " + valueType);
        }
    }

    private <T> T readPojo(ContainerReader<T> reader) throws Exception {
        if (++recursionDepth > recursionLimit) {
            throw new IOException("Object had too many levels of nesting");
        }

        final int size = inputStream.readFixed32();
        final int oldLimit = inputStream.pushLimit(size);
        final TypeId typeId = readTypeId();
        final T result = reader.accept(typeId);
        inputStream.popLimit(oldLimit);

        recursionDepth--;
        return result;
    }

    private TypeId readTypeId() throws Exception {
        // 固定5字节
        final byte nameSpace = inputStream.readRawByte();
        final int classId = inputStream.readFixed32();
        return new TypeId(nameSpace, classId);
    }

    @FunctionalInterface
    interface ContainerReader<T> {

        T accept(TypeId typeId) throws Exception;

    }

    private Object readAnyPojo(TypeId typeId) throws Exception {
        final PojoCodec<?> pojoCodec = getPojoCodec(typeId);
        if (pojoCodec != null) {
            return pojoCodec.readObject(this, codecRegistry);
        }

        final Object result;
        final Class<?> type = getTypeIdMapper().ofId(typeId);
        if (type != null) {
            result = readAnyPojoByType(type);
        } else {
            result = readAnyPojoById(typeId);
        }
        return result;
    }

    @Nullable
    private PojoCodec<?> getPojoCodec(TypeId typeId) {
        final Class<?> type = serializer.typeIdMapper.ofId(typeId);
        if (type == null) {
            return null;
        }
        return codecRegistry.get(type);
    }

    private Object readAnyPojoByType(Class<?> type) throws Exception {
        // 集合
        if (Collection.class.isAssignableFrom(type)) {
            final Supplier<? extends Collection<Object>> factory = getCollectionFactory(type);
            if (factory != null) {
                return readCollectionImpl(factory);
            }
            if (Set.class.isAssignableFrom(type)) {
                return readAsDefaultSet();
            }
            return readAsDefaultList();
        }

        // map
        if (Map.class.isAssignableFrom(type)) {
            final Supplier<? extends Map<Object, Object>> factory = getMapFactory(type);
            if (factory != null) {
                return readMapImpl(factory);
            }
            return readAsDefaultMap();
        }

        // 数组
        if (type.isArray()) {
            return readArrayImpl(type.getComponentType());
        }

        // protoBuf枚举
        if (ProtocolMessageEnum.class.isAssignableFrom(type)) {
            final Internal.EnumLiteMap<?> enumLiteMap = serializer.protocolEnumMap.get(type);
            if (enumLiteMap == null) {
                throw new NullPointerException(type.getName() + " enumLiteMap is null");
            }
            return readProtoEnumImpl(enumLiteMap);
        }

        // 其实可以读取到一个List，但暂时不这么做
        throw new IOException("Unknown type " + type);
    }

    private Object readAsDefaultSet() throws Exception {
        return readCollectionImpl(LinkedHashSet::new);
    }

    private Object readAsDefaultList() throws Exception {
        return readCollectionImpl(ArrayList::new);
    }

    private Object readAsDefaultMap() throws Exception {
        return readMapImpl(LinkedHashMap::new);
    }

    @SuppressWarnings("unchecked")
    private Supplier<? extends Collection<Object>> getCollectionFactory(Class<?> type) {
        return (Supplier<? extends Collection<Object>>) serializer.collectionFactoryMap.get(type);
    }

    @SuppressWarnings("unchecked")
    private Supplier<? extends Map<Object, Object>> getMapFactory(Class<?> type) {
        return (Supplier<? extends Map<Object, Object>>) serializer.mapFactoryMap.get(type);
    }

    private Object readAnyPojoById(TypeId typeId) throws Exception {
        // 集合命名空间
        if (typeId.getNamespace() == TypeId.NAMESPACE_COLLECTION) {
            if (typeId.equals(TypeId.DEFAULT_SET)) {
                return readAsDefaultSet();
            }
            return readAsDefaultList();
        }
        // Map命名空间
        if (typeId.getNamespace() == TypeId.NAMESPACE_MAP) {
            return readAsDefaultMap();
        }
        // 数组命名空间
        if (typeId.getNamespace() == TypeId.NAMESPACE_ARRAY) {
            return readArrayImpl(Object.class);
        }
        // 其实可以读取到一个List，但暂时不这么做
        throw new IOException("Unknown typeId " + typeId);
    }

    @Nullable
    @Override
    public <E> E readObject(Supplier<E> factory) throws Exception {
        final BinaryValueType currentValueType = inputStream.readType();
        if (currentValueType == BinaryValueType.NULL) {
            return null;
        }

        return readPojo(typeId -> readToSubType(typeId, factory));
    }

    private <E> E readToSubType(TypeId typeId, Supplier<E> factory) throws Exception {
        final PojoCodec<?> pojoCodec = getPojoCodec(typeId);
        if (null == pojoCodec) {
            throw new IOException("Unsupported typeId " + typeId);
        }
        // 先检查是否支持读取字段，可减少不必要的对象创建
        ensureSupportReadFields(pojoCodec);

        final E result = factory.get();

        // 确保是超类型的codec，再转型
        ensureSubType(result, pojoCodec);
        @SuppressWarnings("unchecked") final PojoCodec<? super E> castPojoCodec = (PojoCodec<? super E>) pojoCodec;
        castPojoCodec.readFields(this, result, codecRegistry);

        return result;
    }

    private static void ensureSubType(Object instance, PojoCodec<?> pojoCodec) throws IOException {
        if (!pojoCodec.getEncoderClass().isAssignableFrom(instance.getClass())) {
            final String msg = String.format("Incompatible class, expected: %s, but read %s ",
                    instance.getClass().getName(), pojoCodec.getEncoderClass().getName());
            throw new IOException(msg);
        }
    }

    private static void ensureSupportReadFields(PojoCodec<?> pojoCodec) throws IOException {
        if (!pojoCodec.isReadFieldsSupported()) {
            throw new IOException(pojoCodec.getEncoderClass().getName() + " don't support readFields!");
        }
    }

    @Override
    public <T> T readPreDeserializeObject() throws Exception {
        final BinaryValueType valueType = inputStream.readType();
        if (valueType != BinaryValueType.BINARY) {
            @SuppressWarnings("unchecked") final T result = (T) readObjectImpl(valueType);
            return result;
        }
        // 解码字节数组的内容
        final int size = inputStream.readFixed32();
        int oldLimit = inputStream.pushLimit(size);
        final T result = readObject();
        inputStream.popLimit(oldLimit);
        return result;
    }

    private Object readProtoEnumImpl(Internal.EnumLiteMap<?> enumLiteMap) throws Exception {
        // 需要按照POJO的方式，必须调用其它基本值方法
        final int number = readInt();
        return enumLiteMap.findValueByNumber(number);
    }
    // ------------------------------------------ 数组/集合等多态处理 ------------------------------------------

    private <T> T readNullablePojo(final BinaryValueType currentValueType, ContainerReader<T> reader) throws Exception {
        if (currentValueType == BinaryValueType.NULL) {
            return null;
        }
        checkValueType(BinaryValueType.OBJECT, currentValueType);
        return readPojo(reader);
    }

    @Nullable
    @Override
    public <T> T readArray(@Nonnull Class<?> componentType) throws Exception {
        Objects.requireNonNull(componentType, "componentType");

        final BinaryValueType currentValueType = inputStream.readType();
        // 字节数组拦截
        if (currentValueType == BinaryValueType.BINARY) {
            return (T) readBytesImpl();
        }

        final Object array = readNullablePojo(currentValueType, typeId -> readArrayImpl(componentType));
        @SuppressWarnings("unchecked") final T result = (T) array;
        return result;
    }

    /**
     * 目前这样写仅仅是为了简单，自己封装数组的扩容等操作的话肯定性能更好一点，但是代码太难看
     */
    private Object readArrayImpl(@Nonnull Class<?> componentType) throws Exception {
        // fastutil的List可以获取内部数组
        final ObjectArrayList<Object> list = readCollectionImpl(ObjectArrayList::new);
        final Object array = convertArray(list.elements(), list.size(), componentType);
        // help gc
        list.clear();
        return array;
    }

    /**
     * 需要递归的转换
     */
    private static Object convertArray(Object[] src, int length, Class<?> componentType) {
        final Object array = Array.newInstance(componentType, length);
        for (int index = 0; index < length; index++) {
            final Object element = src[index];
            if (componentType.isArray()) {
                final Object[] childSrc = (Object[]) element;
                Array.set(array, index, convertArray(childSrc, childSrc.length, componentType.getComponentType()));
            } else {
                Array.set(array, index, element);
            }
        }
        return array;
    }

    @Nullable
    @Override
    public <C extends Collection<E>, E> C readCollection(@Nonnull Supplier<? extends C> collectionFactory) throws Exception {
        Objects.requireNonNull(collectionFactory, "collectionFactory");

        final BinaryValueType currentValueType = inputStream.readType();
        return readNullablePojo(currentValueType, typeId -> readCollectionImpl(collectionFactory));
    }

    private <C extends Collection<E>, E> C readCollectionImpl(Supplier<C> collectionFactory) throws Exception {
        final C collection = collectionFactory.get();
        while (!inputStream.isAtEnd()) {
            collection.add(readObject());
        }
        return collection;
    }

    @Nullable
    @Override
    public <M extends Map<K, V>, K, V> M readMap(@Nonnull Supplier<? extends M> mapFactory) throws Exception {
        Objects.requireNonNull(mapFactory, "mapFactory");

        final BinaryValueType currentValueType = inputStream.readType();
        return readNullablePojo(currentValueType, typeId -> readMapImpl(mapFactory));
    }

    private <M extends Map<K, V>, K, V> M readMapImpl(Supplier<M> mapFactory) throws Exception {
        final M map = mapFactory.get();
        while (!inputStream.isAtEnd()) {
            final K k = readObject();
            final V v = readObject();
            map.put(k, v);
        }
        return map;
    }

    // -------------------------------------------- 其它 ---------------------------------------

    public void close() throws Exception {

    }

    @Override
    public BinarySerializer serializer() {
        return serializer;
    }

    @Override
    public CodecRegistry codecRegistry() {
        return codecRegistry;
    }

    @Override
    public ReaderContext readStartObject() throws Exception {
        readTypeAndCheck(BinaryValueType.OBJECT);

        if (++recursionDepth > recursionLimit) {
            throw new IOException("Object had too many levels of nesting");
        }

        final int size = inputStream.readFixed32();
        final int oldLimit = inputStream.pushLimit(size);
        final TypeId typeId = readTypeId();

        return new DefaultReaderContext(recursionDepth, oldLimit, typeId);
    }

    @Override
    public boolean isEndOfObject() throws Exception {
        return inputStream.isAtEnd();
    }

    @Override
    public void readEndObject(ReaderContext context) throws Exception {
        if (!(context instanceof DefaultReaderContext)) {
            throw new IllegalArgumentException("Invalid context type");
        }

        if (!inputStream.isAtEnd()) {
            throw new IllegalStateException("Not reach the end of the object");
        }

        final DefaultReaderContext defaultReaderContext = (DefaultReaderContext) context;

        if (defaultReaderContext.oldRecursionDepth != recursionDepth) {
            throw new IllegalArgumentException("Bad context");
        }

        inputStream.popLimit(defaultReaderContext.oldLimit);

        recursionDepth--;
    }

    static class DefaultReaderContext implements ReaderContext {

        final int oldRecursionDepth;
        final int oldLimit;
        final TypeId typeId;

        DefaultReaderContext(int oldRecursionDepth, int oldLimit, TypeId typeId) {
            this.oldRecursionDepth = oldRecursionDepth;
            this.oldLimit = oldLimit;
            this.typeId = typeId;
        }

        @Override
        public TypeId typeId() {
            return typeId;
        }
    }

}
