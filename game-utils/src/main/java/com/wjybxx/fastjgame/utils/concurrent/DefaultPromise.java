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

import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.concurrent.internal.AbstractPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * 默认的{@link Promise}实现。
 * 去掉了netty中的很多我们不需要使用的东西，以及去除部分优化(降低复杂度)。
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
    private List<FutureListenerEntry<? super V>> listenerEntries = null;
    /**
     * 当前是否有线程正在通知监听器们。我们必须阻止并发的通知 和 保持监听器的先入先出顺序(先添加的先被通知)。
     */
    @GuardedBy("this")
    private boolean notifyingListeners = false;

    /**
     * 默认的通知线程，只有在该线程下才可以进行派发。
     */
    private final EventLoop defaultExecutor;

    public DefaultPromise(@Nonnull EventLoop defaultExecutor) {
        this.defaultExecutor = defaultExecutor;
    }

    @Override
    public EventLoop defaultExecutor() {
        return defaultExecutor;
    }

    @Override
    protected void checkDeadlock() {
        // 基础实现认为默认的通知线程就是工作者线程
        EventLoopUtils.checkDeadLock(defaultExecutor);
    }

    @Override
    public Promise<V> await() throws InterruptedException {
        super.await();
        return this;
    }

    @Override
    public Promise<V> awaitUninterruptibly() {
        super.awaitUninterruptibly();
        return this;
    }

    // ------------------------------------------------- 添加监听器 ------------------------------------------

    @Override
    public Promise<V> onComplete(@Nonnull FutureListener<? super V> listener) {
        // null is safe
        addListener(new FutureListenerEntry<>(listener, defaultExecutor));
        return this;
    }

    @Override
    public Promise<V> onComplete(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        addListener(new FutureListenerEntry<>(listener, bindExecutor));
        return this;
    }

    private void addListener(@Nonnull FutureListenerEntry<? super V> listenerEntry) {
        // 不管是否已完成，先加入等待通知集合
        synchronized (this) {
            _addListener0(listenerEntry);
        }

        // 必须检查完成状态，如果已进入完成状态，通知刚刚加入监听器们（否则可能丢失通知）（早已完成的状态下）
        // 因为状态改变 和 锁没有关系
        if (isDone()) {
            notifyAllListeners();
        }
    }

    private void _addListener0(@Nonnull FutureListenerEntry<? super V> listenerEntry) {
        if (listenerEntries == null) {
            listenerEntries = new ArrayList<>(4);
        }
        listenerEntries.add(listenerEntry);
    }

    @Override
    public Promise<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener) {
        addListener(new FutureListenerEntry<>(listener, defaultExecutor));
        return this;
    }

    @Override
    public Promise<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        addListener(new FutureListenerEntry<>((FutureListener<? super V>) listener, bindExecutor));
        return this;
    }

    @Override
    public Promise<V> onFailure(@Nonnull FailedFutureListener<? super V> listener) {
        addListener(new FutureListenerEntry<>(listener, defaultExecutor));
        return this;
    }

    @Override
    public Promise<V> onFailure(@Nonnull FailedFutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        addListener(new FutureListenerEntry<>((FutureListener<? super V>) listener, bindExecutor));
        return this;
    }

    // ------------------------------------------------- 通知监听器 ------------------------------------------

    @Override
    protected final boolean needNotifyListeners() {
        // 该方法运行在锁下 - 超类在调用该方法前持有了锁
        return listenerEntries != null && !notifyingListeners;
    }

    @Override
    protected final void notifyAllListeners() {
        if (defaultExecutor.inEventLoop()) {
            notifyAllListenersNow();
        } else {
            ConcurrentUtils.safeExecute(defaultExecutor, this::notifyAllListenersNow);
        }
    }

    private void notifyAllListenersNow() {
        // 用于拉取最新的监听器，避免长时间的占有锁
        List<FutureListenerEntry<? super V>> listenerEntries;
        synchronized (this) {
            // 有线程正在进行通知 或当前 没有监听器，则不需要当前线程进行通知
            if (notifyingListeners || null == this.listenerEntries) {
                return;
            }
            // 标记为正在通知(每一个正在通知的线程都会将所有的监听器通知一遍)
            notifyingListeners = true;
            listenerEntries = this.listenerEntries;
            this.listenerEntries = null;
        }

        for (; ; ) {
            // 通知当前批次的监听器(此时不需要获得锁) -- 但是这里不能抛出异常，否则可能死锁 -- notifyingListeners无法恢复
            notifyAllListenerNowSafely(this, listenerEntries);

            // 通知完当前批次后，检查是否有新的监听器加入
            synchronized (this) {
                if (null == this.listenerEntries) {
                    // 通知完毕
                    this.notifyingListeners = false;
                    break;
                }
                // 有新的监听器加入，拉取最新的监听器，继续通知 -- 可以保证被通知的顺序
                listenerEntries = this.listenerEntries;
                this.listenerEntries = null;
            }
        }
    }

    public static <V> void notifyAllListenerNowSafely(NFuture<V> future, List<FutureListenerEntry<? super V>> listenerEntries) {
        for (FutureListenerEntry<? super V> listenerEntry : listenerEntries) {
            notifyListenerNowSafely(future, listenerEntry);
        }
    }

    /**
     * 安全的通知一个监听器，不可以抛出异常，否则可能死锁，必须允许在默认的通知线程下
     *
     * @param future        future
     * @param listenerEntry 监听器信息
     */
    @SuppressWarnings({"unchecked"})
    public static <V> void notifyListenerNowSafely(@Nonnull NFuture<V> future, @Nonnull FutureListenerEntry listenerEntry) {
        try {
            if (EventLoopUtils.inEventLoop(listenerEntry.executor)) {
                listenerEntry.listener.onComplete(future);
            } else {
                ConcurrentUtils.safeExecute(listenerEntry.executor, newNotifyTask(future, listenerEntry));
            }
        } catch (Throwable e) {
            logger.warn("An exception was thrown by " + future.getClass().getName() + ".onComplete()", e);
        }
    }

    /**
     * 创建一个新的通知任务
     */
    private static <V> Runnable newNotifyTask(@Nonnull NFuture<V> future, @Nonnull FutureListenerEntry listener) {
        return () -> notifyListenerNowSafely(future, listener);
    }
}
