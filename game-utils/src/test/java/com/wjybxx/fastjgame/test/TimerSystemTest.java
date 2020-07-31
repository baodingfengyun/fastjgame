/*
 *
 *  * Copyright 2019 wjybxx
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.wjybxx.fastjgame.test;

import com.wjybxx.fastjgame.utils.time.TimeUtils;
import com.wjybxx.fastjgame.utils.time.CachedTimeProvider;
import com.wjybxx.fastjgame.utils.time.TimeProviders;
import com.wjybxx.fastjgame.utils.timer.DefaultTimerSystem;
import com.wjybxx.fastjgame.utils.timer.TimerHandle;
import com.wjybxx.fastjgame.utils.timer.TimerSystem;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.IntStream;

/**
 * 新的定时器系统的测试
 * <p>
 * 节选输出：
 * --------------- sleepTimeSec 9 ---------------------- （初始）
 * timeoutTask1 1565234223563
 * timeoutTask2 1565234223563
 * fixDelayTask 1565234223563
 * fixRateTask 1565234223563
 * fixRateTask 1565234223564
 * fixRateTask 1565234223564
 * <p>
 * --------------- sleepTimeSec 7 ---------------------- （中途）
 * fixRateTask 1565234292568
 * fixRateTask 1565234292568
 * fixRateTask 1565234292568
 * fixDelayTask 1565234292568
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/8
 * github - https://github.com/hl845740757
 */
public class TimerSystemTest {

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        final CachedTimeProvider timeProvider = TimeProviders.newCachedTimeProvider(System.currentTimeMillis());
        final TimerSystem timerSystem = new DefaultTimerSystem(timeProvider);

        // 局部变量是为了调试(debug能获取到引用)

        final TimerHandle handle0 = timerSystem.newTimeout(2 * TimeUtils.SEC, TimerSystemTest::nextTick);

        final TimerHandle handle1 = timerSystem.newTimeout(2 * TimeUtils.SEC, handle -> {
            System.out.println("two second " + System.currentTimeMillis());
            timerSystem.newTimeout(-1, handle01 -> System.out.println("newTimeout - handle01"));
        });

        final TimerHandle handle2 = timerSystem.newTimeout(2 * TimeUtils.SEC, TimerSystemTest::handle02);

        final TimerHandle handle3 = timerSystem.newTimeout(TimeUtils.SEC, timerHandle -> {
            System.out.println("one second " + System.currentTimeMillis());
        });

        final TimerHandle handle4 = timerSystem.newFixedDelay(0, 3 * TimeUtils.SEC, handle -> {
            System.out.println("fixDelayTask " + System.currentTimeMillis());
        });

        final TimerHandle handle5 = timerSystem.newFixRate(0, 3 * TimeUtils.SEC, handle -> {
            System.out.println("fixRateTask " + System.currentTimeMillis());
        });

        IntStream.rangeClosed(1, 10).forEach(index -> {
            timeProvider.update(System.currentTimeMillis());

            timerSystem.tick();
            // 睡眠的时长不同，会打乱执行节奏
            int sleepTimeSec = ThreadLocalRandom.current().nextInt(1, 10);
            System.out.println("\n --------------- sleepTimeSec " + sleepTimeSec + " ----------------------");
            LockSupport.parkNanos(TimeUtils.NANO_PER_MILLISECOND * TimeUtils.SEC * sleepTimeSec);
        });

        timerSystem.close();
    }

    private static void nextTick(TimerHandle handle) {
        System.out.println("two second2 " + System.currentTimeMillis());
        handle.timerSystem().nextTick(handle0 -> System.out.println("nextTick - handle0"));
    }

    private static void handle02(TimerHandle handle) {
        System.out.println("two second2 " + System.currentTimeMillis());
        handle.timerSystem().newTimeout(-2, handle02 -> System.out.println("newTimeout - handle02"));
    }

}
