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

import com.wjybxx.fastjgame.utils.annotation.UnstableApi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/4
 */
@UnstableApi
public class VoidFuture implements ListenableFuture<Object> {

    @Override
    public final boolean isVoid() {
        return true;
    }

    // --------------------------------------- 任何状态查询都立即返回 ----------------------------------------

    @Override
    public final boolean isCancellable() {
        return false;
    }

    @Override
    public final boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public final boolean isDone() {
        return false;
    }

    @Override
    public final boolean isSuccess() {
        return false;
    }

    @Override
    public final boolean isCancelled() {
        return false;
    }

    @Nullable
    @Override
    public final Void getNow() {
        return null;
    }

    @Nullable
    @Override
    public final Throwable cause() {
        return null;
    }

    // --------------------------------------- 任何阻塞式调用，立即抛出异常 -------------------------------

    @Override
    public final Void get() throws InterruptedException, CompletionException {
        fail();
        return null;
    }

    @Override
    public final Void get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, CompletionException, TimeoutException {
        fail();
        return null;
    }

    @Override
    public final Void join() throws CompletionException {
        fail();
        return null;
    }

    @Override
    public ListenableFuture<Object> await() throws InterruptedException {
        fail();
        return this;
    }

    @Override
    public ListenableFuture<Object> awaitUninterruptibly() {
        fail();
        return this;
    }

    @Override
    public boolean await(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        fail();
        return false;
    }

    @Override
    public boolean awaitUninterruptibly(long timeout, @Nonnull TimeUnit unit) {
        fail();
        return false;
    }

    private static void fail() {
        throw new IllegalStateException("void future");
    }

    // ---------------------------------------- 禁止添加监听器 -------------------------------------

    @Override
    public ListenableFuture<Object> onComplete(@Nonnull FutureListener<? super Object> listener) {
        fail();
        return this;
    }

    @Override
    public ListenableFuture<Object> onComplete(@Nonnull FutureListener<? super Object> listener, @Nonnull Executor bindExecutor) {
        fail();
        return this;
    }

    @Override
    public ListenableFuture<Object> removeListener(@Nonnull FutureListener<? super Object> listener) {
        return this;
    }
}
