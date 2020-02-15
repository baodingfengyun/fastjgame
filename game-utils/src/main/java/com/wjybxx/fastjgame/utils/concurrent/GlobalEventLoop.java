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

import com.wjybxx.fastjgame.utils.ConcurrentUtils;

import javax.annotation.Nonnull;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.concurrent.*;

/**
 * 全局的EventLoop。它是一个单线程的EventLoop，它不适合处理一些耗时的、阻塞的操作，
 * 仅仅适合处理一些简单的事件，当没有其它的更好的选择时可以使用{@link GlobalEventLoop}。
 * <p>
 * 它会在没有任务后自动关闭。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14
 * github - https://github.com/hl845740757
 */
public class GlobalEventLoop extends AbstractEventLoop {

    public static final GlobalEventLoop INSTANCE = new GlobalEventLoop();

    /**
     * 线程自动关闭的安静期,3秒
     */
    private static final long QUIET_PERIOD_INTERVAL = 3;
    /**
     * GlobalEventLoop当前使用的线程
     */
    private volatile Thread thread;
    /**
     * 真正管理线程的executorService
     */
    private final ExecutorService executorService;

    /**
     * 不可以在GlobalEventLoop上等待其关闭
     */
    private final ListenableFuture<?> terminationFuture = new FailedFuture<>(this, new UnsupportedOperationException());

    private GlobalEventLoop() {
        super(null);

        // 采用代理实现比较省心啊
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, 1,
                QUIET_PERIOD_INTERVAL, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new InnerThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
        // 允许核心线程超时是实现自动关闭的关键。
        threadPoolExecutor.allowCoreThreadTimeOut(true);

        executorService = threadPoolExecutor;
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
        executorService.execute(() -> ConcurrentUtils.safeExecute(task));
    }

    private final class InnerThreadFactory implements ThreadFactory {

        private final DefaultThreadFactory threadFactory;

        private InnerThreadFactory() {
            this.threadFactory = new DefaultThreadFactory("GLOBAL_EVENT_LOOP");
        }

        @Override
        public Thread newThread(@Nonnull Runnable r) {
            Thread thread = threadFactory.newThread(r);

            // 这部分内容是在netty的GlobalEventExecutor中看见的，这个问题我确实没想到过。
            // classLoader泄漏：
            // 如果创建线程的时候，未指定contextClassLoader,那么将会继承父线程(创建当前线程的线程)的contextClassLoader，见Thread.init()方法。
            // 如果父线程contextClassLoader是自定义类加载器，那么新创建的线程将继承(使用)该contextClassLoader，在线程未回收期间，将导致自定义类加载器无法回收。
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

            // executor是单线程的，那么创建的线程就是我们EventLoop的线程
            GlobalEventLoop.this.thread = thread;
            return thread;
        }
    }
}
