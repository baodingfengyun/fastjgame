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

package com.wjybxx.fastjgame.net.socket;

import com.wjybxx.fastjgame.net.common.*;
import com.wjybxx.fastjgame.utils.CodecUtils;
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
public abstract class BaseSocketCodec extends ChannelDuplexHandler {

    private static final Logger logger = LoggerFactory.getLogger(BaseSocketCodec.class);
    /**
     * 协议编解码工具
     */
    private final ProtocolCodec codec;
    /**
     * 出现异常次数
     */
    private int errorCount;

    protected BaseSocketCodec(ProtocolCodec codec) {
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
     * @param ctx                  ctx
     * @param batchSocketMessageTO 批量消息包
     * @throws Exception error
     */
    protected final void writeBatchMessage(ChannelHandlerContext ctx, BatchSocketMessageTO batchSocketMessageTO) throws Exception {
        // 批量协议包
        long ack = batchSocketMessageTO.getAck();
        for (SocketMessage message : batchSocketMessageTO.getSocketMessageList()) {
            writeSingleMsg(ctx, ack, message, ctx.voidPromise());
        }
        ctx.flush();
    }

    /**
     * 单个消息传输
     *
     * @param ctx             ctx
     * @param socketMessageTO 单个消息对象
     * @throws Exception error
     */
    protected final void writeSingleMsg(ChannelHandlerContext ctx, SocketMessageTO socketMessageTO, ChannelPromise promise) throws Exception {
        writeSingleMsg(ctx, socketMessageTO.getAck(), socketMessageTO.getSocketMessage(), promise);
    }

    private void writeSingleMsg(ChannelHandlerContext ctx, long ack, SocketMessage socketMessage, ChannelPromise promise) throws Exception {
        switch (socketMessage.getWrappedMessage().type()) {
            case RPC_REQUEST:
                // rpc请求
                writeRpcRequestMessage(ctx, ack, socketMessage, promise);
                break;
            case RPC_RESPONSE:
                // RPC响应
                writeRpcResponseMessage(ctx, ack, socketMessage, promise);
                break;
            case ONE_WAY_MESSAGE:
                // 单向消息
                writeOneWayMessage(ctx, ack, socketMessage, promise);
                break;
            case PING_PONG:
                // 心跳消息
                writeAckPingPongMessage(ctx, ack, socketMessage, promise);
                break;
            default:
                throw new IOException("Unexpected message type " + socketMessage.getWrappedMessage().type());
        }
    }

    // ---------------------------------------------- 请求和应答协议  ---------------------------------------

    /**
     * 编码协议1 - 连接请求
     *
     * @param ctx                    ctx
     * @param sessionId              channel对应的会话id
     * @param localGuid              我的标识
     * @param socketConnectRequestTO 请求传输对象
     */
    final void writeConnectRequest(ChannelHandlerContext ctx, String sessionId, long localGuid, SocketConnectRequestTO socketConnectRequestTO, ChannelPromise promise) {
        final SocketConnectRequest socketConnectRequest = socketConnectRequestTO.getConnectRequest();
        final int contentLength = 8 + 4 + 8 + sessionId.length();
        ByteBuf head = newHeadByteBuf(ctx, contentLength, NetMessageType.CONNECT_REQUEST);

        head.writeLong(localGuid);
        head.writeInt(socketConnectRequest.getVerifyingTimes());
        head.writeLong(socketConnectRequestTO.getAck());

        // sessionId
        final byte[] sessionIdBytes = CodecUtils.getBytesUTF8(sessionId);
        head.writeBytes(sessionIdBytes);

        appendSumAndWrite(ctx, head, promise);
    }

    /**
     * 解码协议1 - 建立连接请求
     */
    final SocketConnectRequestEvent readConnectRequest(Channel channel, ByteBuf msg, SocketPortContext portExtraInfo) {
        long remoteGuid = msg.readLong();
        int verifyingTimes = msg.readInt();
        long ack = msg.readLong();

        // sessionId
        byte[] sessionIdBytes = NetUtils.readRemainBytes(msg);
        String sessionId = CodecUtils.newStringUTF8(sessionIdBytes);

        SocketConnectRequest socketConnectRequest = new SocketConnectRequest(verifyingTimes);
        return new SocketConnectRequestEvent(channel, sessionId, remoteGuid, ack, socketConnectRequest, portExtraInfo);
    }

    /**
     * 编码协议2 - 建立连接应答
     */
    final void writeConnectResponse(ChannelHandlerContext ctx, SocketConnectResponseTO socketConnectResponseTO, ChannelPromise promise) {
        ByteBuf byteBuf = newHeadByteBuf(ctx, 1 + 4 + 8, NetMessageType.CONNECT_RESPONSE);

        SocketConnectResponse socketConnectResponse = socketConnectResponseTO.getConnectResponse();
        byteBuf.writeByte(socketConnectResponse.isSuccess() ? 1 : 0);
        byteBuf.writeInt(socketConnectResponse.getVerifyingTimes());
        byteBuf.writeLong(socketConnectResponseTO.getAck());

        appendSumAndWrite(ctx, byteBuf, promise);
    }

    /**
     * 解码协议2 - 连接响应
     */
    final SocketConnectResponseEvent readConnectResponse(Channel channel, String sessionId, ByteBuf msg) {
        boolean success = msg.readByte() == 1;
        int verifyingTimes = msg.readInt();
        long ack = msg.readLong();

        SocketConnectResponse socketConnectResponse = new SocketConnectResponse(success, verifyingTimes);
        return new SocketConnectResponseEvent(channel, sessionId, ack, socketConnectResponse);
    }

    // ---------------------------------------------- 心跳协议  ---------------------------------------

    /**
     * 心跳协议编码
     */
    private void writeAckPingPongMessage(ChannelHandlerContext ctx, long ack, SocketMessage socketMessage, ChannelPromise promise) {
        ByteBuf byteBuf = newHeadByteBuf(ctx, 8 + 8, NetMessageType.PING_PONG);

        byteBuf.writeLong(socketMessage.getSequence());
        byteBuf.writeLong(ack);

        appendSumAndWrite(ctx, byteBuf, promise);
    }

    /**
     * 心跳协议解码
     */
    final SocketMessageEvent readAckPingPongMessage(Channel channel, String sessionId, ByteBuf msg) {
        long sequence = msg.readLong();
        long ack = msg.readLong();

        return new SocketMessageEvent(channel, sessionId, sequence, ack, PingPongMessage.INSTANCE);
    }

    // ---------------------------------------------- rpc请求和响应 ---------------------------------------

    /**
     * 编码rpc请求包
     */
    private void writeRpcRequestMessage(ChannelHandlerContext ctx, long ack, SocketMessage socketMessage, ChannelPromise promise) {
        RpcRequestMessage rpcRequest = (RpcRequestMessage) socketMessage.getWrappedMessage();
        ByteBuf head = newHeadByteBuf(ctx, 8 + 8 + 8 + 1, NetMessageType.RPC_REQUEST);
        // 捎带确认消息
        head.writeLong(socketMessage.getSequence());
        head.writeLong(ack);
        // rpc请求头
        head.writeLong(rpcRequest.getRequestGuid());
        head.writeByte(rpcRequest.isSync() ? 1 : 0);
        // 合并之后发送
        appendSumAndWrite(ctx, tryEncodeAndMergeBody(ctx.alloc(), head, rpcRequest.getRequest()), promise);
    }

    /**
     * 解码rpc请求包
     */
    final SocketMessageEvent readRpcRequestMessage(Channel channel, String sessionId, ByteBuf msg) {
        // 捎带确认消息
        long sequence = msg.readLong();
        long ack = msg.readLong();
        // rpc请求头
        long requestGuid = msg.readLong();
        boolean sync = msg.readByte() == 1;
        // 请求内容
        Object request = tryDecodeBody(msg);

        RpcRequestMessage rpcRequestMessage = new RpcRequestMessage(requestGuid, sync, request);
        return new SocketMessageEvent(channel, sessionId, sequence, ack, rpcRequestMessage);
    }

    /**
     * 编码rpc响应包
     */
    private void writeRpcResponseMessage(ChannelHandlerContext ctx, long ack, SocketMessage socketMessage, ChannelPromise promise) {
        RpcResponseMessage rpcResponseMessage = (RpcResponseMessage) socketMessage.getWrappedMessage();
        ByteBuf head = newHeadByteBuf(ctx, 8 + 8 + 8 + 4, NetMessageType.RPC_RESPONSE);
        // 捎带确认信息
        head.writeLong(socketMessage.getSequence());
        head.writeLong(ack);
        // 响应内容
        head.writeLong(rpcResponseMessage.getRequestGuid());
        final RpcResponse rpcResponse = rpcResponseMessage.getRpcResponse();
        head.writeInt(rpcResponse.getResultCode().getNumber());

        if (rpcResponse.getBody() != null) {
            appendSumAndWrite(ctx, tryEncodeAndMergeBody(ctx.alloc(), head, rpcResponse.getBody()), promise);
        } else {
            appendSumAndWrite(ctx, head, promise);
        }

    }

    /**
     * 解码rpc响应包
     */
    final SocketMessageEvent readRpcResponseMessage(Channel channel, String sessionId, ByteBuf msg) {
        // 捎带确认信息
        long sequence = msg.readLong();
        long ack = msg.readLong();
        // 响应内容
        long requestGuid = msg.readLong();
        RpcResultCode resultCode = RpcResultCode.forNumber(msg.readInt());

        // 响应body
        final Object body = msg.readableBytes() > 0 ? tryDecodeBody(msg) : null;

        RpcResponseMessage rpcResponseMessage = new RpcResponseMessage(requestGuid, new RpcResponse(resultCode, body));
        return new SocketMessageEvent(channel, sessionId, sequence, ack, rpcResponseMessage);
    }

    // ------------------------------------------ 单向消息 --------------------------------------------

    /**
     * 编码单向协议包
     */
    private void writeOneWayMessage(ChannelHandlerContext ctx, long ack, SocketMessage socketMessage, ChannelPromise promise) {
        OneWayMessage oneWayMessage = (OneWayMessage) socketMessage.getWrappedMessage();
        ByteBuf head = newHeadByteBuf(ctx, 8 + 8, NetMessageType.ONE_WAY_MESSAGE);
        // 捎带确认
        head.writeLong(socketMessage.getSequence());
        head.writeLong(ack);
        // 合并之后发送
        appendSumAndWrite(ctx, tryEncodeAndMergeBody(ctx.alloc(), head, oneWayMessage.getMessage()), promise);
    }

    /**
     * 解码单向协议
     */
    final SocketMessageEvent readOneWayMessage(Channel channel, String sessionId, ByteBuf msg) {
        // 捎带确认
        long sequence = msg.readLong();
        long ack = msg.readLong();
        // 消息内容
        Object message = tryDecodeBody(msg);

        OneWayMessage oneWayMessage = new OneWayMessage(message);
        return new SocketMessageEvent(channel, sessionId, sequence, ack, oneWayMessage);
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
    private ByteBuf tryEncodeAndMergeBody(ByteBufAllocator allocator, ByteBuf head, Object bodyData) {
        return Unpooled.wrappedBuffer(head, tryEncodeBody(allocator, bodyData));
    }

    /**
     * 尝试编码body
     *
     * @param allocator byteBuf分配器
     * @param bodyData  待编码的body
     * @return 编码后的数据
     */
    private ByteBuf tryEncodeBody(ByteBufAllocator allocator, Object bodyData) {
        try {
            return codec.writeObject(allocator, bodyData);
        } catch (Exception e) {
            // 为了不影响该连接上的其它消息，需要捕获异常
            logger.warn("serialize body {} caught exception.", bodyData.getClass().getName(), e);
        }
        return Unpooled.EMPTY_BUFFER;
    }

    /**
     * 尝试解码消息身体
     *
     * @param data 协议内容
     * @return 为了不引用该连接上的其它消息，如果解码失败返回null。
     */
    @Nullable
    private Object tryDecodeBody(ByteBuf data) {
        try {
            return codec.readObject(data);
        } catch (Exception e) {
            // 为了不影响该连接上的其它消息，需要捕获异常
            logger.warn("deserialize body caught exception", e);
        }
        return null;
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
     * 创建一个消息头的byteBuf
     *
     * @param ctx           handlerContext，用于获取allocator
     * @param contentLength 有效内容的长度
     * @return 足够空间的byteBuf可以直接写入内容部分
     */
    private ByteBuf newHeadByteBuf(ChannelHandlerContext ctx, int contentLength, NetMessageType netNetMessageType) {
        return NetUtils.newHeadByteBuf(ctx, contentLength, netNetMessageType.pkgType);
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
