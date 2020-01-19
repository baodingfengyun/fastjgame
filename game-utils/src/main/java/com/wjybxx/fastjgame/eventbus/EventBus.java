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
import com.google.common.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Map;

/**
 * EventBus的一个简单实现。
 * 1. 它并不是一个线程安全的对象
 * 2. 它也不是一个标准的EventBus实现，比如就没有取消注册的接口，也没有单独的dispatcher、Registry
 * 3. 它也没有对继承体系进行完整的支持（监听接口或抽象类），主要考虑到性能可能不好 - 等到我确实需要的时候再添加吧。
 * -{@link TypeToken#getTypes()}会有所帮助。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/23
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public final class EventBus implements EventHandlerRegistry, EventDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(EventBus.class);
    /**
     * 默认事件数大小
     */
    private static final int DEFAULT_EXPECTED_SIZE = 128;
    /**
     * 事件类型到处理器的映射
     */
    private final Map<Object, EventHandler<?>> handlerMap;

    public EventBus() {
        this(DEFAULT_EXPECTED_SIZE);
    }

    public EventBus(int expectedSize) {
        handlerMap = Maps.newHashMapWithExpectedSize(expectedSize);
    }

    @Override
    public void post(@Nonnull Object event) {
        if (event instanceof GenericEvent) {
            final GenericEvent genericEvent = (GenericEvent) event;
            final GenericEventKey genericEventKey = new GenericEventKey(genericEvent.getClass(), genericEvent.child().getClass());
            postEventImp(event, genericEventKey);
        } else {
            postEventImp(event, event.getClass());
        }
    }

    private <T> void postEventImp(final @Nonnull T event, final @Nonnull Object eventKey) {
        @SuppressWarnings("unchecked") final EventHandler<? super T> handler = (EventHandler<? super T>) handlerMap.get(eventKey);
        if (null == handler) {
            // 对应的事件处理器可能忘记了注册
            logger.warn("{}'s listeners may forgot register!", eventKey.toString());
            return;
        }

        try {
            handler.onEvent(event);
        } catch (Exception e) {
            logger.warn("threadFactoryEvent caught exception! EventClassInfo {}, handler info {}",
                    event.getClass().getName(),
                    handler.getClass().getName(), e);
        }
    }

    @Override
    public <T> void register(@Nonnull Class<T> eventType, @Nonnull EventHandler<? super T> handler) {
        if (GenericEvent.class.isAssignableFrom(eventType)) {
            throw new UnsupportedOperationException();
        }
        addHandlerImp(eventType, handler);
    }

    @Override
    public <T extends GenericEvent<U>, U> void register(@Nonnull Class<T> genericType, Class<U> childType, @Nonnull EventHandler<? super T> handler) {
        addHandlerImp(new GenericEventKey(genericType, childType), handler);
    }

    @SuppressWarnings("unchecked")
    private void addHandlerImp(@Nonnull Object eventKey, @Nonnull EventHandler<?> handler) {
        final EventHandler<?> existHandler = handlerMap.get(eventKey);
        if (null == existHandler) {
            handlerMap.put(eventKey, handler);
            return;
        }
        if (existHandler instanceof CompositeEventHandler) {
            ((CompositeEventHandler) existHandler).addHandler(handler);
        } else {
            handlerMap.put(eventKey, new CompositeEventHandler(existHandler, handler));
        }
    }

    @Override
    public void release() {
        handlerMap.clear();
    }

    @Immutable
    private static class GenericEventKey {

        private final Class<?> parentType;
        private final Class<?> childType;

        private GenericEventKey(Class<?> parentType, Class<?> childType) {
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
