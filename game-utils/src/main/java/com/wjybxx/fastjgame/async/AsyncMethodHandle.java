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

import com.wjybxx.fastjgame.concurrent.FutureListener;
import com.wjybxx.fastjgame.concurrent.FutureResult;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.concurrent.ExecutionException;

/**
 * 异步方法的句柄。
 * Q: {@link ListenableFuture}搭配{@link FutureListener}不就可以实现吗异步操作和同步操作吗，为何还要有这么一个抽象？
 * A: @link ListenableFuture}确实可以实现异步操作和同步操作，但是它的异步api成本有点高。原因如下:
 * 如果直接在 {@link ListenableFuture}上实现
 * {@code onSuccess(FutureListener)}
 * {@code onFailure(FutureListener)}
 * {@code onComplete(FutureListener)}三个api。
 * 1. 可能会添加三次回调(三次获取锁，读取volatile变量)，这个成本其实可能很低，因此此时一般是无竞争锁，但也可能较高。
 * 2. future完成时，可能会提交三个回调任务到逻辑线程。
 * 3. 回调执行时，可能又会读取好几次volatile数据。
 * <p>
 * 而我们如果仅仅是想异步执行的话，以上有太多冗余。我们对其封装之后，可以进行一些优化。
 * 最基本的优化就是，我们将提前设置好的回调整合为一个回调，放置到{@link ListenableFuture}中。
 * 更进一步的话，我们甚至可以去掉{@link ListenableFuture}，完全基于线程间传递消息完成异步。
 *
 * @param <V> the type of return type
 * @param <T> the type of method owner
 * @param <F> the type of future result
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/5 18:52
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public interface AsyncMethodHandle<V, T, F extends FutureResult<V>> {

    /**
     * 在指定对象上执行对应的方法，但不监听方法的执行结果。
     *
     * @param typeObj 方法的执行对象
     */
    void execute(@Nonnull T typeObj);

    /**
     * 在指定对象上执行对应的方法，并监听执行结果。
     * call是不是很好记住？那就多用它。
     * <p>
     * 注意：
     * 1. 一旦调用了call方法，回调信息将被重置。
     * 2. 没有设置回调，则表示不关心结果。
     *
     * @param typeObj 方法的执行对象
     */
    void call(@Nonnull T typeObj);

    /**
     * 执行同步调用，如果执行成功，则返回对应的调用结果。
     * <p>
     * 注意：
     * 1. 少使用同步调用，必要的时候使用同步可以降低编程复杂度，但是大量使用会大大降低吞吐量。
     * 2. 即使添加了回调，这些回调也会被忽略。
     *
     * @param typeObj 方法的执行对象
     * @return result 执行结果
     * @throws ExecutionException 方法的执行异常将封装为{@link ExecutionException}
     */
    V syncCall(@Nonnull T typeObj) throws ExecutionException;

    /**
     * 设置成功时执行的回调。
     * 注意：只有当后续调用的是{@link #call(Object)}系列方法时才会有效。
     *
     * @param listener 回调逻辑
     * @return this
     */
    AsyncMethodHandle<V, T, F> onSuccess(GenericFutureSuccessResultListener<F, ? super V> listener);

    /**
     * 设置成功时执行的回调。
     * 注意：只有当后续调用的是{@link #call(Object)}系列方法时才会有效。
     *
     * @param listener 回调逻辑
     * @return this
     */
    AsyncMethodHandle<V, T, F> onFailure(GenericFutureFailureResultListener<F, ? super V> listener);

    /**
     * 设置成功时执行的回调。
     * 注意：只有当后续调用的是{@link #call(Object)}系列方法时才会有效。
     *
     * @param listener 回调逻辑
     * @return this
     */
    AsyncMethodHandle<V, T, F> onComplete(GenericFutureResultListener<F, ? super V> listener);
}
