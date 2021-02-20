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

import javax.annotation.Nonnull;

/**
 * 动态子键的事件
 * <p>
 * Q: 该设计为了解决什么问题？
 * A: 一般情况下，我们只以事件的类型作为事件派发过程中的键。但有时我们期望能结合事件中的某个属性再次分发一次，这时便需要额外的键。
 */
public interface DynamicChildEvent {

    /**
     * Q: 子键为什么是Object？
     * A: 如果我们将子键限制为某一类型，我们将不能使用既有类型，如int/string等进行分发。
     *
     * @return 子键
     */
    @Nonnull
    Object childKey();

}
