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

package com.wjybxx.fastjgame.util.eventbus;

import com.google.common.collect.Maps;
import com.wjybxx.fastjgame.util.function.FunctionUtils;
import com.wjybxx.fastjgame.util.pool.ObjectPool;
import com.wjybxx.fastjgame.util.pool.SingleObjectPool;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * EventBus的一个简单实现，它默认支持所有的普通事件和所有的泛型事件，并不对事件做任何要求。
 * <h3>NOTES</h3>
 * 1. 它并不是一个线程安全的对象。
 * 2. 它也没有对继承体系进行完整的支持（监听接口或抽象类），主要考虑到性能可能不好。
 * <h3>如何分发</h3>
 * 先以{@code event}的类型直接分发一次，如果事件为{@link GenericEvent}的子类型，再<b>联合</b>{@link GenericEvent#childKey()}分发一次。
 * <p>
 * Q: 为何要以{@link GenericEvent}的类型分发一次?
 * A: 这允许监听者监听某一类事件，而不是某一个具体事件。
 * <p>
 * 注意：新增的监听器可能立即响应事件。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/23
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class DefaultEventBus implements EventBus {

    /**
     * eventKey -> handler
     * eventKey：{@link Class} 或 {@link ComposeEventKey}
     */
    private final Map<Object, EventHandler<?>> handlerMap;
    private final Predicate<Class<?>> filter;
    private final Predicate<Class<? extends DynamicChildEvent>> genericFilter;

    /** 暂时先使用{@link SingleObjectPool}，考虑到递归的情况较少，单个缓存就足以减少许多对象创建 */
    private final ObjectPool<ComposeEventKey> keyPool = new SingleObjectPool<>(ComposeEventKey::new, ComposeEventKey::reset);
    /** 递归深度 - 防止死循环 */
    private int recursionDepth;

    public DefaultEventBus() {
        this(EventBusUtils.DEFAULT_EXPECTED_SIZE);
    }

    public DefaultEventBus(int expectedSize) {
        this(expectedSize, FunctionUtils.alwaysTrue(), FunctionUtils.alwaysTrue());
    }

    public DefaultEventBus(int expectedSize, Predicate<Class<?>> filter, Predicate<Class<? extends DynamicChildEvent>> genericFilter) {
        this.handlerMap = Maps.newHashMapWithExpectedSize(expectedSize);
        this.filter = filter;
        this.genericFilter = genericFilter;
    }

    @Override
    public final void post(@Nonnull Object event) {
        if (recursionDepth >= EventBusUtils.RECURSION_LIMIT) {
            throw new IllegalStateException("event had too many levels of nesting");
        }
        recursionDepth++;
        try {
            // 先以对象的类型分发一次
            postImp(event, event.getClass());
            // 再联合子键分发一次
            if (event instanceof DynamicChildEvent) {
                final ObjectPool<ComposeEventKey> keyPool = this.keyPool;
                final ComposeEventKey composeEventKey = keyPool.get();
                composeEventKey.init(event.getClass(), ((DynamicChildEvent) event).childKey());
                postImp(event, composeEventKey);
                keyPool.returnOne(composeEventKey);
            }
        } finally {
            recursionDepth--;
        }
    }

    private void postImp(final @Nonnull Object event, final @Nonnull Object eventKey) {
        EventBusUtils.postEventImp(handlerMap, event, eventKey);
    }

    @Override
    public final <T> boolean register(@Nonnull Class<T> eventType, @Nullable String customData, @Nonnull EventHandler<? super T> handler) {
        if (filter.test(eventType)) {
            addHandlerImp(eventType, handler);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public final <T extends DynamicChildEvent> boolean register(@Nonnull Class<T> parentType, @Nonnull Object childKey, @Nullable String customData, @Nonnull EventHandler<? super T> handler) {
        if (genericFilter.test(parentType)) {
            addHandlerImp(newComposeEventKey(parentType, childKey), handler);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public <T> void deregister(@Nonnull Class<T> eventType, @Nonnull EventHandler<? super T> handler) {
        if (filter.test(eventType)) {
            removeHandlerImp(eventType, handler);
        }
    }

    @Override
    public <T extends DynamicChildEvent> void deregister(@Nonnull Class<T> parentType, @Nonnull Object childKey, @Nonnull EventHandler<? super T> handler) {
        if (genericFilter.test(parentType)) {
            removeHandlerImp(newComposeEventKey(parentType, childKey), handler);
        }
    }

    private <T> void addHandlerImp(@Nonnull Object eventKey, @Nonnull EventHandler<? super T> handler) {
        EventBusUtils.addHandlerImp(handlerMap, eventKey, handler);
    }

    private <T> void removeHandlerImp(Object key, @Nonnull EventHandler<? super T> handler) {
        EventBusUtils.removeHandlerImp(handlerMap, key, handler);
    }

    @Override
    public final void release() {
        handlerMap.clear();
    }

    private static ComposeEventKey newComposeEventKey(@Nonnull DynamicChildEvent event) {
        return new ComposeEventKey();
    }

    private static Object newComposeEventKey(Class<?> masterKey, Object childKey) {
        Objects.requireNonNull(masterKey, "masterKey");
        Objects.requireNonNull(childKey, "childKey");
        return new ComposeEventKey(masterKey, childKey);
    }

    /**
     * 泛型事件使用的key
     */
    private static final class ComposeEventKey {

        private Class<?> masterKey;
        private Object childKey;

        ComposeEventKey() {
        }

        ComposeEventKey(@Nonnull Class<?> masterKey, @Nonnull Object childKey) {
            this.masterKey = masterKey;
            this.childKey = childKey;
        }

        void init(@Nonnull Class<?> masterKey, @Nonnull Object childKey) {
            this.masterKey = masterKey;
            this.childKey = childKey;
        }

        void reset() {
            this.masterKey = null;
            this.childKey = null;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || o.getClass() != ComposeEventKey.class) {
                return false;
            }

            final ComposeEventKey that = (ComposeEventKey) o;
            if (masterKey != that.masterKey) {
                return false;
            }

            // 这里冗余了一次引用相等测试 - 在多数情况下都是有利的
            return childKey == that.childKey || childKey.equals(that.childKey);
        }

        @Override
        public int hashCode() {
            return 31 * masterKey.hashCode() + childKey.hashCode();
        }

        @Override
        public String toString() {
            return "ComposeEventKey{" +
                    "parentType=" + masterKey +
                    ", childType=" + childKey +
                    '}';
        }
    }
}
