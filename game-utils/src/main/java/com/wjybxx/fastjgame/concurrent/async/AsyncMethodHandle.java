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
 * 异步方法的句柄。
 *
 * @param <T> the type of method owner
 * @param <V> the type of return type
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/5 18:52
 * github - https://github.com/hl845740757
 */
public interface AsyncMethodHandle<T, V> {

    /**
     * 在指定对象上执行对应的方法，但不监听方法的执行结果。
     *
     * @param typeObj 方法的执行对象
     */
    void execute(@Nonnull T typeObj);

    /**
     * 在指定对象上执行对应的方法，并返回可监听结果的future。
     *
     * @param typeObj 方法的执行对象
     * @return listener，可用于监听本次call调用的结果。
     */
    @Nonnull
    ListenableFuture<V> call(@Nonnull T typeObj);

}
