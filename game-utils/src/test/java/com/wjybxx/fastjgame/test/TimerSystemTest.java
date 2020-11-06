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

import com.wjybxx.fastjgame.util.time.CachedTimeProvider;
import com.wjybxx.fastjgame.util.time.TimeProviders;
import com.wjybxx.fastjgame.util.time.TimeUtils;
import com.wjybxx.fastjgame.util.timer.DefaultTimerSystem;
import com.wjybxx.fastjgame.util.timer.FixedTimesHandle;
import com.wjybxx.fastjgame.util.timer.TimerHandle;
import com.wjybxx.fastjgame.util.timer.TimerSystem;

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
        final TimerHandle handle00 = timerSystem.newTimeout(TimeUtils.SEC, timerHandle -> {
            System.out.println("one second " + System.currentTimeMillis());
        });

        final TimerHandle handle1 = timerSystem.newTimeout(2 * TimeUtils.SEC, handle -> {
            System.out.println("two second " + System.currentTimeMillis());
        });

        final TimerHandle handle2 = timerSystem.newTimeout(2 * TimeUtils.SEC, TimerSystemTest::callNextTick);
        final TimerHandle handle3 = timerSystem.newTimeout(2 * TimeUtils.SEC, TimerSystemTest::callNewTimeout);

        final TimerHandle handle4 = timerSystem.newFixedDelay(0, 3 * TimeUtils.SEC, handle -> {
            System.out.println("fixDelayTask " + System.currentTimeMillis());
        });

        final TimerHandle handle5 = timerSystem.newFixedRate(0, 3 * TimeUtils.SEC, handle -> {
            System.out.println("fixRateTask " + System.currentTimeMillis());
        });

        final TimerHandle handle6 = timerSystem.newFixedTimes(0, 3 * TimeUtils.SEC, 3, handle -> {
            final FixedTimesHandle h = (FixedTimesHandle) handle;
            System.out.println(String.format("fixedTimes, executedTimes: %s, remianTimes: %s", h.executedTimes(), h.remainTimes()));
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

    private static void callNextTick(TimerHandle handle) {
        System.out.println("callNextTick");
        handle.timerSystem().nextTick(handle0 -> System.out.println("callNextTick callback"));
    }

    private static void callNewTimeout(TimerHandle handle) {
        System.out.println("callNewTimeout");
        handle.timerSystem().newTimeout(-2, handle02 -> System.out.println("callNewTimeout callback"));
    }

}
