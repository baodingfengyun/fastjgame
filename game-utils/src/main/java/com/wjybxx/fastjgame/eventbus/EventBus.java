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

import com.google.common.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.IdentityHashMap;

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
     * 事件类型到处理器的映射
     */
    private final IdentityHashMap<Class<?>, EventHandler<?>> handlerMap = new IdentityHashMap<>(64);
    /**
     * 泛型事件处理器的类型
     */
    private final IdentityHashMap<Class<?>, IdentityHashMap<Class<?>, EventHandler<?>>> genericHandlerMap = new IdentityHashMap<>(8);

    public EventBus() {

    }

    @Override
    public void post(@Nonnull Object event) {
        if (event instanceof GenericEvent) {
            final GenericEvent genericEvent = (GenericEvent) event;
            final IdentityHashMap<Class<?>, EventHandler<?>> childHandlerMap = genericHandlerMap.get(genericEvent.getClass());
            if (childHandlerMap == null) {
                logger.warn("{}'s listeners may forgot register!", genericEvent.getClass().getName());
                return;
            }
            postEventImp(childHandlerMap, genericEvent, genericEvent.child().getClass());
        } else {
            postEventImp(handlerMap, event, event.getClass());
        }
    }

    private static <T> void postEventImp(IdentityHashMap<Class<?>, EventHandler<?>> handlerMap, @Nonnull T event, @Nonnull Class<?> keyClazz) {
        @SuppressWarnings("unchecked") final EventHandler<? super T> handler = (EventHandler<? super T>) handlerMap.get(keyClazz);
        if (null == handler) {
            // 对应的事件处理器可能忘记了注册
            logger.warn("{}'s listeners may forgot register!", keyClazz.getName());
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
        addHandlerImp(handlerMap, eventType, handler);
    }

    @Override
    public <T extends GenericEvent<U>, U> void register(@Nonnull Class<T> genericType, Class<U> childType, @Nonnull EventHandler<? super T> handler) {
        final IdentityHashMap<Class<?>, EventHandler<?>> childHandlerMap = genericHandlerMap.computeIfAbsent(genericType, k -> new IdentityHashMap<>(64));
        addHandlerImp(childHandlerMap, childType, handler);
    }

    @SuppressWarnings("unchecked")
    public static void addHandlerImp(IdentityHashMap<Class<?>, EventHandler<?>> handlerMap, @Nonnull Class<?> keyClass, @Nonnull EventHandler<?> handler) {
        final EventHandler<?> existHandler = handlerMap.get(keyClass);
        if (null == existHandler) {
            handlerMap.put(keyClass, handler);
            return;
        }
        if (existHandler instanceof CompositeEventHandler) {
            ((CompositeEventHandler) existHandler).addHandler(handler);
        } else {
            handlerMap.put(keyClass, new CompositeEventHandler(existHandler, handler));
        }
    }

    @Override
    public void release() {
        handlerMap.clear();
        genericHandlerMap.clear();
    }

}
