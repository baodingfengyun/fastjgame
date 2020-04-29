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
import java.util.Objects;
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
    volatile Object result;

    /**
     * 当前对象上的所有监听器，使用栈方式存储
     * 如果{@code stack}为{@link Completion#TOMBSTONE}，表明当前Future已完成，且正在进行通知，或已通知完毕。
     */
    private volatile Completion stack = null;

    /**
     * 是否需要调用{@link #notifyAll()} - 是否有线程阻塞在当前{@code Future}上，减少锁获取和notifyAll调用。
     */
    private volatile boolean signalNeeded;

    public AbstractPromise() {

    }

    public AbstractPromise(V result) {
        this.result = encodeValue(result);
    }

    public AbstractPromise(Throwable cause) {
        result = new AltResult(cause);
    }

    /**
     * 非取消完成可以由初始状态或不可取消状态进入完成状态
     * CAS{@code null}或者{@link #UNCANCELLABLE} 到指定结果值
     */
    private boolean internalComplete(Object result) {
        return RESULT.compareAndSet(this, null, result)
                || RESULT.compareAndSet(this, UNCANCELLABLE, result);
    }

    /**
     * 取消只能由初始状态(null)切换为完成状态
     * CAS {@code null}到{@link AltResult}
     */
    private boolean internalCompleteCancellation(CancellationException ex) {
        return RESULT.compareAndSet(this, null, new AltResult(ex));
    }

    /**
     * CAS {@code null}到{@link #UNCANCELLABLE}
     *
     * @return the oldValue
     */
    private Object internalUnCancellable() {
        return RESULT.compareAndExchange(this, null, UNCANCELLABLE);
    }

    /**
     * 标记为需要通知
     */
    private void markSignalNeeded() {
        signalNeeded = true;
    }

    /**
     * 是否需要被通知(是否存在被阻塞的线程)
     */
    private boolean isSignalNeeded() {
        return signalNeeded;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 异常结果包装对象，只有该类型表示失败。
     */
    static class AltResult {

        final Throwable cause;

        AltResult(Throwable cause) {
            this.cause = cause;
        }
    }

    /* ------------------------- 开放给Completion的方法 -------------------------- */

    private Object encodeValue(V value) {
        return (value == null) ? NIL : value;
    }

    @SuppressWarnings("unchecked")
    final V decodeValue(Object result) {
        return result == NIL ? null : (V) result;
    }

    final boolean completeNull() {
        return internalComplete(NIL);
    }

    final boolean completeValue(V value) {
        return internalComplete(encodeValue(value));
    }

    // 在异常处理上不同于CompletableFuture，这里保留原始结果和异常，不强制将异常转换为{@link CompletionException}。
    // 这样有助于客户端捕获正确的异常类型，而不是一个奇怪的CompletionException

    /**
     * 如果一个{@link Completion}在计算中出现异常，则使用该方法使目标进入完成状态。
     */
    final boolean completeThrowable(@Nonnull Throwable x) {
        return internalComplete(new AltResult(x));
    }

    /**
     * 使用依赖项的结果进入完成状态，通常表示当前{@link Completion}只是一个简单的中继。
     */
    final boolean completeRelay(Object r) {
        return internalComplete(r);
    }

    /**
     * 使用依赖项的异常结果进入完成状态，通常表示当前{@link Completion}只是一个简单的中继。
     */
    final boolean completeRelayThrowable(Object r) {
        return internalComplete(r);
    }

    // --------------------------------------------- 查询 --------------------------------------------------

    /**
     * 解释一下为什么多线程的代码喜欢将volatile变量存为临时变量或传递给某一个方法做判断。
     * 为了保证数据的一致性，当对volatile执行多个操作时，如果不把volatile变量保存下来，则每次读取的结果可能是不一样的。
     */
    static boolean isDone0(Object result) {
        // 当result不为null，且不是不可取消占位符的时候表示已进入完成状态
        return result != null && result != UNCANCELLABLE;
    }

    private static boolean isCompletedExceptionally0(Object result) {
        return result instanceof AltResult;
    }

    private static boolean isCancelled0(Object result) {
        // null instanceOf 总是返回false
        return getCause0(result) instanceof CancellationException;
    }

    private static Throwable getCause0(Object result) {
        return (result instanceof AltResult) ? ((AltResult) result).cause : null;
    }

    private static boolean isCancellable0(Object result) {
        // 当且仅当result为null的时候可取消
        return result == null;
    }

    @Override
    public final boolean isDone() {
        return isDone0(result);
    }

    @Override
    public final boolean isCompletedExceptionally() {
        return isCompletedExceptionally0(result);
    }

    @Override
    public final boolean isCancelled() {
        return isCancelled0(result);
    }

    @Override
    public final boolean isCancellable() {
        return isCancellable0(result);
    }

    // ---------------------------------------------- 非阻塞式获取结果 -----------------------------------------------------

    @Override
    public final V getNow() {
        final Object localResult = result;
        if (isDone0(localResult)) {
            return reportJoin(localResult);
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

        if (r instanceof AltResult) {
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
        return getCause0(result);
    }

    @Override
    public final boolean acceptNow(@Nonnull BiConsumer<? super V, ? super Throwable> action) {
        final Object localResult = result;
        if (!isDone0(localResult)) {
            return false;
        }

        if (localResult == NIL) {
            action.accept(null, null);
            return true;
        }

        if (localResult instanceof AltResult) {
            action.accept(null, ((AltResult) localResult).cause);
        } else {
            @SuppressWarnings("unchecked") final V value = (V) localResult;
            action.accept(value, null);
        }
        return true;
    }

    // ------------------------------------------------- 状态迁移 --------------------------------------------
    @Override
    public final boolean setUncancellable() {
        final Object result = internalUnCancellable();
        // 到这里result一定不为null，当前为不可取消状态 或 结束状态
        return result == UNCANCELLABLE || !isCancelled0(result);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        final Object localResult = result;

        if (isCancellable0(localResult)
                && internalCompleteCancellation(new CancellationException())) {
            // 判断一次是否可取消，可以减少异常创建
            postComplete(this);
            return true;
        }

        // 可能是已取消或被其它线程取消
        return isCancelled0(localResult) || isCancelled();
    }

    @Override
    public final void setSuccess(V result) {
        if (internalComplete(encodeValue(result))) {
            postComplete(this);
            return;
        }

        throw new IllegalStateException("Already complete");
    }

    @Override
    public final boolean trySuccess(V result) {
        if (internalComplete(encodeValue(result))) {
            postComplete(this);
            return true;
        }
        return false;
    }

    @Override
    public final void setFailure(@Nonnull Throwable cause) {
        Objects.requireNonNull(cause, "cause");

        if (internalComplete(new AltResult(cause))) {
            postComplete(this);
            return;
        }

        throw new IllegalStateException("Already complete");
    }

    @Override
    public final boolean tryFailure(@Nonnull Throwable cause) {
        Objects.requireNonNull(cause, "cause");

        if (internalComplete(new AltResult(cause))) {
            postComplete(this);
            return true;
        }

        return false;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public final V get() throws InterruptedException, ExecutionException {
        final Object localResult = result;
        if (isDone0(localResult)) {
            return reportGet(localResult);
        }

        await();

        return reportGet(result);
    }

    @SuppressWarnings("unchecked")
    private static <T> T reportGet(Object r) throws ExecutionException {
        if (r == NIL) {
            return null;
        }

        if (r instanceof AltResult) {
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
        final Object localResult = result;
        if (isDone0(localResult)) {
            return reportGet(localResult);
        }

        if (await(timeout, unit)) {
            return reportGet(result);
        }

        throw new TimeoutException();
    }

    @Override
    public final V join() throws CompletionException {
        final Object localResult = result;
        if (isDone0(localResult)) {
            return reportJoin(localResult);
        }

        awaitUninterruptibly();

        return reportJoin(result);
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

    // Modes for Completion.tryFire. Signedness matters.
    /**
     * 同步调用模式，表示压栈过程中发现{@code Future}已进入完成状态，从而调用的{@link Completion#tryFire(int)}。
     * 1. 如果在该模式下使下一个{@code Future}进入完成状态，则直接触发目标{@code Future}的完成事件，即调用{@link #postComplete(AbstractPromise)}。
     * 2. 在该模式，表示首次调用，需要判断是否能立即运行。
     */
    static final int SYNC = 0;
    /**
     * 异步调用模式，表示提交到{@link Executor}之后调用{@link Completion#tryFire(int)}
     * 1. 如果在该模式下使下一个{@code Future}进入完成状态，则直接触发目标{@code Future}的完成事件，即调用{@link #postComplete(AbstractPromise)}。
     * 2. 在该模式，表示非首次调用，立即运行。
     */
    static final int ASYNC = 1;
    /**
     * 嵌套调用模式，表示由{@link #postComplete(AbstractPromise)}中触发调用。
     * 1. 如果在该模式下使下一个{@code Future}进入完成状态，不直接触发目标{@code Future}的完成事件，而是返回目标{@code Future}由当前{@code Future}代为推送。
     * 2. 在该模式，表示首次调用，需要判断是否能立即运行。
     */
    static final int NESTED = -1;

    static boolean isSyncOrNestedMode(int mode) {
        return mode <= 0;
    }

    /**
     * 是否是嵌套模式
     */
    static boolean isNestedMode(int mode) {
        return mode < 0;
    }

    static abstract class Completion implements Runnable {

        static final Completion TOMBSTONE = new Completion() {
            @Override
            AbstractPromise<Object> tryFire(int mode) {
                return null;
            }
        };

        /**
         * 下一个节点 - 非Volatile，通过CAS-{@link AbstractPromise#stack}来保证可见性
         */
        Completion next;

        @Override
        public final void run() {
            tryFire(ASYNC);
        }

        /**
         * 当依赖的某个{@code Future}进入完成状态时，该方法会被调用。
         * 如果tryFire使得另一个{@code Future}进入完成状态，分两种情况：
         * 1. mode指示不要调用{@link #postComplete(AbstractPromise)}方法时，则返回新进入完成状态的{@code Future}。
         * 2. mode指示可以调用{@link #postComplete(AbstractPromise)}方法时，则直接其进入完成状态的事件。
         */
        abstract AbstractPromise<?> tryFire(int mode);

    }

    final void pushCompletion(Completion newHead) {
        // 如果future已完成，则立即执行
        if (isDone()) {
            newHead.tryFire(SYNC);
            return;
        }

        Completion expectedHead = stack;
        Completion realHead;

        while (true) {
            // 由下面的CAS保证可见性
            newHead.next = expectedHead;

            realHead = (Completion) STACK.compareAndExchange(this, expectedHead, newHead);

            if (realHead == expectedHead) {
                // 成功添加completion到头部，其会在Future进入完成状态时被通知
                return;
            }

            if (realHead == Completion.TOMBSTONE) {
                // 有线程触发了Future的完成事件，该completion需要立即被通知
                break;
            }

            // retry
            expectedHead = realHead;
        }


        // 到这里的时候 head == TOMBSTONE，表示目标Future已进入完成状态，且正在被通知或已经通知完毕。
        // 由于Future已进入完成状态，且我们的Completion压栈失败，因此新的completion需要当前线程来通知
        newHead.next = null;
        newHead.tryFire(SYNC);
    }

    /**
     * 推送future的完成事件。
     * - 声明为静态会更清晰易懂
     */
    static void postComplete(AbstractPromise<?> future) {
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
                // help gc
                curr.next = null;

                // Completion的tryFire实现不可以抛出异常，否则会导致其它监听器也丢失信号
                future = curr.tryFire(NESTED);

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
        if (isSignalNeeded()) {
            synchronized (this) {
                notifyAll();
            }
        }
    }

    /**
     * 清空当前{@code Future}上的监听器，并将当前{@code Future}上的监听器逆序方式插入到{@code onto}前面。
     * <p>
     * Q: 这步操作是要干什么？<br>
     * A: 由于一个{@link Completion}在执行时可能使另一个{@code Future}进入完成状态，如果不做处理的话，则可能产生一个很深的递归，
     * 从而造成堆栈溢出，也影响性能。该操作就是将可能通知的监听器由树结构展开为链表结构，消除深嵌套的递归。
     * Guava中{@code AbstractFuture}和{@link CompletableFuture}都有类似处理。
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
        // 2. 将当前栈内元素逆序，因为即使在接口层进行了说明（不提供监听器执行时序保证），但仍然有人依赖于监听器的执行时序(期望先添加的先执行)
        // 3. 将逆序后的元素插入到'onto'前面，即插入到原本要被通知的下一个监听器的前面

        Completion head;
        do {
            head = stack;
        } while (!STACK.compareAndSet(this, head, Completion.TOMBSTONE));

        Completion ontoHead = onto;
        while (head != null) {
            Completion tmpHead = head;
            head = head.next;

            tmpHead.next = ontoHead;
            ontoHead = tmpHead;
        }
        return ontoHead;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final VarHandle RESULT;
    private static final VarHandle STACK;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            RESULT = l.findVarHandle(AbstractPromise.class, "result", Object.class);
            STACK = l.findVarHandle(AbstractPromise.class, "stack", Completion.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
