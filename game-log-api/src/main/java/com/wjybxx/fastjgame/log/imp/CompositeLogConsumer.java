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

package com.wjybxx.fastjgame.log.imp;

import com.wjybxx.fastjgame.log.core.GameLog;
import com.wjybxx.fastjgame.log.core.LogConsumer;
import com.wjybxx.fastjgame.log.utils.LogConsumerUtils;
import com.wjybxx.fastjgame.util.concurrent.EventLoop;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 为多个consumer提供单一视图
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/29
 * github - https://github.com/hl845740757
 */
public final class CompositeLogConsumer<T extends GameLog> implements LogConsumer<T> {

    /**
     * 订阅的共同的topic
     */
    private final String topic;
    /**
     * 这些子节点订阅了相同的topic
     */
    private final List<LogConsumer<T>> children = new ArrayList<>(2);

    public CompositeLogConsumer(final String topic, @Nonnull LogConsumer<T> first, @Nonnull LogConsumer<T> second) {
        this.topic = topic;
        children.add(first);
        children.add(second);
    }

    public void addChild(@Nonnull LogConsumer<T> child) {
        assert child.subscribedTopics().contains(topic);
        children.add(child);
    }

    @Nullable
    @Override
    public EventLoop appEventLoop() {
        return null;
    }

    @Override
    public Set<String> subscribedTopics() {
        return Collections.singleton(topic);
    }

    @Override
    public void consume(T gameLog) throws Exception {
        for (LogConsumer<T> child : children) {
            LogConsumerUtils.consumeSafely(child, gameLog);
        }
    }

}
