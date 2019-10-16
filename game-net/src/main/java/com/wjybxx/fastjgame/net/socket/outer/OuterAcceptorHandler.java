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

import com.wjybxx.fastjgame.manager.AcceptorManager;
import com.wjybxx.fastjgame.manager.NetManagerWrapper;
import com.wjybxx.fastjgame.net.common.*;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.session.SessionDuplexHandlerAdapter;
import com.wjybxx.fastjgame.net.session.SessionHandlerContext;
import com.wjybxx.fastjgame.net.socket.*;
import com.wjybxx.fastjgame.net.socket.inner.InnerPongSupportHandler;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.NetUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;

import java.util.ArrayList;
import java.util.List;

/**
 * session服务端方维持socket使用的handler
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/16
 * github - https://github.com/hl845740757
 */
public class OuterAcceptorHandler extends SessionDuplexHandlerAdapter {

    private int maxPendingMessages;
    private int maxCacheMessages;
    /**
     * 会话channel一定不为null
     */
    private Channel channel;
    /**
     * 消息队列
     */
    private final MessageQueue messageQueue = new MessageQueue();
    /**
     * 这是客户端第几次请求成功的
     */
    private int verifyingTimes;

    private OuterAcceptorHandler(Channel channel, int verifyingTimes) {
        this.channel = channel;
        this.verifyingTimes = verifyingTimes;
    }

    @Override
    public void handlerAdded(SessionHandlerContext ctx) throws Exception {
        final SocketSessionConfig config = (SocketSessionConfig) ctx.session().config();
        maxPendingMessages = config.maxPendingMessages();
        maxCacheMessages = config.maxCacheMessages();
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
        SocketEvent event = (SocketEvent) msg;
        if (event.channel() != channel) {
            // 这里不包含建立连接请求，因此channel必须相等
            NetUtils.closeQuietly(event.channel());
            return;
        }
        if (event instanceof SocketDisconnectEvent) {
            // socket断开连接了，等待客户端重连
            return;
        }
        // -- 走到这应该是messageEvent
        SocketMessageEvent messageEvent = (SocketMessageEvent) msg;
        if (messageEvent.getSequence() != messageQueue.getAck()) {
            // 不是期望的下一个消息，证明出现了丢包等错误，丢弃该包 - go-back-n重传机制
            return;
        }
        if (!messageQueue.isAckOK(messageEvent.getAck())) {
            // ack错误，需要进行纠正 - 部分包未接收到
            return;
        }
        // 更新消息队列和ack
        messageQueue.updateSentQueue(messageEvent.getAck());
        messageQueue.setAck(messageEvent.getSequence() + 1);

        // 继续发送消息
        emit(ctx);

        // 传递给下一个handler
        ctx.fireRead(messageEvent.getWrappedMessage());
    }

    private void emit(SessionHandlerContext ctx) {
        int cacheMessages = messageQueue.getCacheMessages();
        if (cacheMessages == 0) {
            // 没有待发送的消息
            return;
        }
        int emitNum = maxCacheMessages - messageQueue.getPendingMessages();
        if (emitNum <= 0) {
            // 不可以继续发送，判断它比判断isWritable的消耗更小，因此放前面
            return;
        }
        if (!channel.isWritable()) {
            // channel暂时不可写
            return;
        }
        if (cacheMessages == 1) {
            // 未发送到发送队列
            OuterSocketMessage outerSocketMessage = messageQueue.getUnsentQueue().pollFirst();
            messageQueue.getSentQueue().add(outerSocketMessage);
            // 真正发送
            OuterSocketMessageTO outerSocketMessageTO = new OuterSocketMessageTO(messageQueue.getAck(), outerSocketMessage);
            channel.write(outerSocketMessageTO);
        } else {
            int realEmitNum = Math.min(cacheMessages, emitNum);
            List<OuterSocketMessage> messageList = new ArrayList<>(realEmitNum);

        }
    }

    @Override
    public void write(SessionHandlerContext ctx, Object msg) throws Exception {
        NetMessage netMessage = (NetMessage) msg;
        OuterSocketMessage outerSocketMessage = new OuterSocketMessage(messageQueue.nextSequence(), netMessage);
        if (!channel.isWritable()) {
            // channel暂时不可写 - 这是netty自身的流量整形功能
            messageQueue.getUnsentQueue().addLast(outerSocketMessage);
            return;
        }
        if (messageQueue.getPendingMessages() < maxPendingMessages) {
            // 未达流量限制条件，直接尝试发送
            messageQueue.getSentQueue().add(outerSocketMessage);
            // 服务器不处理超时信息
            OuterSocketMessageTO outerSocketMessageTO = new OuterSocketMessageTO(messageQueue.getAck(), outerSocketMessage);
            channel.write(outerSocketMessageTO);
        } else {
            if (messageQueue.getCacheMessages() >= maxCacheMessages) {
                // 超出缓存上限，关闭session
                ctx.session().close();
                return;
            }
            // 达到流量限制，压入队列稍后发送
            messageQueue.getUnsentQueue().addLast(outerSocketMessage);
        }
    }

    @Override
    public void flush(SessionHandlerContext ctx) throws Exception {
        final OuterSocketMessage outerSocketMessage = messageQueue.getUnsentQueue().peekLast();
        if (null != outerSocketMessage) {
            // 还有消息在队列中，标记一下
            outerSocketMessage.setAutoFlush(true);
        } else {
            // 所有消息都已发送到channel，直接flush
            channel.flush();
        }
    }

    @Override
    public void close(SessionHandlerContext ctx) throws Exception {
        // 通知对方关闭，同时关闭channel
        notifyConnectFail(channel, verifyingTimes);
    }

    /**
     * 接收到一个重连请求
     */
    private void onRcvOuterConnectRequest(SessionHandlerContext ctx, SocketConnectRequestEvent event) {
        final SocketConnectRequest connectRequest = event.getConnectRequest();
        SocketSessionImp session = (SocketSessionImp) ctx.session();
        if (session.isClosed()) {
            // session 已关闭
            notifyConnectFail(event.channel(), event);
            return;
        }
        if (!messageQueue.isAckOK(event.getAck())) {
            // ack错误 - 无法重连
            notifyConnectFail(event.channel(), event);
            return;
        }
        if (connectRequest.getVerifyingTimes() <= verifyingTimes) {
            // 这是一个旧请求
            notifyConnectFail(event.channel(), event);
            return;
        }
        // -- 验证完成
        if (event.channel() != channel) {
            // 如果是新的socket，则需要关闭旧的连接
            NetUtils.closeQuietly(channel);
        }

        // 更新状态
        channel = event.channel();
        verifyingTimes = connectRequest.getVerifyingTimes();
        messageQueue.updateSentQueue(event.getAck());

        // 通知对方重连成功
        notifyConnectSuccess(channel, event, messageQueue.getAck());

        // 重发消息
        resend();
    }

    private void resend() {
        // 服务器方不需要处理ack等信息，直接发送
        // 注意：必须进行拷贝，因为原列表可能在发出之后被修改，不可共享
        BatchSocketMessageTO batchSocketMessageTO = new OuterBatchSocketMessageTO(messageQueue.getAck(),
                new ArrayList<>(messageQueue.getSentQueue()));
        // voidPromise，不追踪结果
        channel.writeAndFlush(batchSocketMessageTO, channel.voidPromise());
    }

    // ------------------------------------------------------- 建立连接请求 ----------------------------------------------

    /**
     * 接收到一个外网建立连接请求 - 开启消息确认机制的请求
     */
    public static void onRcvOuterConnectRequest(SocketConnectRequestEvent event, NetManagerWrapper netManagerWrapper, AcceptorManager acceptorManager) {
        final Channel channel = event.channel();
        final Session existSession = acceptorManager.getSession(event.sessionId());
        if (existSession == null) {
            // 尝试建立一个新的session
            if (event.getAck() != MessageQueue.INIT_ACK) {
                // 初始ack错误
                notifyConnectFail(channel, event);
                return;
            }
            // 建立连接成功
            final SocketPortContext portExtraInfo = event.getPortExtraInfo();
            final SocketSessionImp session = new SocketSessionImp(portExtraInfo.getNetContext(),
                    event.sessionId(),
                    event.remoteGuid(),
                    portExtraInfo.getSessionConfig(),
                    netManagerWrapper,
                    acceptorManager);

            // 初始化管道
            session.pipeline()
                    .addLast(new OuterAcceptorHandler(channel, event.getConnectRequest().getVerifyingTimes()))
                    .addLast(new InnerPongSupportHandler())
                    .addLast(new OneWaySupportHandler());

            // 判断是否支持rpc
            if (session.config().isRpcAvailable()) {
                session.pipeline().addLast(new RpcSupportHandler());
            }

            // 激活session并传递激活事件
            session.tryActive();
            session.pipeline().fireSessionActive();

            // 通知对方建立session成功
            notifyConnectSuccess(channel, event, MessageQueue.INIT_ACK);
        } else {
            // 尝试重连
            final SessionHandlerContext firstContext = existSession.pipeline().firstContext();
            if (firstContext == null) {
                // 错误的请求
                notifyConnectFail(channel, event);
                return;
            }
            if (firstContext.handler() instanceof OuterAcceptorHandler) {
                // 自己处理重连请求
                ((OuterAcceptorHandler) firstContext.handler()).onRcvOuterConnectRequest(firstContext, event);
            } else {
                // 错误的请求
                notifyConnectFail(channel, event);
            }
        }
    }

    /**
     * 建立连接成功 - 告知对方成功
     */
    private static void notifyConnectSuccess(Channel channel, SocketConnectRequestEvent connectRequestEvent, long ack) {
        final SocketConnectResponse connectResponse = new SocketConnectResponse(true, connectRequestEvent.getConnectRequest().getVerifyingTimes());
        final OuterSocketConnectResponseTO connectResponseTO = new OuterSocketConnectResponseTO(ack, connectResponse);
        // 告知重连成功
        channel.writeAndFlush(connectResponseTO);
    }

    /**
     * 建立连接失败 - 告知对方失败(会使对方关闭session)
     */
    private static void notifyConnectFail(Channel channel, SocketConnectRequestEvent connectRequestEvent) {
        notifyConnectFail(channel, connectRequestEvent.getConnectRequest().getVerifyingTimes());
    }

    /**
     * 建立连接失败 - 告知对方失败(会使对方关闭session)
     */
    private static void notifyConnectFail(Channel channel, int verifyingTimes) {
        final SocketConnectResponse connectResponse = new SocketConnectResponse(false, verifyingTimes);
        final OuterSocketConnectResponseTO connectResponseTO = new OuterSocketConnectResponseTO(-1, connectResponse);
        // 告知验证失败，且发送之后关闭channel - 会使对方session也关闭
        channel.writeAndFlush(connectResponseTO)
                .addListener(ChannelFutureListener.CLOSE);
    }
}
