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

package com.wjybxx.fastjgame.net;

import com.wjybxx.fastjgame.concurrent.DefaultPromise;
import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.FutureListener;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

/**
 * RpcPromise基本实现，不论如何，执行结果都是成功，赋值结果必须是RpcResponse对象。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/3
 * github - https://github.com/hl845740757
 */
public class DefaultRpcPromise extends DefaultPromise<RpcResponse> implements RpcPromise {

    /**
     * 发起rpc请求的用户的线程。
     * (默认的通知线程，这样的目的是可以避免死锁)
     */
    private final EventLoop userEventLoop;
    /**
     * 最终过期时间(毫秒)
     */
    private final long timeoutMs;

    /**
     * @param workerEventLoop 创建该promise的EventLoop，为了支持用户调用{@link #await()}系列方法，避免死锁问题。
     * @param userEventLoop   发起rpc调用的用户所在的EventLoop
     * @param timeoutMs       promise超时时间
     */
    public DefaultRpcPromise(NetEventLoop workerEventLoop, EventLoop userEventLoop, long timeoutMs) {
        super(workerEventLoop);
        this.userEventLoop = userEventLoop;
        this.timeoutMs = System.currentTimeMillis() + timeoutMs;
    }

    protected EventLoop getUserEventLoop() {
        return userEventLoop;
    }

    // ------------------------------------------- get不抛出中断以外异常 ----------------------------------------

    @Override
    public RpcResponse get() throws InterruptedException {
        await();
        return getNow();
    }

    @Override
    public RpcResponse get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        await(timeout, unit);
        return getNow();
    }

    // ---------------------------------------------- 超时检测 ------------------------------------------------

    @Override
    public RpcResponse getNow() {
        // 如果时间到了，还没有结果，那么需要标记为超时
        if (System.currentTimeMillis() >= timeoutMs && !isDone()) {
            trySuccess(RpcResponse.TIMEOUT);
        }
        return super.getNow();
    }

    @Override
    public void await() throws InterruptedException {
        // 有限的等待
        await(timeoutMs - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        assert isDone();
    }

    @Override
    public void awaitUninterruptibly() {
        // 有限的等待
        awaitUninterruptibly(timeoutMs - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        assert isDone();
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        final long expectMillis = unit.toMillis(timeout);
        final long remainMillis = timeoutMs - System.currentTimeMillis();
        //  如果期望的时间超过剩余时间，那么必须有结果
        if (expectMillis >= remainMillis) {
            if (super.await(remainMillis, TimeUnit.MILLISECONDS)) {
                return true;
            }
            trySuccess(RpcResponse.TIMEOUT);
            return true;
        } else {
            return super.await(expectMillis, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
        final long expectMillis = unit.toMillis(timeout);
        final long remainMillis = timeoutMs - System.currentTimeMillis();
        //  如果期望的时间超过剩余时间，那么必须有结果
        if (expectMillis >= remainMillis) {
            if (super.awaitUninterruptibly(remainMillis, TimeUnit.MILLISECONDS)) {
                return true;
            }
            trySuccess(RpcResponse.TIMEOUT);
            return true;
        } else {
            return super.awaitUninterruptibly(expectMillis, TimeUnit.MILLISECONDS);
        }
    }

    // ----------------------------------------------- 将失败转移为成功 ----------------------------------------------

    @Override
    public void setFailure(@Nonnull Throwable cause) {
        super.setSuccess(new RpcResponse(RpcResultCode.LOCAL_EXCEPTION, cause));
    }

    @Override
    public boolean tryFailure(@Nonnull Throwable cause) {
        return super.trySuccess(new RpcResponse(RpcResultCode.LOCAL_EXCEPTION, cause));
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return tryCompleted(RpcResponse.CANCELLED, true);
    }

    // ------------------------------------------------- 监听器管理 ----------------------------------------------------

    @Override
    public void addListener(@Nonnull FutureListener<? super RpcResponse> listener) {
        // 监听器也默认执行在用户线程中，保证时序
        addListener(listener, userEventLoop);
    }

    @Override
    public void addCallback(RpcCallback rpcCallback) {
        // Rpc回调默认执行在用户线程下
        addCallback(rpcCallback, userEventLoop);
    }

    @Override
    public void addCallback(RpcCallback rpcCallback, EventLoop eventLoop) {
        addListener(future -> {
            rpcCallback.onComplete(future.getNow());
        }, eventLoop);
    }
}
