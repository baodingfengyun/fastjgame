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

import com.wjybxx.fastjgame.net.rpc.NetMessage;
import com.wjybxx.fastjgame.net.rpc.PingPongMessage;
import com.wjybxx.fastjgame.net.session.SessionHandlerContext;
import com.wjybxx.fastjgame.net.socket.*;
import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/17
 * github - https://github.com/hl845740757
 */
class OuterUtils {

    private OuterUtils() {

    }

    /**
     * 继续发送消息
     *
     * @param channel            socket对应的channel
     * @param messageQueue       消息队列
     * @param maxPendingMessages 最大填充消息数
     * @param ackDeadline        ack超时时间
     */
    private static void emit(final Channel channel, final MessageQueue messageQueue,
                             final int maxPendingMessages,
                             final long ackDeadline) {
        // 计算本批次发送的消息数
        final int emitNum = Math.min(messageQueue.getCacheMessages(), maxPendingMessages - messageQueue.getPendingMessages());
        if (emitNum <= 0) {
            // 暂时没有消息可发送
            return;
        }

        if (!channel.isWritable()) {
            // channel暂时不可写(包括关闭)，netty自身的流量控制逻辑
            return;
        }

        if (emitNum == 1) {
            // 添加到已发送队列
            final OuterSocketMessage outerSocketMessage = messageQueue.getCacheQueue().pollFirst();
            assert null != outerSocketMessage;
            messageQueue.getPendingQueue().add(outerSocketMessage);

            // 设置ack超时时间
            outerSocketMessage.setAckDeadline(ackDeadline);

            // 真正发送
            final OuterSocketMessageTO outerSocketMessageTO = new OuterSocketMessageTO(messageQueue.getAck(), outerSocketMessage);
            channel.writeAndFlush(outerSocketMessageTO, channel.voidPromise());
        } else {
            // 可发送多个消息，批量传输
            final List<SocketMessage> messageList = new ArrayList<>(emitNum);
            for (int index = 0; index < emitNum; index++) {
                // 添加到已发送队列
                final OuterSocketMessage outerSocketMessage = messageQueue.getCacheQueue().pollFirst();
                assert null != outerSocketMessage;
                messageQueue.getPendingQueue().add(outerSocketMessage);

                // 设置ack超时时间
                outerSocketMessage.setAckDeadline(ackDeadline);

                // 添加到传输列表
                messageList.add(outerSocketMessage);
            }
            // 真正发送
            final OuterBatchSocketMessageTO batchSocketMessageTO = new OuterBatchSocketMessageTO(messageQueue.getAck(), messageList);
            channel.writeAndFlush(batchSocketMessageTO, channel.voidPromise());
        }
    }

    /**
     * 尝试发送一个消息
     *
     * @param ctx                handler的上下文
     * @param channel            socket对应的channel
     * @param messageQueue       消息队列
     * @param maxCacheMessages   最大允许缓存的消息数
     * @param msg                要发送的消息
     * @param maxPendingMessages 最大填充消息数
     * @param ackDeadline        ack超时时间
     */
    static void write(final SessionHandlerContext ctx, final Channel channel,
                      final MessageQueue messageQueue, final int maxCacheMessages,
                      final NetMessage msg,
                      int maxPendingMessages, long ackDeadline) {

        if (ctx.session().isClosed()) {
            // session已关闭，丢弃消息
            return;
        }

        if (msg == PingPongMessage.PING || msg == PingPongMessage.PONG) {
            // 心跳协议立即发送 - 且不入队列
            SocketPingPongMessageTO pingPongMessageTO = new OuterPingPongMessageTO(messageQueue.getAck(), (PingPongMessage) msg);
            channel.writeAndFlush(pingPongMessageTO, channel.voidPromise());
            return;
        }

        if (messageQueue.getCacheMessages() >= maxCacheMessages) {
            // 超出缓存上限，关闭session
            ctx.session().close();
            return;
        }

        // 压入缓存队列稍后发送
        final OuterSocketMessage outerSocketMessage = new OuterSocketMessage(messageQueue.nextSequence(), msg);
        messageQueue.getCacheQueue().addLast(outerSocketMessage);

        if (messageQueue.getCacheMessages() >= maxPendingMessages / 2
                && messageQueue.getPendingMessages() <= maxPendingMessages / 2) {
            // 缓存的足够多了，尝试发送（如果待确认的消息超过限制的半数，那么不发送）
            emit(channel, messageQueue, maxPendingMessages, ackDeadline);
        }
    }

    /**
     * 清空缓冲队列
     *
     * @param ctx                handler的上下文
     * @param channel            socket对应的channel
     * @param messageQueue       消息队列
     * @param maxPendingMessages 最大填充消息数
     * @param ackDeadline        ack超时时间
     */
    static void flush(SessionHandlerContext ctx, final Channel channel,
                      final MessageQueue messageQueue, final int maxPendingMessages,
                      final long ackDeadline) {
        if (ctx.session().isClosed()) {
            // session已关闭，丢弃消息
            return;
        }
        emit(channel, messageQueue, maxPendingMessages, ackDeadline);
    }

    /**
     * 重发填充队列中的消息
     *
     * @param channel      socket对应的channel
     * @param messageQueue 消息队列
     * @param ackDeadline  ack超时时间
     */
    static void resend(final Channel channel, final MessageQueue messageQueue, final long ackDeadline) {
        final int pendingMessages = messageQueue.getPendingMessages();
        if (pendingMessages == 0) {
            // 没有消息待发送
            return;
        }
        // 注意：
        // 1. 必须进行拷贝，因为原列表可能在发出之后被修改，不可共享
        // 2. 每次真正发送的时候都需要更新ack超时时间
        // 3. 必须要发送出去，因此不判断channel的可写状态
        final ArrayList<SocketMessage> socketMessageList = new ArrayList<>(pendingMessages);
        for (OuterSocketMessage socketMessage : messageQueue.getPendingQueue()) {
            socketMessage.setAckDeadline(ackDeadline);
            socketMessageList.add(socketMessage);
        }

        // 执行发送，使用voidPromise，不追踪操作结果(可减少消耗)
        BatchSocketMessageTO batchSocketMessageTO = new OuterBatchSocketMessageTO(messageQueue.getAck(), socketMessageList);
        channel.writeAndFlush(batchSocketMessageTO, channel.voidPromise());
    }

    /**
     * 读取用户消息
     *
     * @param ctx                读取消息的handler对应的ctx，需要向下传递消息
     * @param event              消息事件
     * @param messageQueue       消息队列 - 需要校验sequence、ack，以及发送消息
     * @param channel            socket对应的channel
     * @param maxPendingMessages 最大可填充的消息数
     * @param ackDeadline        ack超时时间
     */
    static void readMessage(final SessionHandlerContext ctx, final SocketMessageEvent event,
                            final MessageQueue messageQueue,
                            final Channel channel,
                            final int maxPendingMessages,
                            final long ackDeadline) {

        // 1. 先尝试更新ack
        if (event.getSequence() == messageQueue.getAck()) {
            // 是期望的消息
            messageQueue.setAck(event.getSequence() + 1);

            // 传递给下一个handler进行逻辑处理
            ctx.fireRead(event.getWrappedMessage());
        }

        // 2. 立即确认 - 通过心跳返回最新的ack
        if (event.isEndOfBatch()) {
            // 触发一次心跳读写 - 立即返回一个心跳响应包进行捎带确认
            // 比使用session的fireWrite要优雅一点
            ctx.fireRead(PingPongMessage.PING);
        }

        // 3. 更新发送队列
        if (messageQueue.isAckOK(event.getAck())) {
            // 更新消息队列
            messageQueue.updatePendingQueue(event.getAck());

            // 继续发送消息
            emit(channel, messageQueue, maxPendingMessages, ackDeadline);
        }
    }

    /**
     * 读取心跳消息
     *
     * @param ctx                读取消息的handler对应的ctx，需要向下传递消息
     * @param event              消息事件
     * @param messageQueue       消息队列 - 需要校验sequence、ack，以及发送消息
     * @param channel            socket对应的channel
     * @param maxPendingMessages 最大可填充的消息数
     * @param ackDeadline        ack超时时间
     */
    static void readPingPong(final SessionHandlerContext ctx, final SocketPingPongEvent event,
                             final MessageQueue messageQueue,
                             final Channel channel,
                             final int maxPendingMessages,
                             final long ackDeadline) {

        // 1. 立即响应心跳 - 附带捎带确认
        ctx.fireRead(event.getPingOrPong());

        // 2. 更新发送队列
        if (messageQueue.isAckOK(event.getAck())) {
            // 更新消息队列
            messageQueue.updatePendingQueue(event.getAck());

            // 继续发送消息
            emit(channel, messageQueue, maxPendingMessages, ackDeadline);
        }
    }
}
