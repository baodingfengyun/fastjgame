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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 复合事件处理器，为同一个事件的多个处理器提供一个单一的视图。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/24
 * github - https://github.com/hl845740757
 */
class CompositeEventHandler<T> implements EventHandler<T> {

    private final List<EventHandler<? super T>> children = new ArrayList<>(4);
    private int recursionDepth;
    private boolean containsNull;

    CompositeEventHandler(@Nonnull EventHandler<? super T> first,
                          @Nonnull EventHandler<? super T> second) {
        children.add(first);
        children.add(second);
    }

    void addHandler(@Nonnull EventHandler<? super T> handler) {
        children.add(handler);
    }

    void removeHandler(EventHandler<? super T> handler) {
        final int index = children.indexOf(handler);
        if (index >= 0) {
            children.set(index, null);
            containsNull = true;
        }
    }

    @Override
    public void onEvent(@Nonnull T event) throws Exception {
        recursionDepth++;
        try {
            // 必须使用最新的size，因为在迭代的过程中可能增删，不过我们是延迟删除的
            // 删除的可能是已遍历过的元素，因此即便这里迭代的时候没有遇见null，最终也是可能遇见null的
            for (int i = 0; i < children.size(); i++) {
                EventHandler<? super T> handler = children.get(i);
                if (null == handler) {
                    continue;
                }
                EventBusUtils.invokeHandlerSafely(event, handler);
            }
        } finally {
            recursionDepth--;
            if (recursionDepth == 0 && this.containsNull) {
                this.containsNull = false;
                children.removeIf(Objects::isNull);
            }
        }
    }
}
