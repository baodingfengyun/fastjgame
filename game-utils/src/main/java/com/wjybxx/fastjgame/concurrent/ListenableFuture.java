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

package com.wjybxx.fastjgame.concurrent;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.*;

/**
 * 可监听的future。
 * 在Netty和Curator里面见着了一些好的设计，但是他们的设计有很多用不上的或易错误使用的东西，进行简化。
 * guava里也有类似的。
 * <p>
 * Q: Listener的通知顺序是否和添加顺序一致？
 * A: 是的。
 * <p>
 * Q:为什么没有保留netty中的 {@code sync()}系列方法？
 * A:sync方法没有声明任何异常，但是却可能抛出异常！sync的语义更贴近于等待任务完成，但是其实现会在任务失败后抛出异常，一不小心会着道的，
 * 更建议使用{@link #await()} 和 {@link #isSuccess()}进行处理。
 * <p>
 * Q:为什么没有像netty一样支持流式语法？
 * A:基于安全性的考量，因为要实现流式语法，会导致大量的方法被重写，本应该是final的方法也不能声明为final，在重写的过程中，如果疏忽将可能破坏线程安全性。
 * 不支持流式语法有时候是有点不方便，但也不会太麻烦，但是安全性是有保证的。
 *
 * @param <V> 值类型
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14
 * github - https://github.com/hl845740757
 */
public interface ListenableFuture<V> extends Future<V> {

    // ----------------------------------------  查询 ----------------------------------------

    /**
     * 查询任务是否已完成。
     * 任务可能由于 正常完成，出现异常，或 被取消 进入完成状态 -- 这些情况都会返回true，他们都表示完成状态。
     *
     * @return 任务已进入完成状态则返回true。
     */
    @Override
    boolean isDone();

    /**
     * 查询future关联的操作是否顺利完成了。
     *
     * @return 当且仅当该future对应的task顺利完成时返回true。
     */
    boolean isSuccess();

    /**
     * 查询任务是否被取消。
     *
     * @return 当且仅当该future关联的task由于取消进入完成状态时返回true。
     */
    @Override
    boolean isCancelled();

    /**
     * 查询future关联的任务是否可以被取消。
     * <p>
     *
     * @return 当且仅当future关联的任务可以通过{@link #cancel(boolean)}被取消时返回true。
     */
    boolean isCancellable();

    // ------------------------------------------ 操作 -------------------------------------------

    /**
     * 获取task的结果。
     * 如果future关联的task尚未完成，则阻塞等待至任务完成，并返回计算的结果。
     * 如果future关联的task已完成，则立即返回结果。
     * <p>
     * 注意：
     * 如果future关联的task没有返回值(操作完成返回null)，此时不能根据返回值做任何判断。对于这种情况，
     * 你可以使用{@link #isSuccess()},作为更好的选择。
     *
     * @return task的结果
     * @throws CancellationException 如果任务被取消了，则抛出该异常
     * @throws ExecutionException    如果在计算过程中出现了其它异常导致任务失败，则抛出该异常。
     * @throws InterruptedException  如果当前线程在等待过程中被中断，则抛出该异常。
     */
    @Override
    V get() throws InterruptedException, ExecutionException;

    /**
     * 在限定时间内获取task的结果。
     * 如果future关联的task尚未完成，则等待一定时间，等待期间如果任务完成，则返回计算的结果。
     * 如果future关联的task已完成，则立即返回结果。
     * <p>
     * 注意：
     * 如果future关联的task没有返回值(操作完成返回null)，此时不能根据返回值做任何判断。对于这种情况，
     * 你可以使用{@link #isSuccess()},作为更好的选择。
     *
     * @param timeout 最大等待时间
     * @param unit    timeout的时间单位
     * @return future关联的task的计算结果
     * @throws CancellationException 如果任务被取消了，则抛出该异常
     * @throws ExecutionException    如果在计算过程中出现了其它异常导致任务失败，则抛出该异常。
     * @throws InterruptedException  如果当前线程在等待过程中被中断，则抛出该异常。
     * @throws TimeoutException      在限定时间内task未完成(等待超时)，则抛出该异常。
     */
    @Override
    V get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;


    /**
     * 尝试非阻塞的获取当前结果，当前仅当任务正常完成时返回期望的结果，否则返回null，即：
     * 1. 如果future关联的task还未完成 {@link #isDone() false}，则返回null。
     * 2. 如果任务被取消或失败，则返回null。
     * <p>
     * 注意：
     * 如果future关联的task没有返回值(操作完成返回null)，此时不能根据返回值做任何判断。对于这种情况，
     * 你可以使用{@link #isSuccess()},作为更好的选择。
     *
     * @return task执行结果
     */
    V getNow();

    /**
     * 非阻塞获取导致任务失败的原因。
     * 当future关联的任务被取消或由于异常进入完成状态后，该方法将返回操作失败的原因。
     *
     * @return 失败的原因。原始原因，而不是被包装后的{@link ExecutionException}
     * 如果future关联的task已正常完成，则返回null。
     * 如果future关联的task还未进入完成状态，则返回null。
     */
    @Nullable
    Throwable cause();

    /**
     * {@inheritDoc}
     * <p>
     * 如果取消成功，会使得Future进入完成状态，并且{@link #cause()}将返回{@link CancellationException}。
     * <p>
     * If the cancellation was successful it will fail the future with an {@link CancellationException}.
     */
    @Override
    boolean cancel(boolean mayInterruptIfRunning);

    // -------------------------------- 等待进入完成状态  --------------------------------------

    /**
     * 等待future进入完成状态。
     * await()不会查询任务的结果，在Future进入完成状态之后就返回，方法返回后，接下来的{@link #isDone()}调用都将返回true。
     *
     * @throws InterruptedException 如果在等待期间线程被中断，则抛出中断异常。
     */
    void await() throws InterruptedException;

    /**
     * 等待future进入完成状态，等待期间不响应中断，并默默的丢弃，在方法返回前会重置中断状态。
     * 在方法返回之后，接下来的{@link #isDone()}调用都将返回true。
     */
    void awaitUninterruptibly();

    /**
     * 在指定的时间范围内等待，直到future关联的任务进入完成状态。
     * 如果正常返回，接下来的{@link #isDone()}调用都将返回true。
     *
     * @param timeout 等待的最大时间，<b>如果小于等于0，表示不阻塞</b>
     * @param unit    时间单位
     * @return 当且仅当future关联的task在指定时间内进入了完成状态，返回true。也就是接下来的{@link #isDone() true} 。
     * @throws InterruptedException 如果当前线程在等待期间被中断
     */
    boolean await(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * 在指定的时间范围内等待，直到future关联的任务进入完成状态，并且在等待期间不响应中断。
     * 在等待期间，会捕获中断，并默默的丢弃，在方法返回前会恢复中断状态。
     *
     * @param timeout 等待的最大时间，<b>如果小于等于0，表示不阻塞</b>
     * @param unit    时间单位
     * @return 当且仅当future关联的任务，在特定时间范围内进入完成状态时返回true。也就是接下来的{@link #isDone() true}。
     */
    boolean awaitUninterruptibly(long timeout, TimeUnit unit);

    // ------------------------------------- 监听 --------------------------------------

    /**
     * 添加一个监听者到当前Future。传入的特定的Listener将会在Future计算完成时{@link #isDone() true}被通知。
     * 如果当前Future已经计算完成，那么将立即被通知。
     * 注意：
     * 1. 该监听器将在默认的事件分发线程中执行。当你的代码支持并发调用的时候，那么使用该方法注册监听器即可。
     * 2. 同一个listener反复添加会共存。
     *
     * @param listener 要添加的监听器。PECS Listener作为消费者，可以把生产的结果V 看做V或V的超类型消费，因此需要使用super。
     */
    void addListener(@Nonnull FutureListener<? super V> listener);

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
     * @param bindExecutor 监听器执行的线程
     */
    void addListener(@Nonnull FutureListener<? super V> listener, @Nonnull EventLoop bindExecutor);

    /**
     * 移除监听器中第一个与指定Listener匹配的监听器，如果该Listener没有进行注册，那么什么也不会做。
     *
     * @param listener 要移除的监听器
     * @return 是否删除了一个listener
     */
    boolean removeListener(@Nonnull FutureListener<? super V> listener);

    /**
     * 移除监听器中第一个与Listener与bindExecutor都匹配的监听器，如果不存在完全匹配的，则什么也不会做。
     * 一般来说{@link #removeListener(FutureListener)}就可满足需求。
     *
     * @param listener     要移除的监听器
     * @param bindExecutor 该监听器注册时关联的Executor
     * @return 是否删除了一个listener
     */
    boolean removeListener(@Nonnull FutureListener<? super V> listener, @Nonnull EventLoop bindExecutor);
}
