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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Map;
import java.util.function.Predicate;

/**
 * EventBus的一个简单实现，它默认支持所有的普通事件和所有的泛型事件，并不对事件做任何要求。
 * <h3>NOTES</h3>
 * 1. 它并不是一个线程安全的对象。
 * 2. 它也不是一个标准的EventBus实现，比如就没有取消注册的接口，也没有单独的dispatcher、Registry
 * 3. 它也没有对继承体系进行完整的支持（监听接口或抽象类），主要考虑到性能可能不好。
 * <h3>如何分发</h3>
 * 先以{@code event}的类型直接分发一次，如果事件为{@link GenericEvent}的子类型，再<b>联合</b>{@link GenericEvent#child()}的类型分发一次。
 * <p>
 * Q: 为何{@link GenericEvent}也以它的类型直接分发一次？
 * A: 这允许监听者监听某一类事件，而不是某一个具体事件。
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
     * eventKey：{@link Class} 或 {@link GenericEventKey}
     */
    private final Map<Object, EventHandler<?>> handlerMap;
    private final Predicate<Class<?>> filter;
    private final Predicate<Class<? extends GenericEvent<?>>> genericFilter;

    public DefaultEventBus() {
        this(EventBusUtils.DEFAULT_EXPECTED_SIZE);
    }

    public DefaultEventBus(int expectedSize) {
        this(expectedSize, FunctionUtils.alwaysTrue(), FunctionUtils.alwaysTrue());
    }

    public DefaultEventBus(int expectedSize, Predicate<Class<?>> filter, Predicate<Class<? extends GenericEvent<?>>> genericFilter) {
        this.handlerMap = Maps.newHashMapWithExpectedSize(expectedSize);
        this.filter = filter;
        this.genericFilter = genericFilter;
    }

    @Override
    public final void post(@Nonnull Object event) {
        postEventImp(event, event.getClass());

        if (event instanceof GenericEvent) {
            postEventImp(event, newGenericEventKey((GenericEvent<?>) event));
        }
    }

    private void postEventImp(final @Nonnull Object event, final @Nonnull Object eventKey) {
        EventBusUtils.postEventImp(handlerMap, event, eventKey);
    }

    @Override
    public final <T> void register(@Nonnull Class<T> eventType, @Nonnull EventHandler<? super T> handler) {
        if (filter.test(eventType)) {
            addHandlerImp(eventType, handler);
        }
    }

    @Override
    public final <T extends GenericEvent<?>> void register(@Nonnull Class<T> parentType, @Nonnull Class<?> childType, @Nonnull EventHandler<? super T> handler) {
        if (genericFilter.test(parentType)) {
            addHandlerImp(newGenericEventKey(parentType, childType), handler);
        }
    }

    private <T> void addHandlerImp(@Nonnull Object eventKey, @Nonnull EventHandler<? super T> handler) {
        EventBusUtils.addHandlerImp(handlerMap, eventKey, handler);
    }

    @Override
    public final void release() {
        handlerMap.clear();
    }

    /**
     * 为泛型事件创建一个key
     *
     * @param genericEvent 泛型事件
     * @return 用于定位handler的key
     */
    @Nonnull
    private static Object newGenericEventKey(GenericEvent<?> genericEvent) {
        return new GenericEventKey(genericEvent.getClass(), genericEvent.child().getClass());
    }

    /**
     * 为泛型事件创建一个key
     *
     * @param parentType 父事件类型
     * @param childType  子事件类型
     * @return 用于定位handler的key
     */
    private static Object newGenericEventKey(Class<? extends GenericEvent<?>> parentType, Class<?> childType) {
        return new GenericEventKey(parentType, childType);
    }

    /**
     * 泛型事件使用的key
     */
    @Immutable
    private static final class GenericEventKey {

        private final Class<?> parentType;
        private final Class<?> childType;

        GenericEventKey(@Nonnull Class<?> parentType, @Nonnull Class<?> childType) {
            this.parentType = parentType;
            this.childType = childType;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || o.getClass() != GenericEventKey.class) {
                return false;
            }

            final GenericEventKey that = (GenericEventKey) o;
            return parentType == that.parentType && childType == that.childType;
        }

        @Override
        public int hashCode() {
            return 31 * parentType.hashCode() + childType.hashCode();
        }

        @Override
        public String toString() {
            return "GenericEventKey{" +
                    "parentType=" + parentType +
                    ", childType=" + childType +
                    '}';
        }
    }
}
