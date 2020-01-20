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

import javax.annotation.Nonnull;

/**
 * 只支持特定事件和特定泛型事件的EventBus
 * <p>
 * 它在注册handler时会进行过滤，对于不关注的handler直接丢弃。
 * 在抛出事件时，如果抛出限定以外的事件，则会抛出异常。
 * <p>
 * Q: 它的存在意义又是什么？<br>
 * A: 鼓励程序中使用多个EventBus，而不是使用一个大而全的EventBus，能友好的拆分职责。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/20
 * github - https://github.com/hl845740757
 */
public abstract class SpecialEventBus<E, G extends GenericEvent<?>> extends EventBus {

    private final TypeParameterMatcher eventMatcher;
    private final TypeParameterMatcher genericEventMatcher;

    public SpecialEventBus() {
        this(EventBusUtils.DEFAULT_EXPECTED_SIZE);
    }

    public SpecialEventBus(int expectedSize) {
        super(expectedSize);
        eventMatcher = TypeParameterMatcher.findTypeMatcher(this, SpecialEventBus.class, "E");
        genericEventMatcher = TypeParameterMatcher.findTypeMatcher(this, SpecialEventBus.class, "G");

    }

    @Override
    public final void post(@Nonnull Object event) {
        if (genericEventMatcher.matchInstance(event)) {
            // 必须先测试 GenericEvent，否则可能可能无法工作
            @SuppressWarnings("unchecked") G g = (G) event;
            postEventImp(g, newGenericEventKey(g));
            return;
        }

        if (eventMatcher.matchInstance(event)) {
            postEventImp(event, event.getClass());
            return;
        }

        throw new IllegalStateException();
    }

    /**
     * 明确地抛出指定类型的事件 - 可以减少一些检查。
     *
     * @param event 泛型参数约束的事件
     */
    public final void postExplicitly(E event) {
        postEventImp(event, event.getClass());
    }

    /**
     * 明确地抛出指定类型的事件 - 可以减少一些检查。
     *
     * @param event 泛型参数约束的事件
     */
    public final void postExplicitly(G event) {
        postEventImp(event, newGenericEventKey(event));
    }

    @Override
    protected final boolean accept(@Nonnull Class<?> eventType) {
        return eventMatcher.matchClass(eventType);
    }

    @Override
    protected final boolean acceptGeneric(@Nonnull Class<? extends GenericEvent<?>> genericType) {
        return genericEventMatcher.matchClass(genericType);
    }
}
