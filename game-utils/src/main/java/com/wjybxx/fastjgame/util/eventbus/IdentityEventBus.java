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

import com.wjybxx.fastjgame.util.function.FunctionUtils;

import javax.annotation.Nonnull;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 一个特殊的EventBus实现，它适用于这样的场景：
 * 如果确保出现在某个泛型事件中的child不会在其它任何事件中出现（也不会独立派发），也就是说{@link GenericEvent}的类型在<b>联合分发</b>时是可以忽略的情况下，可以使用该实现。
 * <h3>优势</h3>
 * 既避免了创建大量的小对象，也提高了性能。
 * <h3>如何分发</h3>
 * 先以{@code event}的类型直接分发一次，如果事件为{@link GenericEvent}的子类型，再以{@link GenericEvent#child()}的类型分发一次。
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
    private final Map<Class<?>, EventHandler<?>> handlerMap;
    private final Predicate<Class<?>> filter;
    private final Predicate<Class<? extends GenericEvent<?>>> genericFilter;

    /**
     * 保证{@code childType}只在某一个泛型事件中出现。
     */
    private final Map<Class<?>, Class<?>> childType2ParentTypeMap;

    public IdentityEventBus() {
        this(EventBusUtils.DEFAULT_EXPECTED_SIZE);
    }

    public IdentityEventBus(int expectedSize) {
        this(expectedSize, FunctionUtils.alwaysTrue(), FunctionUtils.alwaysTrue());
    }

    public IdentityEventBus(int expectedSize, Predicate<Class<?>> filter, Predicate<Class<? extends GenericEvent<?>>> genericFilter) {
        this.handlerMap = new IdentityHashMap<>(expectedSize);
        this.childType2ParentTypeMap = new IdentityHashMap<>(expectedSize);
        this.filter = filter;
        this.genericFilter = genericFilter;
    }

    @Override
    public final void post(@Nonnull Object event) {
        postImp(event, event.getClass());

        if (event instanceof GenericEvent) {
            final GenericEvent<?> genericEvent = (GenericEvent<?>) event;
            postImp(event, genericEvent.child().getClass());
        }
    }

    private void postImp(final @Nonnull Object event, final Class<?> eventKey) {
        EventBusUtils.postEventImp(handlerMap, event, eventKey);
    }

    @Override
    public final <T> void register(@Nonnull Class<T> eventType, @Nonnull EventHandler<? super T> handler) {
        if (filter.test(eventType)) {
            ensureChildTypeNonDuplicate(eventType, eventType);
            addHandlerImp(eventType, handler);
        }
    }

    /**
     * 保证{@code childType}只在某一个泛型事件中出现。
     */
    private void ensureChildTypeNonDuplicate(Class<?> childType, Class<?> parentType) {
        final Class<?> existParentType = childType2ParentTypeMap.get(childType);
        if (existParentType == null) {
            childType2ParentTypeMap.put(childType, parentType);
            return;
        }

        if (existParentType != parentType) {
            final String msg = String.format("childType: %s has more than one parentType, existParentType: %s, newParentType: %s",
                    childType.getName(), existParentType.getName(), parentType.getName());
            throw new IllegalArgumentException(msg);
        }
    }

    @Override
    public final <T extends GenericEvent<?>> void register(@Nonnull Class<T> parentType, @Nonnull Class<?> childType, @Nonnull EventHandler<? super T> handler) {
        if (genericFilter.test(parentType)) {
            ensureChildTypeNonDuplicate(childType, parentType);
            addHandlerImp(childType, handler);
        }
    }

    private void addHandlerImp(Class<?> keyClass, EventHandler<?> handler) {
        EventBusUtils.addHandlerImp(handlerMap, keyClass, handler);
    }

    @Override
    public final void release() {
        handlerMap.clear();
        childType2ParentTypeMap.clear();
    }

}