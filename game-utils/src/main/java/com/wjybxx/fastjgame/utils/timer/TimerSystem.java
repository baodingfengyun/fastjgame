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

package com.wjybxx.fastjgame.utils.timer;

import com.wjybxx.fastjgame.utils.timeprovider.TimeProvider;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * 定时器系统，非线程安全。
 *
 * <h3>时序保证</h3>
 * 1. {@link #newTimeout(long, TimerTask)}和{@link #nextTick(TimerTask)}类型的任务之间，有严格的时序保证，当过期时间(超时时间)相同时，先提交的一定先执行。
 * 2. {@link #newFixedDelay(long, long, TimerTask)} {@link #newFixRate(long, long, TimerTask)}类型的任务，与其它任何一个任务都不具备时序保证。
 * <p>
 * Q: 为什么继承{@link TimeProvider}？
 * A: timer的运行一定依赖于一个时钟，可以把该时钟告诉给用户。
 * <p>
 * 注意：子类实现必须在保证时序的条件下解决可能的死循环问题。
 * Q: 死循环是如何产生的？
 * A: 如果用户基于{@link #newTimeout(long, TimerTask)}实现循环，则可能添加一个立即执行的timer，而{@link TimerSystem}的没有独立线程的，因此循环创建立即执行的timer，
 * 则可能陷入死循环。这种情况一般不是有意为之，而是某些特殊情况下产生的，比如：下次执行的延迟是计算出来的，而算出来的延迟总是为0或负数。
 *
 * @author wjybxx
 * @version 1.1
 * date - 2019/8/7
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public interface TimerSystem extends TimeProvider {

    // ------------------------------------------ 添加定时器的方法 -----------------------------------

    /**
     * 下一次{@link #tick()}的时候执行。
     *
     * @param task 需要执行的任务
     * @return Timer对应的句柄
     */
    default TimeoutHandle nextTick(@Nonnull TimerTask task) {
        return newTimeout(0, task);
    }

    /**
     * 在指定延迟之后执行一次指定任务。
     * 该类型的任务有严格的时序保证！你认为先执行的一定先执行。
     *
     * @param timeout 过期时间，毫秒，如果参数小于等于0，等效于调用{@link #nextTick(TimerTask)}。
     * @param task    需要执行的任务
     * @return Timer对应的句柄
     */
    @Nonnull
    TimeoutHandle newTimeout(long timeout, @Nonnull TimerTask task);

    @Nonnull
    default FixedDelayHandle newFixedDelay(long delay, @Nonnull TimerTask task) {
        return newFixedDelay(delay, delay, task);
    }

    /**
     * 以固定的延迟执行任务。
     * 它保证的是：任务的执行间隔大于等于指定延迟时间。
     * (注意：任何周期性的任务与其它任务之间都不具备时序保证)
     *
     * @param initialDelay 首次执行延迟，毫秒。
     * @param delay        执行间隔，毫秒，必须大于0
     * @param task         定时执行的任务
     * @return Timer对应的句柄
     */
    @Nonnull
    FixedDelayHandle newFixedDelay(long initialDelay, long delay, @Nonnull TimerTask task);

    @Nonnull
    default FixedRateHandle newFixRate(long period, @Nonnull TimerTask task) {
        return newFixRate(period, period, task);
    }

    /**
     * 以固定的频率执行指定任务。
     * 它保证的是：任务的执行次数尽量达到目标。一般情况下不需要使用该类型，一般任务建议使用{@link #newFixedDelay(long, long, TimerTask)}。
     * (注意：任何周期性的任务与其它任务之间都不具备时序保证)
     *
     * @param initialDelay 首次执行延迟，毫秒。
     * @param period       执行周期，毫秒，必须大于0
     * @param task         定时执行的任务
     * @return Timer对应的句柄
     */
    @Nonnull
    FixedRateHandle newFixRate(long initialDelay, long period, @Nonnull TimerTask task);

    // ------------------------------------------- 生命周期相关 -----------------------------------

    /**
     * 检查timer执行。
     * <p>
     * Q: 为什么有该方法？
     * A: 因为它没有独立的线程，必须有某个地方来通知它进行检查。因此，递归调用tick将抛出异常。
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

    // ------------------------------------------- 系统时钟 --------------------------------------------

    @Override
    long curTimeMillis();

    @Override
    int curTimeSeconds();
}
