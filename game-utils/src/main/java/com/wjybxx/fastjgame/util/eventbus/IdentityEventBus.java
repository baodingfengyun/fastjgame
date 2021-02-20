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

import com.wjybxx.fastjgame.util.function.FunctionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 一个特殊的EventBus实现，它适用于这样的场景：
 * 如果确保出现在某个泛型事件中的child不会在其它任何事件中出现（也不会独立派发），也就是说{@link DynamicChildEvent}的类型在<b>联合分发</b>时是可以忽略的情况下，可以使用该实现。
 * <h3>优势</h3>
 * 既避免了创建大量的小对象，也提高了性能。
 * <h3>如何分发</h3>
 * 先以{@code event}的类型直接分发一次，如果事件为{@link DynamicChildEvent}的子类型，再以{@link DynamicChildEvent#childKey()}分发一次。
 * <h3>NOTES</h3>
 * 其它信息请参考{@link DefaultEventBus}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/21
 * github - https://github.com/hl845740757
 */
public class IdentityEventBus implements EventBus {

    /**
     * eventKey -> handler
     * eventKey：{@link Class}
     */
    private final Map<Object, EventHandler<?>> handlerMap;
    /**
     * 保证{@code childKey}只在某一个泛型事件中出现。
     */
    private final Map<Object, Class<?>> childKey2ParentTypeMap;

    private final Predicate<Class<?>> filter;
    private final Predicate<Class<? extends DynamicChildEvent>> genericFilter;

    /** 递归深度 - 防止死循环 */
    private int recursionDepth;

    public IdentityEventBus() {
        this(EventBusUtils.DEFAULT_EXPECTED_SIZE);
    }

    public IdentityEventBus(int expectedSize) {
        this(expectedSize, FunctionUtils.alwaysTrue(), FunctionUtils.alwaysTrue());
    }

    public IdentityEventBus(int expectedSize, Predicate<Class<?>> filter, Predicate<Class<? extends DynamicChildEvent>> genericFilter) {
        this.handlerMap = new IdentityHashMap<>(expectedSize);
        this.childKey2ParentTypeMap = new IdentityHashMap<>(expectedSize);
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
            // 再以子键分发一次
            if (event instanceof DynamicChildEvent) {
                postImp(event, ((DynamicChildEvent) event).childKey());
            }
        } finally {
            recursionDepth--;
        }
    }

    private void postImp(final @Nonnull Object event, Object eventKey) {
        EventBusUtils.postEventImp(handlerMap, event, eventKey);
    }

    @Override
    public final <T> boolean register(@Nonnull Class<T> eventType, @Nullable String customData, @Nonnull EventHandler<? super T> handler) {
        if (filter.test(eventType)) {
            ensureChildKeyNonDuplicate(eventType, eventType);
            EventBusUtils.addHandlerImp(handlerMap, eventType, handler);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 保证{@code childKey}只在某一个泛型事件中出现。
     */
    private void ensureChildKeyNonDuplicate(Object childKey, Class<?> parentType) {
        final Class<?> existParentType = childKey2ParentTypeMap.get(childKey);
        if (existParentType == null) {
            childKey2ParentTypeMap.put(childKey, parentType);
            return;
        }
        if (existParentType != parentType) {
            final String msg = String.format("childKey: %s has more than one parentType, existParentType: %s, newParentType: %s",
                    childKey.toString(), existParentType.getName(), parentType.getName());
            throw new IllegalArgumentException(msg);
        }
    }

    @Override
    public final <T extends DynamicChildEvent> boolean register(@Nonnull Class<T> parentType, @Nonnull Object childKey, @Nullable String customData, @Nonnull EventHandler<? super T> handler) {
        if (genericFilter.test(parentType)) {
            ensureChildKeyNonDuplicate(childKey, parentType);
            EventBusUtils.addHandlerImp(handlerMap, childKey, handler);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public <T> void deregister(@Nonnull Class<T> eventType, @Nonnull EventHandler<? super T> handler) {
        if (filter.test(eventType)) {
            EventBusUtils.removeHandlerImp(handlerMap, eventType, handler);
        }
    }

    @Override
    public <T extends DynamicChildEvent> void deregister(@Nonnull Class<T> parentType, @Nonnull Object childKey, @Nonnull EventHandler<? super T> handler) {
        if (genericFilter.test(parentType)) {
            EventBusUtils.removeHandlerImp(handlerMap, childKey, handler);
            // 注意：这里并不删除子键到父键的映射
        }
    }

    @Override
    public final void release() {
        handlerMap.clear();
        childKey2ParentTypeMap.clear();
    }

}