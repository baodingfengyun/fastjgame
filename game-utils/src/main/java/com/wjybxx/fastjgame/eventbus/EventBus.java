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
public class EventBus implements EventHandlerRegistry, EventDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(EventBus.class);
    /**
     * 默认的初始容量
     */
    private static final int DEFAULT_INIT_CAPACITY = 128;
    /**
     * 事件类型到处理器的映射
     */
    private final Map<Class<?>, EventHandler<?>> handlerMap;

    public EventBus() {
        this(DEFAULT_INIT_CAPACITY);
    }

    public EventBus(int initCapacity) {
        handlerMap = new IdentityHashMap<>(initCapacity);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> void post(@Nonnull T event) {
        final Class<?> keyClazz = event.getClass();
        final EventHandler<T> eventHandler = (EventHandler<T>) handlerMap.get(keyClazz);
        if (null == eventHandler) {
            // 对应的事件处理器可能忘记了注册
            logger.warn("{}'s listeners may forgot register!", keyClazz.getName());
            return;
        }
        try {
            eventHandler.onEvent(event);
        } catch (Exception e) {
            logger.warn("onEvent caught exception! KeyClassInfo {}, EventInfo {}, handler info {}",
                    keyClazz.getName(),
                    event.getClass().getName(),
                    eventHandler.getClass().getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> void register(@Nonnull Class<T> eventType, @Nonnull EventHandler<T> handler) {
        final EventHandler<T> existHandler = (EventHandler<T>) handlerMap.get(eventType);
        if (null == existHandler) {
            handlerMap.put(eventType, handler);
        } else {
            if (existHandler instanceof CompositeEventHandler) {
                ((CompositeEventHandler<T>) existHandler).addHandler(handler);
            } else {
                handlerMap.put(eventType, new CompositeEventHandler<>(existHandler, handler));
            }
        }
    }

    @Override
    public void release() {
        handlerMap.clear();
    }
}
