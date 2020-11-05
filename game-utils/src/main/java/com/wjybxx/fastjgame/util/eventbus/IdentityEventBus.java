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
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 一个特殊的EventBus实现，用于特定场景下的性能优化。
 * <h3>适用场景</h3>
 * 它仅适用于这样的场景：
 * 如果确保出现在某个泛型事件中的child不会在其它任何事件中出现，也就是说{@link GenericEvent}的类型在<b>联合分发</b>时是可以忽略的情况下，可以使用该实现。
 * <h3>如何分发</h3>
 * 如果{@link #post(Object)}抛出的事件为 {@link GenericEvent}，先以{@link GenericEvent}的类型分发一次，再以{@link GenericEvent#child()}的类型分发一次。
 * 如果{@link #post(Object)}抛出的事件非 {@link GenericEvent}，则以{@code event}的类型进行分发。
 * <h3>NOTES</h3>
 * 使用该实现务必慎重，其它信息请参考{@link DefaultEventBus}
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

    public IdentityEventBus() {
        this(EventBusUtils.DEFAULT_EXPECTED_SIZE);
    }

    public IdentityEventBus(int expectedSize) {
        this(expectedSize, FunctionUtils.alwaysTrue(), FunctionUtils.alwaysTrue());
    }

    public IdentityEventBus(int expectedSize, Predicate<Class<?>> filter, Predicate<Class<? extends GenericEvent<?>>> genericFilter) {
        this.handlerMap = new IdentityHashMap<>(expectedSize);
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
            addHandlerImp(eventType, handler);
        }
    }

    @Override
    public final <T extends GenericEvent<?>> void register(@Nonnull Class<T> genericType, @Nonnull Class<?> childType, @Nonnull EventHandler<? super T> handler) {
        if (genericFilter.test(genericType)) {
            addHandlerImp(childType, handler);
        }
    }

    private void addHandlerImp(Class<?> keyClass, EventHandler<?> handler) {
        EventBusUtils.addHandlerImp(handlerMap, keyClass, handler);
    }

    @Override
    public final void release() {
        handlerMap.clear();
    }

}