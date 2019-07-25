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

package com.wjybxx.fastjgame.concurrent;

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
public class DefaultEventLoop extends SingleThreadEventLoop{

    public DefaultEventLoop(@Nullable EventLoopGroup parent, @Nonnull ThreadFactory threadFactory) {
        super(parent, threadFactory);
    }

    public DefaultEventLoop(@Nullable EventLoopGroup parent, @Nonnull ThreadFactory threadFactory, @Nonnull RejectedExecutionHandler rejectedExecutionHandler) {
        super(parent, threadFactory, rejectedExecutionHandler);
    }

    /**
     * 它的事件循环仅仅是拉取提交的任务并执行
     */
    @Override
    protected void loop() {
        Runnable task;
        for (;;) {
            task = takeTask();
            if (null != task) {
                safeExecute(task);
            }
            // 检查是否需要退出
            if (confirmShutdown()) {
                break;
            }
        }
    }
}
