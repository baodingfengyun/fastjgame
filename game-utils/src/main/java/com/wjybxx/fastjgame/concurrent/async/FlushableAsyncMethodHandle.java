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

package com.wjybxx.fastjgame.concurrent.async;

import com.wjybxx.fastjgame.concurrent.ListenableFuture;

import javax.annotation.Nonnull;

/**
 * 可刷新缓冲区的异步方法句柄。
 * 由于异步方法可能在某个对方排队，使用带有{@code flush}的执行方法时，将会尝试刷新缓冲区，以尽快执行。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/7
 * github - https://github.com/hl845740757
 */
public interface FlushableAsyncMethodHandle<T, V> extends AsyncMethodHandle<T, V> {

    /**
     * 在指定对象上执行对应的方法，但不监听方法的执行结果。
     * 且如果方法在某个缓冲区排队，那么会尝试刷新缓冲区，以尽快执行。
     *
     * @param typeObj 方法的执行对象
     */
    void executeAndFlush(@Nonnull T typeObj);

    /**
     * 在指定对象上执行对应的方法，并返回可监听结果的future。
     * 且如果方法在某个缓冲区排队，那么会尝试刷新缓冲区，以尽快执行。
     *
     * @param typeObj 方法的执行对象
     * @return future，可用于监听本次call调用的结果。
     */
    @Nonnull
    ListenableFuture<V> callAndFlush(@Nonnull T typeObj);
}
