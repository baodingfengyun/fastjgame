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
import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.manager.NetConfigManager;
import com.wjybxx.fastjgame.manager.SessionManager;
import com.wjybxx.fastjgame.utils.EventLoopUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * Session的模板实现。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
public abstract class AbstractSession implements Session{

    private static final Logger logger = LoggerFactory.getLogger(AbstractSession.class);

    private final SessionPipeline pipeline = new DefaultSessionPipeline(this);

    /**
     * 获取网络配置管理器
     * @return NetConfigManager
     */
    protected abstract NetConfigManager getNetConfigManager();

    /**
     * 获取该session对应的管理器
     * @return SessionManager
     */
    protected abstract SessionManager getSessionManager();

    @Override
    public final long localGuid() {
        return netContext().localGuid();
    }

    @Override
    public final RoleType localRole() {
        return netContext().localRole();
    }

    public NetEventLoop netEventLoop() {
        return netContext().netEventLoop();
    }

    public EventLoop localEventLoop() {
        return netContext().localEventLoop();
    }

    @Nonnull
    @Override
    public SessionPipeline pipeline() {
        return pipeline;
    }

    @Override
    public final void send(@Nonnull Object message) {
        // 逻辑层检测，会话已关闭，立即返回
        if (!isActive()) {
            logger.info("session is already closed, send message failed.");
            return;
        }
        netEventLoop().execute(() -> {
            sendOneWayMessage(message);
        });
    }

    @Override
    public void rpc(@Nonnull Object request, @Nonnull RpcCallback callback) {
        rpc(request, callback, getNetConfigManager().rpcCallbackTimeoutMs());
    }

    @Override
    public void rpc(@Nonnull Object request, @Nonnull RpcCallback callback, long timeoutMs) {
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("timeoutMs");
        }
        // 逻辑层校验，会话已关闭，提交回调
        if (!isActive()) {
            // 讲道理应该是在用户线程下
            EventLoopUtils.executeOrRun(localEventLoop(), () -> {
                callback.onComplete(RpcResponse.SESSION_CLOSED);
            });
            return;
        }
        // 提交到网络层
        netEventLoop().execute(() -> {
            sendAsyncRpcRequest(request, timeoutMs, localEventLoop(), callback);
        });
    }

    @Nonnull
    @Override
    public final RpcFuture rpc(@Nonnull Object request) {
        return rpc(request, getNetConfigManager().rpcCallbackTimeoutMs());
    }

    @Nonnull
    @Override
    public final RpcFuture rpc(@Nonnull Object request, long timeoutMs) {
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("timeoutMs");
        }
        // 逻辑层校验，会话已关闭，立即返回结果
        if (!isActive()) {
            return netEventLoop().newCompletedRpcFuture(localEventLoop(), RpcResponse.SESSION_CLOSED);
        }
        final RpcPromise rpcPromise = netEventLoop().newRpcPromise(localEventLoop(), timeoutMs);
        // 提交执行
        netEventLoop().execute(() -> {
            sendAsyncRpcRequest(request, timeoutMs, rpcPromise);
        });
        // 返回给调用者
        return rpcPromise;
    }

    @Nonnull
    @Override
    public final RpcResponse syncRpc(@Nonnull Object request) throws InterruptedException {
        return syncRpc(request, getNetConfigManager().syncRpcTimeoutMs());
    }

    @Nonnull
    @Override
    public final RpcResponse syncRpc(@Nonnull Object request, long timeoutMs) throws InterruptedException {
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("timeoutMs");
        }
        // 逻辑层校验，会话已关闭，立即返回结果
        if (!isActive()) {
            return RpcResponse.SESSION_CLOSED;
        }
        final RpcPromise rpcPromise = netEventLoop().newRpcPromise(localEventLoop(), timeoutMs);
        // 提交执行
        netEventLoop().execute(() -> {
            sendSyncRpcRequest(request, timeoutMs, rpcPromise);
        });
        // RpcPromise保证了不会等待超过限时时间
        return rpcPromise.get();
    }

    @Nonnull
    @Override
    public RpcResponse syncRpcUninterruptibly(@Nonnull Object request) {
        return syncRpcUninterruptibly(request, getNetConfigManager().syncRpcTimeoutMs());
    }

    @Nonnull
    @Override
    public RpcResponse syncRpcUninterruptibly(@Nonnull Object request, long timeoutMs) {
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("timeoutMs");
        }
        // 逻辑层校验，会话已关闭，立即返回结果
        if (!isActive()) {
            return RpcResponse.SESSION_CLOSED;
        }
        final RpcPromise rpcPromise = netEventLoop().newRpcPromise(localEventLoop(), timeoutMs);
        // 提交执行
        netEventLoop().execute(() -> {
            sendSyncRpcRequest(request, timeoutMs, rpcPromise);
        });
        // RpcPromise保证了不会等待超过限时时间
        rpcPromise.awaitUninterruptibly();
        // 一定有结果
        return rpcPromise.getNow();
    }

    @Nonnull
    @Override
    public <T> RpcResponseChannel<T> newResponseChannel(@Nonnull RpcRequestContext context) {
        return newResponseChannel((DefaultRpcRequestContext) context);
    }

    final <T> SessionRpcResponseChannel<T> newResponseChannel(@Nonnull DefaultRpcRequestContext context) {
        return new SessionRpcResponseChannel<>(this, context);
    }

    // ------------------------------------------- 发送消息接口，运行在网络线程下 ---------------------------------------

    /** 发送单向消息 */
    final void sendOneWayMessage(@Nonnull Object message) {
        getSessionManager().send(localGuid(), remoteGuid(), message);
    }

    /**
     * 发送异步rpc请求
     * @param request 请求内容
     * @param timeoutMs 超时时间
     * @param userEventLoop 用户线程
     * @param rpcCallback 回调函数
     */
    final void sendAsyncRpcRequest(@Nonnull Object request, long timeoutMs, @Nonnull EventLoop userEventLoop, @Nonnull RpcCallback rpcCallback) {
        getSessionManager().rpc(localGuid(), remoteGuid(), request, timeoutMs, userEventLoop, rpcCallback);
    }

    /**
     * 发送异步rpc请求
     * @param request 请求内容
     * @param timeoutMs 超时时间
     * @param rpcPromise 存储结果的promise
     */
    final void sendAsyncRpcRequest(@Nonnull Object request, long timeoutMs, @Nonnull RpcPromise rpcPromise) {
        getSessionManager().rpc(localGuid(), remoteGuid(), request, timeoutMs, false, rpcPromise);
    }

    /**
     * 发送同步rpc请求
     * @param request 请求内容
     * @param timeoutMs 超时时间
     * @param rpcResponsePromise 存储结果的promise
     */
    private void sendSyncRpcRequest(@Nonnull Object request, long timeoutMs, RpcPromise rpcResponsePromise) {
        getSessionManager().rpc(localGuid(), remoteGuid(), request, timeoutMs, true, rpcResponsePromise);
    }

    /**
     * 发送rpc响应
     * @param sync 是否是同步rpc请求
     * @param requestGuid 请求对应的id
     * @param rpcResponse 请求对应的响应
     */
    final void sendRpcResponse(boolean sync, long requestGuid, RpcResponse rpcResponse) {
        getSessionManager().sendRpcResponse(localGuid(), remoteGuid(), requestGuid, sync, rpcResponse);
    }

    /**
     * Session创建的RpcResponseChannel，立即返回结果
     */
    private static class SessionRpcResponseChannel<T> extends AbstractRpcResponseChannel<T>{

        private final AbstractSession session;
        private final DefaultRpcRequestContext context;

        private SessionRpcResponseChannel(AbstractSession session, DefaultRpcRequestContext context) {
            this.session = session;
            this.context = context;
        }

        @Override
        protected void doWrite(RpcResponse rpcResponse) {
            // 网络层可能会使用该channel发送结果(提交请求失败的时候)
            EventLoopUtils.executeOrRun(session.netEventLoop(), () -> {
                session.sendRpcResponse(context.sync, context.requestGuid, rpcResponse);
            });
        }
    }
}
