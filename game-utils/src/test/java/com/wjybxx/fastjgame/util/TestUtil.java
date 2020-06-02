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

import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.TimeUtils;
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;

import java.util.Arrays;
import java.util.concurrent.locks.LockSupport;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/6/2
 */
public class TestUtil {

    public static void joinQuietly(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException ignore) {

        }
    }

    public static void sleepQuietly(long timeMs) {
        LockSupport.parkNanos(TimeUtils.NANO_PER_MILLISECOND * timeMs);
    }

    public static void startAndJoin(Thread producer, EventLoop eventLoop, long runTimeMs) {
        producer.start();
        eventLoop.execute(ConcurrentUtils.NO_OP_TASK);

        sleepQuietly(runTimeMs);
        eventLoop.shutdownNow();

        joinQuietly(producer);
        eventLoop.terminationFuture().join();
    }

    public static void startAndJoin(Thread[] producer, EventLoop eventLoop, long runTimeMs) {
        Arrays.stream(producer).forEach(Thread::start);
        eventLoop.execute(ConcurrentUtils.NO_OP_TASK);

        sleepQuietly(runTimeMs);
        eventLoop.shutdownNow();

        Arrays.stream(producer).forEach(TestUtil::joinQuietly);
        eventLoop.terminationFuture().join();
    }
}
