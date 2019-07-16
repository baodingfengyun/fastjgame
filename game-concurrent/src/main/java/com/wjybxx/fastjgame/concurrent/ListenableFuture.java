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
 * @param <V> 值类型
 * @author wjybxx
 */
public interface ListenableFuture<V> extends Future<V>{

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
	 *
	 * returns 当且仅当future关联的任务可以通过{@link #cancel(boolean)}被取消时返回true。
	 */
	boolean isCancellable();

	// ------------------------------------------ 操作 -------------------------------------------

	/**
	 * 获取task的结果。
	 * 如果future关联的task尚未完成，则阻塞等待至任务完成，并返回计算的结果。
	 * 如果future关联的task已完成，则立即返回结果。
	 *
	 * 注意：
	 * 如果future关联的task没有返回值(操作完成返回null)，此时不能根据返回值做任何判断。对于这种情况，
	 * 1. 你可以调用{@link #isDone()} 检查task是否已真正的完成，然后才尝试获取结果。
	 * 2. 你也可以使用{@link #isSuccess()},作为更好的选择。
	 *
	 * @return task的结果
	 * @throws CancellationException 如果任务被取消了，则抛出该异常
	 * @throws ExecutionException 如果在计算过程中出现了其它异常导致任务失败，则抛出该异常。
	 * @throws InterruptedException 如果当前线程在等待过程中被中断，则抛出该异常。
	 */
	@Override
	V get() throws InterruptedException, ExecutionException;

	/**
	 * 在限定时间内获取task的结果。
	 * 如果future关联的task尚未完成，则等待一定时间，等待期间如果任务完成，则返回计算的结果。
	 * 如果future关联的task已完成，则立即返回结果。
	 *
	 * 注意：
	 * 如果future关联的task没有返回值(操作完成返回null)，此时不能根据返回值做任何判断。对于这种情况，
	 * 1. 你可以调用{@link #isDone()} 检查task是否已真正的完成，然后才尝试获取结果。
	 * 2. 你也可以使用{@link #isSuccess()},作为更好的选择。
	 *
	 * @param timeout 最大等待时间
	 * @param unit timeout的时间单位
	 * @return future关联的task的计算结果
	 * @throws CancellationException 如果任务被取消了，则抛出该异常
	 * @throws ExecutionException 如果在计算过程中出现了其它异常导致任务失败，则抛出该异常。
	 * @throws InterruptedException 如果当前线程在等待过程中被中断，则抛出该异常。
	 * @throws TimeoutException 在限定时间内task未完成(等待超时)，则抛出该异常。
	 */
	@Override
	V get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;


	/**
	 * 尝试非阻塞的获取当前结果，当前仅当任务正常完成时返回期望的结果，否则返回null，即：
	 * 1. 如果future关联的task还未完成 {@link #isDone() false}，则返回null。
	 * 2. 如果任务被取消或失败，则返回null。
	 *
	 * 注意：
	 * 如果future关联的task没有返回值(操作完成返回null)，此时不能根据返回值做任何判断。对于这种情况，
	 * 1. 你可以调用{@link #isDone()} 检查task是否已真正的完成，然后才尝试获取结果。
	 * 2. 你也可以使用{@link #isSuccess()},作为更好的选择。
	 *
	 * @return task执行结果
	 */
	V tryGet();

	/**
	 * 非阻塞获取导致任务失败的原因。
	 * 当future关联的任务由于取消或异常进入完成状态后，该方法将返回操作失败的原因。
	 *
	 * @return  失败的原因。{@link CancellationException} 或 {@link ExecutionException}。
	 * 			如果future关联的task已正常完成，则返回null。
	 * 			如果future关联的task还未进入完成状态，则返回null。
	 */
	@Nullable
	Throwable cause();

	/**
	 * {@inheritDoc}
	 *
	 * 如果取消成功，会使得Future进入完成状态，并且{@link #cause()}将返回{@link CancellationException}。
	 *
	 * If the cancellation was successful it will fail the future with an {@link CancellationException}.
	 */
	@Override
	boolean cancel(boolean mayInterruptIfRunning);

	// -------------------------------- 等待进入完成状态  --------------------------------------
	/**
	 * 等待future进入完成状态。
	 * 方法正常返回后，接下来的{@link #isDone()}调用都将返回true。
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
	 * @param timeout 等待的最大时间。如果等于0，等效于{@link #await()}
	 * @param unit 时间单位
	 * @return 当且仅当future关联的task在指定时间内进入了完成状态，返回true。
	 * @throws InterruptedException 如果当前线程在等待期间被中断
	 */
	boolean await(long timeout, TimeUnit unit) throws InterruptedException;

	/**
	 * 在指定的时间范围内等待，直到future关联的任务进入完成状态，并且在等待期间不响应中断。
	 * 在等待期间，会捕获中断，并默默的丢弃，在方法返回前会恢复中断状态。
	 *
	 * @param timeout 等待的最大时间。如果等于0，等效于{@link #awaitUninterruptibly()}
	 * @param unit 时间单位
	 * @return 当且仅当future关联的任务，在特定时间范围内进入完成状态时返回true。
	 */
	boolean awaitUninterruptibly(long timeout, TimeUnit unit);

	// ------------------------------------- 监听 --------------------------------------
	/**
	 * 添加一个监听器，该监听器将在默认的事件分发线程中执行。
	 * 当你的代码支持并发调用的时候，那么使用该方法注册监听器即可。
	 * @param listener 要添加的监听器 PECS Listener作为消费者，可以把生产的结果V 看做V或V的超类型消费，因此需要使用super。
	 */
	void addListener(@Nonnull FutureListener<? super V> listener);

	/**
	 * 添加一个监听器，该监听器将在给定的executor中执行。
	 * 如果要保证事件的顺序，那么{@link Executor}必须是单线程的！
	 *
	 * 当你的执行环境是一个 单线程的executor的时候，可以直接提交到你所在的线程！从而消除事件处理时的同步逻辑。
	 * eg:
	 * <pre>
	 * {@code
	 * 		// this.executor代表当前线程
	 *		addListener(l, this.bindExecutor)
	 * }
	 * </pre>
	 * 注意死锁问题，不可以提交到自身，然后进行同步等待。
	 *
	 * @param listener 要添加的监听器
	 * @param bindExecutor 监听器执行的线程
	 */
	void addListener(@Nonnull FutureListener<? super V> listener, @Nonnull EventLoop bindExecutor);

	/**
	 * 移除指定监听器。
	 *
	 * @param listener 要移除的监听器
	 */
	void removeListener(@Nonnull FutureListener<? super V> listener);

}
