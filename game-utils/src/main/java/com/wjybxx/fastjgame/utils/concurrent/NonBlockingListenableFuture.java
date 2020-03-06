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

package com.wjybxx.fastjgame.utils.concurrent;

import com.wjybxx.fastjgame.utils.annotation.UnstableApi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * 非阻塞的可监听future。
 * 它不提供任何的阻塞式接口，只提供监听api。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/6
 */
public interface NonBlockingListenableFuture<V> {

    /**
     * 查询任务是否已完成。
     * <h3>完成状态</h3>
     * <b>正常完成</b>、<b>被取消结束</b>、<b>执行异常而结束</b>都表示完成状态。
     *
     * @return 任务已进入完成状态则返回true。
     */
    boolean isDone();

    /**
     * 查询future关联的操作是否顺利完成了。
     *
     * @return 当且仅当该future对应的task顺利完成时返回true。
     */
    boolean isSuccess();

    // ---------------------------------------- 取消相关 --------------------------------------

    /**
     * 查询任务是否被取消。
     *
     * @return 当且仅当该future关联的task由于取消进入完成状态时返回true。
     */
    boolean isCancelled();

    /**
     * 查询future关联的任务是否可以被取消。
     *
     * @return true/false 当且仅当future关联的任务可以通过{@link #cancel(boolean)}被取消时返回true。
     */
    boolean isCancellable();

    /**
     * 尝试取消future关联的任务，如果取消成功，会使得Future进入完成状态，并且{@link #cause()}将返回{@link CancellationException}。
     * 1. 如果取消成功，则返回true。
     * 2. 如果任务已经被取消，则返回true。
     * 3. 如果future关联的任务已完成，则返回false。
     *
     * @param mayInterruptIfRunning 是否允许中断工作者线程
     * @return 是否取消成功
     */
    boolean cancel(boolean mayInterruptIfRunning);

    // ------------------------------------- 非阻塞式获取计算结果 --------------------------------------

    /**
     * 非阻塞的获取当前结果：
     * 1. 如果future关联的task还未完成{@link #isDone() false}，则返回null。
     * 2. 如果任务执行成功，则返回对应的结果。
     * 2. 如果任务被取消或失败，则抛出对应的异常。
     * <p>
     * 注意：
     * 如果future关联的task没有返回值(操作完成返回null)，此时不能根据返回值做任何判断。对于这种情况，
     * 你可以使用{@link #isSuccess()},作为更好的选择。
     *
     * @return task执行结果
     * @throws CancellationException 如果任务被取消了，则抛出该异常
     * @throws CompletionException   如果在计算过程中出现了其它异常导致任务失败，则抛出该异常。
     */
    @Nullable
    V getNow();

    /**
     * 非阻塞获取导致任务失败的原因。
     * 1. 如果future关联的task还未进入完成状态{@link #isDone() false}，则返回null。
     * 2. 当future关联的任务被取消或由于异常进入完成状态后，该方法将返回操作失败的原因。
     * 3. 如果future关联的task已正常完成，则返回null。
     *
     * @return 失败的原因
     */
    @Nullable
    Throwable cause();

    // ------------------------------------- 监听 --------------------------------------

    /**
     * 添加一个监听者到当前Future。传入的特定的Listener将会在Future计算完成时{@link #isDone() true}被通知。
     * 如果当前Future已经计算完成，那么将立即被通知。
     * 注意：
     * 1. 该监听器将在默认的事件分发线程中执行。当你的代码支持并发调用的时候，那么使用该方法注册监听器即可。
     * 2. 同一个listener反复添加会共存。
     *
     * @param listener 要添加的监听器。
     * @return this
     */
    NonBlockingListenableFuture<V> onComplete(@Nonnull FutureListener<? super V> listener);

    /**
     * 添加一个监听者到当前Future。传入的特定的Listener将会在Future计算完成时{@link #isDone() true}被通知。
     * 如果当前Future已经计算完成，那么将立即被通知。
     * 注意：同一个listener反复添加会共存。
     * <p>
     * 当你的执行环境是一个executor的时候，可以直接提交到你所在的线程，从而消除事件处理时的同步逻辑。
     * eg:
     * <pre>
     * {@code
     * 		// this.executor代表当前线程
     * 		addListener(listener, this.bindExecutor)
     * }
     * </pre>
     *
     * @param listener     要添加的监听器
     * @param bindExecutor 监听器的最终执行线程
     * @return this
     */
    NonBlockingListenableFuture<V> onComplete(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor);

    default NonBlockingListenableFuture<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener) {
        onComplete(listener);
        return this;
    }

    default NonBlockingListenableFuture<V> onFailure(@Nonnull FailedFutureListener<? super V> listener) {
        onComplete(listener);
        return this;
    }

    /**
     * 移除监听器中第一个与指定Listener匹配的监听器，如果该Listener没有进行注册，那么什么也不会做。
     *
     * @param listener 要移除的监听器
     * @return this
     */
    ListenableFuture<V> removeListener(@Nonnull FutureListener<? super V> listener);

    // ------------------------------------- 用于支持占位的voidFuture --------------------------------------

    /**
     * 如果该方法返回true，表示该对象仅仅用于占位。
     * 任何<b>阻塞式调用</b>和<b>添加监听器</b>都将抛出异常。
     * <p>
     * Q: 它的主要目的？
     * A: 减少开销。其实任何使用{@link VoidFuture}的地方，都可以使用正常的future，
     * 只是会有额外的开销。在某些场景使用{@link VoidFuture}将节省很多开销。
     */
    @UnstableApi
    boolean isVoid();
}
