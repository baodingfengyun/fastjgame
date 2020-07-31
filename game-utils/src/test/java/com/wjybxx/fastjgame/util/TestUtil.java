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

package com.wjybxx.fastjgame.util;

import com.wjybxx.fastjgame.utils.concurrent.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.time.TimeUtils;
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/6/2
 */
public class TestUtil {

    public static final long TEST_TIMEOUT = 15 * 1000;

    public static void joinQuietly(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException ignore) {

        }
    }

    public static void sleepQuietly(long timeMs) {
        LockSupport.parkNanos(TimeUtils.NANO_PER_MILLISECOND * timeMs);
    }

    public static void startAndJoin(List<Thread> producers, EventLoop eventLoop, long runTimeMs) {
        eventLoop.execute(ConcurrentUtils.NO_OP_TASK);
        producers.forEach(Thread::start);

        sleepQuietly(runTimeMs);
        eventLoop.shutdownNow();

        producers.forEach(TestUtil::joinQuietly);
        eventLoop.terminationFuture().join();
    }

    public static void startAndJoin(List<Thread> threads, AtomicBoolean stop, long runTimeMs) {
        threads.forEach(Thread::start);

        sleepQuietly(runTimeMs);
        stop.set(true);

        threads.forEach(TestUtil::joinQuietly);

        // 重置
        stop.set(false);
    }
}
