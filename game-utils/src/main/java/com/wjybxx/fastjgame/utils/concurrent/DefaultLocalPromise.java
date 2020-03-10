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
 * {@link LocalPromise}的默认实现。
 * <p>
 * 建议使用{@link EventLoop#newLocalPromise()}代替构造方法，利用如下:
 * 1. 更清晰。
 * 2. 当切换实现时，成本较低。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/6
 */
@ThreadSafe
public class DefaultLocalPromise<V> extends AbstractPromise<V> implements LocalPromise<V> {

    /**
     * 用户线程。
     */
    private final EventLoop appEventLoop;

    /**
     * 非volatile，也未用锁保护，只有{@link #appEventLoop}线程可以访问。
     */
    @GuardedBy("inEventLoop")
    private Object listenerEntries;

    public DefaultLocalPromise(EventLoop appEventLoop) {
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
            FutureUtils.notifyAllListenerNowSafely(this, listenerEntries);
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

        listenerEntries = FutureUtils.aggregateListenerEntry(listenerEntries, new FutureListenerEntry<>(listener, executor));

        if (isDone()) {
            // 由于限定了只有appEventLoop可以添加监听器，因此可以立即执行回调
            notifyAllListenersNow();
        }
    }

}
