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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ThreadFactory;

/**
 * 默认的事件循环组
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/23
 * github - https://github.com/hl845740757
 */
public class DefaultEventLoopGroup extends AbstractFixedEventLoopGroup {

    public DefaultEventLoopGroup(int nThreads,
                                 @Nonnull ThreadFactory threadFactory,
                                 @Nonnull RejectedExecutionHandler rejectedExecutionHandler) {
        this(nThreads, threadFactory, rejectedExecutionHandler, null);
    }

    public DefaultEventLoopGroup(int nThreads,
                                 @Nonnull ThreadFactory threadFactory,
                                 @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                                 @Nullable EventLoopChooserFactory chooserFactory) {
        super(nThreads, threadFactory, rejectedExecutionHandler, chooserFactory, null);
    }

    @Nonnull
    @Override
    protected EventLoop newChild(int childIndex, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler, Object context) {
        return new DefaultEventLoop(this, threadFactory, rejectedExecutionHandler);
    }
}
