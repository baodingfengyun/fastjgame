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
 * 事件循环线程组，它表示拥有一个或多个事件循环线程。
 * <p>
 * 目前来说不需要实现schedule，就游戏而言，用到的地方并不多，可以换别的方式实现。
 * 在线程内部，定时任务建议使用{@link com.wjybxx.fastjgame.timer.TimerSystem}。
 * <p>
 * 此外，虽然{@link EventLoopGroup}继承自{@link ExecutorService}，其中有些方法并不是很想实现，最好少用。
 *
 * @version 1.0
 * date - 2019/7/14
 * github - https://github.com/hl845740757
 */
public interface EventLoopGroup extends ExecutorService, Iterable<EventLoop> {

    // ------------------------------ 生命周期相关方法 ----------------------------

    /**
     * 查询{@link EventLoopGroup}是否处于正在关闭状态。
     * 正在关闭状态下，拒绝接收新任务，当执行完所有任务后，进入关闭状态。
     *
     * @return 如果该{@link EventLoopGroup}管理的所有{@link EventLoop}正在关闭或已关闭则返回true
     */
    boolean isShuttingDown();

    /**
     * 查询{@link EventLoopGroup}是否处于关闭状态。
     * 关闭状态下，拒绝接收新任务，执行退出前的清理操作，执行完清理操作后，进入终止状态。
     *
     * @return 如果已关闭，则返回true
     */
    @Override
    boolean isShutdown();

    /**
     * 是否已进入终止状态，一旦进入终止状态，表示生命周期真正结束。
     *
     * @return 如果已处于终止状态，则返回true
     */
    @Override
    boolean isTerminated();

    /**
     * 等待EventLoopGroup进入终止状态
     *
     * @param timeout 时间度量
     * @param unit    事件单位
     * @return 在方法返回前是否已进入终止状态
     * @throws InterruptedException 如果在等待期间线程被中断，则抛出该异常。
     */
    @Override
    boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException;

    /**
     * 返回等待线程终止的future。
     * 返回的{@link ListenableFuture}会在该Group管理的所有{@link EventLoop}终止后收到通知.
     */
    ListenableFuture<?> terminationFuture();

    /**
     * 请求关闭 ExecutorService，不再接收新的任务。
     * ExecutorService在执行完现有任务后，进入关闭状态。
     * 如果 ExecutorService 正在关闭，或已经关闭，则方法不产生任何效果。
     * <p>
     * 该方法会立即返回，如果想等待 ExecutorService 进入终止状态，
     * 可以使用{@link #awaitTermination(long, TimeUnit)}或{@link #terminationFuture()} 进行等待
     */
    @Override
    void shutdown();

    /**
     * JDK文档：
     * 请求关闭 ExecutorService，<b>尝试取消所有正在执行的任务，停止所有待执行的任务，并不再接收新的任务。</b>
     * 如果 ExecutorService 已经关闭，则方法不产生任何效果。
     * <p>
     * 该方法会立即返回，如果想等待 ExecutorService 进入终止状态，可以使用{@link #awaitTermination(long, TimeUnit)}
     * 或{@link #terminationFuture()} 进行等待
     * <p>
     * 注意：虽然去除了{@link Deprecated}注解，但是仍然不保证标准的实现，只保证尽快的关闭，基于以下原因：
     * <li>1. 可能无法安全的获取所有的任务(EventLoop架构属于多生产者单消费者模型，会尽量的避免其它线程消费数据)</li>
     * <li>2. 剩余任务数可能过多</li>
     *
     * @return 当前待执行的任务列表。
     */
    @Nonnull
    @Override
    List<Runnable> shutdownNow();
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

    // ------------------------------- 这是我不想支持的任务调度 ---------------------------------

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
    @Nonnull
    EventLoop next();

    /**
     * 给定一个键 - 分配一个{@link EventLoop}。
     * 目的：这样用户总是可以通过key指定选中某一个线程，消除不必要的同步。
     *
     * @param key 计算索引的键
     * @apiNote 必须保证同一个key分配的结果一定是相同的
     */
    @Nonnull
    EventLoop select(int key);

    @Nonnull
    @Override
    Iterator<EventLoop> iterator();
}
