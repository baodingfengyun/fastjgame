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

package com.wjybxx.fastjgame.concurrent;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 具有时效性的future，在限定时间内必定必须进入完成状态。
 * 因此{@link #get()} 和 {@link #await()} 系列方法不会长时间阻塞，都会在超时时间到达后醒来。
 * 默认实现为：
 * 如果在指定时间之内未完成，会以{@link TimeoutException}结束，即{@link ExecutionException#getCause()}为{@link TimeoutException}。
 * 注意：超时后。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/6
 * github - https://github.com/hl845740757
 */
public interface TimeoutFuture<V> extends ListenableFuture<V> {

    @Override
    V get() throws InterruptedException, ExecutionException;

    @Override
    V get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;

    @Override
    TimeoutFuture<V> await() throws InterruptedException;

    @Override
    TimeoutFuture<V> awaitUninterruptibly();

    @Override
    TimeoutFuture<V> addListener(@Nonnull FutureListener<? super V> listener);

    @Override
    TimeoutFuture<V> addListener(@Nonnull FutureListener<? super V> listener, @Nonnull EventLoop bindExecutor);

    @Override
    TimeoutFuture<V> removeListener(@Nonnull FutureListener<? super V> listener);
}
