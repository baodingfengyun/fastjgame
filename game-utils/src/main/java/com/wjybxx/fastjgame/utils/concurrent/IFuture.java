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

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

/**
 * 与JDK的future不同，它不提供任何的阻塞式接口，只提供非阻塞获取结果的api。
 * 它使得一些针对特定场景的轻量级的future成为可能。
 * <p>
 * 这个名字并不好，但是取名为future又会造成极大的混乱。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/29
 */
public interface IFuture<V> {

    /**
     * 查询任务是否已完成。
     * <h3>完成状态</h3>
     * <b>正常完成</b>、<b>被取消结束</b>、<b>执行异常而结束</b>都表示完成状态。
     *
     * @return 任务已进入完成状态则返回true。
     */
    boolean isDone();

    /**
     * 如果future以任何形式的异常完成，则返回true。
     * 包括被取消，以及显式调用{@link Promise#setFailure(Throwable)}和{@link Promise#tryFailure(Throwable)}操作。
     * <p>
     * Q: 它为什么好过{@code isSuccess()}方法？
     * A: 实践中发现，当future返回类型的是{@link Boolean}时候，{@code isSuccess()}会造成极大的混乱。代码大概是这样的:
     * <pre> {@code
     *  if (!future.isSuccess()) {
     *      onFailure(future.cause());
     *  } else {
     *      boolean success = future.getNow();
     *      if(success) {
     *          doSomethingA();
     *      } else{
     *          doSomethingB();
     *      }
     *  }
     * }
     * </pre>
     * 出现了多个不同意义的success，这样的代码非常不好。此外，{@code isSuccess()}方法使用取反表达式的情况较多。
     */
    boolean isCompletedExceptionally();

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
     * 你可以使用{@link #isCompletedExceptionally()},作为更好的选择。
     *
     * @return task执行结果
     * @throws CancellationException 如果任务被取消了，则抛出该异常
     * @throws CompletionException   如果在计算过程中出现了其它异常导致任务失败，则抛出该异常。
     */
    V getNow();

    /**
     * 非阻塞获取导致任务失败的原因。
     * 1. 如果future关联的task还未进入完成状态{@link #isDone() false}，则返回null。
     * 2. 当future关联的任务被取消或由于异常进入完成状态后，该方法将返回操作失败的原因。
     * 3. 如果future关联的task已正常完成，则返回null。
     *
     * @return 失败的原因
     */
    Throwable cause();

}
