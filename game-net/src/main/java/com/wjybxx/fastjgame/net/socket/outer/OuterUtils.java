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

import com.wjybxx.fastjgame.net.common.NetMessage;
import com.wjybxx.fastjgame.net.common.PingPongMessage;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.session.SessionHandlerContext;
import com.wjybxx.fastjgame.net.socket.BatchSocketMessageTO;
import com.wjybxx.fastjgame.net.socket.MessageQueue;
import com.wjybxx.fastjgame.net.socket.SocketMessage;
import com.wjybxx.fastjgame.net.socket.SocketMessageEvent;
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
    static void emit(final Channel channel, final MessageQueue messageQueue, final int maxPendingMessages, final long ackDeadline) {
        final int cacheMessages = messageQueue.getCacheMessages();
        if (cacheMessages <= 0) {
            // 没有待发送的消息
            return;
        }
        final int emitNum = maxPendingMessages - messageQueue.getPendingMessages();
        if (emitNum <= 0) {
            // 不可以继续发送，判断它比判断isWritable的消耗更小，因此放前面
            return;
        }
        if (!channel.isWritable()) {
            // channel暂时不可写，netty自身的流量控制逻辑
            return;
        }
        final int realEmitNum = Math.min(cacheMessages, emitNum);
        if (realEmitNum == 1) {
            // 分配sequence，并添加到已发送队列
            final OuterSocketMessage outerSocketMessage = messageQueue.getCacheQueue().pollFirst();
            assert null != outerSocketMessage;
            outerSocketMessage.setSequence(messageQueue.nextSequence());
            messageQueue.getPendingQueue().add(outerSocketMessage);

            // 设置ack超时时间
            outerSocketMessage.setAckDeadline(ackDeadline);

            // 真正发送
            final OuterSocketMessageTO outerSocketMessageTO = new OuterSocketMessageTO(messageQueue.getAck(), outerSocketMessage);
            channel.writeAndFlush(outerSocketMessageTO, channel.voidPromise());
        } else {
            // 可发送多个消息，批量传输
            final List<SocketMessage> messageList = new ArrayList<>(realEmitNum);
            for (int index = 0; index < realEmitNum; index++) {
                // 分配sequence，并添加到已发送队列
                final OuterSocketMessage outerSocketMessage = messageQueue.getCacheQueue().pollFirst();
                assert null != outerSocketMessage;
                outerSocketMessage.setSequence(messageQueue.nextSequence());
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
     * @param session            session - 需要判断session的状态，以及可能需要关闭session
     * @param channel            socket对应的channel
     * @param messageQueue       消息队列
     * @param msg                要发送的消息
     * @param maxPendingMessages 最大可填充的消息数
     * @param maxCacheMessages   最大允许缓存的消息数
     * @param ackDeadline        ack超时时间
     */
    static void write(final Session session, final Channel channel,
                      final MessageQueue messageQueue, final NetMessage msg,
                      final int maxPendingMessages, final int maxCacheMessages,
                      final long ackDeadline) {

        if (session.isClosed()) {
            // session活跃状态下才发送
            return;
        }

        if (msg == PingPongMessage.INSTANCE) {
            // 心跳协议立即发送
            // 填充过多心跳协议没有意义，而且可能使得已发送队列超出限制过多
            OuterSocketMessage lastMessage = messageQueue.getPendingQueue().peekLast();
            if (null == lastMessage || lastMessage.getWrappedMessage() != msg) {
                final OuterSocketMessage outerSocketMessage = new OuterSocketMessage(msg);
                // 分配sequence，并添加到已发送队列
                outerSocketMessage.setSequence(messageQueue.nextSequence());
                messageQueue.getPendingQueue().add(outerSocketMessage);

                // 设置ack超时时间
                outerSocketMessage.setAckDeadline(ackDeadline);

                // 执行发送并flush
                OuterSocketMessageTO outerSocketMessageTO = new OuterSocketMessageTO(messageQueue.getAck(), outerSocketMessage);
                channel.writeAndFlush(outerSocketMessageTO, channel.voidPromise());
            }
            return;
        }

        if (messageQueue.getCacheMessages() >= maxCacheMessages) {
            // 超出缓存上限，关闭session
            session.close();
            return;
        }

        final OuterSocketMessage outerSocketMessage = new OuterSocketMessage(msg);
        if (messageQueue.getPendingMessages() >= maxPendingMessages) {
            // 达到流量限制，压入队列稍后发送
            messageQueue.getCacheQueue().addLast(outerSocketMessage);
            return;
        }

        if (!channel.isWritable()) {
            // channel暂时不可写 - 这是netty自身的流量整形功能
            messageQueue.getCacheQueue().addLast(outerSocketMessage);
            return;
        }

        // 未达流量限制条件，且channel可写
        // 分配sequence，并添加到已发送队列
        outerSocketMessage.setSequence(messageQueue.nextSequence());
        messageQueue.getPendingQueue().add(outerSocketMessage);

        // 设置ack超时时间
        outerSocketMessage.setAckDeadline(ackDeadline);

        // 执行发送，不着急flush
        OuterSocketMessageTO outerSocketMessageTO = new OuterSocketMessageTO(messageQueue.getAck(), outerSocketMessage);
        channel.write(outerSocketMessageTO, channel.voidPromise());
    }

    /**
     * 清空socket缓冲区
     *
     * @param session      session，需要处理session的状态
     * @param channel      socket对应的channel
     * @param messageQueue 消息队列
     */
    static void flush(Session session, final Channel channel, final MessageQueue messageQueue) {
        if (session.isClosed()) {
            // session活跃状态下才发送
            return;
        }
        if (messageQueue.getPendingMessages() == 0) {
            // 没有消息
            return;
        }
        if (messageQueue.getCacheMessages() == 0) {
            // 数据全部在channel中
            channel.flush();
        }
        // else 不需要flush，真正发送的时候会flush
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
        // 3. 对重发的消息取消追踪标记
        final List<SocketMessage> socketMessageList = new ArrayList<>(pendingMessages);
        for (OuterSocketMessage socketMessage : messageQueue.getPendingQueue()) {
            socketMessage.setAckDeadline(ackDeadline);
            socketMessage.setTraced(false);
            socketMessageList.add(socketMessage);
        }

        // 执行发送，使用voidPromise，不追踪操作结果(可减少消耗)
        BatchSocketMessageTO batchSocketMessageTO = new OuterBatchSocketMessageTO(messageQueue.getAck(), socketMessageList);
        channel.writeAndFlush(batchSocketMessageTO, channel.voidPromise());
    }

    /**
     * 读取一个消息
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
        if (event.getSequence() != messageQueue.getAck()) {
            // 不是期望的下一个消息，证明出现了丢包/乱序等错误，丢弃该包 - go-back-n重传机制
            return;
        }

        if (!messageQueue.isAckOK(event.getAck())) {
            // ack错误，需要进行纠正 - 部分包未接收到
            return;
        }

        // 更新消息队列和ack
        messageQueue.updatePendingQueue(event.getAck());
        messageQueue.setAck(event.getSequence() + 1);

        // 传递给下一个handler进行逻辑处理
        ctx.fireRead(event.getWrappedMessage());

        // 继续发送消息
        emit(channel, messageQueue, maxPendingMessages, ackDeadline);
    }
}
