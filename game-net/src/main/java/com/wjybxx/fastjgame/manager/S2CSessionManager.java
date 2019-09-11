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
import com.wjybxx.fastjgame.net.remote.S2CSession;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.BindException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 服务器到客户端会话管理器。
 * (我接收到的连接)
 * <p>
 * token相关说明请查看文档 关于token.txt
 * <p>
 * 注意：请求登录、重连时，验证失败不能对当前session做任何操作，因为不能证明表示当前session有异常，
 * 只有连接成功时才能操作session。
 * 同理，也不能更新{@link ForbiddenTokenHelper}。
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
 * {@link #notifyTokenCheckFailed(Channel, ConnectRequestEventParam, FailReason)}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 22:14
 * github - https://github.com/hl845740757
 */
public class S2CSessionManager extends RemoteSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(S2CSessionManager.class);

    private NetManagerWrapper managerWrapper;
    private final NetTimeManager netTimeManager;
    private final TokenManager tokenManager;
    private final AcceptorManager acceptorManager;
    private final NetTimerManager timerManager;
    /**
     * 所有用户的会话信息
     */
    private final Long2ObjectMap<UserInfo> userInfoMap = new Long2ObjectOpenHashMap<>();

    @Inject
    public S2CSessionManager(NetTimeManager netTimeManager, NetConfigManager netConfigManager,
                             NetTimerManager timerManager, TokenManager tokenManager,
                             AcceptorManager acceptorManager) {
        super(netConfigManager, netTimeManager);
        this.netTimeManager = netTimeManager;
        this.tokenManager = tokenManager;
        this.acceptorManager = acceptorManager;
        this.timerManager = timerManager;

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
                sessionWrapper.flushBuffer();

                // 检测超时的rpc调用
                CollectionUtils.removeIfAndThen(sessionWrapper.messageQueue.getRpcPromiseInfoMap().values(),
                        rpcPromiseInfo -> netTimeManager.getSystemMillTime() >= rpcPromiseInfo.deadline,
                        rpcPromiseInfo -> commitRpcResponse(sessionWrapper.session, rpcPromiseInfo, RpcResponse.TIMEOUT));
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

        final UserInfo userInfo = userInfoMap.computeIfAbsent(netContext.localGuid(), k -> {
            return new UserInfo(netContext, new ForbiddenTokenHelper(netTimeManager, timerManager, netConfigManager.tokenForbiddenTimeout()));
        });
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
    private SessionWrapper getSessionWrapper(long localGuid, long clientGuid) {
        UserInfo userInfo = userInfoMap.get(localGuid);
        if (null == userInfo) {
            return null;
        }
        return userInfo.sessionWrapperMap.get(clientGuid);
    }

    /**
     * 获取一个可写的session。
     *
     * @param localGuid  from
     * @param clientGuid to
     * @return sessionWrapper
     */
    private SessionWrapper getWritableSession(long localGuid, long clientGuid) {
        SessionWrapper sessionWrapper = getSessionWrapper(localGuid, clientGuid);
        // 会话已被删除
        if (null == sessionWrapper) {
            return null;
        }
        // 会话已关闭
        if (!sessionWrapper.session.isActive()) {
            return null;
        }
        // 缓存需要排除正常缓存阈值
        if (sessionWrapper.getCacheMessageNum() >= netConfigManager.serverMaxCacheNum() + netConfigManager.flushThreshold()) {
            // 缓存过多，删除会话
            removeSession(localGuid, clientGuid, "cacheMessageNum is too much! cacheMessageNum=" + sessionWrapper.getCacheMessageNum());
            return null;
        } else {
            return sessionWrapper;
        }
    }

    /**
     * 发送一条单向消息
     *
     * @param localGuid  from
     * @param clientGuid to
     * @param message    消息内容
     */
    @Override
    public void sendOneWayMessage(long localGuid, long clientGuid, @Nonnull Object message) {
        SessionWrapper sessionWrapper = getWritableSession(localGuid, clientGuid);
        if (null == sessionWrapper) {
            logger.debug("session {}-{} is inactive, but try send message.", localGuid, clientGuid);
            return;
        }
        sessionWrapper.write(new OneWayMessage(message));
    }

    /**
     * 向远程发送一个异步rpc请求
     *
     * @param localGuid     我的标识
     * @param clientGuid    远程节点标识
     * @param request       rpc请求内容
     * @param timeoutMs     超时时间，小于等于0表示不超时
     * @param userEventLoop 用户线程
     * @param rpcCallback   rpc回调
     */
    @Override
    public void sendRpcRequest(long localGuid, long clientGuid, @Nonnull Object request, long timeoutMs, EventLoop userEventLoop, RpcCallback rpcCallback) {
        SessionWrapper sessionWrapper = getWritableSession(localGuid, clientGuid);
        if (null == sessionWrapper) {
            logger.debug("session {}-{} is inactive, but try send asyncRpcRequest.", localGuid, clientGuid);
            ConcurrentUtils.tryCommit(userEventLoop, () -> {
                rpcCallback.onComplete(RpcResponse.SESSION_CLOSED);
            });
            return;
        }
        long deadline = netTimeManager.getSystemMillTime() + timeoutMs;
        RpcPromiseInfo rpcPromiseInfo = RpcPromiseInfo.newInstance(rpcCallback, deadline);

        long requestGuid = sessionWrapper.messageQueue.nextRpcRequestGuid();
        RpcRequestMessage rpcRequest = new RpcRequestMessage(requestGuid, false, request);
        // 发送之前保存信息
        sessionWrapper.messageQueue.getRpcPromiseInfoMap().put(requestGuid, rpcPromiseInfo);

        // 添加到缓存队列，稍后发送
        sessionWrapper.write(rpcRequest);
    }

    /**
     * 向远程发送一个rpc请求
     *
     * @param localGuid  我的标识
     * @param clientGuid 远程节点标识
     * @param request    rpc请求内容
     * @param timeoutMs  超时时间
     * @param rpcPromise 用于监听结果
     */
    @Override
    public void sendSyncRpcRequest(long localGuid, long clientGuid, @Nonnull Object request, long timeoutMs, RpcPromise rpcPromise) {
        SessionWrapper sessionWrapper = getWritableSession(localGuid, clientGuid);
        if (null == sessionWrapper) {
            logger.debug("session {}-{} is inactive, but try send syncRpcRequest.", localGuid, clientGuid);
            rpcPromise.trySuccess(RpcResponse.SESSION_CLOSED);
            return;
        }
        long deadline = netTimeManager.getSystemMillTime() + timeoutMs;
        RpcPromiseInfo rpcPromiseInfo = RpcPromiseInfo.newInstance(rpcPromise, deadline);

        long requestGuid = sessionWrapper.messageQueue.nextRpcRequestGuid();
        RpcRequestMessage rpcRequest = new RpcRequestMessage(requestGuid, true, request);
        // 发送之前保存信息
        sessionWrapper.messageQueue.getRpcPromiseInfoMap().put(requestGuid, rpcPromiseInfo);

        sessionWrapper.writeAndFlush(rpcRequest);
    }

    /**
     * 发送rpc响应
     *
     * @param localGuid   我的id
     * @param clientGuid  远程节点id
     * @param requestGuid 请求对应的编号
     * @param sync        是否是同步rpc调用（目前服务器的监听方还未做缓存，后期可能会用上）
     * @param response    响应结果
     */
    @Override
    public void sendRpcResponse(long localGuid, long clientGuid, long requestGuid, boolean sync, @Nonnull RpcResponse response) {
        SessionWrapper sessionWrapper = getWritableSession(localGuid, clientGuid);
        if (null == sessionWrapper) {
            logger.debug("session {}-{} is inactive, but try send rpcResponse.", localGuid, clientGuid);
            return;
        }
        RpcResponseMessage unsentRpcResponse = new RpcResponseMessage(requestGuid, response);
        if (sync) {
            // 远程发来的同步rpc调用，立即返回
            sessionWrapper.writeAndFlush(unsentRpcResponse);
        } else {
            // 非同步调用，不着急返回
            sessionWrapper.write(unsentRpcResponse);
        }
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
        // 禁用该token及之前的token
        sessionWrapper.userInfo.forbiddenTokenHelper.forbiddenCurToken(sessionWrapper.getToken());
        // 避免捕获SessionWrapper，导致内存泄漏
        final S2CSession session = sessionWrapper.getSession();
        // 设置为已关闭
        session.setClosed();

        // 清理消息队列(需要先执行)
        clearMessageQueue(session, sessionWrapper.messageQueue);

        // 通知客户端退出(这里会关闭channel)
        notifyClientExit(sessionWrapper.getChannel(), sessionWrapper);

        if (postEvent) {
            // 避免捕获SessionWrapper，导致内存泄漏
            final SessionLifecycleAware lifecycleAware = sessionWrapper.getLifecycleAware();
            // 尝试提交到用户线程
            ConcurrentUtils.tryCommit(session.localEventLoop(), () -> {
                lifecycleAware.onSessionDisconnected(session);
            });
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
     * 收到客户端的链接请求(请求验证token)
     *
     * @param requestParam 请求参数
     */
    void onRcvConnectRequest(ConnectRequestEventParam requestParam) {
        final Channel channel = requestParam.channel();

        Token clientToken = tokenManager.decryptToken(requestParam.getTokenBytes());
        // 客户端token不合法(解析错误)
        if (null == clientToken) {
            notifyTokenCheckFailed(channel, requestParam, FailReason.NULL);
            return;
        }
        // 服务器会话已经不存这里了(服务器没有监听或已经关闭) UserInfo不存在
        if (!userInfoMap.containsKey(requestParam.localGuid())) {
            notifyTokenCheckFailed(channel, requestParam, FailReason.SERVER_NOT_EXIST);
            return;
        }
        // 无效token
        if (tokenManager.isFailToken(clientToken)) {
            notifyTokenCheckFailed(channel, requestParam, FailReason.INVALID);
            return;
        }
        // token与请求不匹配
        if (!isRequestMatchToken(requestParam, clientToken)) {
            notifyTokenCheckFailed(channel, requestParam, FailReason.TOKEN_NOT_MATCH_REQUEST);
            return;
        }
        UserInfo userInfo = userInfoMap.get(requestParam.localGuid());
        // 被禁用的旧token
        if (userInfo.forbiddenTokenHelper.isForbiddenToken(clientToken)) {
            notifyTokenCheckFailed(channel, requestParam, FailReason.OLD_REQUEST);
            return;
        }
        // 为什么不能在这里更新forbiddenToken? 因为走到这里还不能证明是一个合法的客户端，不能影响合法客户端的数据。
        SessionWrapper sessionWrapper = getSessionWrapper(requestParam.localGuid(), requestParam.getClientGuid());
        // 不论如何必须是新的channel
        if (isSameChannel(sessionWrapper, channel)) {
            notifyTokenCheckFailed(channel, requestParam, FailReason.SAME_CHANNEL);
            return;
        }
        // 新的token
        if (isNewerToken(sessionWrapper, clientToken)) {
            tryLogin(channel, requestParam, clientToken);
            return;
        }
        // 当前使用的token
        if (isUsingToken(sessionWrapper, clientToken)) {
            tryReconnect(channel, requestParam, clientToken);
            return;
        }
        // 其它(介于使用的两个token之间的token)
        notifyTokenCheckFailed(channel, requestParam, FailReason.OLD_REQUEST);
    }

    /**
     * 是否是同一个channel
     */
    private boolean isSameChannel(SessionWrapper sessionWrapper, Channel channel) {
        return null != sessionWrapper && sessionWrapper.getChannel() == channel;
    }

    /**
     * 是否是更新的token
     */
    private boolean isNewerToken(@Nullable SessionWrapper sessionWrapper, Token clientToken) {
        if (null == sessionWrapper) {
            return true;
        }
        if (netConfigManager.isAllowAutoRelogin()) {
            return clientToken.getCreateSecTime() > sessionWrapper.getToken().getCreateSecTime();
        }
        return false;
    }

    /**
     * 是否是当前会话使用的token
     */
    private boolean isUsingToken(@Nullable SessionWrapper sessionWrapper, Token clientToken) {
        if (null == sessionWrapper) {
            return false;
        }
        // 前一个token为null ==> 客户端一定收到了新的token
        if (sessionWrapper.getPreToken() == null) {
            return tokenManager.isSameToken(sessionWrapper.getToken(), clientToken);
        }
        // 可能收到了，也可能没收到最新的token，但只能是两者之一
        return tokenManager.isSameToken(sessionWrapper.getToken(), clientToken)
                || tokenManager.isSameToken(sessionWrapper.getPreToken(), clientToken);
    }

    /**
     * 请求与是token否匹配。校验token基本信息,校验token是否被修改过
     *
     * @param requestParam 客户端请求参数
     * @param token        客户端携带的token信息
     */
    private boolean isRequestMatchToken(ConnectRequestEventParam requestParam, @Nonnull Token token) {
        // token不是用于该客户端的
        if (requestParam.getClientGuid() != token.getClientGuid()
                || requestParam.localGuid() != token.getServerGuid()) {
            return false;
        }
        // token不是用于该服务器的
        UserInfo userInfo = userInfoMap.get(requestParam.localGuid());
        if (null == userInfo || token.getServerRoleType() != userInfo.netContext.localRole()) {
            return false;
        }
        return true;
    }

    /**
     * 使用更加新的token请求登录
     *
     * @param channel      产生事件的channel
     * @param requestParam 客户端发来的请求参数
     * @param clientToken  客户端携带的token信息 更加新的token，newerToken
     */
    private boolean tryLogin(Channel channel, ConnectRequestEventParam requestParam, @Nonnull Token clientToken) {
        // 不是登录token
        if (!tokenManager.isLoginToken(clientToken)) {
            notifyTokenCheckFailed(channel, requestParam, FailReason.NOT_LOGIN_TOKEN);
            return false;
        }
        // 登录token超时了
        if (tokenManager.isLoginTokenTimeout(clientToken)) {
            notifyTokenCheckFailed(channel, requestParam, FailReason.TOKEN_TIMEOUT);
            return false;
        }
        // 客户端已收到消息数必须为0
        if (requestParam.getAck() != MessageQueue.INIT_ACK) {
            notifyTokenCheckFailed(channel, requestParam, FailReason.ACK);
            return false;
        }

        // 为何要删除旧会话？(前两个问题都是可以解决的，但是第三个问题不能不管)
        // 1.会话是有状态的，无法基于旧状态与新状态的客户端通信(ack,sequence)
        // 2.旧的token需要被禁用
        // 3.必须进行通知，需要让逻辑层知道旧连接彻底断开了，可能有额外逻辑
        removeSession(requestParam.localGuid(), requestParam.getClientGuid(), "reLogin");

        UserInfo userInfo = userInfoMap.get(requestParam.localGuid());
        // 禁用验证成功的token之前的token(不能放到removeSession之前，会导致覆盖)
        userInfo.forbiddenTokenHelper.forbiddenPreToken(clientToken);

        final PortContext portContext = requestParam.getPortContext();
        // 登录成功
        S2CSession session = new S2CSession(userInfo.netContext, managerWrapper,
                requestParam.getClientGuid(), clientToken.getClientRoleType(), portContext.sessionSenderMode);

        SessionWrapper sessionWrapper = new SessionWrapper(userInfo, session, this);
        userInfo.sessionWrapperMap.put(requestParam.getClientGuid(), sessionWrapper);

        // 分配新的token并进入等待状态
        Token nextToken = tokenManager.newLoginSuccessToken(clientToken);
        sessionWrapper.changeToWaitState(channel, portContext, requestParam.getSndTokenTimes(), clientToken, nextToken, nextSessionTimeout());

        notifyTokenCheckSuccess(channel, requestParam, MessageQueue.INIT_ACK, nextToken);
        logger.info("client login success, sessionInfo={}", session);

        // 避免捕获过多的对象，导致内存泄漏
        final SessionLifecycleAware lifecycleAware = portContext.lifecycleAware;
        // 连接建立回调(通知)
        ConcurrentUtils.tryCommit(session.localEventLoop(), () -> {
            lifecycleAware.onSessionConnected(session);
        });
        return true;
    }

    private int nextSessionTimeout() {
        return netTimeManager.getSystemSecTime() + netConfigManager.sessionTimeout();
    }

    /**
     * 客户端尝试断线重连，token是服务器保存的两个token之一。
     *
     * @param channel      产生事件的channel
     * @param requestParam 客户端发来的请求参数
     * @param clientToken  客户端携带的token信息，等于服务器使用的token (usingToken)
     */
    private boolean tryReconnect(Channel channel, ConnectRequestEventParam requestParam, @Nonnull Token clientToken) {
        SessionWrapper sessionWrapper = getSessionWrapper(requestParam.localGuid(), requestParam.getClientGuid());
        assert null != sessionWrapper;
        // 这是一个旧请求
        if (requestParam.getSndTokenTimes() <= sessionWrapper.getSndTokenTimes()) {
            notifyTokenCheckFailed(channel, requestParam, FailReason.OLD_REQUEST);
            return false;
        }
        // 判断客户端ack合法性
        MessageQueue messageQueue = sessionWrapper.getMessageQueue();
        if (!messageQueue.isAckOK(requestParam.getAck())) {
            notifyTokenCheckFailed(channel, requestParam, FailReason.ACK);
            return false;
        }
        // ---- 这里验证成功 ack 和 token都验证通过
        // 禁用验证成功的token之前的token
        sessionWrapper.userInfo.forbiddenTokenHelper.forbiddenPreToken(clientToken);

        // 关闭旧channel
        if (sessionWrapper.getChannel() != channel) {
            NetUtils.closeQuietly(sessionWrapper.getChannel());
        }

        // 更新消息队列
        messageQueue.updateSentQueue(requestParam.getAck());

        // 分配新的token并进入等待状态
        Token nextToken = tokenManager.nextToken(clientToken);
        sessionWrapper.changeToWaitState(channel, requestParam.getPortContext(), requestParam.getSndTokenTimes(), clientToken, nextToken, nextSessionTimeout());

        notifyTokenCheckSuccess(channel, requestParam, messageQueue.getAck(), nextToken);
        logger.info("client reconnect success, sessionInfo={}", sessionWrapper.getSession());

        // 重发已发送未确认的消息
        if (messageQueue.getSentQueue().size() > 0) {
            resend(channel, messageQueue);
        }
        return true;
    }

    /**
     * 通知客户端退出
     *
     * @param channel        会话对应的的channel
     * @param sessionWrapper 会话信息
     */
    private void notifyClientExit(Channel channel, SessionWrapper sessionWrapper) {
        long clientGuid = sessionWrapper.getSession().remoteGuid();
        long serverGuid = sessionWrapper.getSession().localGuid();
        Token failToken = tokenManager.newFailToken(clientGuid, serverGuid);
        notifyTokenCheckResult(channel, sessionWrapper.getSndTokenTimes(), false, -1, failToken);
    }

    /**
     * 通知客户端token验证失败
     * 注意token校验失败，不能认定当前会话失效，可能是错误或非法的连接，因此不能对会话下手
     *
     * @param requestEventParam 客户端的请求信息
     * @param failReason        失败原因，用于记录日志
     */
    private void notifyTokenCheckFailed(Channel channel, ConnectRequestEventParam requestEventParam, FailReason failReason) {
        Token failToken = tokenManager.newFailToken(requestEventParam.getClientGuid(), requestEventParam.localGuid());
        notifyTokenCheckResult(channel, requestEventParam.getConnectRequestTO().getSndTokenTimes(), false, -1, failToken);
        logger.warn("client {} checkTokenResult failed by reason of {}", requestEventParam.getClientGuid(), failReason);
    }

    /**
     * 通知客户端token验证成功
     *
     * @param requestEventParam 客户端的请求信息
     * @param ack               服务器的捎带确认ack
     * @param nextToken         连接成功时新分配的token
     */
    private void notifyTokenCheckSuccess(Channel channel, ConnectRequestEventParam requestEventParam, long ack, Token nextToken) {
        notifyTokenCheckResult(channel, requestEventParam.getSndTokenTimes(), true, ack, nextToken);
    }

    /**
     * 通知客户端token验证结果
     *
     * @param channel       发起请求验证token的channel
     * @param sndTokenTimes 这是客户端的第几次请求
     * @param success       是否成功
     * @param ack           服务器的ack
     * @param token         新的token
     */
    private void notifyTokenCheckResult(Channel channel, int sndTokenTimes, boolean success, long ack, Token token) {
        byte[] encryptToken = tokenManager.encryptToken(token);
        ConnectResponseTO connectResponse = new ConnectResponseTO(sndTokenTimes, success, ack, encryptToken);
        // 这里需要监听结果，不可以使用voidPromise
        ChannelFuture future = channel.writeAndFlush(connectResponse);
        // token验证失败情况下，发送之后，关闭channel
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
        // 在新channel收到客户端的消息时 => 客户端一定收到了token验证结果
        // 确定客户端已收到了新的token,更新channel为已激活状态，并添加禁用的token
        if (sessionWrapper.getPreToken() != null) {
            sessionWrapper.changeToActiveState();
            sessionWrapper.userInfo.forbiddenTokenHelper.forbiddenPreToken(sessionWrapper.getToken());
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
            RpcRequestCommitTask requestCommitTask = new RpcRequestCommitTask(sessionWrapper.session, sessionWrapper.getProtocolDispatcher(),
                    rpcRequestEventParam.getRequestGuid(), rpcRequestEventParam.isSync(), rpcRequestEventParam.getRequest());
            commit(sessionWrapper.session, requestCommitTask);
        });
    }

    /**
     * 当收到发送给客户端的rpc的响应时
     *
     * @param rpcResponseEventParam rpc响应
     */
    void onRcvClientRpcResponse(RpcResponseEventParam rpcResponseEventParam) {
        tryUpdateMessageQueue(rpcResponseEventParam, sessionWrapper -> {
            RpcPromiseInfo rpcPromiseInfo = sessionWrapper.messageQueue.getRpcPromiseInfoMap().remove(rpcResponseEventParam.getRequestGuid());
            if (null != rpcPromiseInfo) {
                commitRpcResponse(sessionWrapper.session, rpcPromiseInfo, rpcResponseEventParam.getRpcResponse());
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
            commit(sessionWrapper.session, new OneWayMessageCommitTask(sessionWrapper.session, sessionWrapper.getProtocolDispatcher(), message));
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
        /**
         * 该用户禁用的所有客户端token信息
         */
        private final ForbiddenTokenHelper forbiddenTokenHelper;

        private UserInfo(NetContext netContext, ForbiddenTokenHelper forbiddenTokenHelper) {
            this.netContext = netContext;
            this.forbiddenTokenHelper = forbiddenTokenHelper;
        }
    }

    /**
     * S2CSession的包装类，不对外暴露细节
     */
    private static final class SessionWrapper {

        /**
         * 建立session与用户的关系
         */
        private final UserInfo userInfo;

        /**
         * 注册的会话信息
         */
        private final S2CSession session;
        /**
         * 对应端口的上下文
         */
        private PortContext portContext;
        /**
         * 会话的消息队列
         */
        private final MessageQueue messageQueue = new MessageQueue();
        /**
         * 会话channel一定不为null
         */
        private Channel channel;
        /**
         * 会话当前token，一定不为null。
         * (如果preToken不为null,则客户端可能还未收到该token)
         */
        private Token token;
        /**
         * 会话过期时间(秒)(时间到则需要移除)
         */
        private int sessionTimeout;
        /**
         * 这是客户端第几次发送token验证
         */
        private int sndTokenTimes;
        /**
         * 在确认客户端收到新的token之前会保留
         */
        private Token preToken = null;
        /**
         * 不想声明为内部类
         */
        private final S2CSessionManager s2CSessionManager;

        SessionWrapper(UserInfo userInfo, S2CSession session, S2CSessionManager s2CSessionManager) {
            this.userInfo = userInfo;
            this.session = session;
            this.s2CSessionManager = s2CSessionManager;
        }

        S2CSession getSession() {
            return session;
        }

        Channel getChannel() {
            return channel;
        }

        Token getToken() {
            return token;
        }

        int getSessionTimeout() {
            return sessionTimeout;
        }

        int getSndTokenTimes() {
            return sndTokenTimes;
        }

        MessageQueue getMessageQueue() {
            return messageQueue;
        }

        Token getPreToken() {
            return preToken;
        }

        /**
         * 切换到等待状态，即确认客户端收到新的token之前，新的token还不能生效
         * (等待客户端真正的产生消息,也就是收到了新的token)
         *
         * @param channel        新的channel
         * @param portContext    所在端口的上下文
         * @param sndTokenTimes  这是对客户端第几次发送token验证
         * @param preToken       上一个token
         * @param nextToken      新的token
         * @param sessionTimeout 会话超时时间
         */
        void changeToWaitState(Channel channel, PortContext portContext, int sndTokenTimes, Token preToken, Token nextToken, int sessionTimeout) {
            this.channel = channel;
            this.portContext = portContext;
            this.token = nextToken;
            this.sessionTimeout = sessionTimeout;
            this.preToken = preToken;
            this.sndTokenTimes = sndTokenTimes;
        }

        /**
         * 切换到激活状态，确定客户端已收到了新的token
         * (收到客户端用当前channel发过来的消息)
         */
        void changeToActiveState() {
            this.preToken = null;
        }

        void setSessionTimeout(int sessionTimeout) {
            this.sessionTimeout = sessionTimeout;
        }

        /**
         * 写入数据到缓存，达到阈值时，缓冲区内容会立即发送
         *
         * @param unsentMessage 为发送的原始消息
         */
        void write(NetMessage unsentMessage) {
            s2CSessionManager.write(channel, messageQueue, unsentMessage);
        }

        /**
         * 清空缓存
         */
        void flushAllUnsentMessage() {
            s2CSessionManager.flushAllUnsentMessage(channel, messageQueue);
        }

        /**
         * 立即发送一个消息(不进入待发送队列，直接进入已发送队列)
         */
        void writeAndFlush(NetMessage unsentMessage) {
            s2CSessionManager.writeAndFlush(channel, messageQueue, unsentMessage);
        }

        /**
         * 检查是否需要清空缓冲区
         */
        void flushBuffer() {
            if (messageQueue.getUnsentQueue().size() > 0) {
                flushAllUnsentMessage();
            }
        }

        /**
         * 获取当前缓存的消息数
         * 缓存过多可能需要关闭会话
         */
        int getCacheMessageNum() {
            return messageQueue.getCacheMessageNum();
        }

        SessionLifecycleAware getLifecycleAware() {
            return portContext.lifecycleAware;
        }

        ProtocolDispatcher getProtocolDispatcher() {
            return portContext.protocolDispatcher;
        }
    }

}
