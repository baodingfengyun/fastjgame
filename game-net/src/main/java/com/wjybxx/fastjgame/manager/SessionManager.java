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
import java.util.ListIterator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * session管理器，定义要实现的接口。
 *
 * Q: 为何要做发送缓冲区与接收缓冲区？
 * A: 减少竞争，对于实时性不高的操作，我们进行批量提交，可以大大减少竞争次数，提高吞吐量。
 * eg: 有50个网络包，我们提交50次和提交1次差距是很大的，网络包越多，差距越明显。
 *
 * Q: 如何禁用缓冲区？
 * A: 修改配置文件，将要禁用的缓冲区的消息数量配置为0即可禁用。
 * 注意查看{@link com.wjybxx.fastjgame.utils.ConfigLoader}中修改配置的文件的两种方式。
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
     * @param localGuid 我的标识
     * @param remoteGuid 远程节点标识
     * @param message 单向消息内容
     */
    public abstract void send(long localGuid, long remoteGuid, @Nonnull Object message);

    /**
     * 向远程发送一个异步rpc请求
     * @param localGuid 我的标识
     * @param remoteGuid 远程节点标识
     * @param request rpc请求内容
     * @param timeoutMs 超时时间，大于0
     * @param userEventLoop 用户线程
     * @param rpcCallback rpc回调
     */
    public abstract void rpc(long localGuid, long remoteGuid, @Nonnull Object request, long timeoutMs, EventLoop userEventLoop, RpcCallback rpcCallback);

    /**
     * 向远程发送一个同步rpc请求
     * @param localGuid 我的标识
     * @param remoteGuid 远程节点标识
     * @param request rpc请求内容
     * @param timeoutMs 超时时间，大于0
     * @param rpcPromise 用于监听结果
     */
    public abstract void syncRpc(long localGuid, long remoteGuid, @Nonnull Object request, long timeoutMs, RpcPromise rpcPromise);

    /**
     * 发送rpc响应
     * @param localGuid 我的id
     * @param remoteGuid 远程节点id
     * @param requestGuid 请求对应的编号
     * @param sync 是否是同步rpc调用，如果是同步rpc调用，需要立即发送，不进入缓存。
     * @param response 响应结果
     */
    public abstract void sendRpcResponse(long localGuid, long remoteGuid, long requestGuid, boolean sync, @Nonnull RpcResponse response);

    /**
     * 删除指定session
     * @param localGuid 我的id
     * @param remoteGuid 远程节点id
     * @param reason 删除会话的原因
     * @return 删除成功，或节点已删除，则返回true。
     */
    public abstract boolean removeSession(long localGuid, long remoteGuid, String reason);

    /**
     * 删除指定用户的所有session
     * @param localGuid 我的id
     * @param reason 删除会话的原因
     */
    public abstract void removeUserSession(long localGuid, String reason);

    /**
     * 当检测到用户所在的线程终止
     * @param userEventLoop 用户所在的EventLoop
     */
    public abstract void onUserEventLoopTerminal(EventLoop userEventLoop);

    // -----------------------------------------------------------  发送缓冲区 --------------------------------------------------------------

    /**
     * 发送一个消息，不立即发送，会先进入缓冲区，达到缓冲区阈值时，清空发送缓冲区
     * @param channel 会话关联的channel
     * @param messageQueue 消息队列
     * @param unsentMessage 待发送的消息
     * @return 是否真正的执行了发送操作
     */
    protected final boolean write(Channel channel, MessageQueue messageQueue, UnsentMessage unsentMessage) {
        // 禁用了发送缓冲区
        if (netConfigManager.flushThreshold() <= 0){
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
     * @param channel 会话关联的channel
     * @param messageQueue 消息队列
     * @param unsentMessage 待发送的消息
     */
    protected final void writeAndFlush(Channel channel, MessageQueue messageQueue, UnsentMessage unsentMessage) {
        channel.writeAndFlush(offerToSentQueueAndBuild(messageQueue, unsentMessage), channel.voidPromise());
    }

    /**
     * 清空发送缓冲区
     * 因何想到了不用再申请空间的骚操作的？因为之前看Collectors的源码的时候看见了。。。
     * {@link java.util.stream.Collectors#groupingBy(Function, Supplier, Collector)}。
     * @param channel 会话关联的channel
     * @param messageQueue 消息队列
     */
    @SuppressWarnings("unchecked")
    protected final void flushAllUnsentMessage(Channel channel, MessageQueue messageQueue) {
        if (messageQueue.getUnsentQueue().size() == 1){
            writeAndFlush(channel, messageQueue, messageQueue.getUnsentQueue().pollFirst());
            return;
        }
        // 使用通配符强制类型转换，绕过编译检查(不再申请空间)
        LinkedList<?> messageTOS = messageQueue.exchangeUnsentMessages();
        ListIterator<Object> iterator = (ListIterator<Object>) messageTOS.listIterator();
        UnsentMessage unsentMessage;
        while (iterator.hasNext()) {
            unsentMessage = (UnsentMessage) iterator.next();
            MessageTO messageTO = offerToSentQueueAndBuild(messageQueue, unsentMessage);
            iterator.set(messageTO);
        }
        // 批量发送
        channel.writeAndFlush(new BatchMessageTO((List<MessageTO>) messageTOS), channel.voidPromise());
    }

    /**
     * 压入已发送队列，并构建传输对象
     * @param messageQueue 消息队列
     * @param unsentMessage 未发送的消息
     * @return transferObject
     */
    private MessageTO offerToSentQueueAndBuild(MessageQueue messageQueue, UnsentMessage unsentMessage) {
        // 分配sequence，sequence也是一种资源
        SentMessage message = unsentMessage.build(messageQueue.nextSequence(), messageQueue);
        // 添加到已发送队列
        messageQueue.getSentQueue().add(message);
        return updateAckAndBuild(messageQueue, message);
    }

    /**
     * 更新ack，并构建传输对象
     * @param messageQueue 消息队列
     * @param sentMessage 准备发送的消息
     * @return transferObject
     */
    private MessageTO updateAckAndBuild(MessageQueue messageQueue, SentMessage sentMessage) {
        // 标记Ack超时时间
        sentMessage.setTimeout(netTimeManager.getSystemMillTime() + netConfigManager.ackTimeout());
        // 构建传输对象
        return sentMessage.build(messageQueue.getAck());
    }

    /**
     * 重发已发送但未确认的信息
     * @param channel 会话关联的channel
     * @param messageQueue 消息队列
     */
    protected final void resend(Channel channel, MessageQueue messageQueue) {
        if (messageQueue.getSentQueue().size() == 1) {
            // 差点写成poll了，已发送的消息只有收到ack确认时才能删除。
            SentMessage message = messageQueue.getSentQueue().peekFirst();
            channel.writeAndFlush(updateAckAndBuild(messageQueue, message), channel.voidPromise());
        } else {
            // 这里是必须申请新空间的，因为的旧的List不能修改
            List<MessageTO> messageTOS = new ArrayList<>(messageQueue.getSentQueue().size());
            for (SentMessage message:messageQueue.getSentQueue()){
                messageTOS.add(updateAckAndBuild(messageQueue, message));
            }
            channel.writeAndFlush(new BatchMessageTO(messageTOS), channel.voidPromise());
        }
    }
    // ----------------------------------------------------------- 提交消息 --------------------------------------------------------------

    /**
     * 立即提交一个消息给应用层
     * @param netContext 用户的网络上下文
     * @param session 会话信息
     * @param commitTask 准备提交的消息
     */
    protected final void commit(NetContext netContext, Session session, CommitTask commitTask) {
        // 应用层请求了关闭，可以减少提交的无效消息数量
        if (!session.isActive()){
            return;
        }
        // 直接提交到应用层
        ConcurrentUtils.tryCommit(netContext.localEventLoop(), commitTask);
    }

    /**
     * 提交一个rpc响应结果
     * @param netContext 用户的网络上下文
     * @param session 会话信息
     * @param rpcPromiseInfo rpc请求的一些信息
     * @param rpcResponse rpc结果
     */
    protected final void commitRpcResponse(NetContext netContext, Session session, RpcPromiseInfo rpcPromiseInfo, RpcResponse rpcResponse) {
        if (rpcPromiseInfo.rpcPromise != null) {
            // 同步rpc调用
            rpcPromiseInfo.rpcPromise.trySuccess(rpcResponse);
        } else {
            // 异步rpc调用
            RpcResponseCommitTask rpcResponseCommitTask = new RpcResponseCommitTask(session, rpcResponse, rpcPromiseInfo.rpcCallback);
            commit(netContext, session, rpcResponseCommitTask);
        }
    }

    // ------------------------------------------------------- 清理操作 ------------------------------------------------

    /**
     * 清理消息队列
     * @param session session上也有需要清理的东西
     * @param messageQueue 消息队列
     */
    protected final void clear(Session session, MessageQueue messageQueue) {
        // 清空发送缓冲区
        session.sender().clearBuffer();
        // 完成所有同步rpc调用
        cleanRpcPromiseInfo(messageQueue.getRpcPromiseInfoMap());
        // 释放能释放的对象
        messageQueue.clear();
    }

    /**
     * 清理Rpc请求信息
     * @param rpcPromiseInfoMap 未完成的Rpc请求
     */
    private void cleanRpcPromiseInfo(Long2ObjectMap<RpcPromiseInfo> rpcPromiseInfoMap) {
        // 取消所有的rpcPromise
        FastCollectionsUtils.removeIfAndThen(rpcPromiseInfoMap,
                (k, rpcPromiseInfo) -> true,
                (k, rpcPromiseInfo) ->
                {
                    if (rpcPromiseInfo.rpcPromise != null) {
                        rpcPromiseInfo.rpcPromise.trySuccess(RpcResponse.SESSION_CLOSED);
                    }
                });
    }

}
