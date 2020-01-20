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

package com.wjybxx.fastjgame.eventbus;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Map;

/**
 * EventBus的一个简单实现，它默认支持所有的普通事件和所有的泛型事件。
 * 你可以通过重写{@link #accept(Class)}和{@link #acceptGeneric(Class)}限定支持的事件。
 * <p>
 * 1. 它并不是一个线程安全的对象。
 * 2. 它也不是一个标准的EventBus实现，比如就没有取消注册的接口，也没有单独的dispatcher、Registry
 * 3. 它也没有对继承体系进行完整的支持（监听接口或抽象类），主要考虑到性能可能不好。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/23
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class EventBus implements EventHandlerRegistry, EventDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(EventBus.class);

    /**
     * eventKey -> handler
     * eventKey：{@link Class} 或 {@link GenericEventKey}
     */
    private final Map<Object, EventHandler<?>> handlerMap;

    public EventBus() {
        this(EventBusUtils.DEFAULT_EXPECTED_SIZE);
    }

    public EventBus(int expectedSize) {
        handlerMap = Maps.newHashMapWithExpectedSize(expectedSize);
    }

    @Override
    public void post(@Nonnull Object event) {
        if (event instanceof GenericEvent) {
            postEventImp(handlerMap, event, newGenericEventKey((GenericEvent) event));
        } else {
            postEventImp(handlerMap, event, event.getClass());
        }
    }

    /**
     * 用于子类抛出事件
     *
     * @see #postEventImp(Map, Object, Object)
     */
    protected final <T> void postEventImp(final @Nonnull T event, final @Nonnull Object eventKey) {
        postEventImp(handlerMap, event, eventKey);
    }

    /**
     * 抛出事件的真正实现
     *
     * @param handlerMap 事件处理器映射
     * @param event      要抛出的事件
     * @param eventKey   事件对应的key
     * @param <T>        事件的类型
     */
    static <T> void postEventImp(Map<Object, EventHandler<?>> handlerMap, @Nonnull T event, @Nonnull Object eventKey) {
        @SuppressWarnings("unchecked") final EventHandler<? super T> handler = (EventHandler<? super T>) handlerMap.get(eventKey);
        if (null == handler) {
            // 对应的事件处理器可能忘记了注册
            logger.warn("{}'s listeners may forgot register!", eventKey);
            return;
        }

        try {
            handler.onEvent(event);
        } catch (Exception e) {
            logger.warn("handler.onEvent caught exception! EventClassInfo {}, EventKey {}, handler info {}",
                    event.getClass().getName(), eventKey, handler.getClass().getName(), e);
        }
    }

    @Override
    public final <T> void register(@Nonnull Class<T> eventType, @Nonnull EventHandler<? super T> handler) {
        if (GenericEvent.class.isAssignableFrom(eventType)) {
            throw new UnsupportedOperationException();
        }

        if (accept(eventType)) {
            addHandler(eventType, handler);
        }
    }

    /**
     * 是否支持该类型的普通事件
     *
     * @return 如果返回false，则会忽略该对应的handler注册
     */
    protected boolean accept(@Nonnull Class<?> eventType) {
        return true;
    }

    @Override
    public final <T extends GenericEvent<?>> void register(@Nonnull Class<T> genericType, @Nonnull Class<?> childType, @Nonnull EventHandler<? super T> handler) {
        if (acceptGeneric(genericType)) {
            addHandler(newGenericEventKey(genericType, childType), handler);
        }
    }

    /**
     * 是否支持该类型的泛型事件
     *
     * @return 如果返回false，则会忽略该对应的handler注册
     */
    protected boolean acceptGeneric(@Nonnull Class<? extends GenericEvent<?>> genericType) {
        return true;
    }

    /**
     * @see #addHandlerImp(Map, Object, EventHandler)
     */
    private <T> void addHandler(@Nonnull Object eventKey, @Nonnull EventHandler<? super T> handler) {
        addHandlerImp(handlerMap, eventKey, handler);
    }

    /**
     * 添加事件处理器的真正实现
     *
     * @param handlerMap 保存事件处理器的map
     * @param eventKey   关注的事件对应的key
     * @param handler    事件处理器
     * @param <T>        事件的类型
     */
    static <T> void addHandlerImp(Map<Object, EventHandler<?>> handlerMap, @Nonnull Object eventKey, @Nonnull EventHandler<? super T> handler) {
        @SuppressWarnings("unchecked") final EventHandler<? super T> existHandler = (EventHandler<? super T>) handlerMap.get(eventKey);
        if (null == existHandler) {
            handlerMap.put(eventKey, handler);
            return;
        }
        if (existHandler instanceof CompositeEventHandler) {
            @SuppressWarnings("unchecked") final CompositeEventHandler<T> compositeEventHandler = (CompositeEventHandler<T>) existHandler;
            compositeEventHandler.addHandler(handler);
        } else {
            handlerMap.put(eventKey, new CompositeEventHandler<>(existHandler, handler));
        }
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
    protected static Object newGenericEventKey(GenericEvent<?> genericEvent) {
        return new GenericEventKey(genericEvent.getClass(), genericEvent.child().getClass());
    }

    /**
     * 为泛型事件创建一个key
     *
     * @param genericType 泛型类型
     * @param childType   泛型事件的子类型
     * @return 用于定位handler的key
     */
    protected static Object newGenericEventKey(Class<? extends GenericEvent<?>> genericType, Class<?> childType) {
        return new GenericEventKey(genericType, childType);
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
