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

import com.wjybxx.fastjgame.utils.CollectionUtils;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.concurrent.internal.AbstractPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

/**
 * 默认的{@link Promise}实现。
 * 去掉了netty中的很多我们不需要使用的东西，以及去除部分优化(降低复杂度)。
 * <p>
 *
 * </pre>
 * <p>
 * 建议使用 {@link EventLoop#newPromise()}代替构造方法。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14
 * github - https://github.com/hl845740757
 */
public class DefaultPromise<V> extends AbstractPromise<V> implements ListenableFuture<V>, Promise<V> {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPromise.class);

    /**
     * 该future上注册的监听器们。
     * 我们使用Null表示一个监听器也没有的状态，因此在删除监听器时，如果size==0，我们会置为null。
     */
    @GuardedBy("this")
    private List<FutureListener<? super V>> listeners = null;
    /**
     * 当前是否有线程正在通知监听器们。我们必须阻止并发的通知 和 保持监听器的先入先出顺序(先添加的先被通知)。
     */
    @GuardedBy("this")
    private boolean notifyingListeners = false;

    /**
     * 默认的通知线程
     */
    private final EventLoop defaultExecutor;

    public DefaultPromise(@Nonnull EventLoop defaultExecutor) {
        this.defaultExecutor = defaultExecutor;
    }

    @Nonnull
    @Override
    public EventLoop defaultExecutor() {
        return defaultExecutor;
    }

    @Override
    protected void checkDeadlock() {
        // 基础实现认为默认的通知线程就是工作者线程
        ConcurrentUtils.checkDeadLock(defaultExecutor);
    }

    @Override
    protected synchronized boolean needNotifyListeners() {
        return listeners != null && !notifyingListeners;
    }

    @Override
    protected void notifyAllListeners() {
        // 用于拉取最新的监听器，避免长时间的占有锁
        List<FutureListener<? super V>> listeners;
        synchronized (this) {
            // 有线程正在进行通知 或当前 没有监听器，则不需要当前线程进行通知
            if (notifyingListeners || null == this.listeners) {
                return;
            }
            // 标记为正在通知(每一个正在通知的线程都会将所有的监听器通知一遍)
            notifyingListeners = true;
            listeners = this.listeners;
            this.listeners = null;
        }

        for (; ; ) {
            // 通知当前批次的监听器(此时不需要获得锁) -- 但是这里不能抛出异常，否则可能死锁 -- notifyingListeners无法恢复
            for (FutureListener<? super V> futureListener : listeners) {
                notifyListenerNowSafely(this, futureListener);
            }
            // 通知完当前批次后，检查是否有新的监听器加入
            synchronized (this) {
                if (null == this.listeners) {
                    // 通知完毕
                    this.notifyingListeners = false;
                    break;
                }
                // 有新的监听器加入，拉取最新的监听器，继续通知 -- 可以保证被通知的顺序
                listeners = this.listeners;
                this.listeners = null;
            }
        }
    }

    /**
     * 安全的通知一个监听器，不可以抛出异常，否则可能死锁
     *
     * @param future   future
     * @param listener 监听器
     */
    public static <V> void notifyListenerNowSafely(@Nonnull NonBlockingFuture<V> future, @Nonnull FutureListener<? super V> listener) {
        try {
            @SuppressWarnings("unchecked") FutureListener<V> castListener = (FutureListener<V>) listener;
            castListener.onComplete(future);
        } catch (Throwable e) {
            logger.warn("An exception was thrown by " + future.getClass().getName() + ".onComplete()", e);
        }
    }


    // ------------------------------------------------- 监听器 ------------------------------------------

    @Override
    public ListenableFuture<V> onComplete(@Nonnull FutureListener<? super V> listener) {
        // null is safe
        addListener0(listener);
        return this;
    }

    @Override
    public ListenableFuture<V> onComplete(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        addListener0(new ExecutorBindListener<>(listener, bindExecutor));
        return this;
    }

    protected final void addListener0(@Nonnull FutureListener<? super V> listener) {
        // 不管是否已完成，先加入等待通知集合
        synchronized (this) {
            if (listeners == null) {
                listeners = new LinkedList<>();
            }
            listeners.add(listener);
        }

        // 必须检查完成状态，如果已进入完成状态，通知刚刚加入监听器们（否则可能丢失通知）（早已完成的状态下）
        // 因为状态改变 和 锁没有关系
        if (isDone()) {
            notifyAllListeners();
        }
    }

    /**
     * 移除第一个匹配的监听器
     *
     * @param predicate 监听器测试条件
     * @return 是否成功删除了一个监听器
     */
    protected final boolean removeFirstMatchListener(Predicate<FutureListener<?>> predicate) {
        synchronized (this) {
            if (listeners == null) {
                return false;
            }

            boolean success = CollectionUtils.removeFirstMatch(listeners, predicate);

            if (listeners.size() == 0) {
                listeners = null;
            }
            return success;
        }
    }


}
