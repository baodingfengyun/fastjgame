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
import javax.annotation.Nullable;
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
public class CompositeEventHandler<T, E> implements EventHandler<T, E> {

    private static final Logger logger = LoggerFactory.getLogger(CompositeEventHandler.class);

    private final List<EventHandler<? super T, ? super E>> children = new ArrayList<>(2);

    public CompositeEventHandler(@Nonnull EventHandler<? super T, ? super E> first,
                                 @Nonnull EventHandler<? super T, ? super E> second) {
        children.add(first);
        children.add(second);
    }

    public CompositeEventHandler addHandler(@Nonnull EventHandler<? super T, ? super E> handler) {
        children.add(handler);
        return this;
    }

    @Override
    public void onEvent(@Nullable T context, @Nonnull E event) throws Exception {
        for (EventHandler<? super T, ? super E> handler : children) {
            try {
                handler.onEvent(context, event);
            } catch (Throwable e) {
                logger.warn("Child onEvent caught exception!", e);
            }
        }
    }
}
