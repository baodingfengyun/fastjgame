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

package com.wjybxx.fastjgame.manager;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.Promise;
import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.net.injvm.JVMC2SSession;
import com.wjybxx.fastjgame.net.injvm.JVMPort;
import com.wjybxx.fastjgame.utils.CollectionUtils;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.FunctionUtils;
import com.wjybxx.fastjgame.utils.NetUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * JVM内部session发起方管理
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/9
 * github - https://github.com/hl845740757
 */
public class JVMC2SSessionManager extends JVMSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(JVMC2SSessionManager.class);

    private NetManagerWrapper netManagerWrapper;
    private JVMS2CSessionManager jvms2CSessionManager;

    /**
     * 所有用户的会话信息
     */
    private final Long2ObjectMap<UserInfo> userInfoMap = new Long2ObjectOpenHashMap<>();
    /**
     * 连接的所有远程线程。
     * 将监听到远程线程终止后，需要删除对应的会话。
     */
    private final Set<EventLoop> remoteEventLoopSet = new HashSet<>();

    @Inject
    public JVMC2SSessionManager(NetTimeManager netTimeManager) {
        super(netTimeManager);
    }

    /**
     * 解决环形依赖问题
     */
    public void setNetManagerWrapper(NetManagerWrapper netManagerWrapper) {
        this.netManagerWrapper = netManagerWrapper;
        this.jvms2CSessionManager = netManagerWrapper.getJvms2CSessionManager();
    }

    public void tick() {
        for (UserInfo userInfo : userInfoMap.values()) {
            for (SessionWrapper sessionWrapper : userInfo.sessionWrapperMap.values()) {
                // 检测超时的rpc调用
                CollectionUtils.removeIfAndThen(sessionWrapper.getRpcPromiseInfoMap().values(),
                        rpcPromiseInfo -> netTimeManager.getSystemMillTime() >= rpcPromiseInfo.deadline,
                        rpcPromiseInfo -> commitRpcResponse(sessionWrapper.getSession(), rpcPromiseInfo, RpcResponse.TIMEOUT));
            }
        }
    }

    /**
     * 获取session
     *
     * @param localGuid  对应的本地用户标识
     * @param serverGuid 服务器guid
     * @return 如果存在则返回对应的session，否则返回null
     */
    @Nullable
    private SessionWrapper getSessionWrapper(long localGuid, long serverGuid) {
        UserInfo userInfo = userInfoMap.get(localGuid);
        if (null == userInfo) {
            return null;
        }
        return userInfo.sessionWrapperMap.get(serverGuid);
    }
    // ---------------------------------------------------- 发送消息 -------------------------------------------------

    protected SessionWrapper getWritableSession(long localGuid, long serverGuid) {
        SessionWrapper sessionWrapper = getSessionWrapper(localGuid, serverGuid);
        // 会话已被删除
        if (null == sessionWrapper) {
            return null;
        }
        // 会话已被关闭（session关闭的状态下，既不发送，也不提交）
        if (!sessionWrapper.getSession().isActive()) {
            return null;
        }
        return sessionWrapper;
    }

    // ---------------------------------------------------- 接收消息 -------------------------------------------------

    public void onRcvOneWayMessage(long localGuid, long serverGuid, Object message) {
        final SessionWrapper sessionWrapper = getSessionWrapper(localGuid, serverGuid);
        if (sessionWrapper != null) {
            commit(sessionWrapper.getSession(), new OneWayMessageCommitTask(sessionWrapper.getSession(), sessionWrapper.protocolDispatcher, message));
        }
    }

    public void onRcvRpcRequestMessage(long localGuid, long serverGuid, long requestGuid, boolean sync, Object request) {
        final SessionWrapper sessionWrapper = getSessionWrapper(localGuid, serverGuid);
        if (sessionWrapper != null) {
            commit(sessionWrapper.getSession(), new RpcRequestCommitTask(sessionWrapper.getSession(), sessionWrapper.protocolDispatcher, requestGuid, sync, request));
        }
    }

    public void onRcvRpcResponse(long localGuid, long serverGuid, long requestGuid, RpcResponse rpcResponse) {
        final SessionWrapper sessionWrapper = getSessionWrapper(localGuid, serverGuid);
        if (sessionWrapper != null) {
            final RpcPromiseInfo rpcPromiseInfo = sessionWrapper.getRpcPromiseInfoMap().remove(requestGuid);
            if (null == rpcPromiseInfo) {
                // 超时之类
                return;
            }
            commitRpcResponse(sessionWrapper.getSession(), rpcPromiseInfo, rpcResponse);
        }
    }

    // ------------------------------------------------------- 删除会话 ---------------------------------------------------

    @Override
    public boolean removeSession(long localGuid, long serverGuid, String reason) {
        UserInfo userInfo = userInfoMap.get(localGuid);
        // 没有该用户的会话
        if (userInfo == null) {
            return true;
        }
        // 删除会话
        SessionWrapper sessionWrapper = userInfo.sessionWrapperMap.remove(serverGuid);
        if (null == sessionWrapper) {
            return true;
        }
        afterRemoved(sessionWrapper, reason, true);
        return true;
    }

    /**
     * 当会话删除之后
     */
    private void afterRemoved(SessionWrapper sessionWrapper, String reason, final boolean postEvent) {
        // 避免捕获SessionWrapper，导致内存泄漏
        final JVMC2SSession session = sessionWrapper.getSession();

        // 标记为已关闭，这里不能调用close，否则死循环了。
        session.setClosed();

        // 清理未完成的rpc请求
        cleanRpcPromiseInfo(session, sessionWrapper.detachRpcPromiseInfoMap());

        // 验证成功过才执行断开回调操作(即调用过onSessionConnected方法)
        if (postEvent && sessionWrapper.remoteEventLoop != null) {
            // 避免捕获SessionWrapper，导致内存泄漏
            final SessionDisconnectAware disconnectAware = sessionWrapper.disconnectAware;
            // 提交到用户线程
            ConcurrentUtils.tryCommit(session.localEventLoop(), () -> {
                disconnectAware.onSessionDisconnected(session);
            });
        }
        logger.info("remove session by reason of {}, session info={}.", reason, session);
    }

    @Override
    public void removeUserSession(long localGuid, String reason) {
        UserInfo userInfo = userInfoMap.remove(localGuid);
        if (null == userInfo) {
            return;
        }
        removeUserSession(userInfo, reason, true);
    }

    /**
     * 删除某个用户的所有会话，(赶脚不必发送通知)
     *
     * @param userInfo  用户信息
     * @param reason    移除会话的原因
     * @param postEvent 是否提交session移除事件
     */
    private void removeUserSession(UserInfo userInfo, String reason, final boolean postEvent) {
        CollectionUtils.removeIfAndThen(userInfo.sessionWrapperMap.values(),
                FunctionUtils::TRUE,
                sessionWrapper -> afterRemoved(sessionWrapper, reason, postEvent));
    }

    /**
     * 当用户所在的EventLoop关闭，不必上报连接断开事件，一定报不成功。
     */
    @Override
    public void onUserEventLoopTerminal(EventLoop userEventLoop) {
        CollectionUtils.removeIfAndThen(userInfoMap.values(),
                userInfo -> userInfo.netContext.localEventLoop() == userEventLoop,
                userInfo -> removeUserSession(userInfo, "onUserEventLoopTerminal", false));
    }

    /**
     * 监听到远程线程终止
     *
     * @param remoteEventLoop 远程线程终止
     */
    private void onRemoteEventLoopTerminal(EventLoop remoteEventLoop) {
        for (UserInfo userInfo : userInfoMap.values()) {
            CollectionUtils.removeIfAndThen(userInfo.sessionWrapperMap.values(),
                    sessionWrapper -> sessionWrapper.remoteEventLoop == remoteEventLoop,
                    sessionWrapper -> afterRemoved(sessionWrapper, "onRemoteEventLoopTerminal", true));
        }
    }

    // --------------------------------------------------  本地会话的实现 ------------------------------------------

    /**
     * 链接到远程。
     *
     * @param netContext         本地信息
     * @param jvmPort            另一个线程的监听信息
     * @param disconnectAware    作为客户端，链接不同的服务器时，可能有不同的生命周期事件处理
     * @param protocolDispatcher 消息处理器
     * @param sessionSenderMode  session发送消息的方式
     * @param promise            用户获取结果的future
     */
    public void connect(@Nonnull NetContext netContext,
                        @Nonnull JVMPort jvmPort,
                        @Nonnull SessionDisconnectAware disconnectAware,
                        @Nonnull ProtocolDispatcher protocolDispatcher,
                        @Nonnull SessionSenderMode sessionSenderMode,
                        @Nonnull Promise<Session> promise) {

        final long localGuid = netContext.localGuid();
        final long remoteGuid = jvmPort.localGuid();
        final RoleType remoteRole = jvmPort.localRole();

        // 会话已存在
        if (getSessionWrapper(localGuid, remoteGuid) != null) {
            promise.tryFailure(new IOException("session already registered."));
            return;
        }
        // 尝试进行连接
        try {
            final EventLoop remoteEventLoop = jvms2CSessionManager.tryConnect(jvmPort, localGuid, netContext.localRole(), netContext.localEventLoop());
            // 建立链接成功
            final UserInfo userInfo = userInfoMap.computeIfAbsent(localGuid, k -> new UserInfo(netContext));
            final JVMC2SSession session = new JVMC2SSession(netContext, remoteGuid, remoteRole, netManagerWrapper, sessionSenderMode);
            final SessionWrapper sessionWrapper = new SessionWrapper(session, jvmPort.getCodec(), disconnectAware, protocolDispatcher, remoteEventLoop);

            // 通知用户
            if (promise.trySuccess(session)) {
                // 保存会话
                userInfo.sessionWrapperMap.put(remoteGuid, sessionWrapper);
                // 监听服务端线程关闭
                if (remoteEventLoopSet.add(remoteEventLoop)) {
                    remoteEventLoop.terminationFuture().addListener(future -> {
                        onRemoteEventLoopTerminal(remoteEventLoop);
                    }, netManagerWrapper.getNetEventLoopManager().eventLoop());
                }
            }
            // else 用户可能取消了，什么也不干，session丢弃
        } catch (Exception e) {
            promise.tryFailure(e);
        }
    }

    private static class UserInfo {

        /**
         * 用户信息
         */
        private final NetContext netContext;

        /**
         * 客户端发起的所有会话，注册时加入，close时删除
         * serverGuid --> session
         */
        private final Long2ObjectMap<SessionWrapper> sessionWrapperMap = new Long2ObjectOpenHashMap<>();

        UserInfo(NetContext netContext) {
            this.netContext = netContext;
        }

    }

    private class SessionWrapper extends ISessionWrapper<JVMC2SSession> {

        /**
         * 该端口上的消息编解码器
         */
        private final ProtocolCodec codec;
        /**
         * 该会话使用的生命周期回调接口
         */
        private final SessionDisconnectAware disconnectAware;
        /**
         * 该会话关联的消息处理器
         */
        private final ProtocolDispatcher protocolDispatcher;

        /**
         * 会话另一方所在的线程(建立连接成功才会有)
         */
        private final EventLoop remoteEventLoop;

        SessionWrapper(JVMC2SSession session,
                       ProtocolCodec codec,
                       SessionDisconnectAware disconnectAware,
                       ProtocolDispatcher protocolDispatcher,
                       EventLoop remoteEventLoop) {
            super(session);
            this.codec = codec;
            this.disconnectAware = disconnectAware;
            this.protocolDispatcher = protocolDispatcher;
            this.remoteEventLoop = remoteEventLoop;
        }

        @Override
        public void sendOneWayMessage(@Nonnull Object message) {
            // 注意交换guid
            jvms2CSessionManager.onRcvOneWayMessage(session.remoteGuid(), session.localGuid(),
                    NetUtils.cloneMessage(message, codec));
        }

        @Override
        public void sendRpcRequest(long requestGuid, boolean sync, @Nonnull Object request) {
            jvms2CSessionManager.onRcvRpcRequestMessage(session.remoteGuid(), session.localGuid(),
                    requestGuid, sync, NetUtils.cloneRpcRequest(request, codec));
        }

        @Override
        public void sendRpcResponse(long requestGuid, boolean sync, @Nonnull RpcResponse response) {
            jvms2CSessionManager.onRcvRpcResponse(session.remoteGuid(), session.localGuid(),
                    requestGuid, NetUtils.cloneRpcResponse(response, codec));
        }
    }
}
