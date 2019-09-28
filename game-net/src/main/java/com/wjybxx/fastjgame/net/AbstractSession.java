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

import com.wjybxx.fastjgame.annotation.Internal;
import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.concurrent.Promise;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.manager.NetManagerWrapper;
import com.wjybxx.fastjgame.net.task.AsyncRpcRequestWriteTask;
import com.wjybxx.fastjgame.net.task.BatchOneWayWriteTask;
import com.wjybxx.fastjgame.net.task.OneWayMessageWriteTask;
import com.wjybxx.fastjgame.net.task.SyncRpcRequestWriteTask;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
     * session绑定到的EventLoop
     */
    private final NetEventLoop netEventLoop;
    /**
     * session关联的管道
     */
    private final SessionPipeline pipeline;
    /**
     * 激活状态 - 因为session都是建立成功的时候创建，因此默认true
     */
    private final AtomicBoolean stateHolder = new AtomicBoolean(true);

    protected AbstractSession(NetContext netContext, NetManagerWrapper managerWrapper) {
        this.netContext = netContext;
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
    public final void send(@Nonnull Object message) {
        if (isActive()) {
            // 会话活动的状态下才会发送
            netEventLoop.execute(new OneWayMessageWriteTask(this, message));
        }
    }

    @Override
    public final void send(@Nonnull List<Object> messageList) {
        if (isActive()) {
            // 会话活动的状态下才会发送
            netEventLoop.execute(new BatchOneWayWriteTask(this, messageList));
        }
    }

    @Override
    public final void call(@Nonnull Object request, @Nonnull RpcCallback callback) {
        if (isActive()) {
            // 会话活动的状态下才会发送
            netEventLoop.execute(new AsyncRpcRequestWriteTask(this, request, callback));
        } else {
            // 会话关闭的情况下，直接执行回调
            callback.onComplete(RpcResponse.SESSION_CLOSED);
        }
    }

    @Override
    @Nonnull
    public final RpcResponse sync(@Nonnull Object request) {
        // 逻辑层校验，会话已关闭，立即返回结果
        if (!isActive()) {
            return RpcResponse.SESSION_CLOSED;
        }
        final RpcPromise rpcPromise = netEventLoop().newRpcPromise(localEventLoop(), config().getSyncRpcTimeoutMs());
        // 提交到网络层执行
        netEventLoop.execute(new SyncRpcRequestWriteTask(this, request, rpcPromise));
        // RpcPromise保证了不会等待超过限时时间
        rpcPromise.awaitUninterruptibly();
        return rpcPromise.getNow();
    }

    @Override
    public final boolean isActive() {
        return stateHolder.get();
    }

    @Override
    public final ListenableFuture<?> close() {
        // 设置为未激活状态
        stateHolder.set(false);
        final Promise<Object> promise = netEventLoop.newPromise();
        // 可能是网络层关闭
        ConcurrentUtils.executeOrRun(netEventLoop, () -> {
            pipeline.fireClose(promise);
        });
        return promise;
    }

    @Override
    public final SessionPipeline pipeline() {
        if (netEventLoop.inEventLoop()) {
            return pipeline;
        } else {
            throw new IllegalStateException("internal api");
        }
    }

    @Override
    public final void tick() {
        if (netEventLoop.inEventLoop()) {
            pipeline.tick();
        } else {
            throw new IllegalStateException("internal api");
        }
    }

    @Internal
    @Override
    public void fireRead(@Nullable Object msg) {
        if (netEventLoop.inEventLoop()) {
            pipeline.fireRead(msg);
        } else {
            throw new IllegalStateException("internal api");
        }
    }

    @Internal
    @Override
    public void fireWrite(@Nonnull Object msg) {
        if (netEventLoop.inEventLoop()) {
            pipeline.fireWrite(msg);
        } else {
            throw new IllegalStateException("internal api");
        }
    }
}
