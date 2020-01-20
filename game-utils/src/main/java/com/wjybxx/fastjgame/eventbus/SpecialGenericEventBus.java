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

import com.wjybxx.fastjgame.reflect.TypeParameterMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 只支持特定泛型事件的eventBus。
 * <p>
 * 它在注册handler时会进行过滤，对于不关注的handler直接丢弃。
 * 在抛出事件时，如果抛出限定以外的事件，则会抛出异常。
 * <p>
 * Q: 它的存在意义是什么？<br>
 * A: 它可以直接根据{@link GenericEvent#child()}的类型进行分发。消耗更小，性能更好，某些场景很合适（只有一类泛型事件的场景）。<br>
 * 此外，鼓励程序中使用多个EventBus，而不是使用一个大而全的EventBus，能友好的拆分职责。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/20
 * github - https://github.com/hl845740757
 */
public abstract class SpecialGenericEventBus<G extends GenericEvent<?>> implements EventHandlerRegistry, EventDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(SpecialGenericEventBus.class);

    private final TypeParameterMatcher genericMatcher;

    private final Map<Class<?>, EventHandler<? super G>> handlerMap;

    protected SpecialGenericEventBus() {
        this(EventBusUtils.DEFAULT_EXPECTED_SIZE);
    }

    protected SpecialGenericEventBus(int expectedSize) {
        genericMatcher = TypeParameterMatcher.findTypeMatcher(this, SpecialGenericEventBus.class, "G");
        handlerMap = new IdentityHashMap<>(expectedSize);
    }

    @Override
    public final void post(@Nonnull Object event) {
        if (genericMatcher.matchInstance(event)) {
            @SuppressWarnings("unchecked") final G genericEvent = (G) event;
            postExplicitly(genericEvent);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * 明确地抛出指定类型的事件 - 可以减少一些检查。
     *
     * @param event 泛型参数约束的事件
     */
    public final void postExplicitly(@Nonnull G event) {
        final EventHandler<? super G> handler = handlerMap.get(event.child().getClass());
        if (handler == null) {
            return;
        }
        try {
            handler.onEvent(event);
        } catch (Exception e) {
            logger.warn("handler.onEvent caught exception", e);
        }
    }

    @Override
    public final <T> void register(@Nonnull Class<T> eventType, @Nonnull EventHandler<? super T> handler) {
        // ignore
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <T extends GenericEvent<?>> void register(@Nonnull Class<T> genericType, @Nonnull Class<?> childType, @Nonnull EventHandler<? super T> handler) {
        if (genericMatcher.matchClass(genericType)) {
            EventBus.addHandlerImp((Map) handlerMap, childType, handler);
        }
    }

    @Override
    public final void release() {
        handlerMap.clear();
    }
}
