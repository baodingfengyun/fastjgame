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

package com.wjybxx.fastjgame.async;

import com.wjybxx.fastjgame.concurrent.FutureResult;
import com.wjybxx.fastjgame.concurrent.GenericFutureFailureResultListener;
import com.wjybxx.fastjgame.concurrent.GenericFutureResultListener;
import com.wjybxx.fastjgame.concurrent.GenericFutureSuccessResultListener;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.concurrent.ExecutionException;

/**
 * 可刷新缓冲区的异步方法句柄。
 * 由于异步方法可能在某个对方排队，使用带有{@code flush}的执行方法时，将会尝试刷新缓冲区，以尽快执行。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/7
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public interface FlushableMethodHandle<T, FR extends FutureResult<V>, V> extends MethodHandle<T, FR, V> {

    /**
     * 在指定对象上执行对应的方法，但不监听方法的执行结果。
     * 且如果方法在某个缓冲区排队，那么会尝试刷新缓冲区，以尽快执行。
     *
     * @param serviceHandle 方法的执行对象
     */
    void executeAndFlush(@Nonnull T serviceHandle);

    /**
     * 在指定对象上执行对应的方法，并监听执行结果。
     * 且如果方法在某个缓冲区排队，那么会尝试刷新缓冲区，以尽快执行。
     * 注意：
     * 1. 一旦调用了call方法，回调信息将被重置。
     * 2. 如果没有设置回调，则表示不关心结果。等价于{@link #executeAndFlush(Object)}
     *
     * @param serviceHandle 方法的执行对象
     */
    void callAndFlush(@Nonnull T serviceHandle);

    /**
     * {@inheritDoc}
     *
     * @param serviceHandle
     * @apiNote 同步调用是很紧急的，因此该方法实现类必须刷新缓冲区，以尽快执行同步调用。
     */
    @Override
    V syncCall(@Nonnull T serviceHandle) throws ExecutionException;

    @Override
    FlushableMethodHandle<T, FR, V> onSuccess(GenericFutureSuccessResultListener<FR, V> listener);

    @Override
    FlushableMethodHandle<T, FR, V> onFailure(GenericFutureFailureResultListener<FR, V> listener);

    @Override
    FlushableMethodHandle<T, FR, V> onComplete(GenericFutureResultListener<FR, V> listener);

}
