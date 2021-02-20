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


import com.wjybxx.fastjgame.util.constant.AbstractConstant;

/**
 * 泛型化的事件类型
 *
 * @param <T> 触发器对应的事件的上下文类型
 */
public abstract class GenericEventType<T> extends AbstractConstant<GenericEventType<T>> {

    protected GenericEventType(int id, String name) {
        super(id, name);
    }

    /**
     * 对事件上下文类型进行强制类型转换。请注意：请确保你进行了“==”判定。
     * <pre>{@code
     *      if (eventType == GenericEventType.STRING_EVENT) {
     *          String context = GenericEventType.STRING_EVENT.cast(eventContext);
     *      }
     * }</pre>
     * Q: 该方法存在的意义?<br>
     * A: 避免自己进行类型转换，否则当事件上下文的类型变化时，将无法在编译期发现。
     * {@link GenericEventType}是类型桥梁。
     */
    @SuppressWarnings("unchecked")
    public T cast(Object context) {
        return (T) context;
    }

}