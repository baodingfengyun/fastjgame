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

package com.wjybxx.fastjgame.misc.log;

import com.wjybxx.fastjgame.core.LogConsumer;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 游戏日志消费者
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/11
 * github - https://github.com/hl845740757
 */
public abstract class GameLogConsumer implements LogConsumer<GameLogRecord> {

    @Override
    public final Set<String> subscribedTopics() {
        return subscribedTopics0().stream()
                .map(LogTopic::name)
                .collect(Collectors.toSet());
    }

    protected abstract Set<LogTopic> subscribedTopics0();

}
