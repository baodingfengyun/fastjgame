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

import com.wjybxx.fastjgame.concurrent.DefaultPromise;
import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.FutureListener;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * RpcPromise基本实现，不论如何，执行结果都是成功，赋值结果必须是RpcResponse对象。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/3
 * github - https://github.com/hl845740757
 */
public class DefaultRpcPromise extends DefaultPromise<RpcResponse> implements RpcPromise {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRpcPromise.class);

    /**
     * 工作线程 - 检查死锁的线程
     */
    private final NetEventLoop workerEventLoop;
    /**
     * 最终过期时间(毫秒)
     */
    private final long deadline;

    /**
     * @param workerEventLoop 创建该promise的EventLoop，禁止等待的线程。
     * @param appEventLoop    发起rpc调用的用户所在的EventLoop
     * @param timeoutMs       promise超时时间
     */
    public DefaultRpcPromise(NetEventLoop workerEventLoop, EventLoop appEventLoop, long timeoutMs) {
        super(appEventLoop);
        this.workerEventLoop = workerEventLoop;
        this.deadline = System.currentTimeMillis() + timeoutMs;
    }

    @Override
    protected void checkDeadlock() {
        ConcurrentUtils.checkDeadLock(workerEventLoop);
    }

    // ------------------------------------------- get不抛出中断以外异常 ----------------------------------------

    @Override
    public RpcResponse get() throws InterruptedException {
        try {
            return super.get();
        } catch (ExecutionException error) {
            logger.error("bad imp", error);
            return new RpcResponse(RpcResultCode.LOCAL_EXCEPTION, error);
        }
    }

    @Override
    public RpcResponse get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, TimeoutException {
        try {
            return super.get(timeout, unit);
        } catch (ExecutionException error) {
            logger.error("bad imp", error);
            return new RpcResponse(RpcResultCode.LOCAL_EXCEPTION, error);
        }
    }

    @Override
    public long deadline() {
        return deadline;
    }

    // ---------------------------------------------- 超时检测 ------------------------------------------------

    @Override
    public RpcResponse getNow() {
        // 如果时间到了，还没有结果，那么需要标记为超时
        if (!isDone() && System.currentTimeMillis() >= deadline) {
            trySuccess(RpcResponse.TIMEOUT);
        }
        return super.getNow();
    }

    @Override
    public RpcPromise await() throws InterruptedException {
        // 有限的等待
        await(deadline - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        assert isDone();
        return this;
    }

    @Override
    public RpcPromise awaitUninterruptibly() {
        // 有限的等待
        awaitUninterruptibly(deadline - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        assert isDone();
        return this;
    }

    @Override
    public boolean await(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        final long expectMillis = unit.toMillis(timeout);
        final long remainMillis = deadline - System.currentTimeMillis();
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
    public boolean awaitUninterruptibly(long timeout, @Nonnull TimeUnit unit) {
        final long expectMillis = unit.toMillis(timeout);
        final long remainMillis = deadline - System.currentTimeMillis();
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

    // ----------------------------------------------- 将失败转换为成功 ----------------------------------------------

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

    // ------------------------------------------------ 支持流式语法 ------------------------------------
    @Override
    public RpcPromise addListener(@Nonnull FutureListener<? super RpcResponse> listener) {
        super.addListener(listener);
        return this;
    }

    @Override
    public RpcPromise addListener(@Nonnull FutureListener<? super RpcResponse> listener, @Nonnull EventLoop bindExecutor) {
        super.addListener(listener, bindExecutor);
        return this;
    }

    @Override
    public RpcPromise removeListener(@Nonnull FutureListener<? super RpcResponse> listener) {
        super.removeListener(listener);
        return this;
    }
}
