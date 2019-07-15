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

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * {@link AbstractListenableFuture}的一个实现，表示它关联的操作早已完成。
 * 任何添加到上面的监听器将立即收到通知。
 * @param <V> the type of value
 * @version 1.0
 * date - 2019/7/14 14:53
 * github - https://github.com/hl845740757
 */
public abstract class CompleteFuture<V> extends AbstractListenableFuture<V> {

    private static final Logger logger = LoggerFactory.getLogger(CompleteFuture.class);

    /** 默认的监听器执行环境 */
    private final EventLoop _executor;

    /**
     * 创建一个实例
     * @param executor 该future用于通知的线程,Listener的执行环境。
     */
    protected CompleteFuture(EventLoop executor) {
        this._executor = executor;
    }

    /**
     * 返回该 {@link CompleteFuture} 关联的用于通知的默认{@link Executor}。
     * (如果构造方法参数为null，那么子类必须返回一个非null的executor)
     */
    @Nonnull
    protected EventLoop executor() {
        return _executor;
    }

    @Override
    public void addListener(@Nonnull FutureListener<? super V> listener) {
        EventLoopUtils.submitOrRun(executor(), ()-> listener.onComplete(this), CompleteFuture::handleException);
    }

    @Override
    public void addListener(@Nonnull FutureListener<? super V> listener, EventLoop bindExecutor) {
        // notify
        EventLoopUtils.submitOrRun(bindExecutor, ()-> listener.onComplete(this), CompleteFuture::handleException);
    }

    /**
     * 处理异常，默认实现仅仅是输出一个异常
     * @param e exception
     */
    private static void handleException(Exception e){
        ConcurrentUtils.recoveryInterrupted(e);
        logger.warn("", e);
    }

    @Override
    public void removeListener(@Nonnull FutureListener<? super V> listener) {
        // NOOP (因为并没有真正添加，因此也不需要移除)
    }

    // 什么时候应该检查中断，不是简单的事，个人觉得这里的操作都已完成，不会造成阻塞(不会执行耗时操作)，因此不需要检查中断

    @Override
    public void await() throws InterruptedException {

    }

    @Override
    public void awaitUninterruptibly() {

    }

    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return true;
    }

    @Override
    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
        return true;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isCancellable() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @param mayInterruptIfRunning this value has no effect in this implementation.
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }
}
