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

    public static <V> void notifyAllListenerNowSafely(@Nonnull final ListenableFuture<V> future, @Nonnull final Object listenerEntries) {
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

    public static <V> void notifyListenerNowSafely(@Nonnull ListenableFuture<V> future, @Nonnull FutureListenerEntry listenerEntry) {
        if (EventLoopUtils.inEventLoop(listenerEntry.executor)) {
            notifyListenerNowSafely(future, listenerEntry.listener);
        } else {
            final FutureListener listener = listenerEntry.listener;
            ConcurrentUtils.safeExecute(listenerEntry.executor, () -> notifyListenerNowSafely(future, listener));
        }
    }

    @SuppressWarnings({"unchecked"})
    private static <V> void notifyListenerNowSafely(@Nonnull ListenableFuture<V> future, @Nonnull FutureListener listener) {
        try {
            listener.onComplete(future);
        } catch (Throwable e) {
            logger.warn("An exception was thrown by " + future.getClass().getName() + ".onComplete()", e);
        }
    }
}
