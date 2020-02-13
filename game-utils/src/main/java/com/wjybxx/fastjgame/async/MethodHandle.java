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
import java.util.concurrent.CompletionException;

/**
 * 异步(远程)方法的句柄。
 * Q: {@link ListenableFuture}搭配{@link FutureListener}不就可以实现吗异步操作和同步操作吗，为何还要有这么一个抽象？
 * A: @link ListenableFuture}确实可以实现异步操作和同步操作，但是它的异步api成本有点高。原因如下:
 * 如果直接在 {@link ListenableFuture}上实现
 * {@code onSuccess(FutureListener)}
 * {@code onFailure(FutureListener)}
 * {@code onComplete(FutureListener)}三个api，存在以下问题：
 * 1. 可能会添加三次回调(多次获取锁，多次读取volatile变量)，这个成本可能很低，因此此时一般是无竞争锁，但也可能较高。
 * 2. future完成时，可能会提交三个回调任务到逻辑线程(这个是关键)。
 * 3. 回调执行时，可能又会读取好几次volatile数据。
 * <p>
 * 而我们如果仅仅是想异步执行的话，以上有太多冗余。我们对其封装之后，可以进行一些优化。
 * 最基本的优化就是，我们将提前设置好的回调整合为一个回调，放置到{@link ListenableFuture}中。
 * 更进一步的话，我们甚至可以去掉{@link ListenableFuture}，完全基于线程间传递消息完成异步。
 * <p>
 * Q: 为什么要有{@code onSuccess()} {@code onFailure()}{@code onComplete()}这样的方法？不能在一个回调里面做完吗？
 * A: 确实可以在一个回调里面完成所有事情。但是会造成大量的样板代码（重复代码），在lambda表达式里写条件语句会显得更糟糕，代码会变得越来越烂，必须要防止这样的代码出现。
 * <p>
 * Q: 为什么没有对{@code client}进行抽象？
 * A: 因为{@link MethodSpec}既是抽象的，又是泛型的，client中定义方法无法很好的约束其类型或泛型参数。
 *
 * @param <T>  the type of method owner
 * @param <FR> the type of future result
 * @param <V>  the type of return type
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/5 18:52
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public interface MethodHandle<T, FR extends FutureResult<V>, V> {

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
    MethodListenable<FR, V> call(@Nonnull T client);

    /**
     * 执行同步调用，如果执行成功，则返回对应的调用结果。
     *
     * @param client 方法的执行对象
     * @return result 执行结果
     * @throws CompletionException 方法的执行异常将封装为{@link CompletionException}
     */
    V syncCall(@Nonnull T client) throws CompletionException;

}
