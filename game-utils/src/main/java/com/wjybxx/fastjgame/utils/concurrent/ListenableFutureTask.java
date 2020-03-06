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

import com.wjybxx.fastjgame.utils.annotation.UnstableApi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.*;

/**
 * 与{@link java.util.concurrent.FutureTask}相似。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14 20:32
 * github - https://github.com/hl845740757
 */
public class ListenableFutureTask<V> implements ListenableFuture<V>, RunnableFuture<V> {

    private final Promise<V> promise;
    private final Callable<V> callable;

    public ListenableFutureTask(Promise<V> promise, Callable<V> callable) {
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

    @Nonnull
    private ListenableFuture<V> getFuture() {
        return promise.getFuture();
    }

    @Override
    public boolean isDone() {
        return getFuture().isDone();
    }

    @Override
    public boolean isSuccess() {
        return getFuture().isSuccess();
    }

    @Override
    public boolean isCancelled() {
        return getFuture().isCancelled();
    }

    @Override
    public boolean isCancellable() {
        return getFuture().isCancellable();
    }

    @Override
    public V get() throws InterruptedException, CompletionException {
        return getFuture().get();
    }

    @Override
    public V get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, CompletionException, TimeoutException {
        return getFuture().get(timeout, unit);
    }

    @Override
    public V join() throws CompletionException {
        return getFuture().join();
    }

    @Override
    @Nullable
    public V getNow() {
        return getFuture().getNow();
    }

    @Override
    @Nullable
    public Throwable cause() {
        return getFuture().cause();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return getFuture().cancel(mayInterruptIfRunning);
    }

    @Override
    public ListenableFuture<V> await() throws InterruptedException {
        return getFuture().await();
    }

    @Override
    public ListenableFuture<V> awaitUninterruptibly() {
        return getFuture().awaitUninterruptibly();
    }

    @Override
    public boolean await(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        return getFuture().await(timeout, unit);
    }

    @Override
    public boolean awaitUninterruptibly(long timeout, @Nonnull TimeUnit unit) {
        return getFuture().awaitUninterruptibly(timeout, unit);
    }

    @Override
    public ListenableFuture<V> onComplete(@Nonnull FutureListener<? super V> listener) {
        return getFuture().onComplete(listener);
    }

    @Override
    public ListenableFuture<V> onComplete(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        return getFuture().onComplete(listener, bindExecutor);
    }

    @Override
    public ListenableFuture<V> removeListener(@Nonnull FutureListener<? super V> listener) {
        return getFuture().removeListener(listener);
    }

    @Override
    @UnstableApi
    public boolean isVoid() {
        return getFuture().isVoid();
    }
}
