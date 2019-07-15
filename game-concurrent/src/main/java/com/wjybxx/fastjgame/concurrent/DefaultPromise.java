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

import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.EventLoopUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 默认的{@link Promise}实现。
 * 去掉了netty中的很多我们不需要使用的东西，以及去除部分优化(降低复杂度)。
 *
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
 *
 * 建议使用 {@link EventLoop#newPromise()}代替构造方法。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14 14:53
 * github - https://github.com/hl845740757
 */
public class DefaultPromise<V> extends AbstractListenableFuture<V> implements Promise<V> {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPromise.class);

    private static final int NANO_PER_MILLSECOND = (int) TimeUnit.MILLISECONDS.toNanos(1);

    /**
     * 表示任务已成功完成。
     * 如果一个任务成功时没有返回值{@link #setSuccess(Object) null}，使用该对象代替。
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
     * 创建Promise的EventLoop，也是任务执行的线程，也是默认的通知用的executor。
     * 如果任务执行期间可能改变executor，那么需要重写{@link #executor()}，以返回最新的executor。
     */
    private final EventLoop _executor;

    /** 该future上注册的监听器们 */
    @Generated("this")
    private List<ListenerEntry<? super V>> waitListeners = null;
    /** 是否正在进行通知 */
    @Generated("this")
    private boolean notifyingListeners = false;

    public DefaultPromise(@Nonnull EventLoop executor) {
        this._executor = executor;
    }

    /**
     * 供子类使用的构造方法。如果使用该构造方法，必须重写{@link #executor()}方法
     */
    protected DefaultPromise() {
        this._executor = null;
    }

    private Object getResult() {
        return resultHolder.get();
    }

    /**
     * 返回最新的用于通知的EventLoop
     * @return nonnull
     */
    @Nonnull
    protected EventLoop executor() {
        return _executor;
    }

    @Override
    public void setSuccess(V result) {
        if (!trySuccess(result)){
            throw new IllegalStateException("complete already, discard result " + result);
        }
    }

    // ----------------------------------------- state begin --------------------------------------------

    /**
     * 判断result是否表示初始状态
     */
    private static boolean isInit0(Object result) {
        return result == null;
    }

    /**
     * 判断result是否表示不可取消状态
     */
    private static boolean isUncancellable0(Object result) {
        return result == UNCANCELLABLE;
    }

    /**
     * 判断result是否表示已完成
     */
    private static boolean isDone0(Object result) {
        return result != null && result != UNCANCELLABLE;
    }

    // ---------------------------------------------- state end ----------------------------------------

    /**
     * 判断结果是否表示执行成功（已进入完成状态，且是正常完成的）。
     *
     * 解释一下为什么多线程的代码喜欢将volatile变量存为临时变量或传递给某一个方法做判断。
     * 原因：保证数据的前后一致性。
     * 当对volatile涉及多个操作时，如果不把volatile变量保存下来，每次读取的结果可能是不一样的！！！
     */
    private static boolean isSuccess0(Object result) {
        return isDone0(result) && !(result instanceof CauseHolder);
    }

    /**
     * 获取表示失败的原因。
     * @return 如果是失败完成状态，则返回对应的异常，否则返回null。
     */
    private static Throwable getCause0(Object result) {
        return (result instanceof CauseHolder) ? ((CauseHolder)result).cause : null;
    }

    /**
     * 判断result是否表示已取消
     */
    private static boolean isCancelled0(Object result) {
        // null instanceOf 总是返回false
        return getCause0(result) instanceof CancellationException;
    }

    @Override
    public boolean trySuccess(V result) {
        // 当future关联的task没有返回值时，使用SUCCESS代替
        return tryCompleted(result == null ? SUCCESS : result, false);
    }

    @Override
    public void setFailure(@Nonnull Throwable cause) {
        if (!tryFailure(cause)){
            throw new IllegalStateException("complete already, discard cause " + cause);
        }
    }

    @Override
    public boolean tryFailure(@Nonnull Throwable cause) {
        return tryCompleted(new CauseHolder(cause), false);
    }

    /**
     * 尝试赋值结果(从未完成状态进入完成状态)
     * @param value 要赋的值，一定不为null
     * @param cancel 是否是取消执行
     * @return 如果赋值成功，则返回true，否则返回false。
     */
    private boolean tryCompleted(@Nonnull Object value, boolean cancel) {
        boolean success;
        if (cancel) {
            success = resultHolder.compareAndSet(null, value);
        } else {
            success = resultHolder.compareAndSet(null, value) || resultHolder.compareAndSet(UNCANCELLABLE, value);
        }

        if (success){
            // 唤醒在改对象上等待结果的所有线程
            synchronized (this){
                this.notifyAll();
            }
            // 广播监听器
            notifyListeners();
            return true;
        } else {
            // 赋值操作无效，已被赋值过了
            return false;
        }
    }

    /**
     * 通知所有的监听器
     */
    private void notifyListeners() {
        // 用于拉取最新的监听器，避免长时间的占有锁
        List<ListenerEntry<? super V>> listeners;
        synchronized (this){
            // 有线程正在进行通知 或当前 没有监听器，则不需要当前线程进行通知
            if (notifyingListeners || null == this.waitListeners){
                return;
            }
            // 标记为正在通知(每一个正在通知的线程都会将所有的监听器通知一遍)
            notifyingListeners = true;
            listeners = this.waitListeners;
            this.waitListeners = null;
        }

        for (;;) {
            // 通知当前批次的监听器(此时不需要获得锁)
            for (ListenerEntry<? super V> listenerEntry:listeners) {
                notifyListener(listenerEntry.listener, listenerEntry.bindExecutor);
            }
            // 通知完当前批次后，检查是否有新的监听器加入
            synchronized (this) {
                if (null == this.waitListeners) {
                    // 通知完毕
                    this.notifyingListeners = false;
                    return;
                }
                // 有新的监听器加入，拉取最新的监听器，继续通知
                listeners = this.waitListeners;
                this.waitListeners = null;
            }
        }
    }

    /**
     * 通知单个监听器，future上的任务已完成。
     */
    private void notifyListener(FutureListener<? super V> listener, @Nullable EventLoop bindExecutor) {
        try {
            // 如果注册监听器时没有绑定执行环境(执行线程)，则使用当前最新的executor
            EventLoop executor = null == bindExecutor ? executor() : bindExecutor;
            EventLoopUtils.submitOrRun(executor, () -> listener.onComplete(this), this::handleException);
        } catch (Exception e) {
            logger.warn("execute notify task caught exception.", e);
        }
    }

    private void handleException(Exception e) {
        ConcurrentUtils.recoveryInterrupted(e);
        logger.warn("invoke onComplete caught Exception.", e);
    }

    @Override
    public boolean setUncancellable() {
        if (resultHolder.compareAndSet(null, UNCANCELLABLE)) {
            return true;
        } else {
            Object result = resultHolder.get();
            // 到这里result一定不为null，当前为不可取消状态 或 结束状态
            if (result == UNCANCELLABLE) {
                // 已经是不可取消状态
                return true;
            } else {
                // 到这里表示已经进入完成状态了，非取消进入完成状态，则返回true
                return !isCancelled0(result);
            }
        }
    }

    @Override
    public boolean isCancellable() {
        // 只有初始状态才可以取消
        return isInit0(resultHolder.get());
    }

    // --------------------------------------------  分割线  -------------------------------------------------------

    @Override
    public boolean isDone() {
        return isDone0(resultHolder.get());
    }

    @Override
    public boolean isSuccess() {
        return isSuccess0(resultHolder.get());
    }

    @Override
    public boolean isCancelled() {
        return isCancelled0(resultHolder.get());
    }

    @Nullable
    @Override
    public Throwable cause() {
        return getCause0(resultHolder.get());
    }

    @SuppressWarnings("unchecked")
    @Override
    public V tryGet() {
        Object result = resultHolder.get();
        if (isSuccess0(result)) {
            return result == SUCCESS ? null : (V) result;
        } else {
            return null;
        }
    }

    /**
     * 尝试取消任务。
     * @param mayInterruptIfRunning 该参数对该promise而言是无效的。
     *                              对该promise而言，在它定义的抽象中，它只是获取结果的凭据，它并不真正执行任务，因此它不也不能真正取消任务。
     *                              因此该参数对该promise而言是无效的。
     *                              这里的取消并不是真正意义上的取消，它不能取消过程，只能取消结果。
     * @return true if succeed
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (!isDone()){
            // 检查一次状态，减少异常生成(填充堆栈)
            return tryCompleted(new CauseHolder(new CancellationException()), true);
        } else {
            return false;
        }
    }

    @Override
    public void await() throws InterruptedException {
        // 先检查一次是否已完成，减小锁竞争
        if (isDone()){
            return;
        }

        // 即将等待之前检查中断标记
        ConcurrentUtils.checkInterrupted();

        // 这里可以在while里面加锁，是因为isDone并不需要获得锁才能检查。
        // synchronize在while内部的话，wait醒来后，申请锁之后才能执行，但是又会立即释放锁，检查到条件不满足后，可能又立即申请锁。
        synchronized (this) {
            while (!isDone()) {
                this.wait();
            }
        }
    }

    @Override
    public void awaitUninterruptibly() {
        // 重复代码写多了容易出事故。
        ConcurrentUtils.awaitUninterruptibly(this, DefaultPromise::await);
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        if (timeout <= 0) {
            await();
            return true;
        }

        // 先检查一次是否已完成，减小锁锁竞争
        if (isDone()) {
            return true;
        }

        // 即将等待之前检查中断标记（在耗时操作开始前，检查中断 -- 要养成习惯）
        ConcurrentUtils.checkInterrupted();

        final long endTime = System.nanoTime() + unit.toNanos(timeout);
        synchronized (this){
            // 获取锁需要时间，因此应该在获取锁之后计算剩余时间
            for (long remainNano = endTime - System.nanoTime(); remainNano > 0; remainNano = endTime - System.nanoTime()) {
                if (isDone()){
                    return true;
                }
                this.wait(remainNano / NANO_PER_MILLSECOND, (int) (remainNano % NANO_PER_MILLSECOND));
            }
        }
        // 再努力尝试一次
        return isDone();
    }

    @Override
    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
        if (timeout <= 0) {
            awaitUninterruptibly();
            return true;
        }

        // 先检查一次是否已完成，减小锁锁竞争
        if (isDone()) {
            return true;
        }
        // 先清除当前中断状态(避免无谓的中断异常)
        boolean interrupted = Thread.interrupted();
        try {

            final long endTime = System.nanoTime() + unit.toNanos(timeout);
            synchronized (this){
                // 获取锁需要时间
                for (long remainNano = endTime - System.nanoTime(); remainNano > 0; remainNano = endTime - System.nanoTime()) {
                    if (isDone()){
                        return true;
                    }
                    try {
                        this.wait(remainNano / 1000000, (int) (remainNano % 1000000));
                    } catch (InterruptedException e){
                        interrupted = true;
                    }
                }
            }
            // 再努力尝试一次
            return isDone();
        } finally {
            // 恢复中断状态
            ConcurrentUtils.recoveryInterrupted(interrupted);
        }
    }

    @Override
    public void addListener(@Nonnull FutureListener<? super V> listener) {
        // null is safe
        addListener(listener, null);
    }

    @Override
    public void addListener(@Nonnull FutureListener<? super V> listener, EventLoop bindExecutor) {
        // 认为尚未完成，加入等待通知集合
        synchronized (this) {
            if (waitListeners == null) {
                waitListeners = new LinkedList<>();
            }
            waitListeners.add(new ListenerEntry<>(listener, bindExecutor));
        }

        // 必须检查完成状态，如果已进入完成状态，通知刚刚加入监听器们（否则可能丢失通知）（早已完成的状态下）
        // 因为状态改变 和 锁没有关系
        if (isDone()) {
            notifyListeners();
        }
    }

    @Override
    public void removeListener(@Nonnull FutureListener<? super V> listener) {
        synchronized (this) {
            if (waitListeners == null) {
                return;
            }
            waitListeners.removeIf(entry -> entry.listener.equals(listener));
            if (waitListeners.size() == 0){
                waitListeners = null;
            }
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
}
