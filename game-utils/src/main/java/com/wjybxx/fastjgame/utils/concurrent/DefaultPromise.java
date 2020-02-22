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

import com.wjybxx.fastjgame.utils.CollectionUtils;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.ThreadUtils;
import com.wjybxx.fastjgame.utils.TimeUtils;
import com.wjybxx.fastjgame.utils.annotation.UnstableApi;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import static com.wjybxx.fastjgame.utils.ThreadUtils.checkInterrupted;

/**
 * 默认的{@link Promise}实现。
 * 去掉了netty中的很多我们不需要使用的东西，以及去除部分优化(降低复杂度)。
 * <p>
 * 任务状态迁移：
 * <pre>
 *       (setUncancellabl) (result == UNCANCELLABLE)     (异常/成功)
 *                   --------> 不可取消状态 ------------------------|
 *                   |         (未完成)                            |
 *  初始状态 ---------|                                            | ----> 完成状态(isDown() == true)
 * (result == null)  |                                            |
 *  (未完成)          |--------------------------------------------|
 *                                 (取消/异常/成功)
 *                 (cancel, tryFailure,setFailure,trySuccess,setSuccess)
 * </pre>
 * <p>
 * 建议使用 {@link EventLoop#newPromise()}代替构造方法。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14
 * github - https://github.com/hl845740757
 */
public class DefaultPromise<V> extends AbstractListenableFuture<V> implements Promise<V> {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPromise.class);

    /**
     * 1毫秒多少纳秒
     */
    private static final int NANO_PER_MILLISECOND = (int) TimeUtils.NANO_PER_MILLISECOND;

    /**
     * 如果一个任务成功时没有结果{@link #setSuccess(Object) null}，使用该对象代替。
     */
    private static final Object SUCCESS = new Object();
    /**
     * 表示future关联的任务进入不可取消状态。
     */
    private static final Object UNCANCELLABLE = new Object();

    /**
     * Future关联的任务的计算结果。
     * null表示还未结束。
     * {@link AtomicReference}用于保证原子性和可见性。
     */
    private final AtomicReference<Object> resultHolder = new AtomicReference<>();

    /**
     * 默认的通知用的executor。
     *
     * @apiNote 1. 如果在创建promise时不能确定，那么需要重写{@link #defaultExecutor()}，以返回默认的executor。
     * 2. 默认情况下：认为该executor就是执行任务的executor，因此检查死锁时也是检查该executor；
     * 如果该executor不是任务的执行环境，你还需要重写{@link #checkDeadlock()}确保检查死锁的正确性。
     */
    private final EventLoop defaultExecutor;

    /**
     * 该future上注册的监听器们。
     * 我们使用Null表示一个监听器也没有的状态，因此在删除监听器时，如果size==0，我们会置为null。
     */
    @GuardedBy("this")
    private List<ListenerEntry<? super V>> listeners = null;
    /**
     * 当前是否有线程正在通知监听器们。我们必须阻止并发的通知 和 保持监听器的先入先出顺序(先添加的先被通知)。
     */
    @GuardedBy("this")
    private boolean notifyingListeners = false;

    /**
     * 在当前对象上阻塞等待的线程数，决定是否需要调用{@link #notifyAll()}。
     * 减少notifyAll调用。（因为鼓励使用回调，而不是阻塞方式的获取结果，因此NotifyAll的几率是很小的）
     */
    @GuardedBy("this")
    private int waiters = 0;

    /**
     * @param defaultExecutor 默认的事件通知线程
     */
    public DefaultPromise(@Nonnull EventLoop defaultExecutor) {
        this.defaultExecutor = defaultExecutor;
    }

    /**
     * 供子类使用的构造方法。如果使用该构造方法，必须重写{@link #defaultExecutor()}方法
     */
    protected DefaultPromise() {
        this.defaultExecutor = null;
    }

    /**
     * 返回最新的用于通知的EventLoop
     *
     * @return nonnull
     */
    @Nonnull
    protected EventLoop defaultExecutor() {
        // noinspection ConstantConditions (屏蔽null警告)
        return defaultExecutor;
    }

    // --------------------------------------------  查询 ----------------------------------------------

    /**
     * 判断result是否表示已完成状态
     */
    private static boolean isDone0(Object result) {
        // 当result不为null，且不是不可取消占位符的时候表示已进入完成状态
        return result != null && result != UNCANCELLABLE;
    }

    /**
     * 判断结果是否表示执行成功（已进入完成状态，且是正常完成的）。
     * <p>
     * 解释一下为什么多线程的代码喜欢将volatile变量存为临时变量或传递给某一个方法做判断。
     * 原因：保证数据的前后一致性。
     * 当对volatile涉及多个操作时，如果不把volatile变量保存下来，每次读取的结果可能是不一样的！！！
     */
    private static boolean isSuccess0(Object result) {
        return isDone0(result) && !(result instanceof CauseHolder);
    }

    /**
     * 获取表示失败的原因。
     *
     * @return 如果是失败完成状态，则返回对应的异常，否则返回null。
     */
    private static Throwable getCause0(Object result) {
        return (result instanceof CauseHolder) ? ((CauseHolder) result).cause : null;
    }

    /**
     * 判断result是否表示已取消
     */
    private static boolean isCancelled0(Object result) {
        // null instanceOf 总是返回false
        return getCause0(result) instanceof CancellationException;
    }

    /**
     * 判断result是否表示可取消状态
     */
    private static boolean isCancellable0(Object result) {
        // 当且仅当result为null的时候可取消
        return result == null;
    }

    // --------------------------------------------- 查询2 --------------------------------------------------

    @Override
    public final boolean isDone() {
        return isDone0(resultHolder.get());
    }

    @Override
    public final boolean isSuccess() {
        return isSuccess0(resultHolder.get());
    }

    @Override
    public final boolean isCancelled() {
        return isCancelled0(resultHolder.get());
    }

    @Override
    public final boolean isCancellable() {
        return isCancellable0(resultHolder.get());
    }

    @Override
    public V get() throws InterruptedException, CompletionException {
        final Object result = resultHolder.get();
        if (isDone0(result)) {
            return reportGet(result);
        }

        await();

        return reportGet(resultHolder.get());
    }

    @Override
    public V get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, CompletionException, TimeoutException {
        final Object result = resultHolder.get();
        if (isDone0(result)) {
            return reportGet(result);
        }

        if (await(timeout, unit)) {
            return reportGet(resultHolder.get());
        }

        throw new TimeoutException();
    }

    @Override
    public V join() throws CompletionException {
        final Object result = resultHolder.get();
        if (isDone0(result)) {
            return reportGet(result);
        }

        awaitUninterruptibly();

        return reportGet(resultHolder.get());
    }

    /**
     * 用于get方法上报结果
     */
    @SuppressWarnings("unchecked")
    private static <T> T reportGet(final Object r) throws CompletionException {
        if (r == SUCCESS) {
            return null;
        }

        if (r instanceof CauseHolder) {
            return rethrowCause(((CauseHolder) r).cause);
        }

        return (T) r;
    }

    @Nullable
    @Override
    public V getNow() {
        final Object result = resultHolder.get();
        if (isSuccess0(result)) {
            return getSuccessResult(result);
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <V> V getSuccessResult(Object result) {
        return result == SUCCESS ? null : (V) result;
    }

    /**
     * 重写时调用{@link #getAsResultImp(BiFunction)}
     */
    @UnstableApi
    @Nullable
    @Override
    public FutureResult<V> getAsResult() {
        return getAsResultImp(DefaultFutureResult::new);
    }

    /**
     * 获取结果的真正实现
     *
     * @param factory 创建结果的工厂，可以考虑单例
     */
    protected final <FR extends FutureResult<V>> FR getAsResultImp(BiFunction<V, Throwable, FR> factory) {
        final Object result = resultHolder.get();

        if (!isDone0(result)) {
            return null;
        }

        if (result instanceof CauseHolder) {
            return factory.apply(null, ((CauseHolder) result).cause);
        }

        return factory.apply(getSuccessResult(result), null);
    }

    @Nullable
    @Override
    public Throwable cause() {
        return getCause0(resultHolder.get());
    }

    // ----------------------------------------------- 更新结果 ------------------------------------------

    @Override
    public void setSuccess(V result) {
        // 如果调用trySuccess，会导致子类覆盖 setSuccess和trySuccess出现问题。
        if (!setSuccess0(result)) {
            throw new IllegalStateException("complete already, discard result " + result);
        }
    }

    @Override
    public boolean trySuccess(V result) {
        return setSuccess0(result);
    }

    @Override
    public void setFailure(@Nonnull Throwable cause) {
        // 如果调用tryFailure，会导致子类覆盖 setFailure 和 tryFailure 出现问题。
        if (!setFailure0(cause)) {
            throw new IllegalStateException("complete already, discard cause " + cause);
        }
    }

    @Override
    public boolean tryFailure(@Nonnull Throwable cause) {
        return setFailure0(cause);
    }

    /**
     * Q:为什么要这么写？
     * A:为了支持子类重写 {@link #setSuccess(Object)} {@link #setFailure(Throwable)}
     * {@link #trySuccess(Object)} {@link #tryFailure(Throwable)}
     */
    private boolean setSuccess0(V result) {
        // 当future关联的task成功，但是没有返回值时，使用SUCCESS代替
        return completeSuccessOrFailure(result == null ? SUCCESS : result);
    }

    private boolean setFailure0(@Nonnull Throwable cause) {
        return completeSuccessOrFailure(new CauseHolder(cause));
    }

    /**
     * 执行成功进入完成状态或执行失败进入完成状态
     *
     * @param value 要赋的值，一定不为null
     * @return 如果赋值成功，则返回true，否则返回false。
     */
    private boolean completeSuccessOrFailure(@Nonnull Object value) {
        // 正常完成可以由初始状态或不可取消状态进入完成状态
        if (resultHolder.compareAndSet(null, value)
                || resultHolder.compareAndSet(UNCANCELLABLE, value)) {
            postComplete();
            return true;
        } else {
            return false;
        }
    }

    /**
     * 提交完成事件
     */
    private void postComplete() {
        if (notifyWaitersAndCheckListeners()) {
            // 有需要广播的监听器，尝试进行广播
            notifyListeners();
        }
    }

    /**
     * 通知在该对象上等待的线程，并检查是否存在监听对象
     * <p>
     * Q:为什么可以先检查listener是否为null，再通知监听器？
     * A:代码执行到这里的时候，结果(volatile)已经对其它线程可见。如果这里认为没有监听器，那么新加入的监听器一定会在添加完之后进行通知。
     * <p>
     * Q: 为什么不在checkNotifyWaiters里面进行通知呢？
     * A: 那会使得{@link #notifyListeners()}整个方法都处于同步代码块中！
     *
     * @return 如果返回true，表示有在该对象上的监听器，那么需要进行通知
     */
    private synchronized boolean notifyWaitersAndCheckListeners() {
        if (waiters > 0) {
            notifyAll();
        }
        return listeners != null;
    }

    /**
     * 通知所有的监听器
     */
    private void notifyListeners() {
        // 用于拉取最新的监听器，避免长时间的占有锁
        List<ListenerEntry<? super V>> listeners;
        synchronized (this) {
            // 有线程正在进行通知 或当前 没有监听器，则不需要当前线程进行通知
            if (notifyingListeners || null == this.listeners) {
                return;
            }
            // 标记为正在通知(每一个正在通知的线程都会将所有的监听器通知一遍)
            notifyingListeners = true;
            listeners = this.listeners;
            this.listeners = null;
        }

        for (; ; ) {
            // 通知当前批次的监听器(此时不需要获得锁) -- 但是这里不能抛出异常，否则可能死锁 -- notifyingListeners无法恢复
            for (ListenerEntry<? super V> listenerEntry : listeners) {
                notifyListener(listenerEntry.listener, listenerEntry.bindExecutor);
            }
            // 通知完当前批次后，检查是否有新的监听器加入
            synchronized (this) {
                if (null == this.listeners) {
                    // 通知完毕
                    this.notifyingListeners = false;
                    break;
                }
                // 有新的监听器加入，拉取最新的监听器，继续通知 -- 可以保证被通知的顺序
                listeners = this.listeners;
                this.listeners = null;
            }
        }
    }

    /**
     * 通知单个监听器，future上的任务已完成。
     */
    private void notifyListener(FutureListener<? super V> listener, @Nullable EventLoop bindExecutor) {
        // 如果注册监听器时没有绑定执行环境(执行线程)，则使用当前最新的executor
        EventLoop executor = null == bindExecutor ? defaultExecutor() : bindExecutor;
        notifyListenerSafely(this, listener, executor);
    }

    /**
     * 安全的通知一个监听器，不可以抛出异常，否则可能死锁
     *
     * @param future   future
     * @param listener 监听器
     * @param executor 监听器的执行环境
     */
    protected static <V> void notifyListenerSafely(@Nonnull ListenableFuture<V> future, @Nonnull FutureListener<? super V> listener, @Nonnull EventLoop executor) {
        try {
            if (executor.inEventLoop()) {
                listener.onComplete(future);
            } else {
                executor.execute(() -> {
                    try {
                        listener.onComplete(future);
                    } catch (Exception e) {
                        ExceptionUtils.rethrow(e);
                    }
                });
            }
        } catch (Throwable e) {
            if (e instanceof RejectedExecutionException) {
                logger.info("Target EventLoop my shutdown. Notify task is rejected.");
            } else {
                logger.warn("An exception was thrown by " + listener.getClass().getName() + ".onComplete()", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param mayInterruptIfRunning 该参数对该promise而言是无效的。
     *                              对该promise而言，在它定义的抽象中，它只是获取结果的凭据，它并不真正执行任务，因此也不能真正取消任务。
     *                              因此该参数对该promise而言是无效的。
     *                              这里的取消并不是真正意义上的取消，它不能取消过程，只能取消结果。
     * @return true if succeed
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        final Object result = resultHolder.get();
        if (isCancelled0(result)) {
            return true;
        }
        return isCancellable0(result) && completeCancellation();
    }

    /**
     * 由取消进入完成状态
     *
     * @return 成功取消则返回true
     */
    private boolean completeCancellation() {
        // 取消只能由初始状态(null)切换为完成状态
        if (resultHolder.compareAndSet(null, new CauseHolder(new CancellationException()))) {
            postComplete();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean setUncancellable() {
        if (resultHolder.compareAndSet(null, UNCANCELLABLE)) {
            return true;
        } else {
            final Object result = resultHolder.get();
            // 到这里result一定不为null，当前为不可取消状态 或 结束状态
            return result == UNCANCELLABLE || !isCancelled0(result);
        }
    }

    // ----------------------------------------- 等待 ----------------------------------------------------

    /**
     * 检查死锁可能。
     */
    protected void checkDeadlock() {
        ConcurrentUtils.checkDeadLock(defaultExecutor());
    }

    /**
     * 等待线程数+1
     */
    private void incWaiters() {
        waiters++;
    }

    /**
     * 等待线程数-1
     */
    private void decWaiters() {
        waiters++;
    }

    /**
     * <pre>
     * {@code
     *         // synchronized的标准模式
     *         synchronized (this) {
     *             while(!condition()) {
     *                 this.wait();
     *             }
     *             doSomething();
     *         }
     *
     *         // 显式锁的标准模式
     *         lock.lock();
     *         try {
     *             while (!isOK()) {
     *                 condition.await();
     *             }
     *             doSomething();
     *         } finally {
     *             lock.unlock();
     *         }
     * }
     * </pre>
     */
    @Override
    public Promise<V> await() throws InterruptedException {
        // 先检查一次是否已完成，减小锁竞争，同时在完成的情况下，等待不会死锁。
        if (isDone()) {
            return this;
        }
        // 检查死锁可能
        checkDeadlock();

        // 检查中断 --- 在执行一个耗时操作之前检查中断是有必要的
        checkInterrupted();

        synchronized (this) {
            while (!isDone()) {
                incWaiters();
                try {
                    this.wait();
                } finally {
                    decWaiters();
                }
            }
        }
        return this;
    }

    @Override
    public Promise<V> awaitUninterruptibly() {
        // 先检查一次是否已完成，减小锁竞争，同时在完成的情况下，等待不会死锁。
        if (isDone()) {
            return this;
        }
        // 检查死锁可能
        checkDeadlock();

        boolean interrupted = false;
        try {
            synchronized (this) {
                while (!isDone()) {
                    incWaiters();
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        interrupted = true;
                    } finally {
                        decWaiters();
                    }
                }
            }
        } finally {
            // 恢复中断
            ThreadUtils.recoveryInterrupted(interrupted);
        }
        return this;
    }

    @Override
    public boolean await(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        // 小于等于0，则不阻塞
        if (timeout <= 0) {
            return isDone();
        }
        // 先检查一次是否已完成，减小锁竞争，同时在完成的情况下，等待不会死锁。
        if (isDone()) {
            return true;
        }
        // 等待之前检查死锁可能
        checkDeadlock();

        // 即将等待之前检查中断标记（在耗时操作开始前检查中断是有必要的 -- 要养成习惯）
        checkInterrupted();
        final long endTime = System.nanoTime() + unit.toNanos(timeout);
        synchronized (this) {
            while (!isDone()) {
                // 获取锁需要时间，因此应该在获取锁之后计算剩余时间
                final long remainNano = endTime - System.nanoTime();
                if (remainNano <= 0) {
                    return false;
                }
                incWaiters();
                try {
                    this.wait(remainNano / NANO_PER_MILLISECOND, (int) (remainNano % NANO_PER_MILLISECOND));
                } finally {
                    decWaiters();
                }
            }
            return true;
        }
    }

    @Override
    public boolean awaitUninterruptibly(long timeout, @Nonnull TimeUnit unit) {
        // 小于等于0，则不阻塞
        if (timeout <= 0) {
            return isDone();
        }
        // 先检查一次是否已完成，减小锁竞争，同时在完成的情况下，等待不会死锁。
        if (isDone()) {
            return true;
        }
        // 检查死锁可能
        checkDeadlock();

        boolean interrupted = false;
        final long endTime = System.nanoTime() + unit.toNanos(timeout);
        try {
            synchronized (this) {
                while (!isDone()) {
                    // 获取锁需要时间，因此应该在获取锁之后计算剩余时间
                    final long remainNano = endTime - System.nanoTime();
                    if (remainNano <= 0) {
                        return false;
                    }
                    incWaiters();
                    try {
                        this.wait(remainNano / NANO_PER_MILLISECOND, (int) (remainNano % NANO_PER_MILLISECOND));
                    } catch (InterruptedException e) {
                        interrupted = true;
                    } finally {
                        decWaiters();
                    }
                }
                return true;
            }
        } finally {
            // 恢复中断状态
            ThreadUtils.recoveryInterrupted(interrupted);
        }
    }

    // ------------------------------------------------- 监听器 ------------------------------------------

    @Override
    public Promise<V> addListener(@Nonnull FutureListener<? super V> listener) {
        // null is safe
        addListener0(listener, null);
        return this;
    }

    @Override
    public Promise<V> addListener(@Nonnull FutureListener<? super V> listener, @Nonnull EventLoop bindExecutor) {
        addListener0(listener, bindExecutor);
        return this;
    }

    /**
     * 真正的添加监听器，可以不指定executor
     *
     * @param listener     监听器
     * @param bindExecutor 监听器绑定的executor，可能为null
     */
    protected final void addListener0(@Nonnull FutureListener<? super V> listener, @Nullable EventLoop bindExecutor) {
        // 不管是否已完成，先加入等待通知集合
        synchronized (this) {
            if (listeners == null) {
                listeners = new LinkedList<>();
            }
            listeners.add(new ListenerEntry<>(listener, bindExecutor));
        }

        // 必须检查完成状态，如果已进入完成状态，通知刚刚加入监听器们（否则可能丢失通知）（早已完成的状态下）
        // 因为状态改变 和 锁没有关系
        if (isDone()) {
            notifyListeners();
        }
    }

    @Override
    public Promise<V> removeListener(@Nonnull FutureListener<? super V> listener) {
        removeFirstMatchListener(listenerEntry -> listenerEntry.listener == listener);
        return this;
    }

    /**
     * 移除第一个匹配的监听器
     *
     * @param predicate 监听器测试条件
     * @return 是否成功删除了一个监听器
     */
    protected final boolean removeFirstMatchListener(Predicate<ListenerEntry<?>> predicate) {
        synchronized (this) {
            if (listeners == null) {
                return false;
            }

            boolean success = CollectionUtils.removeFirstMatch(listeners, predicate);

            if (listeners.size() == 0) {
                listeners = null;
            }
            return success;
        }
    }

    /**
     * 异常holder，只有该类型表示失败。
     * 否则无法区分{@link #setSuccess(Object) exception}。（拿异常当结果就无法区分了）
     */
    private static class CauseHolder {

        private final Throwable cause;

        private CauseHolder(Throwable cause) {
            this.cause = cause;
        }
    }

    /**
     * 单个监听器的执行信息.
     */
    private static class ListenerEntry<V> {

        private final FutureListener<? super V> listener;
        private final EventLoop bindExecutor;

        private ListenerEntry(@Nonnull FutureListener<? super V> listener, @Nullable EventLoop bindExecutor) {
            this.listener = listener;
            this.bindExecutor = bindExecutor;
        }
    }
}
