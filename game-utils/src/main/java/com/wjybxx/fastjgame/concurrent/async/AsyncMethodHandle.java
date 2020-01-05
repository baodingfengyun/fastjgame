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

import java.util.Collection;

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
     * 在指定对象上异步执行对应的方法。
     * call是不是很好记住？那就多用它。
     *
     * @param typeObj 方法的执行对象
     * @return listener，可用于监听本次call调用的结果。
     */
    AsyncMethodListenable<V> call(T typeObj);

    /**
     * 在指定对象上同步执行对应的方法。
     * 如果执行成功，则返回对应的执行结果。
     * 如果执行失败，则抛出{@link AsyncMethodException}，可以通过{@link AsyncMethodException#getCause()}获取真实失败的原因。。
     * 建议在异步方法上少使用同步调用，必要的时候使用同步调用可以降低编程复杂度，但是大量使用会大大降低吞吐量。
     *
     * @param typeObj 方法的执行对象
     * @return result
     */
    V syncCall(T typeObj) throws AsyncMethodException;

    /**
     * 在指定对象上异步执行对应的方法，并不监听方法的执行结果。
     *
     * @param typeObj 方法的执行对象
     * @return this
     */
    AsyncMethodHandle<T, V> invoke(T typeObj);

    /**
     * 在一组对象上异步执行对应的方法，并不监听方法的执行结果。
     *
     * @param typeObjeCollection 方法的执行对象
     * @return this
     */
    AsyncMethodHandle<T, V> invoke(Collection<T> typeObjeCollection);
}
