/*
 *  Copyright 2019 wjybxx
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to iBn writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.wjybxx.fastjgame.utils.concurrent.internal;

import com.wjybxx.fastjgame.utils.concurrent.FutureUtils;
import com.wjybxx.fastjgame.utils.concurrent.IPromise;
import com.wjybxx.fastjgame.utils.concurrent.NonBlockingFuture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 模板实现，它只负责了结果管理（状态迁移），这部分逻辑与锁无关。
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
 *
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/6
 */
public abstract class PromiseBase<V> implements NonBlockingFuture<V>, IPromise<V> {

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
     * {@link AtomicReference}用于保证原子性和可见性。
     */
    final AtomicReference<Object> resultHolder = new AtomicReference<>();

    @Nonnull
    @Override
    public NonBlockingFuture<V> getFuture() {
        return this;
    }

    @Override
    public final boolean isVoid() {
        return false;
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

    private static boolean isSuccess0(Object result) {
        return isDone0(result) && !(result instanceof CauseHolder);
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

    // ---------------------------------------------- 非阻塞式获取结果 -----------------------------------------------------

    @Nullable
    @Override
    public V getNow() {
        final Object result = resultHolder.get();
        if (isDone0(result)) {
            return reportGet(result);
        }
        return null;
    }

    /**
     * 用于get方法上报结果
     */
    @SuppressWarnings("unchecked")
    static <T> T reportGet(final Object r) throws CompletionException {
        if (r == SUCCESS) {
            return null;
        }

        if (r instanceof CauseHolder) {
            return FutureUtils.rethrowCause(((CauseHolder) r).cause);
        }

        return (T) r;
    }

    @Nullable
    @Override
    public Throwable cause() {
        return getCause0(resultHolder.get());
    }

    // ------------------------------------------------- 状态迁移 --------------------------------------------
    @Override
    public final boolean setUncancellable() {
        if (resultHolder.compareAndSet(null, UNCANCELLABLE)) {
            return true;
        } else {
            // 到这里result一定不为null，当前为不可取消状态 或 结束状态
            final Object result = resultHolder.get();
            return result == UNCANCELLABLE || !isCancelled0(result);
        }
    }

    /**
     * @param mayInterruptIfRunning 该参数在这里的实现中没有意义，任务开始执行前可以取消，开始执行后无法取消。
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

    /**
     * 推送future进入完成状态事件
     * 主要用于唤醒等待的线程和通知监听器们。
     * 实现类不可以抛出任何异常。
     */
    protected abstract void postComplete();

    @Override
    public final void setSuccess(V result) {
        if (!trySuccess(result)) {
            throw new IllegalStateException("complete already, discard result " + result);
        }
    }

    @Override
    public final boolean trySuccess(V result) {
        // 当future关联的task成功，但是没有返回值时，使用SUCCESS代替
        return completeSuccessOrFailure(result == null ? SUCCESS : result);
    }

    /**
     * 成功完成或失败完成
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

    @Override
    public final void setFailure(@Nonnull Throwable cause) {
        if (!tryFailure(cause)) {
            throw new IllegalStateException("complete already, discard cause " + cause);
        }
    }

    @Override
    public final boolean tryFailure(@Nonnull Throwable cause) {
        return completeSuccessOrFailure(new CauseHolder(cause));
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
}
