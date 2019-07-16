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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

/**
 * 事件循环线程组。
 *
 * 目前来说不需要实现schedule，就游戏而言，用到的地方并不多，可以换别的方式实现。
 * 此外，虽然{@link EventLoopGroup}继承自{@link ExecutorService}，其中有些方法并不是很想实现，最好少用。
 *
 * (它是组合模式中的容器组件)
 * @version 1.0
 * date - 2019/7/14 14:53
 * github - https://github.com/hl845740757
 */
public interface EventLoopGroup extends ExecutorService, Iterable<EventLoop> {

	/**
	 * 默认的关闭前的安静期 2S
	 */
	long DEFAULT_SHUTDOWN_QUIET_PERIOD = 2;
	/**
	 * 默认的等待关闭超时的时间， 15秒
	 */
	long DEFAULT_SHUTDOWN_TIMEOUT = 15;

	/**
	 * {@link #shutdownGracefully(long, long, TimeUnit)}的快捷调用方式，参数为合理的默认值。
	 * (该方法就不详细解释了，见带参方法)
	 */
	ListenableFuture<?> shutdownGracefully();

	/**
	 * 通知当前{@link EventLoopGroup} 关闭。
	 * 一旦该方法被调用，{@link #isShuttingDown()}将开始返回true,并且当前 executor准备开始关闭自己。
	 * 和{@link #shutdown()}方法不同的是，优雅的关闭将保证在关闭前的安静期没有任务提交。
	 * 如果在安静期提交了一个任务，那么它一定会接受它并重新进入安静期。
	 * (也就是说不推荐使用 {@link ExecutorService#shutdown()} 和 {@link ExecutorService#shutdownNow()}方法。
	 *
	 * Signals this executor that the caller wants the executor to be shut down.  Once this method is called,
	 * {@link #isShuttingDown()} starts to return {@code true}, and the executor prepares to shut itself down.
	 * Unlike {@link #shutdown()}, graceful shutdown ensures that no tasks are submitted for <i>'the quiet period'</i>
	 * (usually a couple seconds) before it shuts itself down.  If a task is submitted during the quiet period,
	 * it is guaranteed to be accepted and the quiet period will start over.
	 *
	 * @param quietPeriod the quiet period as described in the documentation 默认的安静时间(秒)
	 * @param timeout     the maximum amount of time to wait until the executor is {@linkplain #shutdown()}
	 *                    regardless if a task was submitted during the quiet period
	 *                    等待当前executor成功关闭的超时时间，而不管是否有任务在关闭前的安静期提交。
	 * @param unit        the unit of {@code quietPeriod} and {@code timeout}
	 *                    quietPeriod 和 timeout 的时间单位。
	 *
	 * @return the {@link #terminationFuture()}
	 */
	ListenableFuture<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit);

	@Override
	boolean isTerminated();

	/**
	 * 返回等待线程终止的future。
	 * 返回的{@link ListenableFuture}会在该Group管理的所有{@link EventLoop}终止后收到通知.
	 */
	ListenableFuture<?> terminationFuture();

	/**
	 * 查询{@link EventLoopGroup}是否处于正在关闭状态。
	 *
	 * @return 如果该{@link EventLoopGroup}管理的所有{@link EventLoop}正在优雅的关闭或已关闭则返回true
	 */
	boolean isShuttingDown();

	/**
	 * @deprecated {@link #shutdownGracefully(long, long, TimeUnit)} or {@link #shutdownGracefully()} instead.
	 */
	@Override
	@Deprecated
	void shutdown();

	/**
	 * @deprecated {@link #shutdownGracefully(long, long, TimeUnit)} or {@link #shutdownGracefully()} instead.
	 */
	@Nonnull
	@Override
	@Deprecated
	List<Runnable> shutdownNow();

	/**
	 * 返回一个EventLoop用于接下来的调度
	 */
	EventLoop next();

	@Nonnull
	@Override
	Iterator<EventLoop> iterator();

	// ----------------------------- 这是我想要支持的任务调度 ------------------------

	@Override
	void execute(@Nonnull Runnable command);

	@Nonnull
	@Override
	ListenableFuture<?> submit(@Nonnull Runnable task);

	@Nonnull
	@Override
	<V> ListenableFuture<V> submit(@Nonnull Runnable task, V result);

	@Nonnull
	@Override
	<V> ListenableFuture<V> submit(@Nonnull Callable<V> task);

	// ---------------------------- 这是我不想支持的任务调度 ---------------------------------
	@Nonnull
	@Override
	<T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException;

	@Nonnull
	@Override
	<T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) throws InterruptedException;

	@Nonnull
	@Override
	<T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException;

	@Override
	<T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;
}
