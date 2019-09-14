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
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.concurrent.Promise;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.net.injvm.JVMC2SSession;
import com.wjybxx.fastjgame.net.injvm.JVMPort;
import com.wjybxx.fastjgame.net.injvm.JVMS2CSession;
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
    /**
     * 所有用户的会话信息
     */
    private final Long2ObjectMap<UserInfo> userInfoMap = new Long2ObjectOpenHashMap<>();
    /**
     * 连接的所有远程线程。
     * 监听到远程线程终止后，需要删除对应的会话。
     */
    private final Set<EventLoop> remoteEventLoopSet = new HashSet<>();

    @Inject
    public JVMS2CSessionManager(NetTimeManager netTimeManager) {
        super(netTimeManager);
    }

    public void setNetManagerWrapper(NetManagerWrapper netManagerWrapper) {
        this.netManagerWrapper = netManagerWrapper;
        this.jvmc2SSessionManager = netManagerWrapper.getJvmc2SSessionManager();
    }

    public void tick() {
        for (UserInfo userInfo : userInfoMap.values()) {
            for (SessionWrapper sessionWrapper : userInfo.sessionWrapperMap.values()) {
                // 检测超时的rpc调用
                checkRpcTimeout(sessionWrapper);
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
    @Override
    protected SessionWrapper getSessionWrapper(long localGuid, long serverGuid) {
        UserInfo userInfo = userInfoMap.get(localGuid);
        if (null == userInfo) {
            return null;
        }
        return userInfo.sessionWrapperMap.get(serverGuid);
    }

    // --------------------------------------------------- 接收消息 ------------------------------------------------------

    public void onRcvOneWayMessage(long localGuid, long clientGuid, Object message) {
        final SessionWrapper sessionWrapper = getSessionWrapper(localGuid, clientGuid);
        if (sessionWrapper != null) {
            commit(sessionWrapper.getSession(), new OneWayMessageCommitTask(sessionWrapper.getSession(), sessionWrapper.getProtocolDispatcher(), message));
        }
    }

    public void onRcvRpcRequestMessage(long localGuid, long clientGuid, long requestGuid, boolean sync, Object request) {
        final SessionWrapper sessionWrapper = getSessionWrapper(localGuid, clientGuid);
        if (sessionWrapper != null) {
            commit(sessionWrapper.getSession(), new RpcRequestCommitTask(sessionWrapper.getSession(), sessionWrapper.getProtocolDispatcher(), requestGuid, sync, request));
        }
    }

    public void onRcvRpcResponse(long localGuid, long clientGuid, long requestGuid, RpcResponse rpcResponse) {
        final SessionWrapper sessionWrapper = getSessionWrapper(localGuid, clientGuid);
        if (sessionWrapper != null) {
            final RpcPromiseInfo rpcPromiseInfo = sessionWrapper.getRpcPromiseInfoMap().remove(requestGuid);
            if (null == rpcPromiseInfo) {
                // 超时之类
                return;
            }
            commitRpcResponse(sessionWrapper.getSession(), rpcPromiseInfo, rpcResponse);
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
        final JVMS2CSession session = sessionWrapper.getSession();
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

    /**
     * 创建一个JVMPort
     *
     * @param netContext  用户所属的网络环境
     * @param codec       该端口上的编解码器 - 对可变对象进行深复制
     * @param portContext 该端口上的协议处理器
     * @return jvmPort
     */
    public JVMPort bind(@Nonnull NetContext netContext, ProtocolCodec codec, PortContext portContext) {
        final JVMPort jvmPort = new JVMPortImp(netContext.localGuid(), netContext.localRole(),
                codec, portContext, netManagerWrapper.getNetEventLoopManager().eventLoop(), jvmc2SSessionManager);
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
    EventLoop tryConnect(JVMPortImp jvmPort, long clientGuid, RoleType clientRole, EventLoop remoteEventLoop) throws IOException {
        final UserInfo userInfo = userInfoMap.get(jvmPort.localGuid());
        // 用户已取消注册
        if (null == userInfo) {
            throw new IOException("remote node not exist");
        }
        // 用户关闭了该端口
        if (!userInfo.jvmPortList.contains(jvmPort)) {
            throw new IOException("jvmport not exist");
        }
        // 已存在该会话
        if (userInfo.sessionWrapperMap.containsKey(clientGuid)) {
            throw new IOException("session already exist");
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

    // 对外屏蔽实现
    static class JVMPortImp implements JVMPort {
        /**
         * 本地角色guid
         */
        private final long localGuid;
        /**
         * 本地角色类型
         */
        private final RoleType localRole;
        /**
         * 端口上的编解码器
         * 注意：它直接编解码双方的所有消息。
         */
        private final ProtocolCodec codec;
        /**
         * 该端口上的session处理逻辑
         */
        private final PortContext portContext;
        /**
         * 绑定端口的用户所属的EventLoop
         * Q: 为什么要使用绑定端口的用户的{@link NetEventLoop}?
         * A: 可以使得{@link JVMC2SSession}{@link JVMS2CSession}处于同一个{@link NetEventLoop}，可以消除不必与的同步。
         */
        private final NetEventLoop netEventLoop;
        /**
         * 建立连接的管理器
         */
        private final JVMC2SSessionManager jvmc2SSessionManager;

        private JVMPortImp(long localGuid, RoleType localRole, ProtocolCodec codec, PortContext portContext, NetEventLoop netEventLoop, JVMC2SSessionManager jvmc2SSessionManager) {
            this.codec = codec;
            this.portContext = portContext;
            this.localGuid = localGuid;
            this.localRole = localRole;
            this.netEventLoop = netEventLoop;
            this.jvmc2SSessionManager = jvmc2SSessionManager;
        }

        public ProtocolCodec getCodec() {
            return codec;
        }

        PortContext getPortContext() {
            assert netEventLoop.inEventLoop();
            return portContext;
        }

        public long localGuid() {
            return localGuid;
        }

        public RoleType localRole() {
            return localRole;
        }

        @Override
        public ListenableFuture<Session> connect(@Nonnull NetContext netContext,
                                                 @Nonnull SessionLifecycleAware lifecycleAware,
                                                 @Nonnull ProtocolDispatcher protocolDispatcher,
                                                 @Nonnull SessionSenderMode sessionSenderMode) {
            final Promise<Session> promise = netEventLoop.newPromise();
            // 注意：这里是提交到jvmPort所在的NetEventLoop, 是实现线程安全，消除同步的关键
            netEventLoop.execute(() -> {
                jvmc2SSessionManager.connect(netContext, this, lifecycleAware, protocolDispatcher, sessionSenderMode, promise);
            });
            return promise;
        }
    }

    private class SessionWrapper extends ISessionWrapper<JVMS2CSession> {

        /**
         * 会话所属的JVM端口
         */
        private final JVMPortImp jvmPort;
        /**
         * 会话另一方所在的线程(建立连接成功才会有)
         */
        private final EventLoop remoteEventLoop;

        private SessionWrapper(JVMS2CSession session, JVMPortImp jvmPort, EventLoop remoteEventLoop) {
            super(session);
            this.remoteEventLoop = remoteEventLoop;
            this.jvmPort = jvmPort;
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

        @Override
        public void sendOneWayMessage(@Nonnull Object message) {
            // 注意交换guid
            jvmc2SSessionManager.onRcvOneWayMessage(session.remoteGuid(), session.localGuid(),
                    NetUtils.cloneMessage(message, getCodec()));
        }

        @Override
        public void sendRpcRequest(long requestGuid, boolean sync, @Nonnull Object request) {
            jvmc2SSessionManager.onRcvRpcRequestMessage(session.remoteGuid(), session.localGuid(),
                    requestGuid, sync, NetUtils.cloneRpcRequest(request, getCodec()));
        }

        @Override
        public void sendRpcResponse(long requestGuid, boolean sync, @Nonnull RpcResponse response) {
            jvmc2SSessionManager.onRcvRpcResponse(session.remoteGuid(), session.localGuid(),
                    requestGuid, NetUtils.cloneRpcResponse(response, getCodec()));

        }
    }
}
