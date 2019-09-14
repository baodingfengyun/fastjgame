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

import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.net.remote.SocketSession;
import io.netty.channel.Channel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * session管理器，定义要实现的接口。
 * <p>
 * Q: 为何要做发送缓冲区？
 * A: 减少竞争，对于实时性不高的操作，我们进行批量提交，可以大大减少竞争次数，提高吞吐量。
 * eg: 有50个网络包，我们提交50次和提交1次差距是很大的，网络包越多，差距越明显。
 * <p>
 * Q: 如何禁用缓冲区？
 * A: 修改配置文件，将要禁用的缓冲区的消息数量配置为0即可禁用。
 * 注意查看{@link com.wjybxx.fastjgame.utils.ConfigLoader}中修改配置的文件的两种方式。
 * <p>
 * Q: session关闭的情况下，是否还需要将消息发送出去呢？
 * A: 可以发送也不可以不发送，但是必须保证 要么都发送，要么都不发送！不可以选择性的发送！必须保证消息的顺序和用户发送的顺序是一致的!
 * <p>
 * Q：session关闭的情况下，是否还需要将收到的消息提交给应用层呢？
 * Q: 可以提交也可以不提交，但是必须保证 要么都提交，要么都不提交！不可以选择性的提交！必须保证消息的提交顺序和发送方的顺序是一致的!
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
public abstract class SocketSessionManager extends AbstractSessionManager {

    protected final NetConfigManager netConfigManager;
    protected final NetTimeManager netTimeManager;

    protected SocketSessionManager(NetConfigManager netConfigManager, NetTimeManager netTimeManager) {
        super(netTimeManager);
        this.netConfigManager = netConfigManager;
        this.netTimeManager = netTimeManager;
    }

    @Override
    protected abstract SocketSessionWrapper getSessionWrapper(long localGuid, long clientGuid);

    /**
     * 获取一个可写的session。
     *
     * @param localGuid  from
     * @param remoteGuid to
     * @return sessionWrapper
     */
    @Override
    protected final SocketSessionWrapper getWritableSession(long localGuid, long remoteGuid) {
        SocketSessionWrapper sessionWrapper = getSessionWrapper(localGuid, remoteGuid);
        // 会话已被删除
        if (null == sessionWrapper) {
            return null;
        }
        // 会话已被关闭（session关闭的状态下，既不发送，也不提交）
        if (!sessionWrapper.getSession().isActive()) {
            return null;
        }
        // 未确认消息过多，删除会话
        if (sessionWrapper.getUnsentMessageNum() >= netConfigManager.clientMaxCacheNum()) {
            removeSession(localGuid, remoteGuid, "cacheMessage is too much, num = " + sessionWrapper.getUnsentMessageNum());
            return null;
        } else {
            return sessionWrapper;
        }
    }

    // -----------------------------------------------------------  发送缓冲区 --------------------------------------------------------------

    /**
     * 发送一个消息，不立即发送，会先进入缓冲区，达到缓冲区阈值时，清空发送缓冲区
     *
     * @param channel       会话关联的channel
     * @param messageQueue  消息队列
     * @param unsentMessage 待发送的消息
     * @return 是否真正的执行了发送操作
     */
    protected final boolean write(Channel channel, MessageQueue messageQueue, NetMessage unsentMessage) {
        // 禁用了发送缓冲区
        if (netConfigManager.flushThreshold() <= 1) {
            writeAndFlush(channel, messageQueue, unsentMessage);
            return true;
        }
        // 启用了发送缓冲区
        messageQueue.getUnsentQueue().add(unsentMessage);
        if (messageQueue.getUnsentQueue().size() >= netConfigManager.flushThreshold()) {
            // 到达阈值，清空缓冲区
            flushAllUnsentMessage(channel, messageQueue);
            return true;
        }
        return false;
    }

    /**
     * 立即发送一个消息
     *
     * @param channel       会话关联的channel
     * @param messageQueue  消息队列
     * @param unsentMessage 待发送的消息
     */
    protected final void writeAndFlush(Channel channel, MessageQueue messageQueue, final NetMessage unsentMessage) {
        if (messageQueue.getUnsentQueue().size() == 0) {
            alloSequenceAndUpdateAckTimeout(messageQueue, unsentMessage);
            channel.writeAndFlush(new SingleMessageTO(messageQueue.getAck(), unsentMessage), channel.voidPromise());
        } else {
            messageQueue.getUnsentQueue().add(unsentMessage);
            flushAllUnsentMessage(channel, messageQueue);
        }
    }

    /**
     * 清空发送缓冲区
     *
     * @param channel      会话关联的channel
     * @param messageQueue 消息队列
     */
    protected final void flushAllUnsentMessage(Channel channel, MessageQueue messageQueue) {
        if (messageQueue.getUnsentQueue().size() == 1) {
            // 单个发送
            final NetMessage unsentMessage = messageQueue.getUnsentQueue().pollFirst();
            alloSequenceAndUpdateAckTimeout(messageQueue, unsentMessage);
            channel.writeAndFlush(new SingleMessageTO(messageQueue.getAck(), unsentMessage), channel.voidPromise());
        } else {
            // 批量发送
            LinkedList<NetMessage> unsentMessages = messageQueue.exchangeUnsentMessages();
            for (NetMessage unsentMessage : unsentMessages) {
                alloSequenceAndUpdateAckTimeout(messageQueue, unsentMessage);
            }
            channel.writeAndFlush(new BatchMessageTO(messageQueue.getAck(), unsentMessages), channel.voidPromise());
        }
    }

    /**
     * 分配sequence 和 设置ack超时时间
     *
     * @param messageQueue 消息队列
     * @param message      未发送的消息
     */
    private void alloSequenceAndUpdateAckTimeout(MessageQueue messageQueue, NetMessage message) {
        // 分配sequence
        message.setSequence(messageQueue.nextSequence());
        // 添加到已发送队列
        messageQueue.getSentQueue().add(message);
        updateAckTimeout(message);
    }

    /**
     * 更新ack超时时间
     *
     * @param netMessage 准备发送的消息
     */
    private void updateAckTimeout(NetMessage netMessage) {
        // 标记Ack超时时间
        netMessage.setTimeout(netTimeManager.getSystemMillTime() + netConfigManager.ackTimeout());
    }

    /**
     * 重发已发送但未确认的信息
     *
     * @param channel      会话关联的channel
     * @param messageQueue 消息队列
     */
    protected final void resend(Channel channel, MessageQueue messageQueue) {
        if (messageQueue.getSentQueue().size() == 1) {
            // 差点写成poll了，已发送的消息只有收到ack确认时才能删除。
            NetMessage netMessage = messageQueue.getSentQueue().peekFirst();
            updateAckTimeout(netMessage);
            channel.writeAndFlush(new SingleMessageTO(messageQueue.getAck(), netMessage), channel.voidPromise());
        } else {
            // 这里是必须申请新空间的，因为的旧的List不能修改
            List<NetMessage> netMessages = new ArrayList<>(messageQueue.getSentQueue().size());
            for (NetMessage netMessage : messageQueue.getSentQueue()) {
                updateAckTimeout(netMessage);
                netMessages.add(netMessage);
            }
            channel.writeAndFlush(new BatchMessageTO(messageQueue.getAck(), netMessages), channel.voidPromise());
        }
    }

    // ------------------------------------------------------- 清理操作 ------------------------------------------------

    /**
     * 清理消息队列
     *
     * @param sessionWrapper session保证对象
     * @param messageQueue   消息队列
     */
    protected final <T extends Session> void clearMessageQueue(ISessionWrapper<T> sessionWrapper, MessageQueue messageQueue) {
        // 清理消息队列
        messageQueue.detachMessageQueue();
        // 取消所有rpc调用
        cleanRpcPromiseInfo(sessionWrapper.getSession(), sessionWrapper.detachRpcPromiseInfoMap());
        // 清空发送缓冲区 - 后提交的rpc后取消
        sessionWrapper.getSession().sender().clearBuffer();
    }


    protected static abstract class SocketSessionWrapper<T extends SocketSession> extends ISessionWrapper<T> {

        /**
         * 基于socket的session，需要使用消息队列
         */
        protected final MessageQueue messageQueue = new MessageQueue();

        public SocketSessionWrapper(T session) {
            super(session);
        }

        MessageQueue getMessageQueue() {
            return messageQueue;
        }

        public int getUnsentMessageNum() {
            return messageQueue.getUnsentMessageNum();
        }

        @Override
        public final void sendOneWayMessage(@Nonnull Object message) {
            write(new OneWayMessage(message));
        }

        @Override
        public final void sendRpcRequest(long requestGuid, boolean sync, @Nonnull Object request) {
            if (sync) {
                writeAndFlush(new RpcRequestMessage(requestGuid, true, request));
            } else {
                write(new RpcRequestMessage(requestGuid, false, request));
            }
        }

        @Override
        public final void sendRpcResponse(long requestGuid, boolean sync, @Nonnull RpcResponse response) {
            if (sync) {
                writeAndFlush(new RpcResponseMessage(requestGuid, response));
            } else {
                write(new RpcResponseMessage(requestGuid, response));
            }
        }

        protected abstract void write(NetMessage unsentMessage);

        protected abstract void writeAndFlush(NetMessage unsentMessage);

    }
}
