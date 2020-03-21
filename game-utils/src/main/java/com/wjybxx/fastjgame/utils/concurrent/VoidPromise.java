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
public class VoidPromise implements BlockingPromise<Object> {

    private final EventLoop defaultExecutor;

    public VoidPromise(EventLoop defaultExecutor) {
        this.defaultExecutor = defaultExecutor;
    }

    @Override
    public final boolean isVoid() {
        return true;
    }

    @Override
    public EventLoop defaultExecutor() {
        return defaultExecutor;
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
    public final Object getNow() {
        return null;
    }

    @Nullable
    @Override
    public final Throwable cause() {
        return null;
    }

    // --------------------------------------- 任何阻塞式调用，立即抛出异常 -------------------------------

    @Override
    public final Object get() throws InterruptedException, CompletionException {
        fail();
        return null;
    }

    @Override
    public final Object get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, CompletionException, TimeoutException {
        fail();
        return null;
    }

    @Override
    public final Object join() throws CompletionException {
        fail();
        return null;
    }

    @Override
    public VoidPromise await() throws InterruptedException {
        fail();
        return this;
    }

    @Override
    public VoidPromise awaitUninterruptibly() {
        fail();
        return this;
    }

    @Override
    public final boolean await(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        fail();
        return false;
    }

    @Override
    public final boolean awaitUninterruptibly(long timeout, @Nonnull TimeUnit unit) {
        fail();
        return false;
    }

    private static void fail() {
        throw new IllegalStateException("void future");
    }

    // ---------------------------------------- 禁止添加监听器 -------------------------------------

    @Override
    public BlockingPromise<Object> addListener(@Nonnull FutureListener<? super Object> listener) {
        fail();
        return this;
    }

    @Override
    public BlockingPromise<Object> addListener(@Nonnull FutureListener<? super Object> listener, @Nonnull Executor bindExecutor) {
        fail();
        return this;
    }

    // ------------------------------------- 赋值操作不造成任何影响 -----------------------------

    @Override
    public final void setSuccess(Object result) {

    }

    @Override
    public final boolean trySuccess(Object result) {
        return false;
    }

    @Override
    public final void setFailure(@Nonnull Throwable cause) {

    }

    @Override
    public final boolean tryFailure(@Nonnull Throwable cause) {
        return false;
    }

    @Override
    public final boolean setUncancellable() {
        // 这里true更合适 - 因为不可取消
        return true;
    }
}
