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
import com.wjybxx.fastjgame.concurrent.Promise;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.IntSequencer;
import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.net.initializer.ChannelInitializerFactory;
import com.wjybxx.fastjgame.net.initializer.ChannelInitializerSupplier;
import com.wjybxx.fastjgame.net.remote.SocketC2SSession;
import com.wjybxx.fastjgame.utils.CollectionUtils;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.FunctionUtils;
import com.wjybxx.fastjgame.utils.NetUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * 客户端到服务器的会话控制器
 * (我发起的连接)
 * <p>
 * 客户端什么时候断开连接？
 * 1.服务器通知验证失败(服务器让我移除)
 * 2.外部调用{@link #removeSession(long, long, String)}
 * 3.消息缓存数超过限制
 * 4.限定时间内无法连接到服务器
 * 5.验证结果表明服务器的sequence和ack异常时（重连无法恢复到正常状态时）。
 * <p>
 * 什么时候会关闭channel？
 * {@link #removeSession(long, long, String)}
 * {@link C2SSessionState#closeChannel()}
 * {@link ConnectedState#reconnect(String)}
 * <p>
 * 该管理器不是线程安全的，每个{@link NetEventLoop}一个。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 22:10
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class SocketC2SSessionManager extends SocketSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(SocketC2SSessionManager.class);

    private NetManagerWrapper managerWrapper;
    private final AcceptorManager acceptorManager;
    private final TokenManager tokenManager;
    private final NetTimerManager timerManager;

    /**
     * 所有用户的会话信息
     */
    private final Long2ObjectMap<UserInfo> userInfoMap = new Long2ObjectOpenHashMap<>();

    @Inject
    public SocketC2SSessionManager(NetConfigManager netConfigManager, AcceptorManager acceptorManager,
                                   NetTimeManager netTimeManager, TokenManager tokenManager, NetTimerManager timerManager) {
        super(netConfigManager, netTimeManager);
        this.acceptorManager = acceptorManager;
        this.tokenManager = tokenManager;
        this.timerManager = timerManager;
    }

    /**
     * 解决循环依赖
     */
    public void setManagerWrapper(NetManagerWrapper managerWrapper) {
        this.managerWrapper = managerWrapper;
    }

    public void tick() {
        for (UserInfo userInfo : userInfoMap.values()) {
            for (SessionWrapper sessionWrapper : userInfo.sessionWrapperMap.values()) {
                // 状态机刷帧，状态机刷帧应避免直接删除
                if (sessionWrapper.getState() != null) {
                    sessionWrapper.getState().execute();
                }
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
    private SessionWrapper getSessionWrapper(long localGuid, long serverGuid) {
        UserInfo userInfo = userInfoMap.get(localGuid);
        if (null == userInfo) {
            return null;
        }
        return userInfo.sessionWrapperMap.get(serverGuid);
    }

    /**
     * 链接到远程。
     *
     * @param netContext          本地信息
     * @param serverGuid          在登录服或别处获得的serverGuid
     * @param serverType          服务器类型
     * @param hostAndPort         服务器地址
     * @param initializerSupplier 初始化器提供者，如果initializer是线程安全的，可以始终返回同一个对象
     * @param lifecycleAware      生命周期事件处理
     * @param protocolDispatcher  消息处理器
     * @param sessionSenderMode   session发送消息的方式
     * @param promise             传递给用户结果的promise
     */
    public void connect(@Nonnull NetContext netContext, long serverGuid, RoleType serverType, HostAndPort hostAndPort,
                        @Nonnull ChannelInitializerSupplier initializerSupplier,
                        @Nonnull SessionLifecycleAware lifecycleAware,
                        @Nonnull ProtocolDispatcher protocolDispatcher,
                        @Nonnull SessionSenderMode sessionSenderMode,
                        @Nonnull Promise<Session> promise) {
        long localGuid = netContext.localGuid();
        // 已注册
        if (getSessionWrapper(localGuid, serverGuid) != null) {
            throw new IllegalArgumentException("session localGuid " + localGuid + "- serverGuid " + serverGuid + " registered before.");
        }
        // 保存用户信息，因为是发起连接请求，因此方法参数都是针对单个会话的。
        UserInfo userInfo = userInfoMap.computeIfAbsent(localGuid, k -> new UserInfo(netContext));

        // 创建会话
        SocketC2SSession session = new SocketC2SSession(netContext, managerWrapper, serverGuid, serverType, hostAndPort, sessionSenderMode);
        byte[] encryptedLoginToken = tokenManager.newEncryptedLoginToken(netContext.localGuid(), netContext.localRole(), serverGuid, serverType);
        SessionWrapper sessionWrapper = new SessionWrapper(userInfo, initializerSupplier, lifecycleAware, protocolDispatcher, session, encryptedLoginToken, promise);

        // 保存会话
        userInfo.sessionWrapperMap.put(session.remoteGuid(), sessionWrapper);
        // 初始为连接状态
        changeState(sessionWrapper, new ConnectingState(sessionWrapper));

        // 监听用户取消
        promise.addListener(future -> {
            if (future.isCancelled()) {
                removeSession(localGuid, serverGuid, "user cancelled");
            }
        }, managerWrapper.getNetEventLoopManager().eventLoop());
    }

    /**
     * 获取一个可写的session。
     *
     * @param localGuid  form
     * @param serverGuid to
     * @return sessionWrapper
     */
    @Nullable
    @Override
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
        // 缓存需要排除正常缓存阈值
        if (sessionWrapper.getMessageQueue().getCacheMessageNum() >= netConfigManager.clientMaxCacheNum() + netConfigManager.flushThreshold()) {
            // 缓存过多，删除会话
            removeSession(localGuid, serverGuid, "cacheMessageNum is too much!");
            return null;
        } else {
            return sessionWrapper;
        }
    }

    /**
     * 关闭一个会话，如果注册了的话
     *
     * @param localGuid  本地用户标识
     * @param serverGuid 远程节点标识
     */
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
        final SocketC2SSession session = sessionWrapper.getSession();
        // 标记为已关闭，这里不能调用close，否则死循环了。
        session.setClosed();

        // 移除之前进行必要的清理
        if (sessionWrapper.getState() != null) {
            sessionWrapper.getState().closeChannel();
            sessionWrapper.setState(null);
        }

        // 清理消息队列
        clearMessageQueue(sessionWrapper, sessionWrapper.messageQueue);

        if (postEvent) {
            if (sessionWrapper.promise != null) {
                // 未完成连接
                sessionWrapper.promise.tryFailure(new IOException(reason));
            } else {
                // 验证成功过才执行断开回调操作(调用过onSessionConnected方法)
                if (sessionWrapper.getVerifiedSequencer().get() > 0) {
                    // 避免捕获SessionWrapper，导致内存泄漏
                    final SessionLifecycleAware lifecycleAware = sessionWrapper.lifecycleAware;
                    // 提交到用户线程
                    ConcurrentUtils.tryCommit(session.localEventLoop(), () -> {
                        lifecycleAware.onSessionDisconnected(session);
                    });
                }
            }
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

    // region  --------------------------------- 网络事件处理 ---------------------------------

    /**
     * 如果产生事件的channel可用的话，接下来干什么呢？
     *
     * @param then 接下来执行的逻辑
     */
    private <T extends NetEventParam> void ifEventChannelOK(T eventParam, Consumer<C2SSessionState> then) {
        SessionWrapper sessionWrapper = getSessionWrapper(eventParam.localGuid(), eventParam.remoteGuid());
        Channel eventChannel = eventParam.channel();
        // 非法的channel
        if (sessionWrapper == null) {
            NetUtils.closeQuietly(eventChannel);
            return;
        }
        // 校验收到消息的channel是否合法
        C2SSessionState sessionState = sessionWrapper.getState();
        if (!sessionState.isEventChannelOK(eventChannel)) {
            NetUtils.closeQuietly(eventChannel);
            return;
        }
        then.accept(sessionState);
    }

    /**
     * 当收到服务器的Token验证结果
     *
     * @param responseParam 连接响应结果
     */
    void onRcvConnectResponse(ConnectResponseEventParam responseParam) {
        ifEventChannelOK(responseParam, sessionState -> {
            // 无论什么状态，只要当前channel收到token验证失败，都关闭session(移除会话)，它意味着服务器通知关闭。
            if (!responseParam.isSuccess()) {
                NetUtils.closeQuietly(responseParam.channel());
                removeSession(responseParam.localGuid(), responseParam.getServerGuid(), "token check failed.");
                return;
            }
            // token验证成功
            sessionState.onTokenCheckSuccess(responseParam.channel(), responseParam);
        });
    }

    /**
     * 当收到远程的rpc请求时
     *
     * @param rpcRequestEventParam rpc请求
     */
    void onRcvServerRpcRequest(RpcRequestEventParam rpcRequestEventParam) {
        ifEventChannelOK(rpcRequestEventParam, c2SSessionState -> {
            c2SSessionState.onRcvServerRpcRequest(rpcRequestEventParam.channel(), rpcRequestEventParam);
        });
    }

    /**
     * 当收到远程返回的rpc调用结果时
     *
     * @param rpcResponseEventParam rpc调用结果
     */
    void onRcvServerRpcResponse(RpcResponseEventParam rpcResponseEventParam) {
        ifEventChannelOK(rpcResponseEventParam, c2SSessionState -> {
            c2SSessionState.onRcvServerRpcResponse(rpcResponseEventParam.channel(), rpcResponseEventParam);
        });
    }

    /**
     * 当收到服务器的ping包返回时
     *
     * @param ackPongParam 服务器返回的pong包
     */
    void onRevServerAckPong(AckPingPongEventParam ackPongParam) {
        ifEventChannelOK(ackPongParam, c2SSessionState -> {
            c2SSessionState.onRcvServerAckPong(ackPongParam.channel(), ackPongParam);
        });
    }

    /**
     * 当收到服务器的单向消息时
     *
     * @param oneWayMessageEventParam 服务器发来的业务逻辑包
     */
    void onRevServerOneWayMsg(OneWayMessageEventParam oneWayMessageEventParam) {
        ifEventChannelOK(oneWayMessageEventParam, c2SSessionState -> {
            c2SSessionState.onRcvServerMessage(oneWayMessageEventParam.channel(), oneWayMessageEventParam);
        });
    }
    // endregion

    // ------------------------------------------------状态机------------------------------------------------

    /**
     * 切换session的状态
     */
    private void changeState(SessionWrapper sessionWrapper, C2SSessionState newState) {
        sessionWrapper.setState(newState);
        if (sessionWrapper.getState() != null) {
            sessionWrapper.getState().enter();
        }
    }

    /**
     * 客户端只会有三个网络事件(三种类型协议)，
     * 1.token验证结果
     * 2.服务器ack-ping返回协议(ack-pong)
     * 3.服务器发送的正式消息
     * <p>
     * 同时也只有三种状态：
     * 1.尝试连接状态
     * 2.正在验证状态
     * 3.已验证状态
     */
    private abstract class C2SSessionState {

        final SessionWrapper sessionWrapper;

        final SocketC2SSession session;

        C2SSessionState(SessionWrapper sessionWrapper) {
            this.sessionWrapper = sessionWrapper;
            this.session = sessionWrapper.getSession();
        }

        MessageQueue getMessageQueue() {
            return sessionWrapper.messageQueue;
        }

        NetContext getNetContext() {
            return sessionWrapper.userInfo.netContext;
        }

        IntSequencer getVerifiedSequencer() {
            return sessionWrapper.getVerifiedSequencer();
        }

        IntSequencer getSndTokenSequencer() {
            return sessionWrapper.getSndTokenSequencer();
        }

        ProtocolDispatcher getProtocolDispatcher() {
            return sessionWrapper.protocolDispatcher;
        }

        protected abstract void enter();

        protected abstract void execute();

        // 为何不要exit 因为根本不保证exit能走到,此外导致退出状态的原因太多，要做的事情并不一致，因此重要逻辑不能依赖exit

        /**
         * 在session关闭之前进行资源的清理，清理该状态自身持有的资源
         * (主要是channel)
         */
        public abstract void closeChannel();

        /**
         * 产生事件的channel是否OK，客户端只有当前持有的channel是合法的，因此很多地方是比较简单的。
         *
         * @param eventChannel 产生事件的channel
         * @return 当产生事件的channel是期望的channel时返回true
         */
        protected abstract boolean isEventChannelOK(Channel eventChannel);

        /**
         * 当收到服务器的token验证成功消息
         *
         * @param eventChannel 产生事件的channel
         * @param resultParam  返回信息
         */
        protected void onTokenCheckSuccess(Channel eventChannel, ConnectResponseEventParam resultParam) {
            throw new IllegalStateException(this.getClass().getSimpleName());
        }

        /**
         * 当收到服务器的Rpc请求时
         *
         * @param eventChannel         产生事件的channel
         * @param rpcRequestEventParam 服务器发来的rpc请求
         */
        protected void onRcvServerRpcRequest(Channel eventChannel, RpcRequestEventParam rpcRequestEventParam) {
            throw new IllegalStateException(this.getClass().getSimpleName());
        }

        /**
         * 当收到服务器的逻辑消息包
         *
         * @param eventChannel            产生事件的channel
         * @param oneWayMessageEventParam 服务器发来的单向消息
         */
        protected void onRcvServerMessage(Channel eventChannel, OneWayMessageEventParam oneWayMessageEventParam) {
            throw new IllegalStateException(this.getClass().getSimpleName());
        }

        /**
         * 当收到服务器的Rpc响应包的时候
         *
         * @param eventChannel       产生事件的channel
         * @param responseEventParam rpc返回结果
         */
        protected void onRcvServerRpcResponse(Channel eventChannel, RpcResponseEventParam responseEventParam) {
            throw new IllegalStateException(this.getClass().getSimpleName());
        }

        /**
         * 当收到服务器的ack-pong消息包
         *
         * @param eventChannel 产生事件的channel
         * @param ackPongParam 服务器返回的pong包
         */
        protected void onRcvServerAckPong(Channel eventChannel, AckPingPongEventParam ackPongParam) {
            throw new IllegalStateException(this.getClass().getSimpleName());
        }

        /**
         * 添加到待发送队列，默认实现仅仅是放入缓冲区
         *
         * @param unsentMessage 未发送的消息
         */
        protected void write(NetMessage unsentMessage) {
            getMessageQueue().getUnsentQueue().add(unsentMessage);
        }

        /**
         * 尝试立即发送一条消息，默认实现仅仅是放入缓冲区。
         *
         * @param unsentMessage 未发送的消息
         */
        protected void writeAndFlush(NetMessage unsentMessage) {
            getMessageQueue().getUnsentQueue().add(unsentMessage);
        }

    }

    /**
     * 连接状态
     */
    private class ConnectingState extends C2SSessionState {

        private ChannelFuture channelFuture;
        /**
         * 已尝试连接次数
         */
        private int tryTimes = 0;
        /**
         * 连接开始时间
         */
        private long connectStartTime = 0;

        ConnectingState(SessionWrapper sessionWrapper) {
            super(sessionWrapper);
        }

        @Override
        protected void enter() {
            tryConnect();
        }

        private void tryConnect() {
            tryTimes++;
            connectStartTime = netTimeManager.getSystemMillTime();
            channelFuture = acceptorManager.connectAsyn(session.remoteAddress(), sessionWrapper.getInitializerSupplier().get());
            logger.debug("tryConnect remote {} ,tryTimes {}.", session.remoteAddress(), tryTimes);
        }

        @Override
        protected void execute() {
            // 建立连接成功
            if (channelFuture.isSuccess() && channelFuture.channel().isActive()) {
                logger.debug("connect remote {} success, tryTimes {}.", session.remoteAddress(), tryTimes);
                changeState(sessionWrapper, new VerifyingState(sessionWrapper, channelFuture.channel()));
                return;
            }
            // 还未超时
            if (netTimeManager.getSystemMillTime() - connectStartTime < netConfigManager.connectTimeout()) {
                return;
            }
            // 本次建立连接超时，关闭当前future,并再次尝试
            closeFuture();

            if (tryTimes < netConfigManager.connectMaxTryTimes()) {
                // 还可以继续尝试
                tryConnect();
            } else {
                // 无法连接到服务器，移除会话，结束
                // 下一帧删除，避免迭代的过程中进行删除
                timerManager.nextTick(handle -> {
                    removeSession(sessionWrapper.getLocalGuid(), session.remoteGuid(), "can't connect remote " + session.remoteAddress());
                });
            }
        }

        private void closeFuture() {
            NetUtils.closeQuietly(channelFuture);
            channelFuture = null;
        }

        @Override
        public void closeChannel() {
            closeFuture();
        }

        @Override
        protected boolean isEventChannelOK(Channel eventChannel) {
            // 永远返回false，当前状态下不会响应其它事件
            return false;
        }
    }

    /**
     * 已建立链接状态，可重连状态
     */
    private abstract class ConnectedState extends C2SSessionState {
        /**
         * 已建立连接的channel
         * (已连接的意思是：socket已连接)
         */
        protected final Channel channel;

        ConnectedState(SessionWrapper sessionWrapper, Channel channel) {
            super(sessionWrapper);
            this.channel = channel;
        }

        @Override
        public final void closeChannel() {
            NetUtils.closeQuietly(channel);
        }

        /**
         * 只会响应当前channel的消息事件
         */
        @Override
        protected final boolean isEventChannelOK(Channel eventChannel) {
            return this.channel == eventChannel;
        }

        /**
         * 重连
         *
         * @param reason 重连的原因
         */
        final void reconnect(String reason) {
            NetUtils.closeQuietly(channel);
            changeState(sessionWrapper, new ConnectingState(sessionWrapper));
            logger.debug("reconnect by reason of {}", reason);
        }

    }

    /**
     * 正在验证状态。
     * <p>
     * 1.如果限定时间内未收到任何消息，则尝试重新连接。
     * 2.收到其它消息，但未收到token验证结果时：，则会再次进行验证。
     * 3.收到验证结果：
     * <li>任何状态下收到验证失败都会关闭session</li>
     * <li>验证成功且判定服务器的ack正确时，验证完成</li>
     * <li>验证成功但判定服务器的ack错误时，关闭session</li>
     */
    private class VerifyingState extends ConnectedState {
        /**
         * 进入状态机的时间戳，用于计算token响应超时
         */
        private long enterStateMillTime;

        VerifyingState(SessionWrapper sessionWrapper, Channel channel) {
            super(sessionWrapper, channel);
        }

        /**
         * 发送token
         */
        @Override
        protected void enter() {
            enterStateMillTime = netTimeManager.getSystemMillTime();

            int sndTokenTimes = getSndTokenSequencer().incAndGet();
            // 创建验证请求
            ConnectRequestTO connectRequest = new ConnectRequestTO(sessionWrapper.getLocalGuid(), sndTokenTimes,
                    getMessageQueue().getAck(), sessionWrapper.getEncryptedToken());
            channel.writeAndFlush(connectRequest, channel.voidPromise());
            logger.debug("{} times send verify msg to server {}", sndTokenTimes, session);
        }

        @Override
        protected void execute() {
            if (netTimeManager.getSystemMillTime() - enterStateMillTime > netConfigManager.waitTokenResultTimeout()) {
                // 获取token结果超时，重连
                reconnect("wait token result timeout.");
            }
        }

        @Override
        protected void onTokenCheckSuccess(Channel eventChannel, ConnectResponseEventParam resultParam) {
            // 不是等待的结果
            if (resultParam.getSndTokenTimes() != getSndTokenSequencer().get()) {
                return;
            }
            MessageQueue messageQueue = getMessageQueue();
            // 收到的ack有误(有丢包)，这里重连已没有意义(始终有消息漏掉了，无法恢复)
            if (!messageQueue.isAckOK(resultParam.getAck())) {
                removeSession(sessionWrapper.getLocalGuid(), resultParam.getServerGuid(),
                        "(Verifying) server ack is error. ackInfo = " + messageQueue.generateAckErrorInfo(resultParam.getAck()));
                return;
            }
            // 更新消息队列
            sessionWrapper.getMessageQueue().updateSentQueue(resultParam.getAck());
            // 保存新的token
            sessionWrapper.setEncryptedToken(resultParam.getEncryptedToken());
            changeState(sessionWrapper, new VerifiedState(sessionWrapper, channel));
        }

        @Override
        protected void onRcvServerRpcRequest(Channel eventChannel, RpcRequestEventParam rpcRequestEventParam) {
            reconnect("onRcvServerRpcRequest, but missing token result");
        }

        @Override
        protected void onRcvServerRpcResponse(Channel eventChannel, RpcResponseEventParam responseEventParam) {
            reconnect("onRcvServerRpcResponse, but missing token result");
        }

        @Override
        protected void onRcvServerMessage(Channel eventChannel, OneWayMessageEventParam oneWayMessageEventParam) {
            reconnect("onRcvServerMessage, but missing token result");
        }

        @Override
        protected void onRcvServerAckPong(Channel eventChannel, AckPingPongEventParam ackPongParam) {
            reconnect("onRcvServerAckPong, but missing token result");
        }

    }

    /**
     * token验证成功状态
     */
    private class VerifiedState extends ConnectedState {
        /**
         * 当前队列是否有ping包，避免遍历
         */
        private boolean hasPingMessage;
        /**
         * 上次向服务器发送消息的时间。
         * 它的重要作用是避免双方缓存队列过大，尤其是降低服务器压力。
         */
        private int lastSendMessageTime;

        VerifiedState(SessionWrapper sessionWrapper, Channel channel) {
            super(sessionWrapper, channel);
        }

        @Override
        protected void enter() {
            hasPingMessage = false;
            lastSendMessageTime = netTimeManager.getSystemSecTime();

            final Promise<Session> promise = sessionWrapper.detachPromise();
            if (null != promise && !promise.trySuccess(session)) {
                // 激活session失败
                removeSession(session.localGuid(), session.remoteGuid(), "active session failed");
                return;
            }
            int verifiedTimes = getVerifiedSequencer().incAndGet();
            if (promise != null) {
                assert verifiedTimes == 1;
                // 避免捕获错误的对象
                final SessionLifecycleAware lifecycleAware = sessionWrapper.lifecycleAware;
                ConcurrentUtils.tryCommit(session.localEventLoop(), () -> {
                    lifecycleAware.onSessionConnected(session);
                });
                logger.info("first verified success, sessionInfo={}", session);
            } else {
                logger.info("reconnect verified success, verifiedTimes={},sessionInfo={}", verifiedTimes, session);
                // 重发未确认接受到的消息
                resendAndFlush();
            }
        }

        /**
         * 1. 重发那些已发送，但是未被确认的消息
         * 2. 清空未发送的消息
         * <p>
         * Q: 为什么要flush？ <br>
         * A: 这里存在一个时序问题，如果不flush，那么下一帧之前的加急消息会立即发送，而前面的加急消息因为不能立即发送而进入了未发送队列！
         */
        private void resendAndFlush() {
            MessageQueue messageQueue = getMessageQueue();
            // 重发
            if (messageQueue.getSentQueue().size() > 0) {
                SocketC2SSessionManager.this.resend(channel, messageQueue);
            }
            // 清空未发送的消息
            if (messageQueue.getUnsentQueue().size() > 0) {
                flushAllUnsentMessage();
            }
        }

        @Override
        protected void execute() {
            MessageQueue messageQueue = getMessageQueue();
            // 检查消息超时
            if (messageQueue.getSentQueue().size() > 0) {
                long firstMessageTimeout = messageQueue.getSentQueue().getFirst().getTimeout();
                // 超时未收到第一条消息的ack
                if (netTimeManager.getSystemMillTime() >= firstMessageTimeout) {
                    reconnect("first msg of sentQueue timeout.");
                    return;
                }
            }

            // 是否需要发送ack-ping包，ping包服务器收到一定是会返回的，而普通消息则不一定。
            if (isNeedSendAckPing()) {
                messageQueue.getUnsentQueue().add(new AckPingPongMessage());
                hasPingMessage = true;
                logger.info("send ack ping");
            }

            // 有待发送的消息则发送
            if (messageQueue.getUnsentQueue().size() > 0) {
                flushAllUnsentMessage();
            }
        }

        /**
         * 发送所有待发送的消息
         */
        private void flushAllUnsentMessage() {
            SocketC2SSessionManager.this.flushAllUnsentMessage(channel, getMessageQueue());
            lastSendMessageTime = netTimeManager.getSystemSecTime();
        }

        /**
         * 是否需要发送ack-ping包
         * 什么时候需要发？？？
         * 需要同时满足以下条件：
         * 1.当前无ping消息等待结果
         * 2.当前无待发送消息
         * 3.距离最后一条消息发送过去了超时时长的一半 或 长时间未收到服务器消息
         *
         * @return 满足以上条件时返回true，否则返回false。
         */
        private boolean isNeedSendAckPing() {
            // 有ping包还未返回
            if (hasPingMessage) {
                return false;
            }
            MessageQueue messageQueue = getMessageQueue();
            // 有待发送的逻辑包
            if (messageQueue.getUnsentQueue().size() > 0) {
                return false;
            }
            // 判断发送的最后一条消息的的等待确认时长是否过去了一半(降低都是无返回的消息时导致的超时概率)
            // 如果每次发的都是无返回的协议也太极限了，我们在游戏中不考虑这种情况,通过重连解决该问题
            if (messageQueue.getSentQueue().size() > 0) {
                long ackTimeout = messageQueue.getSentQueue().getLast().getTimeout();
                return ackTimeout - netTimeManager.getSystemMillTime() <= netConfigManager.ackTimeout() / 2;
            }
            // 已经有一段时间没有向服务器发送消息了(session超时时间过去1/3)，保活和降低服务器内存压力
            return netTimeManager.getSystemSecTime() - lastSendMessageTime >= netConfigManager.sessionTimeout() / 3;
        }

        @Override
        protected void onRcvServerRpcRequest(Channel eventChannel, RpcRequestEventParam rpcRequestEventParam) {
            // 大量的lambda表达式可能影响性能，目前先不优化，先注意可维护性。
            ifSequenceAndAckOk(rpcRequestEventParam, () -> {
                RpcRequestCommitTask requestCommitTask = new RpcRequestCommitTask(session, sessionWrapper.protocolDispatcher,
                        rpcRequestEventParam.getRequestGuid(), rpcRequestEventParam.isSync(), rpcRequestEventParam.getRequest());
                commit(session, requestCommitTask);
            });
        }

        @Override
        public void onRcvServerRpcResponse(Channel eventChannel, RpcResponseEventParam responseEventParam) {
            ifSequenceAndAckOk(responseEventParam, () -> {
                RpcPromiseInfo rpcPromiseInfo = sessionWrapper.getRpcPromiseInfoMap().remove(responseEventParam.getRequestGuid());
                if (null != rpcPromiseInfo) {
                    commitRpcResponse(sessionWrapper.getSession(), rpcPromiseInfo, responseEventParam.getRpcResponse());
                }
                // else 可能超时了
            });
        }

        @Override
        protected void onRcvServerMessage(Channel eventChannel, OneWayMessageEventParam oneWayMessageEventParam) {
            // 减少lambda表达式捕获的对象
            final Object message = oneWayMessageEventParam.getMessage();
            ifSequenceAndAckOk(oneWayMessageEventParam, () -> {
                commit(session, new OneWayMessageCommitTask(session, sessionWrapper.protocolDispatcher, message));
            });
        }

        @Override
        protected void onRcvServerAckPong(Channel eventChannel, AckPingPongEventParam ackPongParam) {
            hasPingMessage = false;
            ifSequenceAndAckOk(ackPongParam, ConcurrentUtils.NO_OP_TASK);
            logger.info("rcv ack pong");
        }

        /**
         * 如果消息的ack和sequence正常的话，接下来做什么呢？
         * 当服务器发来的消息是期望的下一个消息，且ack正确时执行指定逻辑。
         *
         * @param eventParam 服务器发来的消息(pong包或业务逻辑包)
         */
        final void ifSequenceAndAckOk(MessageEventParam eventParam, Runnable then) {
            MessageQueue messageQueue = getMessageQueue();
            // 不是期望的下一个消息,请求重传
            if (eventParam.getSequence() != messageQueue.getAck() + 1) {
                reconnect("serverSequence != ack()+1, serverSequence=" + eventParam.getSequence() + ", ack=" + messageQueue.getAck());
                return;
            }
            // 服务器ack不对，尝试矫正
            if (!messageQueue.isAckOK(eventParam.getAck())) {
                reconnect("server ack error,ackInfo=" + messageQueue.generateAckErrorInfo(eventParam.getAck()));
                return;
            }
            messageQueue.setAck(eventParam.getSequence());
            messageQueue.updateSentQueue(eventParam.getAck());
            then.run();
        }

        @Override
        protected void write(NetMessage unsentMessage) {
            boolean flushed = SocketC2SSessionManager.this.write(channel, getMessageQueue(), unsentMessage);
            if (flushed) {
                // 清空了缓冲区，记录发送时间
                lastSendMessageTime = netTimeManager.getSystemSecTime();
            }
        }

        @Override
        protected void writeAndFlush(NetMessage unsentMessage) {
            SocketC2SSessionManager.this.writeAndFlush(channel, getMessageQueue(), unsentMessage);
            // 立即发送需要更新发送时间戳
            lastSendMessageTime = netTimeManager.getSystemSecTime();
        }

    }

    // ------------------------------------------------------ 内部封装 ---------------------------------

    /**
     * 用户的所有会话信息
     */
    private static class UserInfo {

        /**
         * 用户信息
         */
        private final NetContext netContext;

        /**
         * 客户端发起的所有会话,注册时加入，close时删除
         * serverGuid --> session
         */
        private final Long2ObjectMap<SessionWrapper> sessionWrapperMap = new Long2ObjectOpenHashMap<>();

        UserInfo(NetContext netContext) {
            this.netContext = netContext;
        }
    }

    /**
     * session包装对象
     * 不将额外信息暴露给应用层，同时实现线程安全。
     */
    private static class SessionWrapper extends SocketSessionWrapper<SocketC2SSession> {

        /**
         * 建立Session和用户之间的关系
         */
        private final UserInfo userInfo;

        /**
         * 该会话使用的initializer提供者。
         * （如果容易用错的话，可以改成{@link ChannelInitializerFactory}）
         */
        private final ChannelInitializerSupplier initializerSupplier;

        /**
         * 该会话使用的生命周期回调接口
         */
        private final SessionLifecycleAware lifecycleAware;
        /**
         * 该会话关联的消息处理器
         */
        private final ProtocolDispatcher protocolDispatcher;

        /**
         * 发送token次数
         */
        private final IntSequencer sndTokenSequencer = new IntSequencer(0);
        /**
         * 验证成功的次数
         * (也等于收到token结果的次数，因为验证失败，就会删除session)
         */
        private final IntSequencer verifiedSequencer = new IntSequencer(0);
        /**
         * 被加密的Token，客户端并不关心具体内容，只是保存用于建立链接
         */
        private byte[] encryptedToken;
        /**
         * 会话当前状态
         */
        private C2SSessionState state;
        /**
         * 用于返回给用户结果
         */
        private Promise<Session> promise;

        SessionWrapper(UserInfo userInfo,
                       ChannelInitializerSupplier initializerSupplier,
                       SessionLifecycleAware lifecycleAware,
                       ProtocolDispatcher protocolDispatcher,
                       SocketC2SSession session,
                       byte[] encryptedToken, Promise<Session> promise) {
            super(session);
            this.userInfo = userInfo;
            this.initializerSupplier = initializerSupplier;
            this.lifecycleAware = lifecycleAware;
            this.protocolDispatcher = protocolDispatcher;
            this.encryptedToken = encryptedToken;
            this.promise = promise;
        }

        C2SSessionState getState() {
            return state;
        }

        void setEncryptedToken(byte[] encryptedToken) {
            this.encryptedToken = encryptedToken;
        }

        void setState(C2SSessionState state) {
            this.state = state;
        }

        byte[] getEncryptedToken() {
            return encryptedToken;
        }

        IntSequencer getSndTokenSequencer() {
            return sndTokenSequencer;
        }

        IntSequencer getVerifiedSequencer() {
            return verifiedSequencer;
        }

        long getLocalGuid() {
            return userInfo.netContext.localGuid();
        }

        ChannelInitializerSupplier getInitializerSupplier() {
            return initializerSupplier;
        }

        NetContext getNetContext() {
            return userInfo.netContext;
        }

        Promise<Session> detachPromise() {
            Promise<Session> result = promise;
            promise = null;
            return result;
        }

        @Override
        protected void write(NetMessage unsentMessage) {
            state.write(unsentMessage);
        }

        @Override
        protected void writeAndFlush(NetMessage unsentMessage) {
            state.writeAndFlush(unsentMessage);
        }
    }

}
