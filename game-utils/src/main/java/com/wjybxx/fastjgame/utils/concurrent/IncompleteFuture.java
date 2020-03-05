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
import com.wjybxx.fastjgame.utils.ThreadUtils;
import com.wjybxx.fastjgame.utils.TimeUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static com.wjybxx.fastjgame.utils.ThreadUtils.checkInterrupted;

/**
 * 未完成的future
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/5
 * github - https://github.com/hl845740757
 */
public abstract class IncompleteFuture<V> extends AbstractListenableFuture<V> {

    /**
     * 1毫秒多少纳秒
     */
    private static final int NANO_PER_MILLISECOND = (int) TimeUtils.NANO_PER_MILLISECOND;

    /**
     * 如果一个任务成功时没有结果，使用该对象代替。
     */
    private static final Object SUCCESS = new Object();
    /**
     * 表示future关联的任务进入不可取消状态。
     */
    private static final Object UNCANCELLABLE = new Object();

    /**
     * Future关联的任务的计算结果。
     * {@link AtomicReference}用于保证原子性和可见性。
     */
    private final AtomicReference<Object> resultHolder = new AtomicReference<>();

    /**
     * 在当前对象上阻塞等待的线程数，决定是否需要调用{@link #notifyAll()}。
     * 减少notifyAll调用。（因为鼓励使用回调，而不是阻塞方式的获取结果，因此NotifyAll的几率是很小的）
     */
    @GuardedBy("this")
    private int waiters = 0;

    /**
     * 工作线程。
     * 此外，它也是默认的通知线程，所有未指定{@link EventLoop}的监听器，都必须在该线程下执行。
     * 主要用于保证监听器的执行顺序，虽然无序执行监听器可以提高性能，但是违反直觉，太危险。
     */
    private final EventLoop workerExecutor;

    public IncompleteFuture(EventLoop workerExecutor) {
        this.workerExecutor = workerExecutor;
    }

    protected EventLoop getWorkerExecutor() {
        return workerExecutor;
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
    public final V get() throws InterruptedException, CompletionException {
        final Object result = resultHolder.get();
        if (isDone0(result)) {
            return reportGet(result);
        }

        await();

        return reportGet(resultHolder.get());
    }

    @Override
    public final V get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, CompletionException, TimeoutException {
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
    public final V join() throws CompletionException {
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
    public final V getNow() {
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

    @Nullable
    @Override
    public final Throwable cause() {
        return getCause0(resultHolder.get());
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
    public final boolean cancel(boolean mayInterruptIfRunning) {
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

    private void postComplete() {
        final boolean needNotifyListeners;

        synchronized (this) {
            notifyAllWaiters();

            needNotifyListeners = needNotifyListeners();
        }

        if (needNotifyListeners) {
            notifyListeners();
        }
    }

    /**
     * 通知所有的等待线程
     */
    private void notifyAllWaiters() {
        if (waiters > 0) {
            notifyAll();
        }
    }

    /**
     * 检查是否需要通知监听器。
     * 该方法运行在锁下，这里是顺带检查一下，可以减少锁的获取次数。
     * 这里最好不要直接通知，避免长时间的占用锁。
     *
     * @return 如果返回true，那么将会尝试调用{@link #notifyListeners()}
     */
    protected abstract boolean needNotifyListeners();

    /**
     * 通知所有的监听器。
     * 此时尚未获得锁，如果需要的话，可自行获取锁。
     */
    protected abstract void notifyListeners();

    // ----------------------------------------- 等待 ----------------------------------------------------

    /**
     * 检查死锁可能。
     */
    protected void checkDeadlock() {
        ConcurrentUtils.checkDeadLock(workerExecutor);
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
    public ListenableFuture<V> await() throws InterruptedException {
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
    public ListenableFuture<V> awaitUninterruptibly() {
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
    public final boolean await(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
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
    public final boolean awaitUninterruptibly(long timeout, @Nonnull TimeUnit unit) {
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

    /**
     * 异常holder，只有该类型表示失败。
     */
    private static class CauseHolder {

        private final Throwable cause;

        private CauseHolder(Throwable cause) {
            this.cause = cause;
        }
    }

    private static final class DefaultPromise2<V> implements Promise<V> {

        private final IncompleteFuture<V> future;

        public DefaultPromise2(IncompleteFuture<V> future) {
            this.future = future;
        }

        @Nonnull
        @Override
        public ListenableFuture<V> getFuture() {
            return future;
        }

        @Override
        public final void setSuccess(V result) {
            if (!trySuccess(result)) {
                throw new IllegalStateException("complete already, discard result " + result);
            }
        }

        @Override
        public final boolean trySuccess(V result) {
            // 当future关联的task成功，但是没有返回值时，使用SUCCESS代替
            return completeSuccessOrFailure(result == null ? IncompleteFuture.SUCCESS : result);
        }

        @Override
        public final void setFailure(@Nonnull Throwable cause) {
            if (!tryFailure(cause)) {
                throw new IllegalStateException("complete already, discard cause " + cause);
            }
        }

        @Override
        public final boolean tryFailure(@Nonnull Throwable cause) {
            return completeSuccessOrFailure(new IncompleteFuture.CauseHolder(cause));
        }

        @Override
        public boolean setUncancellable() {
            if (future.resultHolder.compareAndSet(null, IncompleteFuture.UNCANCELLABLE)) {
                return true;
            } else {
                final Object result = future.resultHolder.get();
                // 到这里result一定不为null，当前为不可取消状态 或 结束状态
                return result == IncompleteFuture.UNCANCELLABLE || !IncompleteFuture.isCancelled0(result);
            }
        }

        /**
         * 执行成功进入完成状态或执行失败进入完成状态
         *
         * @param value 要赋的值，一定不为null
         * @return 如果赋值成功，则返回true，否则返回false。
         */
        private boolean completeSuccessOrFailure(@Nonnull Object value) {
            // 正常完成可以由初始状态或不可取消状态进入完成状态
            if (future.resultHolder.compareAndSet(null, value)
                    || future.resultHolder.compareAndSet(IncompleteFuture.UNCANCELLABLE, value)) {
                future.postComplete();
                return true;
            } else {
                return false;
            }
        }
    }
}
