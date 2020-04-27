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

import com.wjybxx.fastjgame.utils.ThreadUtils;
import com.wjybxx.fastjgame.utils.TimeUtils;

import javax.annotation.Nonnull;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiConsumer;

import static com.wjybxx.fastjgame.utils.ThreadUtils.checkInterrupted;

/**
 * {@link Promise}的模板实现，它负责结果管理（状态迁移），和阻塞API的处理，不负责监听器管理。
 * <p>
 * Q: 为什么用锁实现等待通知？
 * A: 仅仅是为了简单。因为{@code Future}的本意就是不希望用户使用阻塞式API，因此我们不对阻塞API进行优化。
 *
 * <p>
 * 状态迁移：
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
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/6
 */
public abstract class AbstractPromise<V> implements Promise<V> {

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
     */
    private volatile Object resultHolder;

    /**
     * 是否需要调用{@link #notifyAll()} - 是否有线程阻塞在当前{@code Future}上，减少锁获取和notifyAll调用。
     */
    @SuppressWarnings("unused")
    private volatile int signalNeeded;

    public AbstractPromise() {

    }

    public AbstractPromise(V result) {
        resultHolder = result == null ? SUCCESS : result;
    }

    public AbstractPromise(Throwable cause) {
        resultHolder = new CauseHolder(cause);
    }

    /**
     * 成功完成或失败完成
     *
     * @return 如果赋值成功，则返回true，否则返回false。
     */
    private boolean tryComplete(@Nonnull Object value) {
        // 正常完成可以由初始状态或不可取消状态进入完成状态
        if (RESULT_HOLDER.compareAndSet(this, null, value)
                || RESULT_HOLDER.compareAndSet(this, UNCANCELLABLE, value)) {
            postComplete();
            return true;
        } else {
            return false;
        }
    }

    /**
     * 尝试标记为不可取消
     */
    private boolean trySetUncancellable() {
        return RESULT_HOLDER.compareAndSet(this, null, UNCANCELLABLE);
    }

    /**
     * 由取消进入完成状态
     *
     * @return 成功取消则返回true
     */
    private boolean tryCompleteCancellation() {
        // 取消只能由初始状态(null)切换为完成状态
        if (RESULT_HOLDER.compareAndSet(this, null, new CauseHolder(new CancellationException()))) {
            postComplete();
            return true;
        } else {
            return false;
        }
    }

    /**
     * 标记为需要通知
     */
    private void markSignalNeeded() {
        SIGNAL_NEEDED.setVolatile(this, 1);
    }

    /**
     * 清楚是否需要被通知状态，并返回是否需要被通知
     */
    private boolean clearSignalNeeded() {
        return (int) SIGNAL_NEEDED.getAndSet(this, 0) == 1;
    }

    // --------------------------------------------- 查询 --------------------------------------------------

    /**
     * 解释一下为什么多线程的代码喜欢将volatile变量存为临时变量或传递给某一个方法做判断。
     * 为了保证数据的一致性，当对volatile执行多个操作时，如果不把volatile变量保存下来，则每次读取的结果可能是不一样的。
     */
    private static boolean isDone0(Object result) {
        // 当result不为null，且不是不可取消占位符的时候表示已进入完成状态
        return result != null && result != UNCANCELLABLE;
    }

    private static boolean isCompletedExceptionally0(Object result) {
        return result instanceof CauseHolder;
    }

    private static boolean isCancelled0(Object result) {
        // null instanceOf 总是返回false
        return getCause0(result) instanceof CancellationException;
    }

    private static Throwable getCause0(Object result) {
        return (result instanceof CauseHolder) ? ((CauseHolder) result).cause : null;
    }

    private static boolean isCancellable0(Object result) {
        // 当且仅当result为null的时候可取消
        return result == null;
    }

    @Override
    public final boolean isDone() {
        return isDone0(resultHolder);
    }

    @Override
    public final boolean isCompletedExceptionally() {
        return isCompletedExceptionally0(resultHolder);
    }

    @Override
    public final boolean isCancelled() {
        return isCancelled0(resultHolder);
    }

    @Override
    public final boolean isCancellable() {
        return isCancellable0(resultHolder);
    }

    // ---------------------------------------------- 非阻塞式获取结果 -----------------------------------------------------

    @Override
    public final V getNow() {
        final Object result = resultHolder;
        if (isDone0(result)) {
            return reportJoin(result);
        }
        return null;
    }

    /**
     * 不命名为{@code reportGetNow}是为了放大不同之处。
     */
    @SuppressWarnings("unchecked")
    private static <T> T reportJoin(final Object r) {
        if (r == SUCCESS) {
            return null;
        }

        if (r instanceof CauseHolder) {
            return rethrowJoin(((CauseHolder) r).cause);
        }

        return (T) r;
    }

    private static <T> T rethrowJoin(@Nonnull Throwable cause) throws CancellationException, CompletionException {
        if (cause instanceof CancellationException) {
            throw (CancellationException) cause;
        }
        throw new CompletionException(cause);
    }

    @Override
    public final Throwable cause() {
        return getCause0(resultHolder);
    }

    // ------------------------------------------------- 状态迁移 --------------------------------------------
    @Override
    public final boolean setUncancellable() {
        if (trySetUncancellable()) {
            return true;
        } else {
            // 到这里result一定不为null，当前为不可取消状态 或 结束状态
            final Object result = resultHolder;
            return result == UNCANCELLABLE || !isCancelled0(result);
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        final Object result = resultHolder;
        if (isCancelled0(result)) {
            return true;
        }
        return isCancellable0(result) && tryCompleteCancellation();
    }

    /**
     * 安全的推送future进入完成状态事件，由于推送由子类实现，无法保证所有实现都足够安全，因此需要捕获异常。
     */
    private void postComplete() {
        notifyWaiters();
        notifyListeners();
    }

    private void notifyWaiters() {
        if (clearSignalNeeded()) {
            synchronized (this) {
                notifyAll();
            }
        }
    }

    protected abstract void notifyListeners();

    @Override
    public final void setSuccess(V result) {
        if (!trySuccess(result)) {
            throw new IllegalStateException("complete already, discard result " + result);
        }
    }

    @Override
    public final boolean trySuccess(V result) {
        // 当future关联的task成功，但是没有返回值时，使用SUCCESS代替
        return tryComplete(result == null ? SUCCESS : result);
    }

    @Override
    public final void setFailure(@Nonnull Throwable cause) {
        if (!tryFailure(cause)) {
            throw new IllegalStateException("complete already, discard cause " + cause);
        }
    }

    @Override
    public final boolean tryFailure(@Nonnull Throwable cause) {
        return tryComplete(new CauseHolder(cause));
    }

    /**
     * 异常holder，只有该类型表示失败。
     */
    static class CauseHolder {

        final Throwable cause;

        private CauseHolder(Throwable cause) {
            this.cause = cause;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public final void acceptNow(@Nonnull BiConsumer<? super V, ? super Throwable> action) {
        final Object result = resultHolder;
        if (!isDone0(result)) {
            return;
        }

        if (result == SUCCESS) {
            action.accept(null, null);
            return;
        }

        if (result instanceof CauseHolder) {
            action.accept(null, ((CauseHolder) result).cause);
        } else {
            @SuppressWarnings("unchecked") final V value = (V) result;
            action.accept(value, null);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // --------------------------------------------- 阻塞式获取结果 -----------------------------------

    @Override
    public final V get() throws InterruptedException, ExecutionException {
        final Object result = resultHolder;
        if (isDone0(result)) {
            return reportGet(result);
        }

        await();

        return reportGet(resultHolder);
    }

    /**
     * {@link FluentFuture#get()} {@link FluentFuture#get(long, TimeUnit)}
     */
    @SuppressWarnings("unchecked")
    private static <T> T reportGet(Object r) throws ExecutionException {
        if (r == SUCCESS) {
            return null;
        }

        if (r instanceof CauseHolder) {
            return rethrowGet(((CauseHolder) r).cause);
        }

        return (T) r;
    }

    private static <T> T rethrowGet(Throwable cause) throws CancellationException, ExecutionException {
        if (cause instanceof CancellationException) {
            throw (CancellationException) cause;
        }
        throw new ExecutionException(cause);
    }

    @Override
    public final V get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        final Object result = resultHolder;
        if (isDone0(result)) {
            return reportGet(result);
        }

        if (await(timeout, unit)) {
            return reportGet(resultHolder);
        }

        throw new TimeoutException();
    }

    @Override
    public final V join() throws CompletionException {
        final Object result = resultHolder;
        if (isDone0(result)) {
            return reportJoin(result);
        }

        awaitUninterruptibly();

        return reportJoin(resultHolder);
    }

    @Override
    public final Promise<V> await() throws InterruptedException {
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
                markSignalNeeded();
                this.wait();
            }
        }
        return this;
    }

    /**
     * 检查死锁可能。
     */
    private void checkDeadlock() {
        // 实现流式语法后，无法很好的检查死锁问题
    }

    @Override
    public final Promise<V> awaitUninterruptibly() {
        // 先检查一次是否已完成，减小锁竞争，同时在完成的情况下，等待不会死锁。
        if (isDone()) {
            return this;
        }
        // 检查死锁可能
        checkDeadlock();

        boolean interrupted = false;
        boolean marked = false;

        try {
            synchronized (this) {
                while (!isDone()) {
                    if (!marked) {
                        markSignalNeeded();
                        marked = true;
                    }

                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        interrupted = true;
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

        boolean marked = false;

        final long endTime = System.nanoTime() + unit.toNanos(timeout);
        synchronized (this) {
            while (!isDone()) {
                // 获取锁需要时间，因此应该在获取锁之后计算剩余时间
                final long remainNano = endTime - System.nanoTime();
                if (remainNano <= 0) {
                    return false;
                }

                if (!marked) {
                    markSignalNeeded();
                    marked = true;
                }

                this.wait(remainNano / NANO_PER_MILLISECOND, (int) (remainNano % NANO_PER_MILLISECOND));
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
        boolean marked = false;

        final long endTime = System.nanoTime() + unit.toNanos(timeout);
        try {
            synchronized (this) {
                while (!isDone()) {
                    // 获取锁需要时间，因此应该在获取锁之后计算剩余时间
                    final long remainNano = endTime - System.nanoTime();
                    if (remainNano <= 0) {
                        return false;
                    }

                    if (!marked) {
                        markSignalNeeded();
                        marked = true;
                    }

                    try {
                        this.wait(remainNano / NANO_PER_MILLISECOND, (int) (remainNano % NANO_PER_MILLISECOND));
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
                return true;

            }
        } finally {
            // 恢复中断状态
            ThreadUtils.recoveryInterrupted(interrupted);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final VarHandle SIGNAL_NEEDED;
    private static final VarHandle RESULT_HOLDER;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            RESULT_HOLDER = l.findVarHandle(AbstractPromise.class, "resultHolder", Object.class);
            SIGNAL_NEEDED = l.findVarHandle(AbstractPromise.class, "signalNeeded", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }

        // 遵循JDK的处理
        // Reduce the risk of rare disastrous classloading in first call to
        // LockSupport.park: https://bugs.openjdk.java.net/browse/JDK-8074773
        Class<?> ensureLoaded = LockSupport.class;
    }
}
