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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.Executor;

/**
 * 它是一个线程绑定版本的{@link Promise}。
 * 它基于特定的假设，进行了一些激进的优化。
 *
 * <h3>假设</h3>
 * 1. 我们假设用户只在特定线程添加监听器。
 * 2. 我们假设用户在添加监听器的过程中，任务恰好进入完成状态的几率非常小，小到可以忽略。
 * 3. 我们假设future进入完成状态时，总是有监听器待通知。
 *
 * <h3>激进优化</h3>
 * 1. 基于假设1和假设2，因此我们可以消除<b>添加</b>和<b>删除</b>监听器锁过程中产生的竞争。<br>
 * 2. 基于假设3，当future进入完成状态时，只是简单的提交一个通知任务，而不需要任何变量判断 <b>是否存在监听器</b>，<b>用户线程是否正在添加监听器</b>。
 *
 * <p>
 * 上面的假设，其实是很常见的情况，而且应该占多数。
 * 在这之前，因为觉得{@link DefaultBlockingPromise}的实现比较重量级，不太想用，老是想着设计一些奇怪的东西，
 * 之前的“MethodListenable”应该就是个代表了，其实是少了{@link ListenableFuture}抽象导致的。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/6
 */
@ThreadSafe
public class LocalPromise<V> extends AbstractPromise<V> {

    /**
     * 用户线程。
     */
    private final EventLoop appEventLoop;

    /**
     * 非volatile，也未用锁保护，只有{@link #appEventLoop}线程可以访问。
     */
    @GuardedBy("inEventLoop")
    private Object listenerEntries;

    public LocalPromise(EventLoop appEventLoop) {
        this.appEventLoop = appEventLoop;
    }

    @Override
    protected final void postComplete() {
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

        // 由于可能存在带执行的该方法调用，因此这里可能为null。
        if (null == listenerEntries) {
            return;
        }

        try {
            DefaultBlockingPromise.notifyAllListenerNowSafely(this, listenerEntries);
        } finally {
            listenerEntries = null;
        }
    }

    // -------------------------------------------------- 监听器管理 ---------------------------------------------

    @Override
    public final EventLoop defaultExecutor() {
        return appEventLoop;
    }

    @Override
    public Promise<V> onComplete(@Nonnull FutureListener<? super V> listener) {
        addListener(listener, appEventLoop);
        return this;
    }

    @Override
    public Promise<V> onComplete(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        addListener(listener, bindExecutor);
        return this;
    }

    @Override
    public Promise<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener) {
        addListener(listener, appEventLoop);
        return this;
    }

    @Override
    public Promise<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        addListener(listener, bindExecutor);
        return this;
    }

    @Override
    public Promise<V> onFailure(@Nonnull FailedFutureListener<? super V> listener) {
        addListener(listener, appEventLoop);
        return this;
    }

    @Override
    public Promise<V> onFailure(@Nonnull FailedFutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        addListener(listener, bindExecutor);
        return this;
    }

    private void addListener(@Nonnull FutureListener<? super V> listener, @Nonnull Executor executor) {
        EventLoopUtils.ensureInEventLoop(appEventLoop, "Must call from appEventLoop");

        listenerEntries = DefaultBlockingPromise.aggregateListenerEntry(listenerEntries, new FutureListenerEntry<>(listener, executor));

        if (isDone()) {
            // 由于限定了只有appEventLoop可以添加监听器，因此可以立即执行回调
            notifyAllListenersNow();
        }
    }

}
