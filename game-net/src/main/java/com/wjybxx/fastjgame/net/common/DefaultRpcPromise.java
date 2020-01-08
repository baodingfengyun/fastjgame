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

package com.wjybxx.fastjgame.net.common;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.FutureListener;
import com.wjybxx.fastjgame.concurrent.timeout.DefaultTimeoutPromise;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
        ConcurrentUtils.checkDeadLock(workerEventLoop);
    }

    @Nullable
    @Override
    public RpcFutureResult<V> getAsResult() {
        return (RpcFutureResult<V>) super.getAsResult();
    }

    @Override
    protected RpcFutureResult<V> newResult(V result, Throwable cause) {
        return new DefaultRpcFutureResult<>(result, cause);
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
    public RpcPromise<V> addListener(@Nonnull FutureListener<? super V> listener) {
        super.addListener(listener);
        return this;
    }

    @Override
    public RpcPromise<V> addListener(@Nonnull FutureListener<? super V> listener, @Nonnull EventLoop bindExecutor) {
        super.addListener(listener, bindExecutor);
        return this;
    }

    @Override
    public RpcPromise<V> removeListener(@Nonnull FutureListener<? super V> listener) {
        super.removeListener(listener);
        return this;
    }
}
