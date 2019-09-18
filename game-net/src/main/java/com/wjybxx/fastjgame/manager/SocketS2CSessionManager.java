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

package com.wjybxx.fastjgame.manager;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.misc.*;
import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.net.remote.SocketS2CSession;
import com.wjybxx.fastjgame.timer.FixedDelayHandle;
import com.wjybxx.fastjgame.utils.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.BindException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 服务器到客户端会话管理器。
 * (我接收到的连接)
 * <p>
 * 注意：请求登录、重连时，验证失败不能对当前session做任何操作，因为不能证明表示当前session有异常，
 * 只有连接成功时才能操作session。
 * <p>
 * 换句话说：有新的channel请求建立连接，不能代表旧的channel和会话有异常，有可能是新的channel是非法的。
 * <p>
 * 什么时候应该删除session？
 * 1.主动调用{@link #removeSession(long, long, String)}
 * 2.会话超时
 * 3.缓存过多
 * 4.客户端重新登录
 * <p>
 * 什么时候会关闭channel？
 * {@link #removeSession(long, long, String)} 或者说 {@link #notifyClientExit(Channel, SessionWrapper)}
 * {@link #notifyVerifyFailed(Channel, ConnectRequestEventParam, FailReason)}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 22:14
 * github - https://github.com/hl845740757
 */
public class SocketS2CSessionManager extends SocketSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(SocketS2CSessionManager.class);

    private NetManagerWrapper managerWrapper;
    private final NetTimeManager netTimeManager;
    private final AcceptorManager acceptorManager;
    /**
     * 所有用户的会话信息
     */
    private final Long2ObjectMap<UserInfo> userInfoMap = new Long2ObjectOpenHashMap<>();

    @Inject
    public SocketS2CSessionManager(NetTimeManager netTimeManager, NetConfigManager netConfigManager,
                                   NetTimerManager timerManager, AcceptorManager acceptorManager) {
        super(netConfigManager, netTimeManager);
        this.netTimeManager = netTimeManager;
        this.acceptorManager = acceptorManager;

        // 定时检查会话超时的timer(1/3个周期检测一次)
        timerManager.newFixedDelay(netConfigManager.sessionTimeout() / 3 * TimeUtils.SEC, this::checkSessionTimeout);
    }

    /**
     * 打破环形依赖
     */
    public void setManagerWrapper(NetManagerWrapper managerWrapper) {
        this.managerWrapper = managerWrapper;
    }

    public void tick() {
        for (UserInfo userInfo : userInfoMap.values()) {
            for (SessionWrapper sessionWrapper : userInfo.sessionWrapperMap.values()) {
                // 检查清空缓冲区
                sessionWrapper.flushAllUnsentMessage();
                // 检测超时的rpc调用
                checkRpcTimeout(sessionWrapper);
            }
        }
    }

    /**
     * 定时检查会话超时时间
     */
    private void checkSessionTimeout(FixedDelayHandle handle) {
        for (UserInfo userInfo : userInfoMap.values()) {
            CollectionUtils.removeIfAndThen(userInfo.sessionWrapperMap.values(),
                    sessionWrapper -> netTimeManager.getSystemSecTime() >= sessionWrapper.getSessionTimeout(),
                    sessionWrapper -> afterRemoved(sessionWrapper, "session time out!", true));
        }
    }

    /**
     * @see AcceptorManager#bindRange(String, PortRange, ChannelInitializer)
     */
    public HostAndPort bindRange(NetContext netContext, String host, PortRange portRange, ChannelInitializer<SocketChannel> initializer) throws BindException {

        final BindResult bindResult = acceptorManager.bindRange(host, portRange, initializer);

        final UserInfo userInfo = userInfoMap.computeIfAbsent(netContext.localGuid(), k -> new UserInfo(netContext));
        // 保存绑定的端口信息
        userInfo.bindResultList.add(bindResult);

        return bindResult.getHostAndPort();
    }

    /**
     * 获取session
     *
     * @param localGuid  对应的本地用户guid
     * @param clientGuid 连接的客户端的guid
     * @return 如果存在则返回对应的session，否则返回null
     */
    @Override
    protected SessionWrapper getSessionWrapper(long localGuid, long clientGuid) {
        UserInfo userInfo = userInfoMap.get(localGuid);
        if (null == userInfo) {
            return null;
        }
        return userInfo.sessionWrapperMap.get(clientGuid);
    }

    /**
     * 请求移除一个会话
     *
     * @param clientGuid remoteGuid
     * @param reason     要是可扩展的，好像只有字符串最合适
     */
    @Override
    public boolean removeSession(long localGuid, long clientGuid, String reason) {
        UserInfo userInfo = userInfoMap.get(localGuid);
        if (null == userInfo) {
            return true;
        }
        SessionWrapper sessionWrapper = userInfo.sessionWrapperMap.remove(clientGuid);
        if (null == sessionWrapper) {
            return true;
        }
        afterRemoved(sessionWrapper, reason, true);
        return true;
    }

    /**
     * 会话删除之后
     */
    private void afterRemoved(SessionWrapper sessionWrapper, String reason, boolean postEvent) {
        // 避免捕获SessionWrapper，导致内存泄漏
        final SocketS2CSession session = sessionWrapper.getSession();
        // 设置为已关闭
        session.setClosed();

        // 清理消息队列(需要先执行)
        clearMessageQueue(sessionWrapper, sessionWrapper.messageQueue);

        // 通知客户端退出(这里会关闭channel)
        notifyClientExit(sessionWrapper.getChannel(), sessionWrapper);

        if (postEvent) {
            // 尝试提交到用户线程
            ConcurrentUtils.tryCommit(session.localEventLoop(), new DisconnectAwareTask(session, sessionWrapper.getLifecycleAware()));
        }

        logger.info("remove session by reason of {}, session info={}.", reason, session);
    }

    /**
     * 删除某个用户的所有会话，(赶脚不必发送通知)
     *
     * @param localGuid 用户id
     * @param reason    移除会话的原因
     */
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
     * @param postEvent 是否上报session移除事件
     */
    private void removeUserSession(UserInfo userInfo, String reason, boolean postEvent) {
        CollectionUtils.removeIfAndThen(userInfo.sessionWrapperMap.values(),
                FunctionUtils::TRUE,
                sessionWrapper -> afterRemoved(sessionWrapper, reason, postEvent));
        // 绑定的端口需要释放
        userInfo.bindResultList.forEach(bindResult -> NetUtils.closeQuietly(bindResult.getChannel()));
    }

    /**
     * 当用户所在的EventLoop终止了，不必上报连接断开事件，一定报不成功。
     */
    @Override
    public void onUserEventLoopTerminal(EventLoop userEventLoop) {
        CollectionUtils.removeIfAndThen(userInfoMap.values(),
                userInfo -> userInfo.netContext.localEventLoop() == userEventLoop,
                userInfo -> removeUserSession(userInfo, "onUserEventLoopTerminal", false));
    }
    // ------------------------------------------------- 网络事件处理 ---------------------------------------

    /**
     * 收到客户端的连接请求(请求验证)
     *
     * @param requestParam 请求参数
     */
    void onRcvConnectRequest(ConnectRequestEventParam requestParam) {
        final Channel channel = requestParam.channel();
        // 服务器会话已经不存这里了(服务器没有监听或已经关闭) UserInfo不存在
        if (!userInfoMap.containsKey(requestParam.localGuid())) {
            notifyVerifyFailed(channel, requestParam, FailReason.SERVER_NOT_EXIST);
            return;
        }
        SessionWrapper sessionWrapper = getSessionWrapper(requestParam.localGuid(), requestParam.getClientGuid());
        if (null == sessionWrapper) {
            // 首次建立连接
            tryLogin(channel, requestParam);
        } else {
            // 尝试重连
            tryReconnect(channel, requestParam);
        }
    }

    /**
     * 无当前client对应的会话
     *
     * @param channel      产生事件的channel
     * @param requestParam 客户端发来的请求参数
     */
    private boolean tryLogin(Channel channel, ConnectRequestEventParam requestParam) {
        // 客户端已收到消息数必须为0
        if (requestParam.getAck() != MessageQueue.INIT_ACK) {
            notifyVerifyFailed(channel, requestParam, FailReason.ACK_ERROR);
            return false;
        }
        // 保存会话
        UserInfo userInfo = userInfoMap.get(requestParam.localGuid());
        final PortContext portContext = requestParam.getPortContext();

        SocketS2CSession session = new SocketS2CSession(userInfo.netContext, managerWrapper,
                requestParam.getClientGuid(), requestParam.getClientRole(), portContext.sessionSenderMode);
        SessionWrapper sessionWrapper = new SessionWrapper(session, this);
        userInfo.sessionWrapperMap.put(requestParam.getClientGuid(), sessionWrapper);

        sessionWrapper.update(channel, portContext, requestParam.getVerifyingTimes(), nextSessionTimeout());

        // 通知客户端连接成功
        notifyVerifySuccess(channel, requestParam.getVerifyingTimes(), MessageQueue.INIT_ACK);
        logger.info("client login success, sessionInfo={}", session);

        // 连接建立回调(通知)
        ConcurrentUtils.tryCommit(session.localEventLoop(), new ConnectAwareTask(session, portContext.lifecycleAware));
        return true;
    }

    private int nextSessionTimeout() {
        return netTimeManager.getSystemSecTime() + netConfigManager.sessionTimeout();
    }

    /**
     * 客户端尝试断线重连 - 存在该client的会话
     *
     * @param channel      产生事件的channel
     * @param requestParam 客户端发来的请求参数
     */
    private boolean tryReconnect(Channel channel, ConnectRequestEventParam requestParam) {
        SessionWrapper sessionWrapper = getSessionWrapper(requestParam.localGuid(), requestParam.getClientGuid());
        assert null != sessionWrapper;

        // 这是一个旧请求
        if (requestParam.getVerifyingTimes() <= sessionWrapper.getVerifyingTimes()) {
            notifyVerifyFailed(channel, requestParam, FailReason.OLD_REQUEST);
            return false;
        }
        // 判断客户端ack合法性
        MessageQueue messageQueue = sessionWrapper.getMessageQueue();
        if (!messageQueue.isAckOK(requestParam.getAck())) {
            notifyVerifyFailed(channel, requestParam, FailReason.ACK_ERROR);
            return false;
        }
        // 验证成功
        // 关闭旧channel - 如果重连时是新的channel
        if (sessionWrapper.getChannel() != channel) {
            NetUtils.closeQuietly(sessionWrapper.getChannel());
        }

        // 更新消息队列
        messageQueue.updateSentQueue(requestParam.getAck());

        // 更新状态
        sessionWrapper.update(channel, requestParam.getPortContext(), requestParam.getVerifyingTimes(), nextSessionTimeout());

        notifyVerifySuccess(channel, requestParam.getVerifyingTimes(), messageQueue.getAck());
        logger.info("client reconnect success, sessionInfo={}", sessionWrapper.getSession());

        // 重发已发送未确认的消息
        if (messageQueue.getSentQueue().size() > 0) {
            resend(channel, messageQueue);
        }
        sessionWrapper.flushAllUnsentMessage();
        return true;
    }

    /**
     * 通知客户端退出
     *
     * @param channel        会话对应的的channel
     * @param sessionWrapper 会话信息
     */
    private void notifyClientExit(Channel channel, SessionWrapper sessionWrapper) {
        notifyVerifyResult(channel, sessionWrapper.getVerifyingTimes(), false, -1);
    }

    /**
     * 通知客户端验证失败
     * 注意验证失败，不能认定当前会话失效，可能是错误或非法的连接，因此不能对会话下手
     *
     * @param channel      判断不通过的channel
     * @param requestParam 请求参数
     * @param failReason   失败原因，用于记录日志
     */
    private void notifyVerifyFailed(Channel channel, ConnectRequestEventParam requestParam, FailReason failReason) {
        notifyVerifyResult(channel, requestParam.getVerifyingTimes(), false, -1);
        logger.warn("client {} verify failed by reason of {}", requestParam.getClientGuid(), failReason);
    }

    /**
     * 通知客户端验证成功
     *
     * @param channel        判断不通过的channel
     * @param verifyingTimes 这是客户端的第几次请求
     * @param ack            服务器的捎带确认ack
     */
    private void notifyVerifySuccess(Channel channel, int verifyingTimes, long ack) {
        notifyVerifyResult(channel, verifyingTimes, true, ack);
    }

    /**
     * 通知客户端验证结果
     *
     * @param channel        发起连接请求的channel
     * @param verifyingTimes 这是客户端的第几次请求
     * @param success        是否成功
     * @param ack            服务器的ack
     */
    private void notifyVerifyResult(Channel channel, int verifyingTimes, boolean success, long ack) {
        ConnectResponseTO connectResponse = new ConnectResponseTO(verifyingTimes, success, ack);
        // 这里需要监听结果，不可以使用voidPromise
        ChannelFuture future = channel.writeAndFlush(connectResponse);
        // 验证失败情况下，发送之后，关闭channel
        if (!success) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * 尝试用message更新消息队列
     *
     * @param eventParam 消息参数
     * @param then       当且仅当message是当前channel上期望的下一个消息，且ack合法时执行。
     */
    private <T extends MessageEventParam> void tryUpdateMessageQueue(T eventParam, Consumer<SessionWrapper> then) {
        SessionWrapper sessionWrapper = getSessionWrapper(eventParam.localGuid(), eventParam.remoteGuid());
        Channel eventChannel = eventParam.channel();
        if (null == sessionWrapper) {
            NetUtils.closeQuietly(eventChannel);
            return;
        }
        // 必须是相同的channel (isEventChannelOk)
        if (eventChannel != sessionWrapper.getChannel()) {
            NetUtils.closeQuietly(eventChannel);
            return;
        }
        // 更新session超时时间
        sessionWrapper.setSessionTimeout(nextSessionTimeout());

        MessageQueue messageQueue = sessionWrapper.getMessageQueue();
        // 不是期望的下一个消息
        if (eventParam.getSequence() != messageQueue.getAck() + 1) {
            return;
        }
        // 客户端发来的ack错误
        if (!messageQueue.isAckOK(eventParam.getAck())) {
            return;
        }
        // 更新消息队列
        messageQueue.setAck(eventParam.getSequence());
        messageQueue.updateSentQueue(eventParam.getAck());

        // 然后执行自己的逻辑
        then.accept(sessionWrapper);
    }

    /**
     * 当接收到客户端发来的rpc请求时
     *
     * @param rpcRequestEventParam rpc请求参数
     */
    void onRcvClientRpcRequest(RpcRequestEventParam rpcRequestEventParam) {
        tryUpdateMessageQueue(rpcRequestEventParam, sessionWrapper -> {
            RpcRequestCommitTask requestCommitTask = new RpcRequestCommitTask(sessionWrapper.getSession(), sessionWrapper.getProtocolDispatcher(),
                    rpcRequestEventParam.getRequestGuid(), rpcRequestEventParam.isSync(), rpcRequestEventParam.getRequest());
            commit(sessionWrapper.getSession(), requestCommitTask);
        });
    }

    /**
     * 当收到发送给客户端的rpc的响应时
     *
     * @param rpcResponseEventParam rpc响应
     */
    void onRcvClientRpcResponse(RpcResponseEventParam rpcResponseEventParam) {
        tryUpdateMessageQueue(rpcResponseEventParam, sessionWrapper -> {
            RpcPromiseInfo rpcPromiseInfo = sessionWrapper.getRpcPromiseInfoMap().remove(rpcResponseEventParam.getRequestGuid());
            if (null != rpcPromiseInfo) {
                commitRpcResponse(sessionWrapper.getSession(), rpcPromiseInfo, rpcResponseEventParam.getRpcResponse());
            }
            // else 可能超时了
        });
    }

    /**
     * 当接收到客户端发送的单向消息时
     */
    void onRcvClientOneWayMsg(OneWayMessageEventParam oneWayMessageEventParam) {
        // 减少lambda表达式捕获的对象
        final Object message = oneWayMessageEventParam.getMessage();
        tryUpdateMessageQueue(oneWayMessageEventParam, sessionWrapper -> {
            commit(sessionWrapper.getSession(), new OneWayMessageCommitTask(sessionWrapper.getSession(), sessionWrapper.getProtocolDispatcher(), message));
        });
    }

    /**
     * 收到客户端的定时Ack-ping包
     *
     * @param ackPingParam 心跳包参数
     */
    void onRcvClientAckPing(AckPingPongEventParam ackPingParam) {
        tryUpdateMessageQueue(ackPingParam, sessionWrapper -> {
            // ack心跳包立即返回
            sessionWrapper.writeAndFlush(new AckPingPongMessage());
        });
    }
    // -------------------------------------------------- 内部封装 -------------------------------------------

    private static final class UserInfo {

        // ------------- 会话关联的本地对象 -----------------
        /**
         * 用户信息
         */
        private final NetContext netContext;
        /**
         * 该用户绑定的所有端口，关联的channel需要在用户删除后关闭
         */
        private final List<BindResult> bindResultList = new ArrayList<>(4);
        /**
         * 该用户关联的所有会话信息
         */
        private final Long2ObjectMap<SessionWrapper> sessionWrapperMap = new Long2ObjectOpenHashMap<>();

        private UserInfo(NetContext netContext) {
            this.netContext = netContext;
        }
    }

    /**
     * S2CSession的包装类，不对外暴露细节
     */
    private static final class SessionWrapper extends SocketSessionWrapper<SocketS2CSession> {
        /**
         * 该会话所属的端口
         */
        private PortContext portContext;
        /**
         * 会话channel一定不为null
         */
        private Channel channel;
        /**
         * 会话过期时间(秒)(时间到则需要移除)
         */
        private int sessionTimeout;
        /**
         * 客户端发起连接请求的次数
         */
        private int verifyingTimes;
        /**
         * 不想声明为内部类
         */
        private final SocketS2CSessionManager socketS2CSessionManager;

        SessionWrapper(SocketS2CSession session, SocketS2CSessionManager socketS2CSessionManager) {
            super(session);
            this.socketS2CSessionManager = socketS2CSessionManager;
        }

        Channel getChannel() {
            return channel;
        }

        int getSessionTimeout() {
            return sessionTimeout;
        }

        int getVerifyingTimes() {
            return verifyingTimes;
        }

        /**
         * 更新sessionWrapper的信息
         *
         * @param channel        新的channel
         * @param portContext    所在端口的上下文
         * @param verifyingTimes 这是对客户端第几次验证
         * @param sessionTimeout 会话超时时间
         */
        void update(Channel channel, PortContext portContext, int verifyingTimes, int sessionTimeout) {
            this.channel = channel;
            this.portContext = portContext;
            this.sessionTimeout = sessionTimeout;
            this.verifyingTimes = verifyingTimes;
        }

        void setSessionTimeout(int sessionTimeout) {
            this.sessionTimeout = sessionTimeout;
        }

        /**
         * 检查是否需要清空缓冲区
         */
        void flushAllUnsentMessage() {
            if (messageQueue.getUnsentQueue().size() > 0) {
                socketS2CSessionManager.flushAllUnsentMessage(channel, messageQueue);
            }
        }

        SessionLifecycleAware getLifecycleAware() {
            return portContext.lifecycleAware;
        }

        ProtocolDispatcher getProtocolDispatcher() {
            return portContext.protocolDispatcher;
        }

        /**
         * 写入数据到缓存，达到阈值时，缓冲区会自动清空
         *
         * @param unsentMessage 待发送的消息
         */
        protected void write(NetMessage unsentMessage) {
            socketS2CSessionManager.write(channel, messageQueue, unsentMessage);
        }

        /**
         * 立即发送一个消息
         *
         * @param unsentMessage 待发送的消息
         */
        protected void writeAndFlush(NetMessage unsentMessage) {
            socketS2CSessionManager.writeAndFlush(channel, messageQueue, unsentMessage);
        }
    }

}
