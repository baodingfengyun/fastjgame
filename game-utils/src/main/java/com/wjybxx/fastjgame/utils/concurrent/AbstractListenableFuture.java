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
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

/**
 * ListenableFuture的抽象实现
 *
 * @param <V>
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14 14:53
 * github - https://github.com/hl845740757
 */
public abstract class AbstractListenableFuture<V> implements ListenableFuture<V> {

    @UnstableApi
    @Nullable
    @Override
    public FutureResult<V> getAsResult() {
        if (isDone()) {
            return new DefaultFutureResult<>(getNow(), cause());
        } else {
            return null;
        }
    }

    @Override
    public V join() throws CompletionException {
        awaitUninterruptibly();
        try {
            return get();
        } catch (InterruptedException e) {
            // Should not be raised at all.
            throw new InternalError();
        }
    }

    /**
     * 重新抛出失败异常
     *
     * @param cause 任务失败的原因
     * @throws CancellationException 如果任务被取消，则抛出该异常
     * @throws CompletionException   其它原因导致失败
     */
    protected static <T> T rethrowCause(@Nonnull Throwable cause) throws CancellationException, CompletionException {
        if (cause instanceof CancellationException) {
            throw (CancellationException) cause;
        }
        throw new CompletionException(cause);
    }
}
