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

/**
 * 事件循环，它是一个线程的抽象，它一定是单线程的，事件循环如果实现为多线程的，将失去意义。
 *
 * (它是组合模式中的叶子组件，它不能增加子组件)
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14
 * github - https://github.com/hl845740757
 */
public interface EventLoop extends EventLoopGroup {

	/**
	 * GameEventLoop表示非容器组件，始终由自己执行调用。
	 * @return EventLoop,用于接下来的调度操作
	 */
	@Nonnull
	@Override
	EventLoop next();

	/**
	 * 返回该EventLoop线程所在的线程组（管理该EventLoop的容器）。
	 * 如果没有父节点，返回null。
	 */
	@Nullable
	EventLoopGroup parent();

	/**
	 * 当前线程是否是EventLoop线程。
	 * {@link io.netty.channel.EventLoop#inEventLoop()}
	 * @return true/false
	 */
	boolean inEventLoop();

	/**
	 * 创建一个{@link Promise}(一个可写的Future)。
	 * 用户提交一个任务之后，返回给客户端一个Promise，
	 * 使得用户可以获取操作结果和添加监听器。
	 * @param <V> the type of value
	 * @return Promise
	 */
	@Nonnull
	<V> Promise<V> newPromise();

	/**
	 * 创建一个{@link ListenableFuture}，该future表示它关联的任务早已失败。因此{@link ListenableFuture#isSuccess()}总是返回false。
	 * 所有添加到该future上的{@link FutureListener}都会立即被通知。并且该future上的所有阻塞方法会立即返回而不会阻塞。
	 *
	 * @param e 任务失败的原因
	 * @param <V> the type of value
	 * @return ListenableFuture
	 */
	@Nonnull
	<V> ListenableFuture<V> newFailedFuture(@Nonnull Throwable e);

	/**
	 * 创建一个{@link ListenableFuture}，该future表示它关联的任务早已正常完成。因此{@link ListenableFuture#isSuccess()}总是返回true。
	 * 所有添加到该future上的{@link FutureListener}都会立即被通知。并且该future上的所有阻塞方法会立即返回而不会阻塞。
	 *
	 * @param value 结果值
	 * @param <V> the type of value
	 * @return ListenableFuture
	 */
	@Nonnull
	<V> ListenableFuture<V> newSucceededFuture(@Nullable V value);
}
