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

import com.wjybxx.fastjgame.net.rpc.*;
import com.wjybxx.fastjgame.net.serialization.Serializer;
import com.wjybxx.fastjgame.utils.CodecUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.DefaultSocketChannelConfig;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.net.Socket;
import java.util.Iterator;

/**
 * 最开始时为分离的Encoder和Decoder，合并为Codec主要是为了方便阅读。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/7 12:26
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public abstract class AbstractSocketCodec extends ChannelDuplexHandler {

    private static final Logger logger = LoggerFactory.getLogger(AbstractSocketCodec.class);

    /**
     * 序列化工具
     */
    private final Serializer serializer;

    protected AbstractSocketCodec(Serializer serializer) {
        this.serializer = serializer;
    }

    /**
     * 设置channel性能偏好.
     * <p>
     * 可参考 - https://blog.csdn.net/zero__007/article/details/51723434
     * <p>
     * 在 JDK 1.5 中, 还为 Socket 类提供了{@link Socket#setPerformancePreferences(int, int, int)}方法:
     * 以上方法的 3 个参数表示网络传输数据的 3 选指标.
     * connectionTime: 表示用最少时间建立连接.
     * latency: 表示最小延迟.
     * bandwidth: 表示最高带宽.
     * setPerformancePreferences() 方法用来设定这 3 项指标之间的相对重要性.
     * 可以为这些参数赋予任意的整数, 这些整数之间的相对大小就决定了相应参数的相对重要性.
     * 例如, 如果参数 connectionTime 为 2, 参数 latency 为 1, 而参数bandwidth 为 3,
     * 就表示最高带宽最重要, 其次是最少连接时间, 最后是最小延迟.
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ChannelConfig channelConfig = ctx.channel().config();
        if (channelConfig instanceof DefaultSocketChannelConfig) {
            DefaultSocketChannelConfig socketChannelConfig = (DefaultSocketChannelConfig) channelConfig;
            socketChannelConfig.setPerformancePreferences(0, 1, 2);
        }
        ctx.fireChannelActive();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object byteBuf) throws Exception {
        ByteBuf msg = (ByteBuf) byteBuf;
        try {
            // 任何编解码出现问题都会在上层消息判断哪里出现问题，这里并不处理channel数据是否异常
            byte pkgTypeNumber = msg.readByte();
            NetMessageType netMessageType = NetMessageType.forNumber(pkgTypeNumber);
            if (null == netMessageType) {
                // 约定之外的包类型
                throw new IOException("Unknown pkgTypeNumber: " + pkgTypeNumber);
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
    protected final void writeBatchMessage(ChannelHandlerContext ctx, BatchSocketMessageTO batchSocketMessageTO, ChannelPromise promise) throws Exception {
        // 批量协议包 - 主要是这里的list不一定是arrayList，因此不能消除iterator
        final long ack = batchSocketMessageTO.getAck();
        final int size = batchSocketMessageTO.getSocketMessageList().size();
        final Iterator<SocketMessage> iterator = batchSocketMessageTO.getSocketMessageList().iterator();
        for (int count = 1; iterator.hasNext(); count++) {
            SocketMessage socketMessage = iterator.next();
            // 避免太多消息共用一个endOfBatch 1 ~ 23 个包为一个endOfBatch，大于23个包，拆为两个endOfBatch - 16个共用一个endOfBatch
            writeSingleMsg(ctx, ack, count == size || (size - count > 7 && (count & 15) == 0), socketMessage, ctx.voidPromise());
        }
        promise.trySuccess();
    }

    /**
     * 单个消息传输
     *
     * @param ctx             ctx
     * @param socketMessageTO 单个消息对象
     * @throws Exception error
     */
    protected final void writeSingleMsg(ChannelHandlerContext ctx, SocketMessageTO socketMessageTO, ChannelPromise promise) throws Exception {
        writeSingleMsg(ctx, socketMessageTO.getAck(), true, socketMessageTO.getSocketMessage(), promise);
    }

    private void writeSingleMsg(ChannelHandlerContext ctx, long ack, boolean endOfBatch, SocketMessage socketMessage, ChannelPromise promise) throws Exception {
        switch (socketMessage.getWrappedMessage().type()) {
            case RPC_REQUEST:
                // rpc请求
                writeRpcRequestMessage(ctx, ack, endOfBatch, socketMessage, promise);
                break;
            case RPC_RESPONSE:
                // RPC响应
                writeRpcResponseMessage(ctx, ack, endOfBatch, socketMessage, promise);
                break;
            case ONE_WAY_MESSAGE:
                // 单向消息
                writeOneWayMessage(ctx, ack, endOfBatch, socketMessage, promise);
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
     * @param socketConnectRequestTO 请求传输对象
     */
    final void writeConnectRequest(ChannelHandlerContext ctx, String sessionId, SocketConnectRequestTO socketConnectRequestTO, ChannelPromise promise) {
        final SocketConnectRequest socketConnectRequest = socketConnectRequestTO.getConnectRequest();
        final byte[] sessionIdBytes = CodecUtils.getBytesUTF8(sessionId);

        final int contentLength = 4 + 4 + 8 + 8 + 1 + sessionIdBytes.length;
        ByteBuf head = newHeadByteBuf(ctx, contentLength, NetMessageType.CONNECT_REQUEST);

        head.writeInt(socketConnectRequest.getVerifyingTimes());
        head.writeInt(socketConnectRequest.getVerifiedTimes());

        head.writeLong(socketConnectRequestTO.getInitSequence());
        head.writeLong(socketConnectRequestTO.getAck());
        head.writeByte(socketConnectRequestTO.isClose() ? 1 : 0);

        // sessionId放最后可以省去长度标记
        head.writeBytes(sessionIdBytes);

        setLengthAndWrite(ctx, head, promise);
    }

    /**
     * 解码协议1 - 建立连接请求
     */
    final SocketConnectRequestEvent readConnectRequest(Channel channel, ByteBuf msg, SocketPortContext portExtraInfo) {
        // 建立连接请求
        int verifyingTimes = msg.readInt();
        int verifiedTimes = msg.readInt();

        // initSequence和ack
        long initSequence = msg.readLong();
        long ack = msg.readLong();
        boolean close = msg.readByte() == 1;

        // sessionId
        byte[] sessionIdBytes = readRemainBytes(msg);
        String sessionId = CodecUtils.newStringUTF8(sessionIdBytes);

        SocketConnectRequest socketConnectRequest = new SocketConnectRequest(verifyingTimes, verifiedTimes);
        return new SocketConnectRequestEvent(channel, sessionId, initSequence, ack, close, socketConnectRequest, portExtraInfo);
    }

    /**
     * 编码协议2 - 建立连接应答
     */
    final void writeConnectResponse(ChannelHandlerContext ctx, SocketConnectResponseTO socketConnectResponseTO, ChannelPromise promise) {
        ByteBuf byteBuf = newHeadByteBuf(ctx, 1 + 4 + 4 + 8 + 8 + 1, NetMessageType.CONNECT_RESPONSE);

        SocketConnectResponse socketConnectResponse = socketConnectResponseTO.getConnectResponse();

        // 建立连接结果
        byteBuf.writeByte(socketConnectResponse.isSuccess() ? 1 : 0);
        byteBuf.writeInt(socketConnectResponse.getVerifyingTimes());
        byteBuf.writeInt(socketConnectResponse.getVerifiedTimes());

        // initSequence和ack
        byteBuf.writeLong(socketConnectResponseTO.getInitSequence());
        byteBuf.writeLong(socketConnectResponseTO.getAck());
        byteBuf.writeByte(socketConnectResponseTO.isClose() ? 1 : 0);

        setLengthAndWrite(ctx, byteBuf, promise);
    }

    /**
     * 解码协议2 - 连接响应
     */
    final SocketConnectResponseEvent readConnectResponse(Channel channel, String sessionId, ByteBuf msg) {
        boolean success = msg.readByte() == 1;
        int verifyingTimes = msg.readInt();
        int verifiedTimes = msg.readInt();

        long initSequence = msg.readLong();
        long ack = msg.readLong();
        boolean close = msg.readByte() == 1;

        SocketConnectResponse socketConnectResponse = new SocketConnectResponse(success, verifyingTimes, verifiedTimes);
        return new SocketConnectResponseEvent(channel, sessionId, initSequence, ack, close, socketConnectResponse);
    }

    // ---------------------------------------------- 心跳协议  ---------------------------------------

    /**
     * 心跳协议编码
     */
    final void writeAckPingPongMessage(ChannelHandlerContext ctx, SocketPingPongMessageTO pingPongMessageTO, ChannelPromise promise) {
        ByteBuf byteBuf = newHeadByteBuf(ctx, 8 + 1, NetMessageType.PING_PONG);

        byteBuf.writeLong(pingPongMessageTO.getAck());
        byteBuf.writeByte(pingPongMessageTO.getPingOrPong() == PingPongMessage.PING ? 1 : 0);

        setLengthAndWrite(ctx, byteBuf, promise);
    }

    /**
     * 心跳协议解码
     */
    final SocketPingPongEvent readAckPingPongMessage(Channel channel, String sessionId, ByteBuf msg) {
        long ack = msg.readLong();
        if (msg.readByte() == 1) {
            return new SocketPingPongEvent(channel, sessionId, ack, PingPongMessage.PING);
        } else {
            return new SocketPingPongEvent(channel, sessionId, ack, PingPongMessage.PONG);
        }
    }

    // ---------------------------------------------- rpc请求和响应 ---------------------------------------

    /**
     * 编码rpc请求包
     */
    private void writeRpcRequestMessage(ChannelHandlerContext ctx, long ack, boolean endOfBatch, SocketMessage socketMessage, ChannelPromise promise) {
        RpcRequestMessage requestMessage = (RpcRequestMessage) socketMessage.getWrappedMessage();
        ByteBuf head = newHeadByteBuf(ctx, 8 + 8 + 1 + 8 + 1, NetMessageType.RPC_REQUEST);

        // 捎带确认消息
        head.writeLong(socketMessage.getSequence());
        head.writeLong(ack);
        head.writeByte(endOfBatch ? 1 : 0);

        // rpc请求头
        head.writeLong(requestMessage.getRequestGuid());
        head.writeByte(requestMessage.isSync() ? 1 : 0);

        // rpc请求内容 - 合并之后发送
        writeLogicMessageBodyAndWrite(ctx, head, requestMessage.getBody(), promise);
    }

    /**
     * 解码rpc请求包
     */
    final SocketMessageEvent readRpcRequestMessage(Channel channel, String sessionId, ByteBuf msg) {
        // 捎带确认消息
        long sequence = msg.readLong();
        long ack = msg.readLong();
        boolean endOfBatch = msg.readByte() == 1;

        // rpc请求头
        long requestGuid = msg.readLong();
        boolean sync = msg.readByte() == 1;
        // 方法描述信息 - 不限制结构
        Object rpcMethodSpec = tryDecodeBody(msg);

        RpcRequestMessage rpcRequestMessage = new RpcRequestMessage(requestGuid, sync, rpcMethodSpec);
        return new SocketMessageEvent(channel, sessionId, sequence, ack, endOfBatch, rpcRequestMessage);
    }

    /**
     * 编码rpc响应包
     */
    private void writeRpcResponseMessage(ChannelHandlerContext ctx, long ack, boolean endOfBatch, SocketMessage socketMessage, ChannelPromise promise) {
        RpcResponseMessage responseMessage = (RpcResponseMessage) socketMessage.getWrappedMessage();
        ByteBuf head = newHeadByteBuf(ctx, 8 + 8 + 1 + 8 + 1 + 4, NetMessageType.RPC_RESPONSE);

        // 捎带确认信息
        head.writeLong(socketMessage.getSequence());
        head.writeLong(ack);
        head.writeByte(endOfBatch ? 1 : 0);

        // rpc响应头
        head.writeLong(responseMessage.getRequestGuid());
        head.writeByte(responseMessage.isSync() ? 1 : 0);
        head.writeInt(responseMessage.getErrorCode().getNumber());

        if (responseMessage.getErrorCode().isSuccess()) {
            // rpc响应内容 - 合并之后发送
            writeLogicMessageBodyAndWrite(ctx, head, responseMessage.getBody(), promise);
        } else {
            // 错误信息直接编码
            final String errorMsg = (String) responseMessage.getBody();
            final byte[] errorMsgBytes = CodecUtils.getBytesUTF8(errorMsg);
            final ByteBuf bodyByteBuf = ctx.alloc().buffer(errorMsgBytes.length);
            bodyByteBuf.writeBytes(errorMsgBytes);
            setLengthAndWrite(ctx, Unpooled.wrappedBuffer(head, bodyByteBuf), promise);
        }

    }

    /**
     * 解码rpc响应包
     */
    final SocketMessageEvent readRpcResponseMessage(Channel channel, String sessionId, ByteBuf msg) {
        // 捎带确认信息
        long sequence = msg.readLong();
        long ack = msg.readLong();
        boolean endOfBatch = msg.readByte() == 1;

        // 响应头
        long requestGuid = msg.readLong();
        boolean sync = msg.readByte() == 1;
        RpcErrorCode errorCode = RpcErrorCode.forNumber(msg.readInt());

        // 响应内容
        final Object body;
        if (errorCode.isSuccess()) {
            body = tryDecodeBody(msg);
        } else {
            body = CodecUtils.newStringUTF8(readRemainBytes(msg));
        }

        RpcResponseMessage rpcResponseMessage = new RpcResponseMessage(requestGuid, sync, errorCode, body);
        return new SocketMessageEvent(channel, sessionId, sequence, ack, endOfBatch, rpcResponseMessage);
    }

    // ------------------------------------------ 单向消息 --------------------------------------------

    /**
     * 编码单向协议包
     */
    private void writeOneWayMessage(ChannelHandlerContext ctx, long ack, boolean endOfBatch, SocketMessage socketMessage, ChannelPromise promise) {
        OneWayMessage oneWayMessage = (OneWayMessage) socketMessage.getWrappedMessage();
        ByteBuf head = newHeadByteBuf(ctx, 8 + 8 + 1, NetMessageType.ONE_WAY_MESSAGE);

        // 捎带确认
        head.writeLong(socketMessage.getSequence());
        head.writeLong(ack);
        head.writeByte(endOfBatch ? 1 : 0);

        // 合并之后发送
        writeLogicMessageBodyAndWrite(ctx, head, oneWayMessage.getBody(), promise);
    }

    /**
     * 解码单向协议
     */
    final SocketMessageEvent readOneWayMessage(Channel channel, String sessionId, ByteBuf msg) {
        // 捎带确认
        long sequence = msg.readLong();
        long ack = msg.readLong();
        boolean endOfBatch = msg.readBoolean();

        // 消息内容
        Object message = tryDecodeBody(msg);

        OneWayMessage oneWayMessage = new OneWayMessage(message);
        return new SocketMessageEvent(channel, sessionId, sequence, ack, endOfBatch, oneWayMessage);
    }

    // ---------------------------------------------- 分割线 ----------------------------------------------------

    /**
     * 写入逻辑消息的内容并发送
     *
     * @param head 协议头
     * @param body 待编码的body
     */
    private void writeLogicMessageBodyAndWrite(ChannelHandlerContext ctx, ByteBuf head, Object body, ChannelPromise promise) {
        final ByteBuf bodyByteBuf = tryEncodeBody(ctx.alloc(), body);
        setLengthAndWrite(ctx, Unpooled.wrappedBuffer(head, bodyByteBuf), promise);
    }

    /**
     * 尝试编码body
     *
     * @param allocator byteBuf分配器
     * @param bodyData  待编码的body
     * @return 编码后的数据
     */
    private ByteBuf tryEncodeBody(final ByteBufAllocator allocator, final Object bodyData) {
        try {
            return serializer.writeObject(allocator, bodyData);
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
    private Object tryDecodeBody(final ByteBuf data) {
        try {
            return serializer.readObject(data);
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
        // 这里不关闭channel，没有必要关闭
        logger.warn("", cause);
    }

    /**
     * 创建一个消息头的byteBuf
     *
     * @param ctx           handlerContext，用于获取allocator
     * @param contentLength 有效内容的长度
     * @return 足够空间的byteBuf可以直接写入内容部分
     */
    private static ByteBuf newHeadByteBuf(ChannelHandlerContext ctx, int contentLength, NetMessageType netNetMessageType) {
        // 消息长度字段 + 包类型
        ByteBuf byteBuf = ctx.alloc().buffer(4 + 1 + contentLength);
        byteBuf.writeInt(0);
        byteBuf.writeByte(netNetMessageType.pkgType);
        return byteBuf;
    }

    /**
     * 设置长度字段并发送
     *
     * @param ctx     handlerContext，用于将数据发送出去
     * @param byteBuf 待发送的数据包
     * @param promise 操作回执
     */
    private static void setLengthAndWrite(ChannelHandlerContext ctx, ByteBuf byteBuf, ChannelPromise promise) {
        byteBuf.setInt(0, byteBuf.readableBytes() - 4);
        ctx.write(byteBuf, promise);
    }

    /**
     * 将byteBuf中剩余的字节读取到一个字节数组中。
     *
     * @param byteBuf 方法返回之后 readableBytes == 0
     * @return new instance
     */
    @Nonnull
    private static byte[] readRemainBytes(ByteBuf byteBuf) {
        if (byteBuf.readableBytes() == 0) {
            return ArrayUtils.EMPTY_BYTE_ARRAY;
        }
        byte[] result = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(result);
        return result;
    }
}
