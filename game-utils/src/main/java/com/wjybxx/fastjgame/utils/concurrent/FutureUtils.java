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

package com.wjybxx.fastjgame.utils.concurrent;

import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/6
 */
public class FutureUtils {

    private static final Logger logger = LoggerFactory.getLogger(FutureUtils.class);

    private FutureUtils() {
    }

    /**
     * 用于{@link BlockingFuture#get()} 和{@link BlockingFuture#get(long, TimeUnit)}抛出失败异常。
     *
     * @param cause 任务失败的原因
     * @throws CancellationException 如果任务被取消，则抛出该异常
     * @throws ExecutionException    其它原因导致失败
     */
    public static <T> T rethrowGet(Throwable cause) throws CancellationException, ExecutionException {
        if (cause instanceof CancellationException) {
            throw (CancellationException) cause;
        }
        throw new ExecutionException(cause);
    }

    /**
     * 用于{@link ListenableFuture#getNow()}和{@link BlockingFuture#join()}抛出失败异常。
     * <p>
     * 不命名为{@code rethrowGetNow}是为了放大不同之处。
     *
     * @param cause 任务失败的原因
     * @throws CancellationException 如果任务被取消，则抛出该异常
     * @throws CompletionException   其它原因导致失败
     */
    public static <T> T rethrowJoin(@Nonnull Throwable cause) throws CancellationException, CompletionException {
        if (cause instanceof CancellationException) {
            throw (CancellationException) cause;
        }
        throw new CompletionException(cause);
    }

    /**
     * 聚合{@link FutureListenerEntry}
     *
     * @param listenerEntries 当前的所有监听器们
     * @param listenerEntry   新增的监听器
     * @return 合并后的结果
     */
    @Nonnull
    static Object aggregateListenerEntry(@Nullable Object listenerEntries, @Nonnull FutureListenerEntry<?> listenerEntry) {
        if (listenerEntries == null) {
            return listenerEntry;
        }
        if (listenerEntries instanceof FutureListenerEntry) {
            return new FutureListenerEntries((FutureListenerEntry<?>) listenerEntries, listenerEntry);
        } else {
            ((FutureListenerEntries) listenerEntries).addChild(listenerEntry);
            return listenerEntries;
        }
    }

    // ------------------------------------------------ 通知监听器 ----------------------------------------

    /**
     * 通知持有的监听器
     * <p>
     * 如果在通知监听器时，使用{@link EventLoop#inEventLoop()}，将可能造成时序问题，例子如下：
     * 1.线程A添加了监听器A，需要在线程B执行。
     * 2. 线程C将future置为完成状态，线程C进行通知，在通知监听器A时{@link EventLoop#inEventLoop()}判断false，
     * 于是提交了任务到线程B，通知结束。
     * 3. 线程B添加了一个监听器B,需要在线程B执行。由于此时future已经是完成状态了，如果B也可以通知，在通知监听器B时，{@link EventLoop#inEventLoop()}判断为true，
     * 监听器B将立即执行。
     * 4. 线程B执行线程C提交的通知任务，执行监听器A。
     * 在这个例子中：执行环境相同的监听器A和B，先添加的监听器A没有先执行，而后添加的监听器B却先执行了。
     * <p>
     * 要解决这个问题，有两种方案:
     * 1. 去掉{@link EventLoop#inEventLoop()}检测，总是以任务的形式提交，那么A就会先执行。这也是常见架构的方式。
     * 2. 使用指定线程进行通知。这是{@link EventLoop}架构下特定的通知方式。
     * <p>
     * Q: 为什么选择指定线程通知的方式？
     * A: 这里存在一些假设：我们认为{@link ListenableFuture#defaultExecutor()}下执行的回调是最多的。
     * 那么使用{@link ListenableFuture#defaultExecutor()}进行通知，将具有最小的任务提交数，最小的开销！
     * ps:创建future指定合适的eventLoop很有用哦。
     * <p>
     * {@link EventLoop#inEventLoop()}是把双刃剑，一旦使用错误，可能导致严重问题。
     */
    public static <V> void notifyAllListenerNowSafely(@Nonnull final ListenableFuture<V> future, @Nonnull final Object listenerEntries) {
        EventLoopUtils.ensureInEventLoop(future.defaultExecutor(), "Notify listeners must call from default executor");

        if (listenerEntries instanceof FutureListenerEntry) {
            notifyListenerNowSafely(future, (FutureListenerEntry) listenerEntries);
        } else {
            final FutureListenerEntries futureListenerEntries = (FutureListenerEntries) listenerEntries;
            final FutureListenerEntry<?>[] children = futureListenerEntries.getChildren();
            final int size = futureListenerEntries.getSize();
            for (int index = 0; index < size; index++) {
                notifyListenerNowSafely(future, children[index]);
            }
        }
    }

    /**
     * 一定不要开放该方法，只允许{@link #notifyAllListenerNowSafely(ListenableFuture, Object)}调用。
     */
    private static <V> void notifyListenerNowSafely(@Nonnull ListenableFuture<V> future, @Nonnull FutureListenerEntry listenerEntry) {
        if (EventLoopUtils.inEventLoop(listenerEntry.executor)) {
            notifyListenerNowSafely(future, listenerEntry.listener);
        } else {
            final FutureListener listener = listenerEntry.listener;
            ConcurrentUtils.safeExecute(listenerEntry.executor, () -> notifyListenerNowSafely(future, listener));
        }
    }

    /**
     * 一定不要开放该方法，只允许{@link #notifyListenerNowSafely(ListenableFuture, FutureListenerEntry)}调用。
     */
    @SuppressWarnings({"unchecked"})
    private static <V> void notifyListenerNowSafely(@Nonnull ListenableFuture<V> future, @Nonnull FutureListener listener) {
        try {
            listener.onComplete(future);
        } catch (Throwable e) {
            logger.warn("An exception was thrown by " + future.getClass().getName() + ".onComplete()", e);
        }
    }
}
