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

package com.wjybxx.fastjgame.utils.concurrent;


import javax.annotation.Nonnull;
import java.util.concurrent.*;

/**
 * 它继承了JDK的{@link Future}，出现了阻塞式api。
 * <h3>建议</h3>
 * 如果不是必须需要阻塞式的API，应当优先选择{@link ListenableFuture}。
 *
 * @param <V> 值类型
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14
 * github - https://github.com/hl845740757
 */
public interface BlockingFuture<V> extends Future<V>, ListenableFuture<V> {

    // ------------------------------------- 阻塞式获取操作结果 ---------------------------------------

    @Override
    V get() throws InterruptedException, ExecutionException;

    @Override
    V get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;

    /**
     * 阻塞式获取task的结果，阻塞期间不响应中断。
     * 如果future关联的task尚未完成，则阻塞等待至任务完成，并返回计算的结果。
     * 如果future关联的task已完成，则立即返回结果。
     * <p>
     * 注意：
     * 如果future关联的task没有返回值(操作完成返回null)，此时不能根据返回值做任何判断。对于这种情况，
     * 你可以使用{@link #isSuccess()},作为更好的选择。
     *
     * @return task的结果
     * @throws CancellationException 如果任务被取消了，则抛出该异常
     * @throws CompletionException   如果在计算过程中出现了其它异常导致任务失败，则抛出该异常。
     *                               使用非受检异常更好，这里和{@link CompletableFuture#join()}是一样的。
     */
    V join() throws CompletionException;

    // -------------------------------- 阻塞式等待future进入完成状态  --------------------------------------

    /**
     * 在指定的时间范围内等待，直到future关联的任务进入完成状态。
     * 如果正常返回，接下来的{@link #isDone()}调用都将返回true。
     *
     * @param timeout 等待的最大时间，<b>如果小于等于0，表示不阻塞</b>
     * @param unit    时间单位
     * @return 当且仅当future关联的task在指定时间内进入了完成状态，返回true。也就是接下来的{@link #isDone() true} 。
     * @throws InterruptedException 如果当前线程在等待期间被中断
     */
    boolean await(long timeout, @Nonnull TimeUnit unit) throws InterruptedException;

    /**
     * 在指定的时间范围内等待，直到future关联的任务进入完成状态，并且在等待期间不响应中断。
     * 在等待期间，会捕获中断，并默默的丢弃，在方法返回前会恢复中断状态。
     *
     * @param timeout 等待的最大时间，<b>如果小于等于0，表示不阻塞</b>
     * @param unit    时间单位
     * @return 当且仅当future关联的任务，在特定时间范围内进入完成状态时返回true。也就是接下来的{@link #isDone() true}。
     */
    boolean awaitUninterruptibly(long timeout, @Nonnull TimeUnit unit);

    /**
     * 等待future进入完成状态。
     * await()不会查询任务的结果，在Future进入完成状态之后就返回，方法返回后，接下来的{@link #isDone()}调用都将返回true。
     *
     * @return this
     * @throws InterruptedException 如果在等待期间线程被中断，则抛出中断异常。
     */
    BlockingFuture<V> await() throws InterruptedException;

    /**
     * 等待future进入完成状态，等待期间不响应中断，并默默的丢弃，在方法返回前会重置中断状态。
     * 在方法返回之后，接下来的{@link #isDone()}调用都将返回true。
     *
     * @return this
     */
    BlockingFuture<V> awaitUninterruptibly();

    // 仅仅用于支持流式语法

    @Override
    BlockingFuture<V> onComplete(@Nonnull FutureListener<? super V> listener);

    @Override
    BlockingFuture<V> onComplete(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor);

    @Override
    BlockingFuture<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener);

    @Override
    BlockingFuture<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener, @Nonnull Executor bindExecutor);

    @Override
    BlockingFuture<V> onFailure(@Nonnull FailedFutureListener<? super V> listener);

    @Override
    BlockingFuture<V> onFailure(@Nonnull FailedFutureListener<? super V> listener, @Nonnull Executor bindExecutor);
}
