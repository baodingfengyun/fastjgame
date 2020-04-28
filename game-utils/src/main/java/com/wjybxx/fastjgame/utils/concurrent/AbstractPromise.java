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

import com.wjybxx.fastjgame.utils.TimeUtils;

import javax.annotation.Nonnull;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

import static com.wjybxx.fastjgame.utils.ThreadUtils.checkInterrupted;
import static com.wjybxx.fastjgame.utils.ThreadUtils.recoveryInterrupted;

/**
 * {@link Promise}的模板实现，它负责核心逻辑，{@link Completion}的扩展交给子类处理，避免类太大，分散实现。
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
    private static final Object NIL = new Object();
    /**
     * 表示future关联的任务进入不可取消状态。
     */
    private static final Object UNCANCELLABLE = new Object();

    /**
     * Future关联的任务的计算结果，它同时也存储者{@code Future}的状态信息。
     * <ul>
     * <li>{@code null}表示初始状态</li>
     * <li>{@link #UNCANCELLABLE} 表示不可取消状态，表示任务正在进行，不支持取消。</li>
     * <li>{@link #NIL}表示终止状态，表示正常完成，但是计算结果为null</li>
     * <li>{@link AltResult}表示终止状态，表示计算中出现异常，{@link AltResult#cause}为计算失败的原因。</li>
     * <li>其它任何非null值，表示终止状态，表示正常完成，且计算结果非null。</li>
     * </ul>
     */
    private volatile Object resultHolder;

    /**
     * 当前对象上的所有监听器，使用栈方式存储
     */
    private volatile Completion stack = null;

    /**
     * 是否需要调用{@link #notifyAll()} - 是否有线程阻塞在当前{@code Future}上，减少锁获取和notifyAll调用。
     */
    @SuppressWarnings("unused")
    private volatile int signalNeeded;

    public AbstractPromise() {

    }

    public AbstractPromise(V result) {
        resultHolder = result == null ? NIL : result;
    }

    public AbstractPromise(Throwable cause) {
        resultHolder = new AltResult(cause);
    }

    /**
     * 异常结果包装对象，只有该类型表示失败。
     */
    static class AltResult {

        final Throwable cause;

        AltResult(Throwable cause) {
            this.cause = cause;
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 成功完成或失败完成
     *
     * @return 如果赋值成功，则返回true，否则返回false。
     */
    private boolean tryComplete(@Nonnull Object value) {
        // 正常完成可以由初始状态或不可取消状态进入完成状态
        if (RESULT_HOLDER.compareAndSet(this, null, value)
                || RESULT_HOLDER.compareAndSet(this, UNCANCELLABLE, value)) {
            postComplete(this);
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
        if (RESULT_HOLDER.compareAndSet(this, null, new AltResult(new CancellationException()))) {
            postComplete(this);
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
     * 清除是否需要被通知状态，并返回是否需要被通知
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
        return result instanceof AbstractPromise.AltResult;
    }

    private static boolean isCancelled0(Object result) {
        // null instanceOf 总是返回false
        return getCause0(result) instanceof CancellationException;
    }

    private static Throwable getCause0(Object result) {
        return (result instanceof AbstractPromise.AltResult) ? ((AltResult) result).cause : null;
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
        if (r == NIL) {
            return null;
        }

        if (r instanceof AbstractPromise.AltResult) {
            return rethrowJoin(((AltResult) r).cause);
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

    @Override
    public final void setSuccess(V result) {
        if (!trySuccess(result)) {
            throw new IllegalStateException("complete already, discard result " + result);
        }
    }

    @Override
    public final boolean trySuccess(V result) {
        // 当future关联的task成功，但是没有返回值时，使用SUCCESS代替
        return tryComplete(result == null ? NIL : result);
    }

    @Override
    public final void setFailure(@Nonnull Throwable cause) {
        if (!tryFailure(cause)) {
            throw new IllegalStateException("complete already, discard cause " + cause);
        }
    }

    @Override
    public final boolean tryFailure(@Nonnull Throwable cause) {
        return tryComplete(new AltResult(cause));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public final boolean acceptNow(@Nonnull BiConsumer<? super V, ? super Throwable> action) {
        final Object result = resultHolder;
        if (!isDone0(result)) {
            return false;
        }

        if (result == NIL) {
            action.accept(null, null);
            return true;
        }

        if (result instanceof AbstractPromise.AltResult) {
            action.accept(null, ((AltResult) result).cause);
        } else {
            @SuppressWarnings("unchecked") final V value = (V) result;
            action.accept(value, null);
        }
        return true;
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

    @SuppressWarnings("unchecked")
    private static <T> T reportGet(Object r) throws ExecutionException {
        if (r == NIL) {
            return null;
        }

        if (r instanceof AbstractPromise.AltResult) {
            return rethrowGet(((AltResult) r).cause);
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

        // 这里总是假设 signalNeeded 的值为 false - 其实也可以读取 signalNeeded 的真实值。
        boolean marked = false;

        synchronized (this) {
            while (!isDone()) {

                if (!marked) {
                    markSignalNeeded();
                    marked = true;
                }

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
            recoveryInterrupted(interrupted);
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
            recoveryInterrupted(interrupted);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    static abstract class Completion {

        static final Completion TOMBSTONE = new Completion() {
            @Override
            AbstractPromise<Object> tryFire() {
                return null;
            }
        };

        /**
         * 下一个节点 - 非Volatile，通过CAS-{@link AbstractPromise#stack}来保证可见性
         */
        Completion next;

        /**
         * 尝试执行回调逻辑
         *
         * @return 如果tryFire使得另一个Future进入完成状态，返回该Future(在这之前不要推送进入完成事件)
         */
        abstract AbstractPromise<?> tryFire();

    }

    final void pushCompletion(Completion newHead) {
        // 如果future已完成，则立即执行
        if (isDone()) {
            newHead.tryFire();
            return;
        }

        Completion head;
        while ((head = stack) != Completion.TOMBSTONE) {
            // 由下面的CAS保证可见性
            newHead.next = head;

            if (STACK.compareAndSet(this, head, newHead)) {
                // 如果成功添加到头部，证明Future尚未完成，或已完成但还未开始通知，新添加的completion会被其它线程通知
                return;
            }
        }

        // 到这里的时候 head == TOMBSTONE，表示目标Future已进入完成状态，且正在被通知或已经通知完毕。
        // 由于Future已进入完成状态，且我们的Completion压入栈失败，因此新的completion需要当前线程来通知
        newHead.tryFire();
    }

    /**
     * 推送future的完成事件。
     * - 声明为静态会更清晰易懂
     */
    private static void postComplete(AbstractPromise<?> future) {
        Completion next = null;
        outer:
        while (true) {
            // 在通知监听器之前，先唤醒阻塞的线程
            future.releaseWaiters();

            // 将当前future上的监听器添加到next前面
            next = future.clearListeners(next);

            while (next != null) {
                Completion curr = next;
                next = next.next;

                // Completion的tryFire实现不可以抛出异常，否则会导致其它监听器也丢失信号
                future = curr.tryFire();

                if (future != null) {
                    // 如果某个Completion使另一个Future进入完成状态，则更新为新的Future，并重试整个流程
                    continue outer;
                }
            }
            break;
        }
    }

    /**
     * 释放等待中的线程
     */
    private void releaseWaiters() {
        if (clearSignalNeeded()) {
            synchronized (this) {
                notifyAll();
            }
        }
    }

    /**
     * 清空当前{@code Future}上的监听器，并将当前{@code Future}上的监听器逆序方式插入到{@code onto}前面。
     * 这里的实现参考了Guava中{@code AbstractFuture}的实现，相对于{@link CompletableFuture}的实现更简单。
     * <p>
     * Q: 这步操作是要干什么？<br>
     * A: 由于一个{@link Completion}在执行时可能使另一个{@code Future}进入完成状态，如果不做处理的话，则可能产生一个很深的递归，
     * 从而造成堆栈移除，也影响性能。该操作就是将可能通知的监听器由树结构展开为链表结构，消除深嵌套的递归。
     * <pre>
     *      Future1(stack) -> Completion1_1 ->  Completion1_2 -> Completion1_3
     *                              ↓
     *                          Future2(stack) -> Completion2_1 ->  Completion2_2 -> Completion2_3
     *                                                   ↓
     *                                              Future3(stack) -> Completion3_1 ->  Completion3_2 -> Completion3_3
     * </pre>
     * 转换后的结构如下：
     * <pre>
     *      Future1(stack) -> Completion1_1 ->  Completion2_1 ->  Completion2_2 -> Completion2_3 -> Completion1_2 -> Completion1_3
     *                           (已执行)                 ↓
     *                                              Future3(stack) -> Completion3_1 ->  Completion3_2 -> Completion3_3
     * </pre>
     */
    private Completion clearListeners(Completion onto) {
        // 我们需要进行三件事
        // 1. 原子方式将当前Listeners赋值为TOMBSTONE，因为pushCompletion添加的监听器的可见性是由CAS提供的。
        // 2. 将当前栈内元素逆序，因为即使在接口层进行了说明，但仍然有人依赖于监听器的执行时序(期望先添加的先执行)
        // 3. 将逆序后的元素插入到'onto'前面，即插入到原本要被通知的下一个监听器的前面

        Completion head;
        do {
            head = stack;
        } while (!STACK.compareAndSet(this, head, Completion.TOMBSTONE));

        Completion reversedList = onto;
        while (head != null) {
            Completion tmp = head;
            head = head.next;
            tmp.next = reversedList;
            reversedList = tmp;
        }
        return reversedList;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    private static final VarHandle RESULT_HOLDER;
    private static final VarHandle STACK;
    private static final VarHandle SIGNAL_NEEDED;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            RESULT_HOLDER = l.findVarHandle(AbstractPromise.class, "resultHolder", Object.class);
            STACK = l.findVarHandle(AbstractPromise.class, "stack", Completion.class);
            SIGNAL_NEEDED = l.findVarHandle(AbstractPromise.class, "signalNeeded", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
