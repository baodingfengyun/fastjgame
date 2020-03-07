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

package com.wjybxx.fastjgame.utils.concurrent.fast;

import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.concurrent.*;
import com.wjybxx.fastjgame.utils.concurrent.internal.PromiseBase;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.Executor;

/**
 * 它是一个线程绑定版本的{@link NPromise}。
 * 用于用户不能在该对象上进行任何阻塞式操作，因此可以做一些激进的处理。
 *
 * <h3>激进优化</h3>
 * 1. 只能在用户线程下添加监听器（只允许用户添加监听器）。
 * 2. 数据被绑定在用户线程
 * <p>
 * Q: 能否举例？
 * A: 有时其实只有应用逻辑使用回调，而且还是特定线程添加回调，这个时候，其实可以完全消除对锁的需求。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/6
 */
@ThreadSafe
public class FastPromise<V> extends PromiseBase<V> {

    /**
     * 用户线程。
     */
    private final EventLoop appEventLoop;

    /**
     * 非volatile，也未用锁保护，只有{@link #appEventLoop}线程可以访问。
     */
    @GuardedBy("inEventLoop")
    private FutureListener<? super V> futureListeners;

    public FastPromise(EventLoop appEventLoop) {
        this.appEventLoop = appEventLoop;
    }

    @Override
    protected void postComplete() {
        notifyAllListeners();
    }

    private void notifyAllListeners() {
        if (appEventLoop.inEventLoop()) {
            notifyAllListenersNow();
        } else {
            ConcurrentUtils.safeExecute(appEventLoop, this::notifyAllListenersNow);
        }
    }

    private void notifyAllListenersNow() {
        assert appEventLoop.inEventLoop();

        // 由于工作者线程可能提交通知任务，因此这里可能为null。
        if (null == futureListeners) {
            return;
        }

        try {
            DefaultPromise.notifyAllListeners(appEventLoop, this, futureListeners);
        } finally {
            futureListeners = null;
        }
    }

    @Override
    public NPromise<V> onComplete(@Nonnull FutureListener<? super V> listener) {
        addListener0(listener);
        return this;
    }

    @Override
    public NPromise<V> onComplete(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        addListener0(new ExecutorBindListener<>(listener, bindExecutor));
        return this;
    }

    @Override
    public NPromise<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener) {
        addListener0(listener);
        return this;
    }

    @Override
    public NPromise<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        addListener0(new ExecutorBindListener<>(listener, bindExecutor));
        return this;
    }

    @Override
    public NPromise<V> onFailure(@Nonnull FailedFutureListener<? super V> listener) {
        addListener0(listener);
        return this;
    }

    @Override
    public NPromise<V> onFailure(@Nonnull FailedFutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        addListener0(new ExecutorBindListener<>(listener, bindExecutor));
        return this;
    }

    private void addListener0(@Nonnull FutureListener<? super V> listener) {
        ensureInAppEventLoop();

        _addListener0(listener);

        if (isDone()) {
            // 由于限定了只有appEventLoop可以添加监听器，因此可以立即执行回调
            notifyAllListenersNow();
        }
    }

    private void ensureInAppEventLoop() {
        ConcurrentUtils.ensureInEventLoop(appEventLoop);
    }

    private void _addListener0(@Nonnull FutureListener<? super V> listener) {
        if (futureListeners == null) {
            futureListeners = listener;
            return;
        }

        if (futureListeners instanceof CompositeFutureListener) {
            @SuppressWarnings("unchecked") CompositeFutureListener<V> compositeFutureListener = (CompositeFutureListener<V>) this.futureListeners;
            compositeFutureListener.addChild(listener);
        } else {
            futureListeners = new CompositeFutureListener<>(futureListeners, listener);
        }
    }

}
