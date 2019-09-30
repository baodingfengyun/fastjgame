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

package com.wjybxx.fastjgame.net.socket.ordered;

import com.wjybxx.fastjgame.misc.SessionLifecycleAware;
import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.net.socket.*;
import com.wjybxx.fastjgame.utils.NetUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

/**
 * 最开始时为分离的Encoder和Decoder。
 * 那样的问题是不太容易标记channel双方的guid。
 * (会导致协议冗余字段，或使用不必要的同步{@link io.netty.util.AttributeMap})
 * <p>
 * 使用codec会使得协议更加精炼，性能也更好，此外也方便阅读。
 * 它不是线程安全的，也不可共享。
 * <p>
 * baseCodec作为解码过程的最后一步和编码过程的第一步
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/7 12:26
 * github - https://github.com/hl845740757
 */
public abstract class OrderedBaseCodec extends ChannelDuplexHandler {

    private static final Logger logger = LoggerFactory.getLogger(OrderedBaseCodec.class);
    /**
     * 协议编解码工具
     */
    private final ProtocolCodec codec;
    /**
     * 出现异常次数
     */
    private int errorCount;

    protected OrderedBaseCodec(ProtocolCodec codec) {
        this.codec = codec;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NetUtils.setChannelPerformancePreferences(ctx.channel());
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object byteBuf) throws Exception {
        ByteBuf msg = (ByteBuf) byteBuf;
        try {
            long realSum = msg.readLong();
            long logicSum = NetUtils.calChecksum(msg, msg.readerIndex(), msg.readableBytes());
            if (realSum != logicSum) {
                // 校验和不一致
                throw new IOException("realSum=" + realSum + ", logicSum=" + logicSum);
            }
            // 任何编解码出现问题都会在上层消息判断哪里出现问题，这里并不处理channel数据是否异常
            byte pkgTypeNumber = msg.readByte();
            NetMessageType netMessageType = NetMessageType.forNumber(pkgTypeNumber);
            if (null == netMessageType) {
                // 约定之外的包类型
                throw new IOException("null==netEventType " + pkgTypeNumber);
            }
            readMsg(ctx, netMessageType, msg);
        } finally {
            // 解码结束，释放资源
            msg.release();
        }
    }

    /**
     * 子类真正的读取数据
     *
     * @param ctx            ctx
     * @param netMessageType 事件类型
     * @param msg            收到的网络包
     */
    protected abstract void readMsg(ChannelHandlerContext ctx, NetMessageType netMessageType, ByteBuf msg) throws Exception;

    /**
     * 批量消息传输
     *
     * @param ctx                   ctx
     * @param batchOrderedMessageTO 批量消息包
     * @throws Exception error
     */
    protected final void writeBatchMessage(ChannelHandlerContext ctx, BatchOrderedMessageTO batchOrderedMessageTO) throws Exception {
        // 批量协议包
        long ack = batchOrderedMessageTO.getAck();
        for (OrderedMessage message : batchOrderedMessageTO.getOrderedMessageList()) {
            writeSingleMsg(ctx, ack, message, ctx.voidPromise());
        }
        ctx.flush();
    }

    /**
     * 单个消息传输
     *
     * @param ctx                    ctx
     * @param singleOrderedMessageTO 单个消息对象
     * @throws Exception error
     */
    protected final void writeSingleMsg(ChannelHandlerContext ctx, SingleOrderedMessageTO singleOrderedMessageTO, ChannelPromise promise) throws Exception {
        writeSingleMsg(ctx, singleOrderedMessageTO.getAck(), singleOrderedMessageTO.getOrderedMessage(), promise);
    }

    private void writeSingleMsg(ChannelHandlerContext ctx, long ack, OrderedMessage orderedMessage, ChannelPromise promise) throws Exception {
        switch (orderedMessage.getWrappedMessage().type()) {
            case RPC_REQUEST:
                // rpc请求
                writeRpcRequestMessage(ctx, ack, orderedMessage, promise);
                break;
            case RPC_RESPONSE:
                // RPC响应
                writeRpcResponseMessage(ctx, ack, orderedMessage, promise);
                break;
            case ONE_WAY_MESSAGE:
                // 单向消息
                writeOneWayMessage(ctx, ack, orderedMessage, promise);
                break;
            case PING_PONG:
                // 心跳消息
                writeAckPingPongMessage(ctx, ack, orderedMessage, promise);
                break;
            default:
                throw new IOException("Unexpected message type " + orderedMessage.getWrappedMessage().type());
        }
    }
    // ---------------------------------------------- 协议1、2  ---------------------------------------

    /**
     * 编码协议1 - 连接请求
     *
     * @param ctx                   ctx
     * @param orderedConnectRequest 发送的消息
     */
    final void writeConnectRequest(ChannelHandlerContext ctx, OrderedConnectRequest orderedConnectRequest, ChannelPromise promise) {
        int contentLength = 8 + 4 + 4 + 8;
        ByteBuf byteBuf = newInitializedByteBuf(ctx, contentLength, NetMessageType.CONNECT_REQUEST);
        // 原始内容
        ConnectRequest connectRequest = orderedConnectRequest.getConnectRequest();
        byteBuf.writeLong(connectRequest.getClientGuid());
        byteBuf.writeInt(connectRequest.getClientRole().getNumber());
        byteBuf.writeInt(connectRequest.getVerifyingTimes());
        // ack
        byteBuf.writeLong(orderedConnectRequest.getAck());
        appendSumAndWrite(ctx, byteBuf, promise);
    }

    /**
     * 解码协议1 - 连接请求
     */
    final OrderedConnectRequestEvent readConnectRequest(Channel channel, long localGuid, SessionLifecycleAware lifecycleAware, ByteBuf msg) {
        long clientGuid = msg.readLong();
        RoleType clientRole = RoleType.forNumber(msg.readInt());
        int verifyingTimes = msg.readInt();
        long ack = msg.readLong();

        ConnectRequest connectRequest = new ConnectRequest(clientGuid, clientRole, verifyingTimes);
        ConnectRequestEvent connectRequestEvent = new ConnectRequestEvent(channel, localGuid, lifecycleAware, connectRequest);
        return new OrderedConnectRequestEvent(connectRequestEvent, ack);
    }

    /**
     * 编码协议2 - 连接响应
     */
    final void writeConnectResponse(ChannelHandlerContext ctx, OrderedConnectResponse connectResponse, ChannelPromise promise) {
        int contentLength = 1 + 4 + 8;
        ByteBuf byteBuf = newInitializedByteBuf(ctx, contentLength, NetMessageType.CONNECT_RESPONSE);

        byteBuf.writeByte(connectResponse.getConnectResponse().isSuccess() ? 1 : 0);
        byteBuf.writeInt(connectResponse.getConnectResponse().getVerifyingTimes());
        byteBuf.writeLong(connectResponse.getAck());

        appendSumAndWrite(ctx, byteBuf, promise);
    }

    /**
     * 解码协议2 - 连接响应
     */
    final OrderedConnectResponseEvent readConnectResponse(Channel channel, long localGuid, long remoteGuid, ByteBuf msg) {
        boolean success = msg.readByte() == 1;
        int verifyingTimes = msg.readInt();
        long ack = msg.readLong();

        ConnectResponse connectResponse = new ConnectResponse(success, verifyingTimes);
        ConnectResponseEvent connectResponseEvent = new ConnectResponseEvent(channel, localGuid, remoteGuid, connectResponse);
        return new OrderedConnectResponseEvent(connectResponseEvent, ack);
    }

    // ---------------------------------------------- 协议3、4 ---------------------------------------

    /**
     * 3. 编码rpc请求包
     */
    private void writeRpcRequestMessage(ChannelHandlerContext ctx, long ack, OrderedMessage orderedMessage, ChannelPromise promise) {
        RpcRequestMessage rpcRequest = (RpcRequestMessage) orderedMessage.getWrappedMessage();
        ByteBuf head = newInitializedByteBuf(ctx, 8 + 8 + 8 + 1, NetMessageType.RPC_REQUEST);
        // 捎带确认消息
        head.writeLong(ack);
        head.writeLong(orderedMessage.getSequence());
        // rpc请求头
        head.writeLong(rpcRequest.getRequestGuid());
        head.writeByte(rpcRequest.isSync() ? 1 : 0);
        // 合并之后发送
        appendSumAndWrite(ctx, tryMergeBody(ctx.alloc(), head, rpcRequest.getRequest()), promise);
    }

    /**
     * 3. 解码rpc请求包
     */
    final OrderedMessageEvent readRpcRequestMessage(Channel channel, long localGuid, long remoteGuid, ByteBuf msg) {
        // 捎带确认消息
        long ack = msg.readLong();
        long sequence = msg.readLong();
        // rpc请求头
        long requestGuid = msg.readLong();
        boolean sync = msg.readByte() == 1;
        // 请求内容
        Object request = tryDecodeBody(msg);

        RpcRequestMessage rpcRequestMessage = new RpcRequestMessage(requestGuid, sync, request);
        MessageEvent messageEvent = new MessageEvent(channel, localGuid, remoteGuid, rpcRequestMessage);
        return new OrderedMessageEvent(ack, sequence, messageEvent);
    }

    /**
     * 4. 编码rpc 响应包
     */
    private void writeRpcResponseMessage(ChannelHandlerContext ctx, long ack, OrderedMessage orderedMessage, ChannelPromise promise) {
        RpcResponseMessage rpcResponseMessage = (RpcResponseMessage) orderedMessage.getWrappedMessage();
        ByteBuf head = newInitializedByteBuf(ctx, 8 + 8 + 8 + 4, NetMessageType.RPC_RESPONSE);
        // 捎带确认信息
        head.writeLong(ack);
        head.writeLong(orderedMessage.getSequence());
        // 响应内容
        head.writeLong(rpcResponseMessage.getRequestGuid());
        final RpcResponse rpcResponse = rpcResponseMessage.getRpcResponse();
        head.writeInt(rpcResponse.getResultCode().getNumber());

        ByteBuf byteBuf;
        if (rpcResponse.getBody() != null) {
            // 合并之后发送
            byteBuf = tryMergeBody(ctx.alloc(), head, rpcResponse.getBody());
        } else {
            byteBuf = head;
        }
        appendSumAndWrite(ctx, byteBuf, promise);
    }

    /**
     * 4. 解码rpc 响应包
     */
    final OrderedMessageEvent readRpcResponseMessage(Channel channel, long localGuid, long remoteGuid, ByteBuf msg) {
        // 捎带确认信息
        long ack = msg.readLong();
        long sequence = msg.readLong();
        // 响应内容
        long requestGuid = msg.readLong();
        RpcResultCode resultCode = RpcResultCode.forNumber(msg.readInt());
        Object body = null;
        // 有可读的内容，证明有body
        if (msg.readableBytes() > 0) {
            body = tryDecodeBody(msg);
        }
        // 这个包装有点多
        RpcResponseMessage rpcResponseMessage = new RpcResponseMessage(requestGuid, new RpcResponse(resultCode, body));
        MessageEvent messageEvent = new MessageEvent(channel, localGuid, remoteGuid, rpcResponseMessage);
        return new OrderedMessageEvent(ack, sequence, messageEvent);
    }

    // ------------------------------------------ 协议5 --------------------------------------------

    /**
     * 5.编码单向协议包
     */
    private void writeOneWayMessage(ChannelHandlerContext ctx, long ack, OrderedMessage orderedMessage, ChannelPromise promise) {
        OneWayMessage oneWayMessage = (OneWayMessage) orderedMessage.getWrappedMessage();
        ByteBuf head = newInitializedByteBuf(ctx, 8 + 8, NetMessageType.ONE_WAY_MESSAGE);
        // 捎带确认
        head.writeLong(ack);
        head.writeLong(orderedMessage.getSequence());
        // 合并之后发送
        appendSumAndWrite(ctx, tryMergeBody(ctx.alloc(), head, oneWayMessage.getMessage()), promise);
    }

    /**
     * 5.解码单向协议
     */
    final OrderedMessageEvent readOneWayMessage(Channel channel, long localGuid, long remoteGuid, ByteBuf msg) {
        // 捎带确认
        long ack = msg.readLong();
        long sequence = msg.readLong();
        // 消息内容
        Object message = tryDecodeBody(msg);

        OneWayMessage oneWayMessage = new OneWayMessage(message);
        MessageEvent messageEvent = new MessageEvent(channel, localGuid, remoteGuid, oneWayMessage);
        return new OrderedMessageEvent(ack, sequence, messageEvent);
    }

    /**
     * 尝试合并协议头和身体
     *
     * @param allocator byteBuf分配器
     * @param head      协议头
     * @param bodyData  待编码的body
     * @return 合并后的byteBuf
     */
    @Nonnull
    private ByteBuf tryMergeBody(ByteBufAllocator allocator, ByteBuf head, Object bodyData) {
        try {
            ByteBuf body = codec.writeObject(allocator, bodyData);
            return Unpooled.wrappedBuffer(head, body);
        } catch (Exception e) {
            // 为了不影响该连接上的其它消息，需要捕获异常
            logger.warn("deserialize body {} caught exception.", bodyData.getClass().getName(), e);
            return head;
        }
    }

    /**
     * 尝试解码消息身体
     *
     * @param data 协议内容
     * @return 为了不引用该连接上的其它消息，如果解码失败返回null。
     */
    @Nullable
    private Object tryDecodeBody(ByteBuf data) {
        Object message = null;
        try {
            message = codec.readObject(data);
        } catch (Exception e) {
            // 为了不影响该连接上的其它消息，需要捕获异常
            logger.warn("deserialize body caught exception", e);
        }
        return message;
    }
    // ---------------------------------------------- 协议6/7  ---------------------------------------

    /**
     * 编码协议6 心跳包
     */
    private void writeAckPingPongMessage(ChannelHandlerContext ctx, long ack, OrderedMessage orderedMessage, ChannelPromise promise) {
        ByteBuf byteBuf = newInitializedByteBuf(ctx, 8 + 8, NetMessageType.PING_PONG);
        byteBuf.writeLong(ack);
        byteBuf.writeLong(orderedMessage.getSequence());
        appendSumAndWrite(ctx, byteBuf, promise);
    }

    /**
     * 解码协议6/7 - ack心跳包
     */
    final OrderedMessageEvent readAckPingPongMessage(Channel channel, long localGuid, long remoteGuid, ByteBuf msg) {
        long ack = msg.readLong();
        long sequence = msg.readLong();

        MessageEvent messageEvent = new MessageEvent(channel, localGuid, remoteGuid, PingPongMessage.INSTANCE);
        return new OrderedMessageEvent(ack, sequence, messageEvent);
    }
    // ------------------------------------------ 分割线 --------------------------------------------

    /**
     * 远程主机强制关闭了一个连接，这个异常真的有点坑爹
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("", cause);
        if (++errorCount > 3) {
            logger.error("close channel by reason of too many error caught!");
            NetUtils.closeQuietly(ctx);
        }
    }

    /**
     * 创建一个初始化好的byteBuf
     * 设置包总长度 和 校验和
     *
     * @param ctx           handlerContext，用于获取allocator
     * @param contentLength 有效内容的长度
     * @return 足够空间的byteBuf可以直接写入内容部分
     */
    private ByteBuf newInitializedByteBuf(ChannelHandlerContext ctx, int contentLength, NetMessageType netNetMessageType) {
        return NetUtils.newInitializedByteBuf(ctx, contentLength, netNetMessageType.pkgType);
    }

    /**
     * 添加校验和并发送
     *
     * @param ctx     handlerContext，用于将数据发送出去
     * @param byteBuf 待发送的数据包
     * @param promise 操作回执
     */
    private void appendSumAndWrite(ChannelHandlerContext ctx, ByteBuf byteBuf, ChannelPromise promise) {
        NetUtils.appendLengthAndCheckSum(byteBuf);
        ctx.write(byteBuf, promise);
    }
}
