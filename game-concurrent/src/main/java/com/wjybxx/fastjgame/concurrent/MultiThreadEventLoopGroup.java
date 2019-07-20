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
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 *
 * @version 1.0
 * date - 2019/7/15 13:29
 * github - https://github.com/hl845740757
 */
public abstract class MultiThreadEventLoopGroup extends AbstractEventLoopGroup {

	/**
	 * 包含的子节点们，用数组，方便分配下一个EventExecutor(通过计算索引来分配)
	 */
	private final EventLoop[] children;

	/**
	 * 只读的子节点集合，封装为一个集合，方便迭代，用于实现{@link Iterable}接口
	 */
	private final List<EventLoop> readonlyChildren;
	/**
	 * 进入终止状态的子节点数量
	 */
	private final AtomicInteger terminatedChildren = new AtomicInteger();
	/**
	 * 监听所有子节点关闭的Listener，当所有的子节点关闭时，会收到关闭成功事件
	 */
	private final Promise<?> terminationFuture = new DefaultPromise(GlobalEventLoop.INSTANCE);
	/**
	 * 选择下一个EventExecutor的方式，策略模式的运用。将选择算法交给Chooser
	 * 目前看见两种： 与操作计算 和 取模操作计算。
	 */
	private final EventLoopChooser chooser;
	/**
	 * 子类构造时传入的context
	 */
	private final Object context;

	protected MultiThreadEventLoopGroup(int nThreads, @Nonnull ThreadFactory threadFactory, @Nullable Object context) {
		this(nThreads, threadFactory, null, context);
	}

	protected MultiThreadEventLoopGroup(int nThreads, ThreadFactory threadFactory, @Nullable EventLoopChooserFactory chooserFactory, @Nullable Object context) {
		if (nThreads <= 0){
			throw new IllegalArgumentException("nThreads must greater than 0");
		}
		if (null == chooserFactory) {
			chooserFactory = new DefaultChooserFactory();
		}


		Executor executor = new ThreadPerTaskExecutor(threadFactory);
		children = new EventLoop[nThreads];

		// 创建指定数目的child，(其实就是创建指定数目的EventLoop，只不过是更高级的封装)
		for (int i = 0; i < nThreads; i ++) {
			children[i] = newChild(executor, context);
		}

		this.context = context;
		this.chooser = chooserFactory.newChooser(children);

		// 监听子节点关闭的Listener，可以看做回调式的CountDownLatch.
		final FutureListener<Object> terminationListener = new FutureListener<Object>() {
			@Override
			public <F extends ListenableFuture<? extends Object>> void onComplete(F future) throws Exception {
				if (terminatedChildren.incrementAndGet() == children.length) {
					terminationFuture.setSuccess(null);
				}
			}
		};

		// 在所有的子节点上监听 它们的关闭事件，当所有的child关闭时，可以获得通知
		for (EventLoop e: children) {
			e.terminationFuture().addListener(terminationListener);
		}


		// 将子节点数组封装为不可变集合，方便迭代(不允许外部改变持有的线程)
		List<EventLoop> modifiable = new ArrayList<>();
		Collections.addAll(modifiable, children);
		this.readonlyChildren = Collections.unmodifiableList(modifiable);
	}

	/**
	 * 子类自己决定创建EventLoop的方式和具体的类型
	 * @param executor 用于创建线程
	 * @param context 构造方法中传入的上下文
	 * @return
	 */
	protected abstract EventLoop newChild(Executor executor, Object context);

	// -------------------------------------  子类生命周期管理 --------------------------------

	@Override
	public ListenableFuture<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
		// 它不真正的管理线程内逻辑，只是管理持有的线程的生命周期，由child自己管理自己的生命周期。
		for (EventLoop l: children) {
			l.shutdownGracefully(quietPeriod, timeout, unit);
		}
		return terminationFuture();
	}

	@Override
	public ListenableFuture<?>  terminationFuture() {
		return terminationFuture;
	}

	@Override
	public EventLoop next() {
		return chooser.next();
	}

	@Override
	public boolean isShuttingDown() {
		return Arrays.stream(children).allMatch(EventLoop::isShuttingDown);
	}

	@Override
	public boolean isShutdown() {
		return Arrays.stream(children).allMatch(EventLoop::isShutdown);
	}

	@Override
	public boolean isTerminated() {
		return Arrays.stream(children).allMatch(EventLoop::isTerminated);
	}

	@Override
	public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
		// 这代码来自netty，虽然我能看明白
		long deadline = System.nanoTime() + unit.toNanos(timeout);
		loop: for (EventLoop l: children) {
			for (;;) {
				long timeLeft = deadline - System.nanoTime();
				// 超时了，跳出loop标签对应的循环，即查询是否所有child都终止了
				if (timeLeft <= 0) {
					break loop;
				}
				// 当前child进入了终止状态，跳出死循环检查下一个child
				if (l.awaitTermination(timeLeft, TimeUnit.NANOSECONDS)) {
					break;
				}
			}
		}
		// 可能是超时了，可能时限内成功终止了
		return isTerminated();
	}

	// ------------------------------------- 迭代 ----------------------------

	@Nonnull
	@Override
	public Iterator<EventLoop> iterator() {
		return readonlyChildren.iterator();
	}

	@Override
	public void forEach(Consumer<? super EventLoop> action) {
		readonlyChildren.forEach(action);
	}

	@Override
	public Spliterator<EventLoop> spliterator() {
		return readonlyChildren.spliterator();
	}
}
