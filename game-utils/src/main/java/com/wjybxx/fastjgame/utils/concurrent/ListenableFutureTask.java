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

    public ListenableFutureTask(EventLoop executor, Callable<V> callable) {
        this.promise = executor.newPromise();
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
    public boolean isDone() {
        return promise.isDone();
    }

    @Override
    public boolean isSuccess() {
        return promise.isSuccess();
    }

    @Override
    public boolean isCancelled() {
        return promise.isCancelled();
    }

    @Override
    public boolean isCancellable() {
        return promise.isCancellable();
    }

    @Override
    public V get() throws InterruptedException, CompletionException {
        return promise.get();
    }

    @Override
    public V get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, CompletionException, TimeoutException {
        return promise.get(timeout, unit);
    }

    @Override
    public V join() throws CompletionException {
        return promise.join();
    }

    @Override
    @Nullable
    public V getNow() {
        return promise.getNow();
    }

    @Override
    @Nullable
    public Throwable cause() {
        return promise.cause();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return promise.cancel(mayInterruptIfRunning);
    }

    @Override
    public ListenableFuture<V> await() throws InterruptedException {
        return promise.await();
    }

    @Override
    public ListenableFuture<V> awaitUninterruptibly() {
        return promise.awaitUninterruptibly();
    }

    @Override
    public boolean await(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        return promise.await(timeout, unit);
    }

    @Override
    public boolean awaitUninterruptibly(long timeout, @Nonnull TimeUnit unit) {
        return promise.awaitUninterruptibly(timeout, unit);
    }

    @Override
    public ListenableFuture<V> onComplete(@Nonnull FutureListener<? super V> listener) {
        return promise.onComplete(listener);
    }

    @Override
    public ListenableFuture<V> onComplete(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        return promise.onComplete(listener, bindExecutor);
    }

    @Override
    public ListenableFuture<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener) {
        return promise.onSuccess(listener);
    }

    @Override
    public ListenableFuture<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        return promise.onSuccess(listener, bindExecutor);
    }

    @Override
    public ListenableFuture<V> onFailure(@Nonnull FailedFutureListener<? super V> listener) {
        return promise.onFailure(listener);
    }

    @Override
    public ListenableFuture<V> onFailure(@Nonnull FailedFutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        return promise.onFailure(listener, bindExecutor);
    }

    @Override
    @UnstableApi
    public boolean isVoid() {
        return promise.isVoid();
    }
}
