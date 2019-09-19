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

package com.wjybxx.fastjgame.timer;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * 定时器系统，非线程安全。
 *
 * <h3>时序保证</h3>
 * 1. {@link #newTimeout(long, TimerTask)}类型的任务之间，有严格的时序保证，当过期时间(超时时间)相同时，先提交的一定先执行。
 * 2. {@link #newFixedDelay(long, long, TimerTask)} {@link #newFixRate(long, long, TimerTask)}类型的任务，与其它任何一个任务都不具备时序保证。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/7
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public interface TimerSystem {

    // ------------------------------------------ 添加定时器的方法 -----------------------------------

    /**
     * 在指定延迟之后执行一次指定任务。
     * 该类型的任务有严格的时序保证！你认为先执行的一定先执行。
     *
     * @param timeout 过期时间，毫秒，如果参数小于等于0，则表示在下一次{@link #tick()}的时候执行。等效于调用{@link #nextTick(TimerTask)}。
     * @param task    需要执行的任务
     * @return Timer对应的句柄
     */
    @Nonnull
    TimeoutHandle newTimeout(long timeout, @Nonnull TimerTask<TimeoutHandle> task);

    /**
     * 下一次{@link #tick()}的时候执行
     *
     * @param task 需要执行的任务
     * @return Timer对应的句柄
     */
    default TimeoutHandle nextTick(@Nonnull TimerTask<TimeoutHandle> task) {
        return newTimeout(0, task);
    }

    /**
     * 以固定的延迟执行任务。
     * 它保证的是：两次任务的执行间隔大于等于指定延迟时间。
     * (注意：任何周期性的任务之间都不具备时序保证)
     *
     * @param initialDelay 首次执行延迟，毫秒。
     * @param delay        执行间隔，毫秒，必须大于0
     * @param task         定时执行的任务
     * @return Timer对应的句柄
     */
    @Nonnull
    FixedDelayHandle newFixedDelay(long initialDelay, long delay, @Nonnull TimerTask<FixedDelayHandle> task);

    /**
     * {@link #newFixedDelay(long, long, TimerTask)}的快捷调用方式，首次执行延迟为0，下一次tick的时候就会执行。
     */
    @Nonnull
    default FixedDelayHandle newFixedDelay(long delay, @Nonnull TimerTask<FixedDelayHandle> task) {
        return newFixedDelay(0, delay, task);
    }

    /**
     * 以固定的频率执行指定任务。
     * 它保证的是：任务的执行次数尽量达到目标。一般情况下不需要使用该类型，一般任务建议使用{@link #newFixedDelay(long, long, TimerTask)}。
     * (注意：任何周期性的任务之间都不具备时序保证)
     *
     * @param initialDelay 首次执行延迟，毫秒。
     * @param period       执行周期，毫秒，必须大于0
     * @param task         定时执行的任务
     * @return Timer对应的句柄
     */
    @Nonnull
    FixedRateHandle newFixRate(long initialDelay, long period, @Nonnull TimerTask<FixedRateHandle> task);

    /**
     * {@link #newFixRate(long, long, TimerTask)}的快捷调用方式，首次执行延迟为0，下一次tick的时候就会执行。
     */
    @Nonnull
    default FixedRateHandle newFixRate(long period, @Nonnull TimerTask<FixedRateHandle> task) {
        return newFixRate(0, period, task);
    }

    // ------------------------------------------- 生命周期相关 -----------------------------------

    /**
     * 检查timer执行。
     * 为什么有该方法？因为它没有独立的线程，必须有某个地方来通知它进行检查。
     */
    void tick();

    /**
     * timer系统是否已关闭。
     *
     * @return 当timer系统已关闭时返回true。
     */
    boolean isClosed();

    /**
     * 关闭timer系统，在这之前的任务都会被取消，新添加的任务都会直接进入终止状态！
     * 目的：显式的进行清理，避免内存泄漏（避免某一个handle引用了该timer系统，导致该timer系统上的所有任务都得不到释放的情况）
     */
    void close();

}
