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

package com.wjybxx.fastjgame.net.rpc;

import com.wjybxx.fastjgame.net.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.net.exception.RpcException;
import com.wjybxx.fastjgame.net.exception.RpcTimeoutException;
import com.wjybxx.fastjgame.utils.concurrent.*;
import com.wjybxx.fastjgame.utils.concurrent.timeout.DefaultTimeoutPromise;
import com.wjybxx.fastjgame.utils.concurrent.timeout.TimeoutFutureListener;

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * RpcPromise基本实现。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/3
 * github - https://github.com/hl845740757
 */
public class DefaultRpcPromise<V> extends DefaultTimeoutPromise<V> implements RpcPromise<V> {

    /**
     * 工作线程 - 检查死锁的线程
     */
    private final NetEventLoop workerEventLoop;

    /**
     * @param workerEventLoop 创建该promise的EventLoop，禁止等待的线程。
     * @param appEventLoop    发起rpc调用的用户所在的EventLoop
     * @param timeoutMs       promise超时时间
     */
    public DefaultRpcPromise(NetEventLoop workerEventLoop, EventLoop appEventLoop, long timeoutMs) {
        super(appEventLoop, timeoutMs, TimeUnit.MILLISECONDS);
        this.workerEventLoop = workerEventLoop;
    }

    @Override
    protected void checkDeadlock() {
        EventLoopUtils.checkDeadLock(workerEventLoop);
    }

    @Override
    public final boolean isTimeout() {
        return cause() instanceof RpcTimeoutException;
    }

    @Override
    public boolean isRpcException() {
        return isRpcException0(cause());
    }

    @Override
    public RpcErrorCode errorCode() {
        return getErrorCode0(cause());
    }

    static boolean isRpcException0(Throwable cause) {
        return cause instanceof RpcException;
    }

    static RpcErrorCode getErrorCode0(Throwable cause) {
        if (cause instanceof RpcException) {
            return ((RpcException) cause).getErrorCode();
        }
        return null;
    }

    // --------------------------------- 流式语法支持 ------------------------------

    @Override
    public RpcPromise<V> await() throws InterruptedException {
        super.await();
        return this;
    }

    @Override
    public RpcPromise<V> awaitUninterruptibly() {
        super.awaitUninterruptibly();
        return this;
    }

    @Override
    public RpcPromise<V> onTimeout(@Nonnull TimeoutFutureListener<? super V> listener) {
        super.onTimeout(listener);
        return this;
    }

    @Override
    public RpcPromise<V> onTimeout(@Nonnull TimeoutFutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        super.onTimeout(listener, bindExecutor);
        return this;
    }

    @Override
    public RpcPromise<V> onComplete(@Nonnull FutureListener<? super V> listener) {
        super.onComplete(listener);
        return this;
    }

    @Override
    public RpcPromise<V> onComplete(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        super.onComplete(listener, bindExecutor);
        return this;
    }

    @Override
    public RpcPromise<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener) {
        super.onSuccess(listener);
        return this;
    }

    @Override
    public RpcPromise<V> onSuccess(@Nonnull SucceededFutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        super.onSuccess(listener, bindExecutor);
        return this;
    }

    @Override
    public RpcPromise<V> onFailure(@Nonnull FailedFutureListener<? super V> listener) {
        super.onFailure(listener);
        return this;
    }

    @Override
    public RpcPromise<V> onFailure(@Nonnull FailedFutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        super.onFailure(listener, bindExecutor);
        return this;
    }
}
