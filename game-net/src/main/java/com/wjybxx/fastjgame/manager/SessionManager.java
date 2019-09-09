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

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.FastCollectionsUtils;
import io.netty.channel.Channel;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

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
public abstract class SessionManager {

    protected final NetConfigManager netConfigManager;
    protected final NetTimeManager netTimeManager;

    protected SessionManager(NetConfigManager netConfigManager, NetTimeManager netTimeManager) {
        this.netConfigManager = netConfigManager;
        this.netTimeManager = netTimeManager;
    }

    /**
     * 发送一个单向消息到远程
     *
     * @param localGuid  我的标识
     * @param remoteGuid 远程节点标识
     * @param message    单向消息内容
     */
    public abstract void sendOneWayMessage(long localGuid, long remoteGuid, @Nonnull Object message);

    /**
     * 向远程发送一个异步rpc请求
     *
     * @param localGuid     我的标识
     * @param remoteGuid    远程节点标识
     * @param request       rpc请求内容
     * @param timeoutMs     超时时间，大于0
     * @param userEventLoop 用户线程
     * @param rpcCallback   rpc回调
     */
    public abstract void sendRpcRequest(long localGuid, long remoteGuid, @Nonnull Object request, long timeoutMs, EventLoop userEventLoop, RpcCallback rpcCallback);

    /**
     * 向远程发送一个同步rpc请求
     *
     * @param localGuid  我的标识
     * @param remoteGuid 远程节点标识
     * @param request    rpc请求内容
     * @param timeoutMs  超时时间，大于0
     * @param rpcPromise 用于监听结果
     */
    public abstract void sendSyncRpcRequest(long localGuid, long remoteGuid, @Nonnull Object request, long timeoutMs, RpcPromise rpcPromise);

    /**
     * 发送rpc响应
     *
     * @param localGuid   我的id
     * @param remoteGuid  远程节点id
     * @param requestGuid 请求对应的编号
     * @param sync        是否是同步rpc调用，如果是同步rpc调用，需要立即发送，不进入缓存。
     * @param response    响应结果
     */
    public abstract void sendRpcResponse(long localGuid, long remoteGuid, long requestGuid, boolean sync, @Nonnull RpcResponse response);

    /**
     * 删除指定session
     *
     * @param localGuid  我的id
     * @param remoteGuid 远程节点id
     * @param reason     删除会话的原因
     * @return 删除成功，或节点已删除，则返回true。
     */
    public abstract boolean removeSession(long localGuid, long remoteGuid, String reason);

    /**
     * 删除指定用户的所有session
     *
     * @param localGuid 我的id
     * @param reason    删除会话的原因
     */
    public abstract void removeUserSession(long localGuid, String reason);

    /**
     * 当检测到用户所在的线程终止
     *
     * @param userEventLoop 用户所在的EventLoop
     */
    public abstract void onUserEventLoopTerminal(EventLoop userEventLoop);

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
        if (netConfigManager.flushThreshold() <= 0) {
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
    protected final void writeAndFlush(Channel channel, MessageQueue messageQueue, NetMessage unsentMessage) {
        alloSequenceAndUpdateAckTimeout(messageQueue, unsentMessage);
        channel.writeAndFlush(new SingleMessageTO(messageQueue.getAck(), unsentMessage), channel.voidPromise());
    }

    /**
     * 清空发送缓冲区
     *
     * @param channel      会话关联的channel
     * @param messageQueue 消息队列
     */
    protected final void flushAllUnsentMessage(Channel channel, MessageQueue messageQueue) {
        if (messageQueue.getUnsentQueue().size() == 1) {
            writeAndFlush(channel, messageQueue, messageQueue.getUnsentQueue().pollFirst());
            return;
        }
        LinkedList<NetMessage> unsentMessages = messageQueue.exchangeUnsentMessages();
        for (NetMessage unsentMessage : unsentMessages) {
            alloSequenceAndUpdateAckTimeout(messageQueue, unsentMessage);
        }
        // 批量发送
        channel.writeAndFlush(new BatchMessageTO(messageQueue.getAck(), unsentMessages), channel.voidPromise());
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
    // ----------------------------------------------------------- 提交消息 --------------------------------------------------------------

    /**
     * 立即提交一个消息给应用层
     *
     * @param session    会话信息
     * @param commitTask 准备提交的消息
     */
    protected final void commit(Session session, CommitTask commitTask) {
        if (session.isActive()) {
            ConcurrentUtils.tryCommit(session.localEventLoop(), commitTask);
        }
        // else 丢弃
    }

    /**
     * 提交一个rpc响应结果。
     * rpc调用必须返回一个结果，但是会话关闭的情况下，不能提交真实结果。
     * why？
     * 因为在会话关闭的情况下，单向消息、rpc请求全部被丢弃了，如果提交真实的rpc响应，会导致应用层收到消息的顺序和发送方不一样！！！
     * session关闭的状态下，要么都提交，要么都不提交，不能选择性的提交。
     *
     * @param session        会话信息
     * @param rpcPromiseInfo rpc请求的一些信息
     * @param rpcResponse    rpc结果
     */
    protected final void commitRpcResponse(Session session, RpcPromiseInfo rpcPromiseInfo, RpcResponse rpcResponse) {
        if (rpcPromiseInfo.rpcPromise != null) {
            // 同步rpc调用
            if (session.isActive()) {
                rpcPromiseInfo.rpcPromise.trySuccess(rpcResponse);
            } else {
                rpcPromiseInfo.rpcPromise.trySuccess(RpcResponse.SESSION_CLOSED);
            }
        } else {
            RpcResponseCommitTask rpcResponseCommitTask;
            if (session.isActive()) {
                rpcResponseCommitTask = new RpcResponseCommitTask(rpcResponse, rpcPromiseInfo.rpcCallback);
            } else {
                rpcResponseCommitTask = new RpcResponseCommitTask(RpcResponse.SESSION_CLOSED, rpcPromiseInfo.rpcCallback);
            }
            ConcurrentUtils.tryCommit(session.localEventLoop(), rpcResponseCommitTask);
        }
    }

    // ------------------------------------------------------- 清理操作 ------------------------------------------------

    /**
     * 清理消息队列
     *
     * @param session      session上也有需要清理的东西
     * @param messageQueue 消息队列
     */
    protected final void clearMessageQueue(Session session, MessageQueue messageQueue) {
        // 清理消息队列
        messageQueue.detachMessageQueue();
        // 取消所有rpc调用
        cleanRpcPromiseInfo(session.localEventLoop(), messageQueue.detachRpcPromiseInfoMap());
        // 清空发送缓冲区 - 后提交的rpc后取消
        session.sender().clearBuffer();
    }

    /**
     * 清理Rpc请求信息
     *
     * @param localEventLoop    用户线程
     * @param rpcPromiseInfoMap 未完成的Rpc请求
     */
    private void cleanRpcPromiseInfo(final EventLoop localEventLoop, final Long2ObjectMap<RpcPromiseInfo> rpcPromiseInfoMap) {
        if (rpcPromiseInfoMap.size() == 0) {
            return;
        }
        // 立即执行所有同步rpc调用
        FastCollectionsUtils.removeIfAndThen(rpcPromiseInfoMap,
                (k, rpcPromiseInfo) -> rpcPromiseInfo.rpcPromise != null,
                (k, rpcPromiseInfo) -> rpcPromiseInfo.rpcPromise.trySuccess(RpcResponse.SESSION_CLOSED));
        // 减少不必要的提交
        if (rpcPromiseInfoMap.size() == 0) {
            return;
        }
        // 异步rpc回调，需要提交到用户线程才能执行。
        // 这里批量提交的影响较小，因此选择批量提交
        ConcurrentUtils.tryCommit(localEventLoop, () -> {
            for (RpcPromiseInfo rpcPromiseInfo : rpcPromiseInfoMap.values()) {
                ConcurrentUtils.safeExecute((Runnable) () -> rpcPromiseInfo.rpcCallback.onComplete(RpcResponse.SESSION_CLOSED));
            }
        });
    }

}
