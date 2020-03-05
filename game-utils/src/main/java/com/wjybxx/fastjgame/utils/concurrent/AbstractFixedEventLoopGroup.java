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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 固定线程的EventLoopGroup
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14
 * github - https://github.com/hl845740757
 */
public abstract class AbstractFixedEventLoopGroup extends AbstractEventLoopGroup implements FixedEventLoopGroup {

    private static final Logger logger = LoggerFactory.getLogger(AbstractFixedEventLoopGroup.class);

    /**
     * 监听所有子节点关闭的Listener，当所有的子节点关闭时，会收到关闭成功事件
     */
    private final Promise<?> terminationFuture = new DefaultPromise(GlobalEventLoop.INSTANCE);
    /**
     * 子类构造时传入的context，由子类自己决定如何解析，父类不做处理。
     */
    private final Object context;

    /**
     * 包含的子节点们，用数组，方便分配下一个EventExecutor(通过计算索引来分配)
     */
    private final EventLoop[] children;

    /**
     * 只读的子节点集合，封装为一个集合，方便迭代，用于实现{@link Iterable}接口
     */
    private final List<EventLoop> readonlyChildren;
    /**
     * 选择下一个EventExecutor的方式，策略模式的运用。将选择算法交给Chooser
     * 目前看见两种： 与操作计算 和 取模操作计算。
     */
    private final EventLoopChooser chooser;

    /**
     * @see #AbstractFixedEventLoopGroup(int, ThreadFactory, RejectedExecutionHandler, EventLoopChooserFactory, Object)
     */
    protected AbstractFixedEventLoopGroup(int nThreads,
                                          @Nonnull ThreadFactory threadFactory,
                                          @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                                          @Nullable Object context) {
        this(nThreads, threadFactory, rejectedExecutionHandler, null, context);
    }

    /**
     * @param nThreads       该线程组的线程数
     * @param threadFactory  线程工厂
     * @param chooserFactory EventLoop选择器工厂，负载均衡实现
     * @param context        子类构建时需要的额外信息
     */
    protected AbstractFixedEventLoopGroup(int nThreads,
                                          @Nonnull ThreadFactory threadFactory,
                                          @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                                          @Nullable EventLoopChooserFactory chooserFactory,
                                          @Nullable Object context) {
        if (nThreads <= 0) {
            throw new IllegalArgumentException("nThreads must greater than 0");
        }
        if (null == chooserFactory) {
            chooserFactory = new DefaultChooserFactory();
        }
        // 先保存子类的context，子类可能会使用。
        this.context = context;

        // 创建指定数目的child
        children = new EventLoop[nThreads];
        for (int i = 0; i < nThreads; i++) {
            // 创建newChild的时候有NPE的风险，不过为了制定模板，在这里创建是有必要的，只是子类实现newChild的时候要小心点
            children[i] = Objects.requireNonNull(newChild(i, threadFactory, rejectedExecutionHandler, context));
        }

        // 负载均衡算法
        this.chooser = chooserFactory.newChooser(children);

        // 监听子节点关闭的Listener，可以看做CountDownLatch.
        // 在所有的子节点上监听 它们的关闭事件，当所有的child关闭时，可以获得通知
        final FutureListener<Object> terminationListener = new ChildrenTerminateListener();
        for (EventLoop e : children) {
            e.terminationFuture().addListener(terminationListener);
        }

        // 将子节点数组封装为不可变集合，方便迭代(不允许外部改变持有的线程)
        List<EventLoop> modifiable = new ArrayList<>();
        Collections.addAll(modifiable, children);
        this.readonlyChildren = Collections.unmodifiableList(modifiable);
    }

    /**
     * 子类自己决定创建EventLoop的方式和具体的类型。
     *
     * @param childIndex               这是正在构建的第几个child， 索引0开始
     * @param threadFactory            线程工厂，用于创建线程
     * @param rejectedExecutionHandler 拒绝任务时的处理器
     * @param context                  构造方法中传入的上下文
     * @return EventLoop
     * @apiNote 注意：这里是超类构建的时候调用的，此时子类属性还未被赋值，因此newChild需要的数据必须在context中，使用子类的属性会导致NPE。
     */
    @Nonnull
    protected abstract EventLoop newChild(int childIndex, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler, Object context);

    protected Object getContext() {
        return context;
    }

    // -------------------------------------  子类生命周期管理 --------------------------------

    @Override
    public ListenableFuture<?> terminationFuture() {
        return terminationFuture;
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
        return terminationFuture.await(timeout, unit);
    }

    @Override
    public void shutdown() {
        forEach(EventLoop::shutdown);
    }

    @Nonnull
    @Override
    public List<Runnable> shutdownNow() {
        List<Runnable> tasks = new LinkedList<>();
        for (EventLoop eventLoop : children) {
            tasks.addAll(eventLoop.shutdownNow());
        }
        return tasks;
    }
    // ------------------------------------- 迭代 ----------------------------

    @Nonnull
    @Override
    public EventLoop next() {
        return chooser.next();
    }

    @Nonnull
    @Override
    public EventLoop select(int key) {
        return chooser.select(key);
    }

    @Override
    public int numChildren() {
        return children.length;
    }

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

    /**
     * 子节点终结状态监听器
     */
    private class ChildrenTerminateListener implements FutureListener<Object> {

        /**
         * 已关闭的子节点数量
         */
        private final AtomicInteger terminatedChildren = new AtomicInteger(0);

        private ChildrenTerminateListener() {

        }

        @Override
        public void onComplete(ListenableFuture<Object> future) throws Exception {
            if (terminatedChildren.incrementAndGet() == children.length) {
                try {
                    clean();
                } catch (Throwable e) {
                    logger.error("clean caught exception!", e);
                } finally {
                    terminationFuture.setSuccess(null);
                }
            }
        }
    }

    /**
     * 线程组退出前的清理，用于清理线程组拥有的全局资源。
     *
     * @apiNote 注意：该方法执行在{@link GlobalEventLoop}所在的线程。必须保证线程安全！
     */
    protected void clean() {

    }

}
