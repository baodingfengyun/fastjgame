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

package com.wjybxx.fastjgame.utils.async;

import com.wjybxx.fastjgame.utils.concurrent.FutureResult;
import com.wjybxx.fastjgame.utils.concurrent.ListenableFuture;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.concurrent.CompletionException;

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
public interface FlushableMethodHandle<T, V> extends MethodHandle<T, V> {

    /**
     * 在指定对象上执行对应的方法，但不监听方法的执行结果。
     * 且如果方法在某个缓冲区排队，那么会尝试刷新缓冲区，以尽快执行。
     *
     * @param client 方法的执行对象
     */
    void executeAndFlush(@Nonnull T client);

    /**
     * 在指定对象上执行对应的方法，并监听执行结果。
     * 且如果方法在某个缓冲区排队，那么会尝试刷新缓冲区，以尽快执行。
     *
     * @param client 方法的执行对象
     * @return 监听结果的管理器
     */
    ListenableFuture<V> callAndFlush(@Nonnull T client);

    /**
     * {@inheritDoc}
     *
     * @param client
     * @apiNote 同步调用是很紧急的，因此该方法实现类必须刷新缓冲区，以尽快执行同步调用。
     */
    @Override
    V syncCall(@Nonnull T client) throws CompletionException;

}
