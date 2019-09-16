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
 * 事件循环
 *
 * <h>它是单线程的，提供以下保证：</h>
 * <li>保证任务不会并发执行。</li>
 * <li>还保证任务的执行顺序和提交顺序一致！{@link #execute(Runnable)}{@link #submit(Runnable)}</li>
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14
 * github - https://github.com/hl845740757
 * @apiNote 由于{@link EventLoop}都是单线程的，如果两个{@link EventLoop}存在直接交互，且都使用有界队列，则可能死锁！
 */
public interface EventLoop extends EventLoopGroup {

    /**
     * GameEventLoop表示非容器组件，始终由自己执行调用。
     *
     * @return EventLoop, 用于接下来的调度操作
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
     * 它暗示着：如果当前线程是EventLoop线程，那么可以访问一些线程封闭的数据。
     *
     * @return true/false
     * @apiNote <h3>时序问题</h3>
     * 以下代码可能产生时序问题:
     * <pre>
     * {@code
     * 		if(eventLoop.inEventLoop()) {
     * 	    	doSomething();
     *        } else{
     * 			eventLoop.execute(() -> doSomething());
     *        }
     * }
     * </pre>
     * Q: 产生的原因？
     * A: 单看任意一个线程，该线程的所有操作之间都是有序的，这个应该能理解。
     * 但是出现多个线程执行该代码块时：
     * 1. 所有的非EventLoop线程的操作会进入同一个队列，因此所有的非EventLoop线程之间的操作是有序的！
     * 2. 但是EventLoop线程是直接执行的，并没有进入队列，因此EventLoop线程 和 任意非EventLoop线程之间都没有顺序保证。
     * <p>
     * 例如：有一个全局计数器，每一个线程在执行某个方法之前需要先将计数+1。 如果EventLoop线程也会调用该方法，它可能导致方法内部看见的计数不是顺序的!!!
     * {@code final AtomicInteger counter = new AtomicInteger();}
     * 它有时候是无害的，有时候则是有害的，因此必须想明白是否需要提供全局时序保证！
     */
    boolean inEventLoop();

    /**
     * 创建一个{@link Promise}(一个可写的Future)。
     * 用户提交一个任务之后，返回给客户端一个Promise，
     * 使得用户可以获取操作结果和添加监听器。
     * <p>
     * 注意：最好不要在自己创建的promise上进行阻塞等待，否则可能导致死锁。建议使用{@link FutureListener}。
     * 在检测死锁时会抛出{@link BlockingOperationException}。
     *
     * @param <V> the type of value
     * @return Promise
     */
    @Nonnull
    <V> Promise<V> newPromise();

    /**
     * 创建一个{@link ListenableFuture}，该future表示它关联的任务早已失败。因此{@link ListenableFuture#isSuccess()}总是返回false。
     * 所有添加到该future上的{@link FutureListener}都会立即被通知。并且该future上的所有阻塞方法会立即返回而不会阻塞。
     *
     * @param e   任务失败的原因
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
     * @param <V>   the type of value
     * @return ListenableFuture
     */
    @Nonnull
    <V> ListenableFuture<V> newSucceededFuture(@Nullable V value);
}
