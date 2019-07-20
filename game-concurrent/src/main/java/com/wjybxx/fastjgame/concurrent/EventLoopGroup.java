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

	// ------------------------------ 生命周期相关方法 ----------------------------

	/**
	 * 请求关闭 ExecutorService，不再接收新的任务。
	 * ExecutorService在执行完现有任务后，进入关闭状态。
	 * 如果 ExecutorService 正在关闭，或已经关闭，则方法不产生任何效果。
	 *
	 * 该方法会立即返回，如果想等待 ExecutorService 进入终止状态，
	 * 可以使用{@link #awaitTermination(long, TimeUnit)}或{@link #terminationFuture()} 进行等待
	 */
	@Override
	void shutdown();

	/**
	 * 请求关闭 ExecutorService，<b>尝试取消所有正在执行的任务，停止所有待执行的任务，并不再接收新的任务。</b>
	 * 如果 ExecutorService 已经关闭，则方法不产生任何效果。
	 *
	 * 该方法会立即返回，如果想等待 ExecutorService 进入终止状态，可以使用{@link #awaitTermination(long, TimeUnit)}
	 * 或{@link #terminationFuture()} 进行等待
	 *
	 * @return 当前待执行的任务列表。
	 */
	@Nonnull
	@Override
	List<Runnable> shutdownNow();

	/**
	 * 查询{@link EventLoopGroup}是否处于正在关闭状态。
	 *
	 * @return 如果该{@link EventLoopGroup}管理的所有{@link EventLoop}正在关闭或已关闭则返回true
	 */
	boolean isShuttingDown();

	/**
	 * 查询{@link EventLoopGroup}是否处于关闭状态。
	 * @return 如果已关闭，则返回true
	 */
	@Override
	boolean isShutdown();

	@Override
	boolean isTerminated();

	@Override
	boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException;

	/**
	 * 返回等待线程终止的future。
	 * 返回的{@link ListenableFuture}会在该Group管理的所有{@link EventLoop}终止后收到通知.
	 */
	ListenableFuture<?> terminationFuture();
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


	// --------------------------------- EventLoop管理   --------------------------------
	/**
	 * 返回一个EventLoop用于接下来的调度
	 */
	EventLoop next();

	@Nonnull
	@Override
	Iterator<EventLoop> iterator();
}
