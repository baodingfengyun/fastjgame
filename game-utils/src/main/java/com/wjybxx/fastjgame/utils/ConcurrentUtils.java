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

package com.wjybxx.fastjgame.utils;

import com.wjybxx.fastjgame.concurrent.*;
import com.wjybxx.fastjgame.exception.InternalApiException;
import com.wjybxx.fastjgame.function.AcquireFun;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.*;

/**
 * 并发工具包，常用并发工具方法。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/14 1:02
 * github - https://github.com/hl845740757
 */
public class ConcurrentUtils {

    private static final Logger logger = LoggerFactory.getLogger(ConcurrentUtils.class);

    /**
     * 什么都不做的任务
     */
    public static final Runnable NO_OP_TASK = FunctionUtils.NO_OP_TASK;

    private ConcurrentUtils() {

    }

    /**
     * 在{@link CountDownLatch#await()}上等待，等待期间不响应中断
     *
     * @param countDownLatch 闭锁
     */
    public static void awaitUninterruptibly(@Nonnull CountDownLatch countDownLatch) {
        awaitUninterruptibly(countDownLatch::await);
    }

    /**
     * 在{@link Semaphore#acquire()}上等待，等待期间不响应中断
     *
     * @param semaphore 信号量
     */
    public static void awaitUninterruptibly(Semaphore semaphore) {
        awaitUninterruptibly(semaphore::acquire);
    }

    /**
     * 在等待期间不响应中断
     *
     * @param acquireFun 如果在资源上申请资源
     */
    public static void awaitUninterruptibly(AcquireFun acquireFun) {
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    acquireFun.acquire();
                    break;
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        } finally {
            // 恢复中断状态
            recoveryInterrupted(interrupted);
        }
    }

    // ---------------------------------------- 中断处理 ---------------------------

    /**
     * 恢复中断。
     * 如果是中断异常，则恢复线程中断状态。
     *
     * @param t 异常
     */
    public static void recoveryInterrupted(Throwable t) {
        recoveryInterrupted(t instanceof InterruptedException);
    }

    /**
     * 恢复中断
     *
     * @param interrupted 是否出现了中断
     */
    public static void recoveryInterrupted(boolean interrupted) {
        if (interrupted) {
            try {
                Thread.currentThread().interrupt();
            } catch (SecurityException ignore) {
            }
        }
    }

    /**
     * 检查线程中断状态。
     *
     * @throws InterruptedException 如果线程被中断，则抛出中断异常
     */
    public static void checkInterrupted() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
    }

    // ------------------------------------------- 等待处理 -------------------------------------

    /**
     * 安静地睡眠一会儿
     *
     * @param sleepMillis 要睡眠的时间(毫秒)
     */
    public static void sleepQuietly(int sleepMillis) {
        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException ignore) {

        }
    }

    // ------------------------------------------- 安全地执行 ------------------------------------

    /**
     * 安全的执行一个任务，只是将错误打印到日志，不抛出异常。
     * 对于不甚频繁的方法调用可以进行封装，如果大量的调用可能会对性能有所影响；
     *
     * @param task 要执行的任务，可以将要执行的方法封装为 ()-> safeExecute()
     */
    public static void safeExecute(Runnable task) {
        try {
            task.run();
        } catch (Throwable t) {
            if (t instanceof VirtualMachineError) {
                logger.error("A task raised an exception. Task: {}", task, t);
            } else {
                logger.warn("A task raised an exception. Task: {}", task, t);
            }
        }
    }

    /**
     * 安全的提交任务到指定executor
     *
     * @param executor 任务提交的目的地
     * @param task     待提交的任务
     */
    public static void safeExecute(@Nonnull Executor executor, @Nonnull Runnable task) {
        try {
            executor.execute(task);
            // 这里也不一定真正的提交成功了，因为目标executor的拒绝策略我们并不知晓
        } catch (Throwable e) {
            // may reject
            if (e instanceof RejectedExecutionException) {
                logger.info("Try commit failure, target executor may shutdown.");
            } else {
                logger.warn("execute caught exception!", e);
            }
        }
    }

    /**
     * 重新抛出异常，绕过编译时检查。
     *
     * @param ex 受检异常
     */
    public static <R> R rethrow(@Nonnull Throwable ex) {
        return ExceptionUtils.rethrow(ex);
    }

    // ---------------------------------------------- 事件循环相关 ------------------------------------------------

    /**
     * 线程保护
     *
     * @param eventLoop 事件循环
     */
    public static void ensureInEventLoop(EventLoop eventLoop) {
        if (!eventLoop.inEventLoop()) {
            throw new InternalApiException();
        }
    }

    /**
     * 检查死锁，由于EventLoop是单线程的，因此不能在当前EventLoop上等待另一个任务完成，很可能导致死锁。
     *
     * @param e executor
     */
    public static void checkDeadLock(EventLoop e) {
        if (e != null && e.inEventLoop()) {
            throw new BlockingOperationException();
        }
    }

    /**
     * 检查死锁，由于EventLoop是单线程的，因此不能在当前EventLoop上等待另一个任务完成，很可能导致死锁。
     *
     * @param e   executor
     * @param msg 造成死锁的信息，尽量少拼接字符串。
     */
    public static void checkDeadLock(EventLoop e, String msg) {
        if (e != null && e.inEventLoop()) {
            throw new BlockingOperationException(msg);
        }
    }

    /**
     * 如果当前线程就是EventLoop线程，则直接执行任务，否则进行提交
     *
     * @param eventLoop 事件循环
     * @param task      任务
     */
    public static void executeOrRun(@Nonnull EventLoop eventLoop, Runnable task) {
        if (eventLoop.inEventLoop()) {
            task.run();
        } else {
            eventLoop.execute(task);
        }
    }

    /**
     * 如果当前线程就是EventLoop线程，则直接执行任务，否则进行提交
     *
     * @param eventLoop 事件循环
     * @param task      任务
     * @return future
     */
    public static ListenableFuture<?> submitOrRun(@Nonnull EventLoop eventLoop, Runnable task) {
        return submitOrRun(eventLoop, Executors.callable(task, null));
    }

    /**
     * 如果当前线程就是EventLoop线程，则直接执行任务，否则进行提交
     *
     * @param eventLoop 事件循环
     * @param task      任务
     * @return future
     */
    public static <V> ListenableFuture<V> submitOrRun(@Nonnull EventLoop eventLoop, Callable<V> task) {
        if (eventLoop.inEventLoop()) {
            try {
                V result = task.call();
                return new SucceededFuture<>(eventLoop, result);
            } catch (Throwable e) {
                return new FailedFuture<>(eventLoop, e);
            }
        } else {
            return eventLoop.submit(task);
        }
    }

}
