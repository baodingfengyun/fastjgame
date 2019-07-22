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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 全局的EventLoop。它是一个单线程的EventLoop，它不适合处理一些耗时的、阻塞的操作，
 * 仅仅适合处理一些简单的事件，当没有其它的更好的选择时可以使用{@link GlobalEventLoop}。
 *
 * 它会在没有任务后自动的关闭。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14
 * github - https://github.com/hl845740757
 */
public class GlobalEventLoop extends AbstractEventLoop{

	public static final GlobalEventLoop INSTANCE = new GlobalEventLoop(new InnerThreadFactory());

	/** 线程自动关闭的安静期,3秒 */
	private static final long QUIET_PERIOD_INTERVAL = 3;

	private volatile Thread thread;

	private final ExecutorService delegatedExecutorService;

	/** 不可以在GlobalEventLoop上等待其关闭 */
	private final ListenableFuture<?> terminationFuture = new FailedFuture<Object>(this, new UnsupportedOperationException());

	private GlobalEventLoop(InnerThreadFactory threadFacotry) {
		super(null);

		// 采用代理实现比较省心啊，注意拒绝策略不能使用 Caller Runs，否则会导致补货的线程不对。
		ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, 1 ,
				QUIET_PERIOD_INTERVAL, TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(),
				threadFacotry,
				new ThreadPoolExecutor.AbortPolicy());
		threadPoolExecutor.allowCoreThreadTimeOut(true);

		delegatedExecutorService = threadPoolExecutor;
	}

	@Override
	public boolean inEventLoop() {
		return thread == Thread.currentThread();
	}

	@Override
	public boolean isShuttingDown() {
		return false;
	}

	@Override
	public ListenableFuture<?> terminationFuture() {
		return terminationFuture;
	}

	@Override
	public void shutdown() {
		throw new UnsupportedOperationException("shutdown");
	}

	@Nonnull
	@Override
	public List<Runnable> shutdownNow() {
		throw new UnsupportedOperationException("shutdownNow");
	}

	@Override
	public boolean isShutdown() {
		return false;
	}

	@Override
	public boolean isTerminated() {
		return false;
	}

	@Override
	public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
		return false;
	}

	@Override
	public void execute(@Nonnull Runnable task) {
		delegatedExecutorService.execute(new ThreadCapture(task));
	}

	private static final class InnerThreadFactory implements ThreadFactory {

		private AtomicInteger index = new AtomicInteger(0);

		@Override
		public Thread newThread(@Nonnull Runnable r) {
			Thread thread = new Thread(r, "GLOBAL_EVENT_LOOP_" + index.getAndIncrement());
			// classLoader泄漏：
			// 如果创建线程的时候，未指定contextClassLoader,那么将会继承父线程(创建当前线程的线程)的contextClassLoader，见Thread.init()方法。
			// 如果创建线程的线程contextClassLoader是自定义类加载器，那么新创建的线程将继承(使用)该contextClassLoader，在线程未回收期间，将导致自定义类加载器无法回收。
			// 从而导致ClassLoader内存泄漏，基于自定义类加载器的某些设计可能失效。
			// 我们显式的将其设置为null，表示使用系统类加载器进行加载，避免造成内存泄漏。

			// Set to null to ensure we not create classloader leaks by holds a strong reference to the inherited
			// classloader.
			// See:
			// - https://github.com/netty/netty/issues/7290
			// - https://bugs.openjdk.java.net/browse/JDK-7008595
			AccessController.doPrivileged(new PrivilegedAction<Void>() {
				@Override
				public Void run() {
					thread.setContextClassLoader(null);
					return null;
				}
			});
			return thread;
		}
	}

	private final class ThreadCapture implements Runnable {

		private final Runnable task;

		private ThreadCapture(Runnable task) {
			this.task = task;
		}

		@Override
		public void run() {
			// capture thread
			thread = Thread.currentThread();

			task.run();
		}
	}
}
