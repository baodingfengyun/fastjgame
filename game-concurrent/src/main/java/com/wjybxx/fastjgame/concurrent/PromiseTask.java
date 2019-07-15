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

package com.wjybxx.fastjgame.concurrent;

import javax.annotation.Nonnull;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableFuture;

/**
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14 20:32
 * github - https://github.com/hl845740757
 */
public class PromiseTask<V> extends DefaultPromise<V> implements RunnableFuture<V> {

    private final Callable<V> callable;

    public PromiseTask(@Nonnull EventLoop executor, Callable<V> callable) {
        super(executor);
        this.callable = callable;
    }

    public PromiseTask(@Nonnull EventLoop executor, Runnable runnable, V result) {
        super(executor);
        this.callable = Executors.callable(runnable, result);
    }

    @Override
    public void run() {
        try {
            if (setUncancellableInternal()) {
                V result = callable.call();
                setSuccessInternal(result);
            }
        } catch (Exception e) {
            setFailureInternal(e);
        }
    }

    // --------------- 禁用这些方法，因为PromiseTask既是Promise，也是Task，结果由它自己来赋值
    @Override
    public void setSuccess(V result) {
        throw new IllegalStateException();
    }

    @Override
    public boolean trySuccess(V result) {
        return false;
    }

    @Override
    public void setFailure(@Nonnull Throwable cause) {
        throw new IllegalStateException();
    }

    @Override
    public boolean tryFailure(@Nonnull Throwable cause) {
        return false;
    }

    @Override
    public boolean setUncancellable() {
        return false;
    }
    // --------------  由这些protected方法替代（需要支持子类调用）

    protected final void setSuccessInternal(V result) {
        super.setSuccess(result);
    }

    protected final boolean trySuccessInternal(V result) {
        return super.trySuccess(result);
    }

    protected final void setFailureInternal(Throwable cause) {
        super.setFailure(cause);
    }

    protected final boolean tryFailureInternal(Throwable cause) {
        return super.tryFailure(cause);
    }

    protected final boolean setUncancellableInternal() {
        return super.setUncancellable();
    }
}
