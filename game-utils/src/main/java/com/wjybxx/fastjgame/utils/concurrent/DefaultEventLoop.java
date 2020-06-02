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

package com.wjybxx.fastjgame.utils.concurrent;

import com.wjybxx.fastjgame.utils.concurrent.unbounded.TaskQueueFactory;
import com.wjybxx.fastjgame.utils.concurrent.unbounded.TemplateEventLoop;
import com.wjybxx.fastjgame.utils.concurrent.unbounded.WaitStrategyFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ThreadFactory;

/**
 * 默认的事件循环。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/21
 * github - https://github.com/hl845740757
 */
public class DefaultEventLoop extends TemplateEventLoop {

    public DefaultEventLoop(@Nullable EventLoopGroup parent, @Nonnull ThreadFactory threadFactory, @Nonnull RejectedExecutionHandler rejectedExecutionHandler) {
        super(parent, threadFactory, rejectedExecutionHandler);
    }

    public DefaultEventLoop(@Nullable EventLoopGroup parent, @Nonnull ThreadFactory threadFactory, @Nonnull RejectedExecutionHandler rejectedExecutionHandler, int taskBatchSize) {
        super(parent, threadFactory, rejectedExecutionHandler, taskBatchSize);
    }

    public DefaultEventLoop(@Nullable EventLoopGroup parent, @Nonnull ThreadFactory threadFactory, @Nonnull RejectedExecutionHandler rejectedExecutionHandler, @Nonnull WaitStrategyFactory waitStrategyFactory) {
        super(parent, threadFactory, rejectedExecutionHandler, waitStrategyFactory);
    }

    public DefaultEventLoop(@Nullable EventLoopGroup parent, @Nonnull ThreadFactory threadFactory, @Nonnull RejectedExecutionHandler rejectedExecutionHandler, @Nonnull WaitStrategyFactory waitStrategyFactory, @Nullable TaskQueueFactory taskQueueFactory, int taskBatchSize) {
        super(parent, threadFactory, rejectedExecutionHandler, waitStrategyFactory, taskQueueFactory, taskBatchSize);
    }
}
