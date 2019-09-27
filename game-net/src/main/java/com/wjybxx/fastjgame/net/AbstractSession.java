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

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.concurrent.Promise;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.manager.NetManagerWrapper;
import com.wjybxx.fastjgame.net.handler.AsyncRpcRequestWriteTask;
import com.wjybxx.fastjgame.net.handler.OneWayMessageWriteTask;
import com.wjybxx.fastjgame.net.handler.SyncRpcRequestWriteTask;
import com.wjybxx.fastjgame.net.pipeline.DefaultSessionPipeline;
import com.wjybxx.fastjgame.net.pipeline.SessionPipeline;

import javax.annotation.Nonnull;

/**
 * Session的模板实现。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
public abstract class AbstractSession implements Session {

    /**
     * session关联的本地信息
     */
    protected final NetContext netContext;
    /**
     * session关联的管道
     */
    protected final SessionPipeline pipeline;
    /**
     * {@link NetEventLoop}关联的所有逻辑控制器
     */
    protected final NetManagerWrapper managerWrapper;
    /**
     * session绑定到的EventLoop
     */
    private final NetEventLoop netEventLoop;

    protected AbstractSession(NetContext netContext, NetManagerWrapper managerWrapper) {
        this.netContext = netContext;
        this.managerWrapper = managerWrapper;
        this.pipeline = new DefaultSessionPipeline(this, managerWrapper);
        this.netEventLoop = managerWrapper.getNetEventLoopManager().eventLoop();
    }

    @Override
    public final long localGuid() {
        return netContext.localGuid();
    }

    @Override
    public final RoleType localRole() {
        return netContext.localRole();
    }

    @Override
    public final NetEventLoop netEventLoop() {
        //注意：这里可能和session所属的NetContext中的NetEventLoop不一样
        return netEventLoop;
    }

    @Override
    public final EventLoop localEventLoop() {
        return netContext.localEventLoop();
    }

    @Override
    public SessionPipeline pipeline() {
        return pipeline;
    }

    @Override
    public void send(@Nonnull Object message) {
        netEventLoop.execute(new OneWayMessageWriteTask(this, message));
    }

    @Override
    public void call(@Nonnull Object request, @Nonnull RpcCallback callback) {
        call(request, callback, config().getRpcCallbackTimeoutMs());
    }

    @Override
    public void call(@Nonnull Object request, @Nonnull RpcCallback callback, long timeoutMs) {
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("timeoutMs");
        }
        netEventLoop.execute(new AsyncRpcRequestWriteTask(this, request, callback, timeoutMs));
    }

    @Override
    @Nonnull
    public RpcResponse sync(@Nonnull Object request) {
        return sync(request, config().getSyncRpcTimeoutMs());
    }

    @Override
    @Nonnull
    public RpcResponse sync(@Nonnull Object request, long timeoutMs) {
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("timeoutMs");
        }
        // 逻辑层校验，会话已关闭，立即返回结果
        if (!isActive()) {
            return RpcResponse.SESSION_CLOSED;
        }
        final RpcPromise rpcPromise = netEventLoop().newRpcPromise(localEventLoop(), timeoutMs);
        // 提交到网络层执行
        netEventLoop.execute(new SyncRpcRequestWriteTask(this, request, rpcPromise));
        // RpcPromise保证了不会等待超过限时时间
        rpcPromise.awaitUninterruptibly();
        return rpcPromise.getNow();
    }

    @Override
    public void write(@Nonnull Object msg) {
        pipeline.write(msg);
    }

    @Override
    public void flush() {
        pipeline.flush();
    }

    @Override
    public void writeAndFlush(@Nonnull Object msg) {
        pipeline.writeAndFlush(msg);
    }

    @Override
    public ListenableFuture<?> close() {
        final Promise<Object> promise = netEventLoop.newPromise();
        pipeline.close(promise);
        return promise;
    }

    @Override
    public void close(Promise<?> promise) {
        pipeline.close(promise);
    }

}
