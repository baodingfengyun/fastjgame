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
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/7
 * github - https://github.com/hl845740757
 */
public class EventLoopUtils {

    public static boolean inEventLoop(Executor executor) {
        return executor instanceof EventLoop && ((EventLoop) executor).inEventLoop();
    }

    /**
     * 检查是否在指定线程内，达成数据保护。
     *
     * @param msg 造成不安全的原因，尽量少拼接字符串
     */
    public static void ensureInEventLoop(EventLoop eventLoop, String msg) {
        if (!eventLoop.inEventLoop()) {
            throw new GuardedOperationException(msg);
        }
    }

    public static void ensureInEventLoop(EventLoop eventLoop) {
        ensureInEventLoop(eventLoop, "Must be called from EventLoop thread");
    }

    /**
     * 检查死锁，由于EventLoop是单线程的，因此不能在当前EventLoop上等待另一个任务完成，很可能导致死锁。
     *
     * @param msg 造成死锁的信息，尽量少拼接字符串。
     */
    public static void checkDeadLock(EventLoop eventLoop, String msg) {
        if (eventLoop.inEventLoop()) {
            throw new BlockingOperationException(msg);
        }
    }

    public static void checkDeadLock(EventLoop e) {
        checkDeadLock(e, "Can't call from EventLoop thread");
    }

    /**
     * 如果当前线程就是EventLoop线程，则直接执行任务，否则进行提交
     *
     * @param eventLoop 事件循环
     * @param task      任务
     */
    public static void executeOrRun(@Nonnull EventLoop eventLoop, Runnable task) {
        if (eventLoop.inEventLoop()) {
            task.run();
        } else {
            eventLoop.execute(task);
        }
    }

    /**
     * 如果当前线程就是EventLoop线程，则直接执行任务，否则进行提交
     *
     * @param eventLoop 事件循环
     * @param task      任务
     * @return future
     */
    public static BlockingFuture<?> submitOrRun(@Nonnull EventLoop eventLoop, Runnable task) {
        return submitOrRun(eventLoop, Executors.callable(task, null));
    }

    /**
     * 如果当前线程就是EventLoop线程，则直接执行任务，否则进行提交
     *
     * @param eventLoop 事件循环
     * @param task      任务
     * @return future
     */
    public static <V> BlockingFuture<V> submitOrRun(@Nonnull EventLoop eventLoop, Callable<V> task) {
        if (eventLoop.inEventLoop()) {
            try {
                V result = task.call();
                return new SucceededFuture<>(eventLoop, result);
            } catch (Throwable e) {
                return new FailedFuture<>(eventLoop, e);
            }
        } else {
            return eventLoop.submit(task);
        }
    }
}
