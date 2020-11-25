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

package com.wjybxx.fastjgame.util.timer;


import com.wjybxx.fastjgame.util.misc.Tuple2;
import com.wjybxx.fastjgame.util.misc.Tuple3;
import com.wjybxx.fastjgame.util.time.TimeProvider;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * 定时器系统，非线程安全。
 *
 * <h3>时序保证</h3>
 * 1. 单次执行的任务，如{@link #newTimeout(long, TimerTask)}，有严格的时序保证，当过期时间(超时时间)相同时，先提交的一定先执行。
 * 2. 周期性执行的的任务，如{@link #newFixedDelay(long, long, TimerTask)}，当进入周期运行时，与其它任务之间便不具备时序保证。
 * <h3>上下文信息</h3>
 * 建议将任务依赖的上下文信息使用{@link TimerHandle#attach(Object)}绑定到{@link TimerHandle}，以方便使用方法引用创建timer，不建议使用lambda捕获参数。
 * 参数较少时可以使用{@link Tuple2}或{@link Tuple3}，参数较多时建议定义具体的类型。
 * <p>
 * 注意：子类实现必须在保证时序的条件下解决可能的死循环问题。
 * Q: 死循环是如何产生的？
 * A: 对于周期性任务，我们严格要求了周期间隔大于0，因此周期性的timer不会引发无限循环问题。
 * 但如果用户基于{@link #newTimeout(long, TimerTask)}实现循环，则在执行回调时可能添加一个立即执行的timer（超时时间小于等于0），则可能陷入死循环。
 * 这种情况一般不是有意为之，而是某些特殊情况下产生的，比如：下次执行的延迟是计算出来的，而算出来的延迟总是为0或负数（线程缓存了时间戳，导致计算结果同一帧不会变化）。
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
     * 创建一个在指定延迟之后执行一次的任务。
     * 该类型的任务有严格的时序保证！你认为先执行的一定先执行。
     *
     * @param timeout 过期时间，毫秒。允许小于0，但如果小于0，可能会影响当前帧的部分timer。
     * @param task    需要执行的任务
     * @return Timer对应的句柄
     */
    @Nonnull
    TimeoutHandle newTimeout(long timeout, @Nonnull TimerTask task);

    /**
     * 创建一个以固定延迟执行的任务。
     * 它保证的是：任务的执行间隔大于等于指定延迟时间。
     * (注意：任何周期性的任务与其它任务之间都不具备时序保证)
     *
     * @param initialDelay 首次执行延迟，毫秒。允许小于0，但如果小于0，可能会影响当前帧的部分timer。
     * @param delay        执行间隔，毫秒，必须大于0
     * @param task         定时执行的任务
     * @return Timer对应的句柄
     */
    @Nonnull
    FixedDelayHandle newFixedDelay(long initialDelay, long delay, @Nonnull TimerTask task);

    /**
     * 创建一个以固定频率执行的任务。
     * 它保证的是：指定时间内执行次数尽量满足需求。
     * (注意：任何周期性的任务与其它任务之间都不具备时序保证)
     *
     * @param initialDelay 首次执行延迟，毫秒。允许小于0，但如果小于0，可能会影响当前帧的部分timer。
     * @param period       执行间隔，毫秒，必须大于0
     * @param task         定时执行的任务
     * @return Timer对应的句柄
     */
    @Nonnull
    FixedRateHandle newFixedRate(long initialDelay, long period, @Nonnull TimerTask task);

    /**
     * 创建一个以固定延迟执行，并执行指定次数的任务。
     * 目前设计中，delay的设计与{@link FixedDelayHandle}相同。
     * (注意：任何周期性的任务与其它任务之间都不具备时序保证)
     *
     * @param initialDelay 首次执行延迟，毫秒。允许小于0，但如果小于0，可能会影响当前帧的部分timer。
     * @param period       执行间隔，毫秒，必须大于0。
     * @param times        期望的执行次数，必须大于0。
     * @param task         定时执行的任务
     * @return Timer对应的句柄
     */
    @Nonnull
    FixedTimesHandle newFixedTimes(long initialDelay, long period, int times, @Nonnull TimerTask task);

    /**
     * @param period 执行间隔，毫秒，必须大于0。
     * @param task   定时执行的任务
     * @return Timer对应的句柄
     */
    default FixedDelayHandle newHeartbeatTimer(long period, TimerTask task) {
        final FixedDelayHandle fixedDelayHandle = newFixedDelay(period, period, task);
        fixedDelayHandle.setAutoCloseOnExceptionCaught(false);
        return fixedDelayHandle;
    }

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
     * 向已关闭的timer系统中添加timer会抛出{@link IllegalStateException}
     *
     * @return 当timer系统已关闭时返回true。
     */
    boolean isClosed();

    /**
     * 关闭timer系统。
     * 目的：显式的进行清理，避免内存泄漏（避免某一个handle引用了该timer系统，导致该timer系统上的所有任务都得不到释放的情况）
     */
    void close();

}
