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

import com.wjybxx.fastjgame.concurrent.Promise;
import com.wjybxx.fastjgame.manager.NetTimeManager;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.net.common.*;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.session.SessionDuplexHandlerAdapter;
import com.wjybxx.fastjgame.net.session.SessionHandlerContext;
import com.wjybxx.fastjgame.net.socket.*;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.NetUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

import javax.annotation.Nullable;
import java.net.ConnectException;

/**
 * session客户端方维持socket使用的handler
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/16
 * github - https://github.com/hl845740757
 */
public class OuterConnectorHandler extends SessionDuplexHandlerAdapter {

    private final HostAndPort remoteAddress;
    private final ChannelInitializer<SocketChannel> initializer;

    private NetTimeManager netTimeManager;
    private SessionHandlerContext ctx;
    private SocketSessionConfig config;
    private Promise<Session> connectPromise;
    /**
     * 消息队列
     */
    private final MessageQueue messageQueue = new MessageQueue();
    /**
     * 发起验证请求的次数
     */
    private int verifyingTimes = 0;
    /**
     * 验证成功的次数
     */
    private int verifiedTimes = 0;
    /**
     * 真正通信的channel
     */
    private Channel channel;
    /**
     * socket当前状态(不为null)
     */
    private HandlerState state;

    public OuterConnectorHandler(HostAndPort remoteAddress, ChannelInitializer<SocketChannel> initializer, Promise<Session> connectPromise) {
        this.remoteAddress = remoteAddress;
        this.initializer = initializer;
        this.connectPromise = connectPromise;
    }

    @Override
    public void handlerAdded(SessionHandlerContext ctx) throws Exception {
        // 缓存，减少堆栈深度
        netTimeManager = ctx.managerWrapper().getNetTimeManager();
        this.ctx = ctx;
        config = (SocketSessionConfig) ctx.session().config();

        // 尝试建立socket
        changeState(new ConnectingState());
    }

    @Override
    public void handlerRemoved(SessionHandlerContext ctx) throws Exception {
        super.handlerRemoved(ctx);
    }

    @Override
    public void tick(SessionHandlerContext ctx) throws Exception {
        state.tick();
    }

    @Override
    public void onSessionActive(SessionHandlerContext ctx) throws Exception {
        ConcurrentUtils.safeExecute(ctx.localEventLoop(), new ConnectAwareTask(ctx.session()));
        ctx.fireSessionActive();
    }

    @Override
    public void onSessionInactive(SessionHandlerContext ctx) throws Exception {
        ConcurrentUtils.safeExecute(ctx.localEventLoop(), new DisconnectAwareTask(ctx.session()));
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
            state.read((SocketMessageEvent) event);
            return;
        }

        if (event instanceof SocketChannelInactiveEvent) {
            // socket断开连接
            state.onChannelInactive((SocketChannelInactiveEvent) event);
            return;
        }

        if (event instanceof SocketConnectResponseEvent) {
            // 建立连接应答
            state.onRcvConnectResponse((SocketConnectResponseEvent) event);
        }
    }

    @Override
    public void write(SessionHandlerContext ctx, Object msg) throws Exception {
        if (!ctx.session().isClosed()) {
            state.write((NetMessage) msg);
        }
    }

    @Override
    public void flush(SessionHandlerContext ctx) throws Exception {
        if (!ctx.session().isClosed()) {
            state.flush();
        }
    }

    @Override
    public void close(SessionHandlerContext ctx) throws Exception {
        NetUtils.closeQuietly(channel);
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
            // 默认丢弃
        }

        /**
         * 接收到对方的一个消息
         */
        void read(SocketMessageEvent event) {
            // 默认丢弃
        }

        /**
         * 接收到用户的写请求
         */
        void write(NetMessage msg) {
            if (messageQueue.getPendingMessages() >= config.maxCacheMessages()) {
                // 超出限制了
                ctx.session().close();
                return;
            }
            // 加入缓存队列
            messageQueue.getCacheQueue().addLast(new OuterSocketMessage(msg));
        }

        /**
         * 接收到用户的清空缓冲区请求
         */
        void flush() {
            // 默认不处理
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
            doConnect();
        }

        private void doConnect() {
            tryTimes++;
            connectStartTime = netTimeManager.getSystemMillTime();

            channelFuture = ctx.managerWrapper().getNettyThreadManager().connectAsyn(
                    remoteAddress,
                    config.sndBuffer(),
                    config.rcvBuffer(),
                    config.connectTimeoutMs(),
                    initializer);
            channel = channelFuture.channel();
        }

        @Override
        void tick() {
            // 建立连接成功
            if (channelFuture.isSuccess() && channel.isActive()) {
                changeState(new VerifyingState());
                return;
            }
            // 还未超时
            if (netTimeManager.getSystemMillTime() - connectStartTime < config.connectTimeoutMs()) {
                return;
            }
            // 重试连接
            retryConnect();
        }

        private void retryConnect() {
            // 本次建立连接超时，关闭当前future,并再次尝试
            NetUtils.closeQuietly(channelFuture);

            if (tryTimes < config.maxConnectTryTimes()) {
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
        private long verifyingStateMillTime;

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
            verifyingStateMillTime = netTimeManager.getSystemMillTime();

            final SocketConnectRequest connectRequest = new SocketConnectRequest(verifyingTimes, verifiedTimes);
            final OuterSocketConnectRequestTO connectRequestTO = new OuterSocketConnectRequestTO(messageQueue.getInitSequence(), messageQueue.getAck(),
                    connectRequest);
            channel.writeAndFlush(connectRequestTO, channel.voidPromise());
        }

        @Override
        void tick() {
            if (netTimeManager.getSystemMillTime() - verifyingStateMillTime <= config.connectTimeoutMs()) {
                // 应答还未超时，继续等待
                return;
            }

            // -- 等待建立连接应答超时
            if (verifyingTimes - enterStateVerifyingTimes >= config.maxVerifyTimes()) {
                // 达到最大验证次数，还是没有对应的建立连接应答，重新建立socket -- 未来可能直接关闭session
                NetUtils.closeQuietly(channel);
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
                // 禁止建立连接
                onVerifyingFailure();
                return;
            }

            // 不论首次建立连接应答，还是重连，ack都应该是正确的了
            if (!messageQueue.isAckOK(event.getAck())) {
                // 收到的ack有误(有丢包)，这里重连已没有意义(始终有消息漏掉了，无法恢复)
                onVerifyingFailure();
                return;
            }

            if (verifiedTimes == 0) {
                // 首次建立连接应答 - 需要初始化ack
                messageQueue.setAck(event.getInitSequence() + 1);
            } else {
                // 重连成功 - 需要更新消息队列
                messageQueue.updatePendingQueue(event.getAck());
            }

            // 验证成功
            changeState(new VerifiedState());
        }

        private void onVerifyingFailure() {
            if (connectPromise != null) {
                // 告知用户建立连接失败
                connectPromise.tryFailure(new ConnectException("bad request"));
                connectPromise = null;
            }
            ctx.session().close();
        }
    }

    private class VerifiedState extends HandlerState {

        @Override
        void enter() {
            // 增加验证成功次数
            verifiedTimes++;
            final SocketSessionImp session = (SocketSessionImp) ctx.session();
            if (connectPromise != null) {
                if (connectPromise.trySuccess(session)) {
                    // 激活session并初始化管道
                    session.tryActive();
                    session.pipeline()
                            .addLast(new PingSupportHandler())
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
                connectPromise = null;
            } else {
                // 重发消息
                OuterUtils.resend(session, channel, messageQueue, netTimeManager.getSystemMillTime() + config.ackTimeoutMs());
            }
        }

        @Override
        void tick() {
            // 检查ack超时
            final OuterSocketMessage firstMessage = messageQueue.getPendingQueue().peekFirst();
            if (null != firstMessage) {
                if (netTimeManager.getSystemMillTime() >= firstMessage.getAckDeadline()) {
                    // ack超时了，进行重传验证
                    changeState(new VerifyingState());
                    return;
                }

                // 因为采用的是捎带确认，且一个消息不一定对应的返回，因此需要别的方式进行确认
                // 如果在过去一定时间之后还未收到确认消息，立即发送一个心跳，尝试对前面的消息进行确认 - 心跳对方会立即返回
                if (!firstMessage.isTraced() && firstMessage.getAckDeadline() - netTimeManager.getSystemMillTime() < config.ackTimeoutMs() / 2) {
                    firstMessage.setTraced(true);
                    // 调用session的fireWrite方法，使得能流经心跳控制逻辑
                    ctx.session().fireWrite(PingPongMessage.INSTANCE);
                }
            }
            // 继续发送消息
            OuterUtils.emit(channel, messageQueue, config.maxPendingMessages(), netTimeManager.getSystemMillTime() + config.ackTimeoutMs());
        }

        @Override
        void onChannelInactive(SocketChannelInactiveEvent event) {
            changeState(new ConnectingState());
        }

        @Override
        void onRcvConnectResponse(SocketConnectResponseEvent event) {
            final SocketConnectResponse connectResponse = event.getConnectResponse();
            // 对方保存的是我上一次的请求信息
            if (connectResponse.getVerifyingTimes() != verifyingTimes - 1
                    || connectResponse.getVerifiedTimes() != verifiedTimes - 1) {
                // 不是对应的应答
                return;
            }

            if (!connectResponse.isSuccess()) {
                // 对方通知我关闭
                ctx.session().close();
            }
        }

        @Override
        void read(SocketMessageEvent event) {
            OuterUtils.readMessage(ctx, event, messageQueue,
                    channel,
                    config.maxPendingMessages(),
                    netTimeManager.getSystemMillTime() + config.ackTimeoutMs());
        }

        @Override
        void write(NetMessage msg) {
            OuterUtils.write(ctx.session(), channel, messageQueue,
                    msg,
                    config.maxPendingMessages(),
                    config.maxCacheMessages(),
                    netTimeManager.getSystemMillTime() + config.ackTimeoutMs());
        }

        @Override
        void flush() {
            OuterUtils.flush(ctx.session(), channel, messageQueue);
        }
    }

}
