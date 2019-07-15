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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 游戏线程组。
 * 目前来说不需要实现schedule，就游戏而言，用到的地方并不多，可以换别的方式实现。
 *
 * (它是组合模式中的容器组件)
 * @version 1.0
 * date - 2019/7/14 14:53
 * github - https://github.com/hl845740757
 */
public interface EventLoopGroup extends ExecutorService, Iterable<EventLoop> {

	/**
	 * 查询{@link EventLoopGroup}是否处于正在关闭状态。
	 *
	 * @return 如果该{@link EventLoopGroup}管理的所有{@link EventLoop}正在优雅的关闭或已关闭则返回true
	 */
	boolean isShuttingDown();

	/**
	 * {@link #shutdownGracefully(long, long, TimeUnit)}的快捷调用方式，参数为合理的默认值。
	 * (该方法就不详细解释了，见带参方法)
	 */
	ListenableFuture<?> shutdownGracefully();

	/**
	 * Signals this executor that the caller wants the executor to be shut down.  Once this method is called,
	 * {@link #isShuttingDown()} starts to return {@code true}, and the executor prepares to shut itself down.
	 * Unlike {@link #shutdown()}, graceful shutdown ensures that no tasks are submitted for <i>'the quiet period'</i>
	 * (usually a couple seconds) before it shuts itself down.  If a task is submitted during the quiet period,
	 * it is guaranteed to be accepted and the quiet period will start over.
	 *
	 * @param quietPeriod the quiet period as described in the documentation
	 * @param timeout     the maximum amount of time to wait until the executor is {@linkplain #shutdown()}
	 *                    regardless if a task was submitted during the quiet period
	 * @param unit        the unit of {@code quietPeriod} and {@code timeout}
	 *
	 * @return the {@link #terminationFuture()}
	 */
	ListenableFuture<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit);

	/**
	 * Returns the {@link ListenableFuture} which is notified when all {@link EventLoop}s managed by this
	 * {@link EventLoopGroup} have been terminated.
	 */
	ListenableFuture<?> terminationFuture();

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
}
