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
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import javax.annotation.Nonnull;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * 一个特殊的EventBus实现，它适用于这样的情景:
 * 应用程序能为每一个事件类型提供唯一的hash值，且保证在任意JVM上是相同的，也就是说我们通过hash值进行分发，而不是创建特定的Key类型（如{@link DefaultEventBus}中的实现）。
 * <h3>优势</h3>
 * 相对于{@link IdentityEventBus}适用面更广，相对于{@link DefaultEventBus}可以避免大量的小对象创建。
 * <h3>如何分发</h3>
 * 先以{@code event}的hash值分发一次，如果事件为{@link GenericEvent}的子类型，再以{@link GenericEvent#child()}的类型的hash值分发一次。
 * <h3>NOTES</h3>
 * 其它信息请参考{@link DefaultEventBus}
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
     * 缘由：我们希望hash冲突尽早暴露，且能解决，而不是某些机器上不冲突，某些机器上冲突。
     */
    private final ToIntFunction<Class<?>> hashFunc;
    private final Object2IntMap<Class<?>> typeToHashcodeMap;
    private final Int2ObjectMap<Class<?>> hashcodeToTypeMap;

    private FastEventBus(int expectedSize,
                         Predicate<Class<?>> filter,
                         Predicate<Class<? extends GenericEvent<?>>> genericFilter,
                         ToIntFunction<Class<?>> hashFunc) {
        this.handlerMap = new Long2ObjectOpenHashMap<>(expectedSize);
        this.hashFunc = hashFunc;
        this.filter = filter;
        this.genericFilter = genericFilter;

        this.typeToHashcodeMap = new Object2IntOpenHashMap<>(expectedSize);
        this.hashcodeToTypeMap = new Int2ObjectOpenHashMap<>(expectedSize);

        assert typeToHashcodeMap.defaultReturnValue() == 0;
        assert hashcodeToTypeMap.defaultReturnValue() == null;
    }

    @Override
    public final void post(@Nonnull Object event) {
        // Q: 为什么必须从缓存的hash值里取？
        // A: 因为可能抛出未监听的事件，如果计算的话，未监听的事件可能和监听的事件具有相同的hashcode，从而导致类型转换异常。
        final int parentKey = cachedHashcode(event.getClass());
        if (parentKey == 0) {
            return;
        }

        postImp(event, parentKey);

        if (event instanceof GenericEvent) {
            final GenericEvent<?> genericEvent = (GenericEvent<?>) event;
            final int subKey = cachedHashcode(genericEvent.child().getClass());
            if (subKey == 0) {
                return;
            }
            final long key = MathUtils.composeIntToLong(parentKey, subKey);
            postImp(event, key);
        }
    }

    /**
     * @return 0表示该类型未注册
     */
    private int cachedHashcode(final Class<?> type) {
        return typeToHashcodeMap.getInt(type);
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
            final int key = putHashcode(eventType);
            addHandlerImp(key, handler);
        }
    }

    private int putHashcode(final Class<?> type) {
        final int cachedHashcode = cachedHashcode(type);
        if (cachedHashcode != 0) {
            // 类型已注册
            return cachedHashcode;
        }

        final int hashcode = hashFunc.applyAsInt(type);
        if (hashcode == 0) {
            // hashcode不可以为0，与默认值冲突
            throw new IllegalArgumentException("hashcode cannot be zero");
        }

        final Class<?> existType = hashcodeToTypeMap.get(hashcode);
        if (existType != null) {
            // 与其它类型的hash值相同
            final String msg = String.format("hashcode conflict! existType: %s, newType: %s", existType.getName(), type.getName());
            throw new IllegalArgumentException(msg);
        }

        typeToHashcodeMap.put(type, hashcode);
        hashcodeToTypeMap.put(hashcode, type);

        return hashcode;
    }

    private void addHandlerImp(long key, EventHandler<?> handler) {
        // 这里的装箱不做优化，因为注册handler不是个频繁的操作
        EventBusUtils.addHandlerImp(handlerMap, key, handler);
    }

    @Override
    public final <T extends GenericEvent<?>> void register(@Nonnull Class<T> parentType, @Nonnull Class<?> childType, @Nonnull EventHandler<? super T> handler) {
        if (genericFilter.test(parentType)) {
            final int parentKey = putHashcode(parentType);
            final int subKey = putHashcode(childType);
            final long key = MathUtils.composeIntToLong(parentKey, subKey);
            addHandlerImp(key, handler);
        }
    }

    @Override
    public final void release() {
        handlerMap.clear();
        typeToHashcodeMap.clear();
        hashcodeToTypeMap.clear();
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

    public static ToIntFunction<Class<?>> defaultHashFunc() {
        return DefaultHashFunc.INSTANCE;
    }

    private static class DefaultHashFunc implements ToIntFunction<Class<?>> {

        private static final DefaultHashFunc INSTANCE = new DefaultHashFunc();

        @Override
        public int applyAsInt(Class<?> value) {
            // Q: 为什么不使用simpleName的hashcode？
            // A: 一是因为getSimpleName开销更大，二是因为simpleName可能重复
            return value.getName().hashCode();
        }
    }
}
