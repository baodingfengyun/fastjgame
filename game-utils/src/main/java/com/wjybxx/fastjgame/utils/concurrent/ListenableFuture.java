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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 基于性能方面的考虑，Listener之间不再提供时序保证。
 * {@code addListener}适合用在末尾，这样可以避免产生不必要的下游对象。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/4/27
 * github - https://github.com/hl845740757
 */
public interface ListenableFuture<V> extends Future<V> {

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
     * @param mayInterruptIfRunning 是否允许中断工作者线程，在Future/Promise模式下意义不大
     * @return 是否取消成功
     */
    boolean cancel(boolean mayInterruptIfRunning);

    /**
     * 如果future以任何形式的异常完成（包括被取消)，则返回true。
     * 1. 如果future尚未进入完成状态，则返回false。
     * 2. 如果任务正常完成，则返回false。
     * 3. 当future关联的任务被取消或由于异常进入完成状态，则返回true。
     * <p>
     * 如果返回true，则{@link #cause()}不为null，也就是说该方法等价于{@code cause() != null}。
     */
    boolean isCompletedExceptionally();

    /**
     * 非阻塞获取导致任务失败的原因。
     * 1. 如果future关联的task还未进入完成状态{@link #isDone() false}，则返回null。
     * 2. 如果future关联的task已正常完成，则返回null。
     * 3. 当future关联的任务被取消或由于异常进入完成状态后，该方法将返回操作失败的原因。
     *
     * @return 失败的原因
     */
    Throwable cause();

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
     * 如果{@code Future}已进入完成状态，则立即执行给定动作，否则什么也不做。
     * 该设计其实是一个访问者，即我们知道Future的结果包含正常结果和异常结果，但是如何存储的我们并不知晓，我们进行访问的时候，它可以告诉我们。
     * 它的主要目的是减少读开销，它其实是尝试同时调用{@link #cause()}和{@link #getNow()}。
     *
     * @param action 用于接收当前的执行结果
     * @return 如果执行了给定动作则返回true(即future已完成的情况下返回true)
     */
    boolean acceptNow(@Nonnull BiConsumer<? super V, ? super Throwable> action);

    // ------------------------------------- 阻塞式API --------------------------------------

    /**
     * 阻塞式获取task的结果，阻塞期间不响应中断。
     * 如果future关联的task尚未完成，则阻塞等待至任务完成，并返回计算的结果。
     * 如果future关联的task已完成，则立即返回结果。
     * <p>
     * 注意：
     * 如果future关联的task没有返回值(操作完成返回null)，此时不能根据返回值做任何判断。对于这种情况，
     * 你可以使用{@link #isCompletedExceptionally()},作为更好的选择。
     *
     * @return task的结果
     * @throws CancellationException 如果任务被取消了，则抛出该异常
     * @throws CompletionException   如果在计算过程中出现了其它异常导致任务失败，则抛出该异常。
     *                               使用非受检异常更好，这里和{@link CompletableFuture#join()}是一样的。
     */
    V join();

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
    ListenableFuture<V> await() throws InterruptedException;

    /**
     * 等待future进入完成状态，等待期间不响应中断，并默默的丢弃，在方法返回前会重置中断状态。
     * 在方法返回之后，接下来的{@link #isDone()}调用都将返回true。
     *
     * @return this
     */
    ListenableFuture<V> awaitUninterruptibly();

    // ------------------------------------- 监听器 --------------------------------------

    /**
     * 添加一个监听器，一旦{@code Future}进入完成状态，无论正常完成还是异常完成，给定的操作就将被执行。
     * 如果{@code Future}当前已完成，则立即执行给定的动作。
     * <p>
     * Q: 这个方法有什么用？
     * A: 传递{@code Future}，这样用户可以在不想处理异常时，直接调用{@link #getNow()}方法，可以重新抛出异常。
     * 从而避免异常被隐藏。
     *
     * @return this
     */
    ListenableFuture<V> addListener(FutureListener<? super V> listener);

    ListenableFuture<V> addListener(FutureListener<? super V> listener, Executor executor);

    /**
     * 添加一个监听器，如果当前{@code Future}由于异常完成，则执行给定的操作。
     * 如果{@code Future}当前已失败，则立即执行给定的动作。
     *
     * @return this
     */
    ListenableFuture<V> addFailedListener(Consumer<? super Throwable> action);

    ListenableFuture<V> addFailedListener(Consumer<? super Throwable> action, Executor executor);

}
