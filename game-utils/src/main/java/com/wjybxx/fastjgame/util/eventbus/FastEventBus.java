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

package com.wjybxx.fastjgame.util.eventbus;

import com.wjybxx.fastjgame.util.MathUtils;
import com.wjybxx.fastjgame.util.function.FunctionUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import javax.annotation.Nonnull;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * 高性能的EventBus。
 * <p>
 * Q: 它是如何提速的？
 * A: 在{@link DefaultEventBus}中，泛型事件的key是一个对象，这在大量泛型事件的时候会产生大量的小对象。
 * 我们通过一个基本类型的long值代替生成的对象，从而提高效率。
 * <p>
 * <b>NOTES</b>: 必须为每一个事件类型提供一个唯一的hash值，且保证该值在任意JVM上是相同的。
 *
 * @author wjybxx
 * date - 2020/11/5
 * github - https://github.com/hl845740757
 */
public class FastEventBus implements EventBus {

    private final Long2ObjectMap<EventHandler<?>> handlerMap;
    private final Predicate<Class<?>> filter;
    private final Predicate<Class<? extends GenericEvent<?>>> genericFilter;

    /**
     * 计算一个类型的hash值，要求hash必须保持稳定，当类型相同时，在任意JVM上得到相同的hash值。
     * 我们希望hash冲突尽早暴露，且能解决，而不是某些机器上不冲突，某些机器上冲突。
     */
    private final ToIntFunction<Class<?>> hashFunc;
    /**
     * hash值到类型的映射，用于判断冲突。
     */
    private final Int2ObjectMap<Class<?>> hashToTypeMap;

    private FastEventBus(int expectedSize,
                         Predicate<Class<?>> filter,
                         Predicate<Class<? extends GenericEvent<?>>> genericFilter,
                         ToIntFunction<Class<?>> hashFunc) {
        this.handlerMap = new Long2ObjectOpenHashMap<>(expectedSize);
        this.hashFunc = hashFunc;
        this.filter = filter;
        this.genericFilter = genericFilter;
        this.hashToTypeMap = new Int2ObjectOpenHashMap<>(expectedSize);
    }

    @Override
    public final void post(@Nonnull Object event) {
        final int parentKey = hash(event.getClass());
        postImp(event, parentKey);

        if (event instanceof GenericEvent) {
            final GenericEvent<?> genericEvent = (GenericEvent<?>) event;
            final int subKey = hash(genericEvent.child().getClass());
            final long key = MathUtils.composeIntToLong(parentKey, subKey);
            postImp(event, key);
        }
    }

    private <T> void postImp(final @Nonnull T event, final long eventKey) {
        // 重复编写，避免产生不必要的拆装箱
        @SuppressWarnings("unchecked") final EventHandler<? super T> eventHandler = (EventHandler<? super T>) handlerMap.get(eventKey);
        if (eventHandler == null) {
            return;
        }
        EventBusUtils.invokeHandlerSafely(event, eventHandler);
    }

    @Override
    public final <T> void register(@Nonnull Class<T> eventType, @Nonnull EventHandler<? super T> handler) {
        if (filter.test(eventType)) {
            checkConflict(eventType);

            final int key = hash(eventType);
            addHandlerImp(key, handler);
        }
    }

    private int hash(final Class<?> type) {
        return hashFunc.applyAsInt(type);
    }

    private void checkConflict(final Class<?> type) {
        final int hash = hash(type);
        final Class<?> existType = hashToTypeMap.put(hash, type);
        if (existType != null && existType != type) {
            final String msg = String.format("conflict type! existType: %s, newType: %s", existType.getName(), type.getName());
            throw new IllegalArgumentException(msg);
        }
    }

    @Override
    public final <T extends GenericEvent<?>> void register(@Nonnull Class<T> genericType, @Nonnull Class<?> childType, @Nonnull EventHandler<? super T> handler) {
        if (genericFilter.test(genericType)) {
            checkConflict(genericType);
            checkConflict(childType);

            final long key = MathUtils.composeIntToLong(hash(genericType), hash(childType));
            addHandlerImp(key, handler);
        }
    }

    private void addHandlerImp(long key, EventHandler<?> handler) {
        // 这里的装箱不做优化，因为注册handler不是个频繁的操作
        EventBusUtils.addHandlerImp(handlerMap, key, handler);
    }

    @Override
    public final void release() {
        handlerMap.clear();
        hashToTypeMap.clear();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private int expectedSize = EventBusUtils.DEFAULT_EXPECTED_SIZE;
        private ToIntFunction<Class<?>> hashFunc = DefaultHashFunc.INSTANCE;

        private Predicate<Class<?>> filter = FunctionUtils.alwaysTrue();
        private Predicate<Class<? extends GenericEvent<?>>> genericFilter = FunctionUtils.alwaysTrue();

        Builder() {
        }

        public int getExpectedSize() {
            return expectedSize;
        }

        public Builder setExpectedSize(int expectedSize) {
            this.expectedSize = expectedSize;
            return this;
        }

        public ToIntFunction<Class<?>> getHashFunc() {
            return hashFunc;
        }

        public Builder setHashFunc(ToIntFunction<Class<?>> hashFunc) {
            this.hashFunc = hashFunc;
            return this;
        }

        public Predicate<Class<?>> getFilter() {
            return filter;
        }

        public Builder setFilter(Predicate<Class<?>> filter) {
            this.filter = filter;
            return this;
        }

        public Predicate<Class<? extends GenericEvent<?>>> getGenericFilter() {
            return genericFilter;
        }

        public Builder setGenericFilter(Predicate<Class<? extends GenericEvent<?>>> genericFilter) {
            this.genericFilter = genericFilter;
            return this;
        }

        public EventBus build() {
            return new FastEventBus(expectedSize, filter, genericFilter, hashFunc);
        }
    }

    private static class DefaultHashFunc implements ToIntFunction<Class<?>> {

        private static final DefaultHashFunc INSTANCE = new DefaultHashFunc();

        @Override
        public int applyAsInt(Class<?> value) {
            // Q: 为什么不使用simpleName的hashcode？
            // A: 因为getSimpleName开销更大。
            return value.getName().hashCode();
        }
    }
}
