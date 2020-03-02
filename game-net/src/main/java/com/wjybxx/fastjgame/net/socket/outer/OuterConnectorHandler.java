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

package com.wjybxx.fastjgame.net.socket.outer;

import com.wjybxx.fastjgame.net.manager.NettyThreadManager;
import com.wjybxx.fastjgame.net.misc.HostAndPort;
import com.wjybxx.fastjgame.net.rpc.*;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.session.SessionDuplexHandlerAdapter;
import com.wjybxx.fastjgame.net.session.SessionHandlerContext;
import com.wjybxx.fastjgame.net.socket.*;
import com.wjybxx.fastjgame.net.utils.NetUtils;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.concurrent.Promise;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * session客户端方维持socket使用的handler
 * <p>
 * Q: 为何{@link OuterConnectorHandler}如此复杂而{@link OuterAcceptorHandler}代码较为简单？
 * A: 扩展客户端比扩展服务器更为简单！通过扩展客户端代码增加功能比扩展服务器更加容易。
 * <p>
 * 需要将该代码翻译为前端语言，玩家与服务器之间使用该模式进行通信。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/16
 * github - https://github.com/hl845740757
 */
public class OuterConnectorHandler extends SessionDuplexHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(OuterConnectorHandler.class);

    private final HostAndPort remoteAddress;
    private final ChannelInitializer<SocketChannel> initializer;
    private final NettyThreadManager nettyThreadManager;

    private SessionHandlerContext ctx;
    private SocketSessionConfig config;
    private Promise<Session> _connectPromise;
    /**
     * 消息队列
     */
    private final MessageQueue messageQueue = new MessageQueue();
    /**
     * 发起验证请求的次数 - 每次发起验证请求时增加
     */
    private int verifyingTimes = 0;
    /**
     * 验证成功的次数 - 每次收到成功建立连接应答时增加
     */
    private int verifiedTimes = 0;
    /**
     * 真正通信的channel - 每次建立socket时更新
     */
    private Channel channel;
    /**
     * socket当前状态(不为null)
     */
    private HandlerState state;
    /**
     * 关闭时是否通知对方 - (主动关闭时通知)
     */
    private boolean notify = true;

    public OuterConnectorHandler(HostAndPort remoteAddress,
                                 ChannelInitializer<SocketChannel> initializer,
                                 NettyThreadManager nettyThreadManager,
                                 Promise<Session> connectPromise) {
        this.remoteAddress = remoteAddress;
        this.initializer = initializer;
        this.nettyThreadManager = nettyThreadManager;
        this._connectPromise = connectPromise;
    }

    @Override
    public void handlerAdded(SessionHandlerContext ctx) throws Exception {
        // 缓存，减少堆栈深度
        this.ctx = ctx;
        config = (SocketSessionConfig) ctx.session().config();

        // 尝试建立socket
        changeState(new ConnectingState());
    }

    @Override
    public void tick(SessionHandlerContext ctx) throws Exception {
        state.tick();
    }

    @Override
    public void onSessionActive(SessionHandlerContext ctx) throws Exception {
        ConcurrentUtils.safeExecute(ctx.appEventLoop(), new ConnectAwareTask(ctx.session()));
        ctx.fireSessionActive();
    }

    @Override
    public void onSessionInactive(SessionHandlerContext ctx) throws Exception {
        ConcurrentUtils.safeExecute(ctx.appEventLoop(), new DisconnectAwareTask(ctx.session()));
        ctx.fireSessionInactive();
    }

    @Override
    public void read(SessionHandlerContext ctx, Object msg) {
        final SocketSessionImp session = (SocketSessionImp) ctx.session();
        final SocketEvent event = (SocketEvent) msg;
        if (session.isClosed()) {
            // session已关闭，不再处理事件。
            // 这里不能判断isActive，因为需要在未激活的情况下处理一些事件
            NetUtils.closeQuietly(event.channel());
            return;
        }
        if (event.channel() != channel) {
            // 错误的channel
            NetUtils.closeQuietly(event.channel());
            return;
        }

        if (event instanceof SocketMessageEvent) {
            // 对方的消息 - 出现的概率更大，因此放在其他事件前面
            state.readMessage((SocketMessageEvent) event);
            return;
        }

        if (event instanceof SocketPingPongEvent) {
            // 心跳事件
            state.readPingPong((SocketPingPongEvent) event);
            return;
        }

        if (event instanceof SocketChannelInactiveEvent) {
            // socket断开连接
            state.onChannelInactive((SocketChannelInactiveEvent) event);
            return;
        }

        if (event instanceof SocketConnectResponseEvent) {
            // 建立连接响应 或 通知关闭
            final SocketConnectResponseEvent connectResponseEvent = (SocketConnectResponseEvent) event;
            if (connectResponseEvent.isClose()) {
                // 通知关闭连接
                notify = false;
                this.ctx.session().close();
                return;
            }
            // 建立连接应答
            state.onRcvConnectResponse(connectResponseEvent);
        }
    }

    @Override
    public void write(SessionHandlerContext ctx, Object msg) throws Exception {
        state.write((NetMessage) msg);
    }

    @Override
    public void flush(SessionHandlerContext ctx) throws Exception {
        state.flush();
    }

    @Override
    public void close(SessionHandlerContext ctx) throws Exception {
        if (notify) {
            notifyServerExit();
        }

        messageQueue.cleanMessageQueue();

        final Promise<Session> connectPromise = detachConnectPromise();
        if (null != connectPromise) {
            // 无法建立连接
            connectPromise.tryFailure(new IOException("connect failure"));
        }

        if (logger.isDebugEnabled()) {
            // 打印关闭原因
            logger.debug("close stacktrace {} ", ExceptionUtils.getStackTrace(new RuntimeException()));
        }
    }

    private void notifyServerExit() {
        // 通知服务器关闭session
        final SocketConnectRequest connectRequest = new SocketConnectRequest(verifyingTimes, verifiedTimes);
        final OuterSocketConnectRequestTO connectRequestTO =
                new OuterSocketConnectRequestTO(messageQueue.getInitSequence(), messageQueue.getAck(), true, connectRequest);
        channel.writeAndFlush(connectRequestTO)
                .addListener(ChannelFutureListener.CLOSE);
    }

    // ------------------------------------------ 状态机管理 --------------------------------------------

    private void changeState(@Nullable HandlerState newState) {
        this.state = newState;
        if (null != newState) {
            newState.enter();
        }
    }

    private abstract class HandlerState {

        /**
         * 进入状态
         */
        abstract void enter();

        /**
         * 刷帧
         */
        abstract void tick();

        // Q: 为什么没有exit方法？
        // A: 切换状态的原因，以及新状态不是很好确定，因此不要在exit做清理，不够安全

        /**
         * 监听到socket断开连接
         */
        abstract void onChannelInactive(SocketChannelInactiveEvent event);

        /**
         * 接收到建立连接应答
         */
        void onRcvConnectResponse(SocketConnectResponseEvent event) {
        }

        /**
         * 接收到对方的一个消息
         */
        void readMessage(SocketMessageEvent event) {
        }

        /**
         * 接收到一个心跳事件
         */
        void readPingPong(SocketPingPongEvent event) {
        }

        /**
         * 接收到用户的写请求
         */
        void write(NetMessage msg) {
            if (ctx.session().isClosed()) {
                // session已关闭，丢弃消息
                return;
            }

            if (msg == PingPongMessage.PING || msg == PingPongMessage.PONG) {
                // 心跳消息丢弃
                return;
            }

            if (messageQueue.getCacheMessages() >= config.maxCacheMessages()) {
                // 超出缓存限制
                ctx.session().close();
                return;
            }

            // 放入缓存队列，稍后发送
            messageQueue.getCacheQueue().addLast(new OuterSocketMessage(messageQueue.nextSequence(), msg));
        }

        void flush() {

        }
    }

    /**
     * 重建socket状态
     */
    private class ConnectingState extends HandlerState {

        /**
         * 已尝试连接次数
         */
        private int tryTimes = 0;
        /**
         * 连接开始时间
         */
        private long connectStartTime = 0;
        /**
         * 获取建立连接结果的future
         */
        private ChannelFuture channelFuture;

        @Override
        void enter() {
            // 关闭旧连接
            NetUtils.closeQuietly(channel);
            doConnect();
        }

        private void doConnect() {
            tryTimes++;
            connectStartTime = ctx.timerSystem().curTimeMillis();

            channelFuture = nettyThreadManager.connectAsyn(
                    remoteAddress,
                    config.sndBuffer(),
                    config.rcvBuffer(),
                    config.connectTimeoutMs(),
                    initializer);
            channel = channelFuture.channel();
        }

        @Override
        void tick() {
            if (channelFuture.isDone()) {
                // 操作完成
                if (channelFuture.isSuccess()) {
                    // 建立连接成功
                    changeState(new VerifyingState());
                } else {
                    // 建立连接失败
                    retryConnect();
                }
                return;
            }

            // - 操作尚未完成
            if (ctx.timerSystem().curTimeMillis() - connectStartTime < config.connectTimeoutMs()) {
                // 还未超时
                return;
            }

            // 重试连接
            retryConnect();
        }

        private void retryConnect() {
            // 本次建立连接超时，关闭当前future,并再次尝试
            NetUtils.closeQuietly(channelFuture);

            if (tryTimes < config.maxConnectTimes()) {
                // 还可以继续尝试
                doConnect();
            } else {
                // 无法连接到服务器，移除会话，结束
                ctx.session().close();
            }
        }

        @Override
        void onChannelInactive(SocketChannelInactiveEvent event) {
            // socket断开连接了，重新建立socket
            retryConnect();
        }
    }

    private class VerifyingState extends HandlerState {

        /**
         * 进入验证状态时的验证次数
         */
        private int enterStateVerifyingTimes;
        /**
         * 验证开始时间
         */
        private long verifyingStartTimeMillis;

        @Override
        void enter() {
            enterStateVerifyingTimes = verifyingTimes;
            doVerify();
        }

        /**
         * 发起验证请求
         */
        private void doVerify() {
            verifyingTimes++;
            verifyingStartTimeMillis = ctx.timerSystem().curTimeMillis();

            final SocketConnectRequest connectRequest = new SocketConnectRequest(verifyingTimes, verifiedTimes);
            final OuterSocketConnectRequestTO connectRequestTO =
                    new OuterSocketConnectRequestTO(messageQueue.getInitSequence(), messageQueue.getAck(), false, connectRequest);
            channel.writeAndFlush(connectRequestTO, channel.voidPromise());
        }

        @Override
        void tick() {
            if (ctx.timerSystem().curTimeMillis() - verifyingStartTimeMillis <= config.verifyTimeoutMs()) {
                // 应答还未超时，继续等待
                return;
            }

            // -- 等待建立连接应答超时
            if (verifyingTimes - enterStateVerifyingTimes >= config.maxVerifyTimes()) {
                // 达到最大验证次数，还是没有对应的建立连接应答，重新建立socket -- 未来可能直接关闭session
                changeState(new ConnectingState());
            } else {
                // 重发验证请求
                doVerify();
            }
        }

        @Override
        void onChannelInactive(SocketChannelInactiveEvent event) {
            // socket关闭，重新建立连接
            changeState(new ConnectingState());
        }

        @Override
        void onRcvConnectResponse(SocketConnectResponseEvent event) {
            final SocketConnectResponse connectResponse = event.getConnectResponse();
            if (connectResponse.getVerifyingTimes() != verifyingTimes
                    || connectResponse.getVerifiedTimes() != verifiedTimes) {
                // 不是对应的应答
                return;
            }

            if (!connectResponse.isSuccess()) {
                // 验证失败 - 禁止建立连接
                ctx.session().close();
                return;
            }

            // 不论首次建立连接应答，还是重连，服务器的ack都应该是正确的了
            if (!messageQueue.isAckOK(event.getAck())) {
                // 收到的ack有误(有丢包)，这里重连已没有意义(始终有消息漏掉了，无法恢复)
                ctx.session().close();
                return;
            }

            // 验证成功
            verifiedTimes++;

            if (verifiedTimes == 1) {
                // 首次建立连接成功 - 需要初始化ack
                messageQueue.setAck(event.getInitSequence() + 1);
            } else {
                // 重连成功 - 需要更新消息队列
                messageQueue.updatePendingQueue(event.getAck());
            }

            // 切换到验证成功状态，执行余下逻辑
            changeState(new VerifiedState());
        }
    }

    /**
     * 删除{@link #_connectPromise}并返回
     */
    private Promise<Session> detachConnectPromise() {
        Promise<Session> result = this._connectPromise;
        this._connectPromise = null;
        return result;
    }

    private class VerifiedState extends HandlerState {

        @Override
        void enter() {
            final SocketSessionImp session = (SocketSessionImp) ctx.session();
            final Promise<Session> connectPromise = detachConnectPromise();
            if (connectPromise != null) {
                // 首次建立连接成功
                assert verifiedTimes == 1;
                if (connectPromise.trySuccess(session)) {
                    // 激活session并初始化管道
                    session.tryActive();
                    session.pipeline()
                            .addLast(new PingPingSupportHandler())
                            .addLast(new LazySerializeSupportHandler())
                            .addLast(new OneWaySupportHandler());

                    // 判断是否支持rpc
                    if (session.config().isRpcAvailable()) {
                        session.pipeline().addLast(new RpcSupportHandler());
                    }

                    // 通知session激活事件
                    session.pipeline().fireSessionActive();
                } else {
                    // 用户取消了连接
                    session.closeForcibly();
                }
            } else {
                // 客户端重连成功之后要干两件事：
                // 1. 触发一次读，避免session超时
                ctx.fireRead(PingPongMessage.PONG);
                // 2. 重发消息
                OuterUtils.resend(channel, messageQueue, ctx.timerSystem().curTimeMillis() + config.ackTimeoutMs());
            }
        }

        @Override
        void tick() {
            // 检查ack超时
            final OuterSocketMessage firstMessage = messageQueue.getPendingQueue().peekFirst();
            if (null != firstMessage && ctx.timerSystem().curTimeMillis() > firstMessage.getAckDeadline()) {
                // ack超时，进行重传验证
                changeState(new VerifyingState());
                return;
            }

            // 清空缓冲队列
            OuterUtils.flush(ctx, channel,
                    messageQueue, config.maxPendingMessages(),
                    ctx.timerSystem().curTimeMillis() + config.ackTimeoutMs());
        }

        @Override
        void onChannelInactive(SocketChannelInactiveEvent event) {
            changeState(new ConnectingState());
        }

        @Override
        void readMessage(SocketMessageEvent event) {
            OuterUtils.readMessage(ctx, event, messageQueue,
                    channel,
                    config.maxPendingMessages(),
                    ctx.timerSystem().curTimeMillis() + config.ackTimeoutMs());
        }

        @Override
        void readPingPong(SocketPingPongEvent event) {
            OuterUtils.readPingPong(ctx, event, messageQueue,
                    channel,
                    config.maxPendingMessages(),
                    ctx.timerSystem().curTimeMillis() + config.ackTimeoutMs());
        }

        @Override
        void write(NetMessage msg) {
            OuterUtils.write(ctx, channel,
                    messageQueue, config.maxCacheMessages(),
                    msg,
                    config.maxPendingMessages(),
                    ctx.timerSystem().curTimeMillis() + config.ackTimeoutMs());
        }

        @Override
        void flush() {
            OuterUtils.flush(ctx, channel,
                    messageQueue, config.maxPendingMessages(),
                    ctx.timerSystem().curTimeMillis() + config.ackTimeoutMs());
        }
    }

}
