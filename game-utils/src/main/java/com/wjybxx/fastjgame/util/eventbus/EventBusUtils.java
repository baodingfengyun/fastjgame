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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * EventBus们的工具类
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/20
 * github - https://github.com/hl845740757
 */
public class EventBusUtils {

    private static final Logger logger = LoggerFactory.getLogger(EventBusUtils.class);

    public static final int DEFAULT_EXPECTED_SIZE = 64;
    /**
     * 递归限制 - 避免死循环
     */
    public static final int RECURSION_LIMIT = 16;

    private EventBusUtils() {

    }

    /**
     * 抛出事件的真正实现
     *
     * @param handlerMap 事件处理器映射
     * @param event      要抛出的事件
     * @param eventKey   事件对应的key
     * @param <T>        事件的类型
     */
    public static <K, T> void postEventImp(Map<K, EventHandler<?>> handlerMap, @Nonnull T event, @Nonnull K eventKey) {
        @SuppressWarnings("unchecked") final EventHandler<? super T> handler = (EventHandler<? super T>) handlerMap.get(eventKey);
        if (null == handler) {
            return;
        }

        invokeHandlerSafely(event, handler);
    }

    public static <T> void invokeHandlerSafely(@Nonnull T event, @Nonnull EventHandler<? super T> handler) {
        try {
            handler.onEvent(event);
        } catch (Exception e) {
            final String handlerName = handler.getClass().getName();
            final String eventName = event.getClass().getName();
            logger.warn("handlerName: " + handlerName + ", eventName: " + eventName, e);
        }
    }

    /**
     * 添加事件处理器的真正实现
     *
     * @param handlerMap 保存事件处理器的map
     * @param eventKey   关注的事件对应的key
     * @param handler    事件处理器
     * @param <T>        事件的类型
     */
    public static <K, T> void addHandlerImp(Map<K, EventHandler<?>> handlerMap, @Nonnull K eventKey, @Nonnull EventHandler<? super T> handler) {
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

    public static <K, T> void removeHandlerImp(Map<K, EventHandler<?>> handlerMap, @Nonnull K eventKey, @Nonnull EventHandler<? super T> handler) {
        @SuppressWarnings("unchecked") final EventHandler<? super T> existHandler = (EventHandler<? super T>) handlerMap.get(eventKey);
        if (null == existHandler) {
            return;
        }
        if (existHandler instanceof CompositeEventHandler) {
            @SuppressWarnings("unchecked") final CompositeEventHandler<T> compositeEventHandler = (CompositeEventHandler<T>) existHandler;
            compositeEventHandler.removeHandler(handler);
        } else {
            handlerMap.remove(eventKey);
        }
    }

}

