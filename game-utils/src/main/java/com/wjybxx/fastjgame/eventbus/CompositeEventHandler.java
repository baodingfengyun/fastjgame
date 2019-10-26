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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * 复合事件处理器，为多个事件处理器提供一个单一的视图。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/24
 * github - https://github.com/hl845740757
 */
public class CompositeEventHandler<T> implements EventHandler<T> {

    private static final Logger logger = LoggerFactory.getLogger(CompositeEventHandler.class);

    /**
     * 该节点管理的所有子节点，一般需要改对象的话，我们认为有大于2个子节点的可能，因此不初始化为2
     */
    private final List<EventHandler<T>> children = new ArrayList<>(4);

    public CompositeEventHandler() {

    }

    public CompositeEventHandler(@Nonnull EventHandler<T> first, @Nonnull EventHandler<T> second) {
        children.add(first);
        children.add(second);
    }

    public CompositeEventHandler<T> addHandler(@Nonnull EventHandler<T> handler) {
        children.add(handler);
        return this;
    }

    @Override
    public void onEvent(@Nonnull T event) throws Exception {
        for (EventHandler<T> handler : children) {
            try {
                handler.onEvent(event);
            } catch (Throwable e) {
                logger.warn("Child {} onEvent caught exception!", handler.getClass().getName(), e);
            }
        }
    }
}
