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
import com.wjybxx.fastjgame.net.injvm.JVMC2SSession;
import com.wjybxx.fastjgame.net.injvm.JVMPort;
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
    private final NetTimeManager netTimeManager;
    private final NetTimerManager timerManager;
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
    public JVMC2SSessionManager(NetTimeManager netTimeManager, NetTimerManager timerManager) {
        this.netTimeManager = netTimeManager;
        this.timerManager = timerManager;
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
    // ---------------------------------------------------- 发送消息 -------------------------------------------------

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
            jvms2CSessionManager.onRcvOneWayMessage(remoteGuid, localGuid, NetUtils.cloneMessage(message, sessionWrapper.codec));
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

            jvms2CSessionManager.onRcvRpcRequestMessage(remoteGuid, localGuid, rpcRequestGuid, false, NetUtils.cloneRpcRequest(request, sessionWrapper.codec));
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

            jvms2CSessionManager.onRcvRpcRequestMessage(remoteGuid, localGuid, requestGuid, true, NetUtils.cloneRpcRequest(request, sessionWrapper.codec));
        } else {
            rpcPromise.trySuccess(RpcResponse.SESSION_CLOSED);
        }
    }

    @Override
    public void sendRpcResponse(long localGuid, long remoteGuid, long requestGuid, boolean sync, @Nonnull RpcResponse response) {
        final SessionWrapper sessionWrapper = getWritableSession(localGuid, remoteGuid);
        if (null != sessionWrapper) {
            jvms2CSessionManager.onRcvRpcResponse(remoteGuid, localGuid, requestGuid, NetUtils.cloneRpcResponse(response, sessionWrapper.codec));
        }
    }

    // ---------------------------------------------------- 接收消息 -------------------------------------------------

    public void onRcvOneWayMessage(long localGuid, long serverGuid, Object message) {
        final SessionWrapper sessionWrapper = getSessionWrapper(localGuid, serverGuid);
        if (sessionWrapper != null) {
            // 避免捕获错误的对象
            final JVMC2SSession session = sessionWrapper.session;
            commit(session, new OneWayMessageCommitTask(session, sessionWrapper.protocolDispatcher, message));
        }
    }

    public void onRcvRpcRequestMessage(long localGuid, long serverGuid, long requestGuid, boolean sync, Object request) {
        final SessionWrapper sessionWrapper = getSessionWrapper(localGuid, serverGuid);
        if (sessionWrapper != null) {
            // 避免捕获错误的对象
            final JVMC2SSession session = sessionWrapper.session;
            commit(session, new RpcRequestCommitTask(session, sessionWrapper.protocolDispatcher, requestGuid, sync, request));
        }
    }

    public void onRcvRpcResponse(long localGuid, long serverGuid, long requestGuid, RpcResponse rpcResponse) {
        final SessionWrapper sessionWrapper = getSessionWrapper(localGuid, serverGuid);
        if (sessionWrapper != null) {
            final RpcPromiseInfo rpcPromiseInfo = sessionWrapper.rpcPromiseInfoMap.remove(requestGuid);
            if (null == rpcPromiseInfo) {
                // 超时之类
                return;
            }
            commitRpcResponse(sessionWrapper.session, rpcPromiseInfo, rpcResponse);
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
        final JVMC2SSession session = sessionWrapper.session;

        // 标记为已关闭，这里不能调用close，否则死循环了。
        session.setClosed();

        // 清理未完成的rpc请求
        cleanRpcPromiseInfo(session, sessionWrapper.detachRpcPromiseInfoMap());

        // 验证成功过才执行断开回调操作(即调用过onSessionConnected方法)
        if (postEvent && sessionWrapper.remoteEventLoop != null) {
            // 避免捕获SessionWrapper，导致内存泄漏
            final SessionLifecycleAware lifecycleAware = sessionWrapper.lifecycleAware;
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
     * @param lifecycleAware     作为客户端，链接不同的服务器时，可能有不同的生命周期事件处理
     * @param protocolDispatcher 消息处理器
     * @param sessionSenderMode  session发送消息的方式
     */
    public void connect(@Nonnull NetContext netContext,
                        @Nonnull JVMPort jvmPort,
                        @Nonnull SessionLifecycleAware lifecycleAware,
                        @Nonnull ProtocolDispatcher protocolDispatcher,
                        @Nonnull SessionSenderMode sessionSenderMode) {

        final long localGuid = netContext.localGuid();
        final long remoteGuid = jvmPort.localGuid();
        final RoleType remoteRole = jvmPort.localRole();

        if (getSessionWrapper(localGuid, remoteGuid) != null) {
            throw new IllegalArgumentException("session localGuid " + localGuid + "- remoteGuid " + remoteGuid + " registered before.");
        }

        final UserInfo userInfo = userInfoMap.computeIfAbsent(localGuid, k -> new UserInfo(netContext));
        final JVMC2SSession localSession = new JVMC2SSession(netContext, remoteGuid, remoteRole, netManagerWrapper, sessionSenderMode);
        final SessionWrapper sessionWrapper = new SessionWrapper(localSession, jvmPort.getCodec(), lifecycleAware, protocolDispatcher);
        // 先占坑
        userInfo.sessionWrapperMap.put(remoteGuid, sessionWrapper);

        // 为何要延迟执行？
        // 必须保证时序 - session建立成功必须在该方法返回之后
        timerManager.nextTick(handle -> doConnect(jvmPort, sessionWrapper));
    }

    /**
     * 延迟执行连接操作
     *
     * @param jvmPort        目标“端口”
     * @param sessionWrapper timer关联的sessionWrapper，必须校验！
     */
    private void doConnect(JVMPort jvmPort, SessionWrapper sessionWrapper) {
        final JVMC2SSession session = sessionWrapper.session;
        final SessionWrapper curSessionWrapper = getSessionWrapper(session.localGuid(), session.remoteGuid());
        if (curSessionWrapper != sessionWrapper) {
            // 被删除了 或 重新发起了请求(产生了改变，该session需要丢弃)
            return;
        }
        final EventLoop remoteEventLoop = jvms2CSessionManager.tryConnect(jvmPort, session.localGuid(), session.localRole(), session.localEventLoop());
        if (null != remoteEventLoop) {
            // 建立链接成功
            session.tryActive();
            sessionWrapper.remoteEventLoop = remoteEventLoop;

            // 进行通知 - 局部变量，避免lambda表达式捕获sessionWrapper
            final SessionLifecycleAware lifecycleAware = sessionWrapper.lifecycleAware;
            ConcurrentUtils.tryCommit(session.localEventLoop(), () -> {
                lifecycleAware.onSessionConnected(session);
            });

            // 监听服务端线程关闭
            if (remoteEventLoopSet.add(remoteEventLoop)) {
                remoteEventLoop.terminationFuture().addListener(future -> {
                    onRemoteEventLoopTerminal(remoteEventLoop);
                }, netManagerWrapper.getNetEventLoopManager().eventLoop());
            }
        } else {
            removeSession(session.localGuid(), session.remoteGuid(), "connect failure");
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

    private static class SessionWrapper {

        /**
         * 客户端与服务器之间的会话信息
         */
        private final JVMC2SSession session;
        /**
         * 该端口上的消息编解码器
         */
        private final ProtocolCodec codec;
        /**
         * 该会话使用的生命周期回调接口
         */
        private final SessionLifecycleAware lifecycleAware;
        /**
         * 该会话关联的消息处理器
         */
        private final ProtocolDispatcher protocolDispatcher;

        /**
         * RpcRequestGuid分配器
         */
        private final LongSequencer rpcRequestGuidSequencer = new LongSequencer(0);

        /**
         * 当前会话上的rpc请求
         */
        private Long2ObjectMap<RpcPromiseInfo> rpcPromiseInfoMap = new Long2ObjectLinkedOpenHashMap<>();

        /**
         * 会话另一方所在的线程(建立连接成功才会有)
         */
        private EventLoop remoteEventLoop;

        public SessionWrapper(JVMC2SSession session, ProtocolCodec codec, SessionLifecycleAware lifecycleAware, ProtocolDispatcher protocolDispatcher) {
            this.session = session;
            this.codec = codec;
            this.lifecycleAware = lifecycleAware;
            this.protocolDispatcher = protocolDispatcher;
        }

        public long nextRpcRequestGuid() {
            return rpcRequestGuidSequencer.incAndGet();
        }

        /**
         * 删除rpcPromiseInfoMap并返回
         */
        public Long2ObjectMap<RpcPromiseInfo> detachRpcPromiseInfoMap() {
            Long2ObjectMap<RpcPromiseInfo> result = rpcPromiseInfoMap;
            rpcPromiseInfoMap = null;
            return result;
        }

    }
}
