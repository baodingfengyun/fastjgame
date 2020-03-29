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
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link Promise}的模板实现，它只负责结果管理（状态迁移），这部分逻辑完全无锁。
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

    private static final Logger logger = LoggerFactory.getLogger(AbstractPromise.class);

    /**
     * 如果一个任务成功时没有结果{@link #setSuccess(Object) null}，使用该对象代替。
     */
    static final Object SUCCESS = new Object();
    /**
     * 表示future关联的任务进入不可取消状态。
     */
    private static final Object UNCANCELLABLE = new Object();

    /**
     * Future关联的任务的计算结果。
     * {@link AtomicReference}用于保证原子性和可见性。
     */
    final AtomicReference<Object> resultHolder = new AtomicReference<>();

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
        return isDone0(resultHolder.get());
    }

    @Override
    public final boolean isCompletedExceptionally() {
        return isCompletedExceptionally0(resultHolder.get());
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

    @Override
    public final V getNow() {
        final Object result = resultHolder.get();
        if (isDone0(result)) {
            return reportJoin(result);
        }
        return null;
    }

    /**
     * {@link ListenableFuture#getNow()}和{@link BlockingFuture#join()}方法上报结果。
     * 不命名为{@code reportGetNow}是为了放大不同之处。
     */
    @SuppressWarnings("unchecked")
    static <T> T reportJoin(final Object r) {
        if (r == SUCCESS) {
            return null;
        }

        if (r instanceof CauseHolder) {
            return FutureUtils.rethrowJoin(((CauseHolder) r).cause);
        }

        return (T) r;
    }

    @Override
    public final Throwable cause() {
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
        return isCancellable0(result) && tryCompleteCancellation();
    }

    /**
     * 由取消进入完成状态
     *
     * @return 成功取消则返回true
     */
    private boolean tryCompleteCancellation() {
        // 取消只能由初始状态(null)切换为完成状态
        if (resultHolder.compareAndSet(null, new CauseHolder(new CancellationException()))) {
            postCompleteSafely();
            return true;
        } else {
            return false;
        }
    }

    /**
     * 安全的推送future进入完成状态事件，由于推送由子类实现，无法保证所有实现都足够安全，因此需要补货异常。
     */
    private void postCompleteSafely() {
        try {
            postComplete();
        } catch (Throwable e) {
            logger.error("PostComplete caught exception, bad promise implementation {}", this.getClass(), e);
        }
    }

    /**
     * 推送future进入完成状态事件。
     * 主要用于唤醒等待的线程和通知监听器们。
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
        return tryComplete(result == null ? SUCCESS : result);
    }

    /**
     * 成功完成或失败完成
     *
     * @param value 要赋的值，一定不为null
     * @return 如果赋值成功，则返回true，否则返回false。
     */
    private boolean tryComplete(@Nonnull Object value) {
        // 正常完成可以由初始状态或不可取消状态进入完成状态
        if (resultHolder.compareAndSet(null, value)
                || resultHolder.compareAndSet(UNCANCELLABLE, value)) {
            postCompleteSafely();
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
}
