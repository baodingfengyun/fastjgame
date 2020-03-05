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

import javax.annotation.Nonnull;
import java.util.concurrent.Callable;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 与{@link java.util.concurrent.FutureTask}相似。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14 20:32
 * github - https://github.com/hl845740757
 */
public class PromiseTask<V> implements RunnableFuture<V> {

    private final Promise<V> promise;
    private final Callable<V> callable;

    PromiseTask(Promise<V> promise, Callable<V> callable) {
        this.promise = promise;
        this.callable = callable;
    }

    @Override
    public void run() {
        try {
            if (promise.setUncancellable()) {
                V result = callable.call();
                promise.setSuccess(result);
            }
        } catch (Throwable e) {
            promise.setFailure(e);
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return promise.getFuture().cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return promise.getFuture().isCancelled();
    }

    @Override
    public boolean isDone() {
        return promise.getFuture().isDone();
    }

    @Override
    public V get() throws InterruptedException {
        return promise.getFuture().get();
    }

    @Override
    public V get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, TimeoutException {
        return promise.getFuture().get(timeout, unit);
    }
}
