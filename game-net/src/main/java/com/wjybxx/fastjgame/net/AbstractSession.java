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
import com.wjybxx.fastjgame.manager.NetManagerWrapper;
import com.wjybxx.fastjgame.manager.SessionManager;

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
     * session关联的远程角色guid
     */
    protected final long remoteGuid;
    /**
     * session关联的远程角色类型
     */
    protected final RoleType remoteRole;
    /**
     * {@link NetEventLoop}关联的所有逻辑控制器
     */
    protected final NetManagerWrapper managerWrapper;
    /**
     * 消息发送模式
     */
    private final SessionSenderMode sessionSenderMode;
    /**
     * 真正执行消息发送组件
     */
    private final Sender sender;
    /**
     * session绑定到的EventLoop
     */
    private final NetEventLoop netEventLoop;

    protected AbstractSession(NetContext netContext, long remoteGuid, RoleType remoteRole,
                              NetManagerWrapper managerWrapper, SessionSenderMode sessionSenderMode) {
        this.netContext = netContext;
        this.remoteGuid = remoteGuid;
        this.remoteRole = remoteRole;
        this.managerWrapper = managerWrapper;
        this.sessionSenderMode = sessionSenderMode;
        this.netEventLoop = managerWrapper.getNetEventLoopManager().eventLoop();
        if (sessionSenderMode == SessionSenderMode.DIRECT) {
            sender = new DirectSender(this);
        } else if (sessionSenderMode == SessionSenderMode.UNSHARABLE) {
            sender = new UnsharableSender(this);
        } else {
            throw new IllegalArgumentException("Unsupported sessionSenderMode " + sessionSenderMode);
        }
    }

    /**
     * 获取网络配置管理器
     *
     * @return NetConfigManager
     */
    protected final NetConfigManager getNetConfigManager() {
        return managerWrapper.getNetConfigManager();
    }

    /**
     * 获取该session对应的管理器
     *
     * @return SessionManager
     */
    @Nonnull
    protected abstract SessionManager getSessionManager();

    @Override
    public final long localGuid() {
        return netContext.localGuid();
    }

    @Override
    public final RoleType localRole() {
        return netContext.localRole();
    }

    @Override
    public final long remoteGuid() {
        return remoteGuid;
    }

    @Override
    public final RoleType remoteRole() {
        return remoteRole;
    }

    @Override
    public final SessionSenderMode senderMode() {
        return sessionSenderMode;
    }

    @Override
    public void send(@Nonnull Object message) {
        sender.send(message);
    }

    @Override
    public void rpc(@Nonnull Object request, @Nonnull RpcCallback callback) {
        sender.rpc(request, callback, getNetConfigManager().rpcCallbackTimeoutMs());
    }

    @Override
    public void rpc(@Nonnull Object request, @Nonnull RpcCallback callback, long timeoutMs) {
        sender.rpc(request, callback, timeoutMs);
    }

    @Override
    @Nonnull
    public RpcResponse syncRpc(@Nonnull Object request) throws InterruptedException {
        return sender.syncRpc(request, getNetConfigManager().syncRpcTimeoutMs());
    }

    @Override
    @Nonnull
    public RpcResponse syncRpc(@Nonnull Object request, long timeoutMs) throws InterruptedException {
        return sender.syncRpc(request, timeoutMs);
    }

    @Override
    @Nonnull
    public RpcResponse syncRpcUninterruptibly(@Nonnull Object request) {
        return sender.syncRpcUninterruptibly(request, getNetConfigManager().syncRpcTimeoutMs());
    }

    @Override
    @Nonnull
    public RpcResponse syncRpcUninterruptibly(@Nonnull Object request, long timeoutMs) {
        return sender.syncRpcUninterruptibly(request, timeoutMs);
    }

    @Override
    public void flush() {
        sender.flush();
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
    public final Sender sender() {
        return sender;
    }

    // ------------------------------------------- 发送消息接口，必须运行在网络线程下 ---------------------------------------

    /**
     * 发送单向消息
     */
    final void sendOneWayMessage(@Nonnull Object message) {
        getSessionManager().sendOneWayMessage(localGuid(), remoteGuid(), message);
    }

    /**
     * 发送异步rpc请求
     *
     * @param request     请求内容
     * @param timeoutMs   超时时间
     * @param rpcCallback 回调函数
     */
    final void sendAsyncRpcRequest(@Nonnull Object request, long timeoutMs, @Nonnull RpcCallback rpcCallback) {
        getSessionManager().sendRpcRequest(localGuid(), remoteGuid(), request, timeoutMs, localEventLoop(), rpcCallback);
    }

    /**
     * 发送同步rpc请求
     *
     * @param request            请求内容
     * @param timeoutMs          超时时间
     * @param rpcResponsePromise 存储结果的promise
     */
    final void sendSyncRpcRequest(@Nonnull Object request, long timeoutMs, RpcPromise rpcResponsePromise) {
        getSessionManager().sendSyncRpcRequest(localGuid(), remoteGuid(), request, timeoutMs, rpcResponsePromise);
    }

    /**
     * 发送rpc响应
     *
     * @param requestGuid 请求对应的id
     * @param sync        是否是同步rpc请求
     * @param rpcResponse 请求对应的响应
     */
    final void sendRpcResponse(long requestGuid, boolean sync, RpcResponse rpcResponse) {
        getSessionManager().sendRpcResponse(localGuid(), remoteGuid(), requestGuid, sync, rpcResponse);
    }
}
