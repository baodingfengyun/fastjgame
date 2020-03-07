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

import java.util.concurrent.Executor;

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


}
