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
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * ThreadPerMessage Design Pattern
 * 正常情况下不推荐使用，会造成大量的资源浪费。
 * 这里是为了保证能创建足够的线程用的。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14
 * github - https://github.com/hl845740757
 */
public final class ThreadPerTaskExecutor implements Executor {

    private final ThreadFactory threadFactory;

    public ThreadPerTaskExecutor(ThreadFactory threadFactory) {
        if (threadFactory == null) {
            throw new NullPointerException("threadFactory");
        }
        this.threadFactory = threadFactory;
    }

    @Override
    public void execute(@Nonnull Runnable command) {
        // 为每一个任务新建一个线程
        threadFactory.newThread(command).start();
    }
}