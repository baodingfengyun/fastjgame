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
import com.wjybxx.fastjgame.misc.LongSequencer;
import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.net.injvm.JVMPort;
import com.wjybxx.fastjgame.net.injvm.JVMS2CSession;
import com.wjybxx.fastjgame.utils.CollectionUtils;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.FunctionUtils;
import com.wjybxx.fastjgame.utils.NetUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 监听方session管理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/9
 * github - https://github.com/hl845740757
 */
public class JVMS2CSessionManager extends JVMSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(JVMS2CSessionManager.class);

    private NetManagerWrapper netManagerWrapper;
    private JVMC2SSessionManager jvmc2SSessionManager;
    private final NetTimerManager netTimerManager;
    private final NetTimeManager netTimeManager;
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
    public JVMS2CSessionManager(NetTimerManager netTimerManager, NetTimeManager netTimeManager) {
        this.netTimerManager = netTimerManager;
        this.netTimeManager = netTimeManager;
    }

    public void setNetManagerWrapper(NetManagerWrapper netManagerWrapper) {
        this.netManagerWrapper = netManagerWrapper;
        this.jvmc2SSessionManager = netManagerWrapper.getJvmc2SSessionManager();
    }

    public void tick() {
        for (UserInfo userInfo : userInfoMap.values()) {
            for (SessionWrapper sessionWrapper : userInfo.sessionWrapperMap.values()) {
                // 检测超时的rpc调用
                CollectionUtils.removeIfAndThen(sessionWrapper.rpcPromiseInfoMap.values(),
                        rpcPromiseInfo -> netTimeManager.getSystemMillTime() >= rpcPromiseInfo.deadline,
                        rpcPromiseInfo -> commitRpcResponse(sessionWrapper.session, rpcPromiseInfo, RpcResponse.TIMEOUT));
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

    // ---------------------------------------------------- 发送消息 -------------------------------------------
    private SessionWrapper getWritableSession(long localGuid, long serverGuid) {
        SessionWrapper sessionWrapper = getSessionWrapper(localGuid, serverGuid);
        // 会话已被删除
        if (null == sessionWrapper) {
            return null;
        }
        // 会话已被关闭（session关闭的状态下，既不发送，也不提交）
        if (!sessionWrapper.session.isActive()) {
            return null;
        }
        return sessionWrapper;
    }


    @Override
    public void sendOneWayMessage(long localGuid, long remoteGuid, @Nonnull Object message) {
        final SessionWrapper sessionWrapper = getWritableSession(localGuid, remoteGuid);
        if (null != sessionWrapper) {
            // 注意需要交换guid
            jvmc2SSessionManager.onRcvOneWayMessage(remoteGuid, localGuid, NetUtils.cloneMessage(message, sessionWrapper.getCodec()));
        }
    }

    @Override
    public void sendRpcRequest(long localGuid, long remoteGuid, @Nonnull Object request, long timeoutMs, EventLoop userEventLoop, RpcCallback rpcCallback) {
        final SessionWrapper sessionWrapper = getWritableSession(localGuid, remoteGuid);
        if (null != sessionWrapper) {
            // 创建rpc对应的上下文
            long rpcRequestGuid = sessionWrapper.nextRpcRequestGuid();
            long deadline = netTimeManager.getSystemMillTime() + timeoutMs;
            RpcPromiseInfo rpcPromiseInfo = RpcPromiseInfo.newInstance(rpcCallback, deadline);
            sessionWrapper.rpcPromiseInfoMap.put(remoteGuid, rpcPromiseInfo);

            jvmc2SSessionManager.onRcvRpcRequestMessage(remoteGuid, localGuid, rpcRequestGuid, false, NetUtils.cloneRpcRequest(request, sessionWrapper.getCodec()));
        } else {
            ConcurrentUtils.tryCommit(userEventLoop, () -> {
                rpcCallback.onComplete(RpcResponse.SESSION_CLOSED);
            });
        }
    }

    @Override
    public void sendSyncRpcRequest(long localGuid, long remoteGuid, @Nonnull Object request, long timeoutMs, RpcPromise rpcPromise) {
        final SessionWrapper sessionWrapper = getWritableSession(localGuid, remoteGuid);
        if (null != sessionWrapper) {
            // 创建rpc对应的上下文
            long deadline = netTimeManager.getSystemMillTime() + timeoutMs;
            RpcPromiseInfo rpcPromiseInfo = RpcPromiseInfo.newInstance(rpcPromise, deadline);
            long requestGuid = sessionWrapper.nextRpcRequestGuid();
            sessionWrapper.rpcPromiseInfoMap.put(requestGuid, rpcPromiseInfo);

            jvmc2SSessionManager.onRcvRpcRequestMessage(remoteGuid, localGuid, requestGuid, true, NetUtils.cloneRpcRequest(request, sessionWrapper.getCodec()));
        } else {
            rpcPromise.trySuccess(RpcResponse.SESSION_CLOSED);
        }
    }

    @Override
    public void sendRpcResponse(long localGuid, long remoteGuid, long requestGuid, boolean sync, @Nonnull RpcResponse response) {
        final SessionWrapper sessionWrapper = getWritableSession(localGuid, remoteGuid);
        if (null != sessionWrapper) {
            // 注意需要交换guid
            jvmc2SSessionManager.onRcvRpcResponse(remoteGuid, localGuid, requestGuid, NetUtils.cloneRpcResponse(response, sessionWrapper.getCodec()));
        }
    }

    // --------------------------------------------------- 接收消息 ------------------------------------------------------

    public void onRcvOneWayMessage(long localGuid, long clientGuid, Object message) {
        final SessionWrapper sessionWrapper = getSessionWrapper(localGuid, clientGuid);
        if (sessionWrapper != null) {
            // 避免捕获错误的对象
            final JVMS2CSession session = sessionWrapper.session;
            commit(session, new OneWayMessageCommitTask(session, sessionWrapper.getProtocolDispatcher(), message));
        }
    }

    public void onRcvRpcRequestMessage(long localGuid, long clientGuid, long requestGuid, boolean sync, Object request) {
        final SessionWrapper sessionWrapper = getSessionWrapper(localGuid, clientGuid);
        if (sessionWrapper != null) {
            // 避免捕获错误的对象
            final JVMS2CSession session = sessionWrapper.session;
            commit(session, new RpcRequestCommitTask(session, sessionWrapper.getProtocolDispatcher(), requestGuid, sync, request));
        }
    }

    public void onRcvRpcResponse(long localGuid, long clientGuid, long requestGuid, RpcResponse rpcResponse) {
        final SessionWrapper sessionWrapper = getSessionWrapper(localGuid, clientGuid);
        if (sessionWrapper != null) {
            final RpcPromiseInfo rpcPromiseInfo = sessionWrapper.rpcPromiseInfoMap.remove(requestGuid);
            if (null == rpcPromiseInfo) {
                // 超时之类
                return;
            }
            commitRpcResponse(sessionWrapper.session, rpcPromiseInfo, rpcResponse);
        }
    }

    // --------------------------------------------------- session删除 -----------------------------------------------------
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
        final JVMS2CSession session = sessionWrapper.session;
        // 标记为已关闭，这里不能调用close，否则死循环了。
        session.setClosed();

        // 清理未完成的rpc请求
        cleanRpcPromiseInfo(session, sessionWrapper.detachRpcPromiseInfoMap());

        if (postEvent) {
            // 避免捕获SessionWrapper，导致内存泄漏
            final SessionLifecycleAware lifecycleAware = sessionWrapper.getLifecycleAware();
            // 提交到用户线程
            ConcurrentUtils.tryCommit(session.localEventLoop(), () -> {
                lifecycleAware.onSessionDisconnected(session);
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
     * 删除某个用户的所有会话
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

    // ----------------------------------------  监听和建立连接 ---------------------------------------------------


    public JVMPort bind(@Nonnull NetContext netContext, ProtocolCodec codec, PortContext portContext) {
        final JVMPort jvmPort = new JVMPort(netContext.localGuid(), netContext.localRole(), codec, portContext, netManagerWrapper);
        final UserInfo userInfo = userInfoMap.computeIfAbsent(netContext.localGuid(), k -> new UserInfo(netContext));
        userInfo.jvmPortList.add(jvmPort);
        return jvmPort;
    }

    /**
     * 尝试建立链接
     *
     * @param jvmPort         要连接的JVM内部端口
     * @param clientGuid      客户端guid
     * @param clientRole      客户端角色类型
     * @param remoteEventLoop 客户端所在线程
     * @return 监听方的eventLoop
     */
    @Nullable
    public EventLoop tryConnect(JVMPort jvmPort, long clientGuid, RoleType clientRole, EventLoop remoteEventLoop) {
        final UserInfo userInfo = userInfoMap.get(jvmPort.localGuid());
        // 用户已取消注册
        if (null == userInfo) {
            return null;
        }
        // 用户关闭了该端口
        if (!userInfo.jvmPortList.contains(jvmPort)) {
            return null;
        }
        // 已存在该会话
        if (userInfo.sessionWrapperMap.containsKey(clientGuid)) {
            throw new IllegalArgumentException("client " + clientGuid + " already connected!");
        }
        final PortContext portContext = jvmPort.getPortContext();
        final JVMS2CSession jvms2CSession = new JVMS2CSession(userInfo.netContext, clientGuid, clientRole, netManagerWrapper, portContext.sessionSenderMode);
        SessionWrapper sessionWrapper = new SessionWrapper(jvms2CSession, jvmPort, remoteEventLoop);
        userInfo.sessionWrapperMap.put(clientGuid, sessionWrapper);

        // 建立会话成功
        final SessionLifecycleAware lifecycleAware = portContext.lifecycleAware;
        ConcurrentUtils.tryCommit(jvms2CSession.localEventLoop(), () -> {
            lifecycleAware.onSessionConnected(jvms2CSession);
        });

        // 监听客户端线程关闭
        if (remoteEventLoopSet.add(remoteEventLoop)) {
            remoteEventLoop.terminationFuture().addListener(future -> {
                onRemoteEventLoopTerminal(remoteEventLoop);
            }, netManagerWrapper.getNetEventLoopManager().eventLoop());
        }

        return jvms2CSession.localEventLoop();
    }

    // -------------------------------------

    private static class UserInfo {

        /**
         * 用户信息
         */
        private final NetContext netContext;

        /**
         * 用户绑定的所有端口
         */
        private final List<JVMPort> jvmPortList = new ArrayList<>(4);

        /**
         * 客户端发起的所有会话，注册时加入，close时删除
         * guid --> session
         */
        private final Long2ObjectMap<SessionWrapper> sessionWrapperMap = new Long2ObjectOpenHashMap<>();

        UserInfo(NetContext netContext) {
            this.netContext = netContext;
        }

    }

    private static class SessionWrapper {

        /**
         * 客户端与服务器之间的会话信息
         */
        private final JVMS2CSession session;
        /**
         * 会话所属的JVM端口
         */
        private final JVMPort jvmPort;
        /**
         * 会话另一方所在的线程(建立连接成功才会有)
         */
        private final EventLoop remoteEventLoop;
        /**
         * RpcRequestGuid分配器
         */
        private final LongSequencer rpcRequestGuidSequencer = new LongSequencer(0);
        /**
         * 当前会话上的rpc请求
         */
        private Long2ObjectMap<RpcPromiseInfo> rpcPromiseInfoMap = new Long2ObjectLinkedOpenHashMap<>();

        public SessionWrapper(JVMS2CSession session, JVMPort jvmPort, EventLoop remoteEventLoop) {
            this.session = session;
            this.remoteEventLoop = remoteEventLoop;
            this.jvmPort = jvmPort;
        }

        public long nextRpcRequestGuid() {
            return rpcRequestGuidSequencer.incAndGet();
        }

        /**
         * 删除rpcPromiseInfoMap并返回
         */
        Long2ObjectMap<RpcPromiseInfo> detachRpcPromiseInfoMap() {
            Long2ObjectMap<RpcPromiseInfo> result = rpcPromiseInfoMap;
            rpcPromiseInfoMap = null;
            return result;
        }

        ProtocolCodec getCodec() {
            return jvmPort.getCodec();
        }

        SessionLifecycleAware getLifecycleAware() {
            return jvmPort.getPortContext().lifecycleAware;
        }

        ProtocolDispatcher getProtocolDispatcher() {
            return jvmPort.getPortContext().protocolDispatcher;
        }
    }
}
