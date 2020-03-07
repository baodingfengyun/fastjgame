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

import com.wjybxx.fastjgame.utils.concurrent.NListenableFuture;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.concurrent.CompletionException;

/**
 * 异步(远程)方法的句柄。
 * <p>
 * Q: 为什么没有对{@code client}进行抽象？
 * A: 因为{@link MethodSpec}既是抽象的，又是泛型的，client中定义方法无法很好的约束其类型或泛型参数。
 *
 * @param <T> the type of method owner
 * @param <V> the type of return type
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/5 18:52
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public interface MethodHandle<T, V> {

    /**
     * 在指定对象上执行对应的方法，但不监听方法的执行结果。
     *
     * @param client 方法的执行对象
     */
    void execute(@Nonnull T client);

    /**
     * 在指定对象上执行对应的方法，并监听执行结果。
     * call是不是很好记住？那就多用它。
     *
     * @param client 方法的执行对象
     * @return 用于监听结果的监听管理器
     */
    NListenableFuture<V> call(@Nonnull T client);

    /**
     * 执行同步调用，如果执行成功，则返回对应的调用结果。
     *
     * @param client 方法的执行对象
     * @return result 执行结果
     * @throws CompletionException 方法的执行异常将封装为{@link CompletionException}
     */
    V syncCall(@Nonnull T client) throws CompletionException;

}
