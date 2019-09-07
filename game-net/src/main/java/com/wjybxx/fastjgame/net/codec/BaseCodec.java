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

package com.wjybxx.fastjgame.net.codec;

import com.wjybxx.fastjgame.net.*;
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
 *
 * 使用codec会使得协议更加精炼，性能也更好，此外也方便阅读。
 * 它不是线程安全的，也不可共享。
 *
 * baseCodec作为解码过程的最后一步和编码过程的第一步
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/7 12:26
 * github - https://github.com/hl845740757
 */
public abstract class BaseCodec extends ChannelDuplexHandler {

    private static final Logger logger = LoggerFactory.getLogger(BaseCodec.class);
    /**
     * 协议编解码工具
     */
    private final ProtocolCodec codec;

    protected BaseCodec(ProtocolCodec codec) {
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
            long logicSum = NetUtils.calChecksum(msg,msg.readerIndex(),msg.readableBytes());
            if (realSum != logicSum){
                // 校验和不一致
                closeCtx(ctx,"realSum="+realSum + ", logicSum="+logicSum);
                return;
            }
            // 任何编解码出现问题都会在上层消息判断哪里出现问题，这里并不处理channel数据是否异常
            byte pkgTypeNumber = msg.readByte();
            NetPackageType netPackageType = NetPackageType.forNumber(pkgTypeNumber);
            if (null == netPackageType){
                // 约定之外的包类型
                closeCtx(ctx,"null==netEventType " + pkgTypeNumber);
                return;
            }
            readMsg(ctx, netPackageType, msg);
        }finally {
            // 解码结束，释放资源
            msg.release();
        }
    }

    /**
     * 子类真正的读取数据
     * @param ctx ctx
     * @param netPackageType 事件类型
     * @param msg 收到的网络包
     */
    protected abstract void readMsg(ChannelHandlerContext ctx, NetPackageType netPackageType, ByteBuf msg) throws Exception;

    // ---------------------------------------------- 协议1、2  ---------------------------------------
    /**
     * 编码协议1 - 连接请求
     * @param ctx ctx
     * @param msgTO 发送的消息
     */
    final void writeConnectRequest(ChannelHandlerContext ctx, ConnectRequestTO msgTO, ChannelPromise promise) {
        byte[] encryptedToken=msgTO.getTokenBytes();
        int contentLength = 8 + 4 + 8 + encryptedToken.length;
        ByteBuf byteBuf = newInitializedByteBuf(ctx, contentLength, NetPackageType.CONNECT_REQUEST);

        byteBuf.writeLong(msgTO.getClientGuid());
        byteBuf.writeInt(msgTO.getSndTokenTimes());
        byteBuf.writeLong(msgTO.getAck());
        byteBuf.writeBytes(encryptedToken);
        appendSumAndWrite(ctx, byteBuf, promise);
    }

    /**
     * 解码协议1 - 连接请求
     */
    final ConnectRequestTO readConnectRequest(ByteBuf msg) {
        long clientGuid=msg.readLong();
        int sndTokenTimes=msg.readInt();
        long ack=msg.readLong();
        byte[] encryptedToken= NetUtils.readRemainBytes(msg);

        return new ConnectRequestTO(clientGuid, sndTokenTimes, ack, encryptedToken);
    }

    /**
     * 编码协议2 - 连接响应
     */
    final void writeConnectResponse(ChannelHandlerContext ctx, ConnectResponseTO msgTO, ChannelPromise promise) {
        byte[] encryptedToken=msgTO.getEncryptedToken();

        int contentLength = 4 + 1 + 8 + encryptedToken.length;
        ByteBuf byteBuf = newInitializedByteBuf(ctx, contentLength, NetPackageType.CONNECT_RESPONSE);

        byteBuf.writeInt(msgTO.getSndTokenTimes());
        byteBuf.writeByte(msgTO.isSuccess()?1:0);
        byteBuf.writeLong(msgTO.getAck());
        byteBuf.writeBytes(msgTO.getEncryptedToken());

        appendSumAndWrite(ctx, byteBuf, promise);
    }

    /**
     * 解码协议2 - 连接响应
     */
    final ConnectResponseTO readConnectResponse(ByteBuf msg) {
        int sndTokenTimes=msg.readInt();
        boolean success=msg.readByte()==1;
        long ack=msg.readLong();
        byte[] encryptedToken= NetUtils.readRemainBytes(msg);

        return new ConnectResponseTO(sndTokenTimes, success, ack, encryptedToken);
    }

    // ---------------------------------------------- 协议3、4 ---------------------------------------
    /**
     * 3. 编码rpc请求包
     */
    final void writeRpcRequestMessage(ChannelHandlerContext ctx, long ack, RpcRequestMessage rpcRequest, ChannelPromise promise) {
        ByteBuf head = newInitializedByteBuf(ctx, 8 + 8 + 8 + 1, NetPackageType.RPC_REQUEST);
        // 捎带确认消息
        head.writeLong(ack);
        head.writeLong(rpcRequest.getSequence());
        // rpc请求头
        head.writeLong(rpcRequest.getRequestGuid());
        head.writeByte(rpcRequest.isSync() ? 1 : 0);
        // 合并之后发送
        appendSumAndWrite(ctx, tryMergeBody(ctx.alloc(), head, rpcRequest.getRequest(), codec::encodeRpcRequest), promise);
    }

    /**
     * 3. 解码rpc请求包
     */
    final RpcRequestEventParam readRpcRequestMessage(Channel channel, long localGuid, long remoteGuid, ByteBuf msg) {
        // 捎带确认消息
        long ack = msg.readLong();
        long sequence = msg.readLong();
        // rpc请求头
        long requestGuid = msg.readLong();
        boolean sync = msg.readByte() == 1;
        // 请求内容
        Object request = tryDecodeBody(msg, codec::decodeRpcRequest);
        return new RpcRequestEventParam(channel, localGuid, remoteGuid, ack, sequence, requestGuid, sync, request);
    }

    /**
     * 4. 编码rpc 响应包
     */
    final void writeRpcResponseMessage(ChannelHandlerContext ctx, long ack, RpcResponseMessage sentRpcResponse, ChannelPromise promise) {
        ByteBuf head = newInitializedByteBuf(ctx, 8 + 8 + 8 + 4, NetPackageType.RPC_RESPONSE);
        // 捎带确认信息
        head.writeLong(ack);
        head.writeLong(sentRpcResponse.getSequence());
        // 响应内容
        head.writeLong(sentRpcResponse.getRequestGuid());
        final RpcResponse rpcResponse = sentRpcResponse.getRpcResponse();
        head.writeInt(rpcResponse.getResultCode().getNumber());

        ByteBuf byteBuf;
        if (rpcResponse.getBody() != null) {
            // 合并之后发送
            byteBuf = tryMergeBody(ctx.alloc(), head, rpcResponse.getBody(), codec::encodeRpcResponse);
        } else {
            byteBuf = head;
        }
        appendSumAndWrite(ctx, byteBuf, promise);
    }

    /**
     * 4. 解码rpc 响应包
     */
    final RpcResponseEventParam readRpcResponseMessage(Channel channel, long localGuid, long remoteGuid, ByteBuf msg) {
        // 捎带确认信息
        long ack= msg.readLong();
        long sequence = msg.readLong();
        // 响应内容
        long requestGuid = msg.readLong();
        RpcResultCode resultCode = RpcResultCode.forNumber(msg.readInt());
        Object body = null;
        // 有可读的内容，证明有body
        if (msg.readableBytes() > 0) {
            body = tryDecodeBody(msg, codec::decodeRpcResponse);
        }
        return new RpcResponseEventParam(channel, localGuid, remoteGuid, ack, sequence, requestGuid, new RpcResponse(resultCode, body));
    }

    // ------------------------------------------ 协议5 --------------------------------------------

    /**
     * 5.编码单向协议包
     */
    final void writeOneWayMessage(ChannelHandlerContext ctx, long ack, OneWayMessage sentOneWayMessage, ChannelPromise promise) {
        ByteBuf head = newInitializedByteBuf(ctx, 8 + 8, NetPackageType.ONE_WAY_MESSAGE);
        // 捎带确认
        head.writeLong(ack);
        head.writeLong(sentOneWayMessage.getSequence());
        // 合并之后发送
        appendSumAndWrite(ctx, tryMergeBody(ctx.alloc(), head, sentOneWayMessage.getMessage(), codec::encodeMessage), promise);
    }

    /**
     * 5.解码单向协议
     */
    final OneWayMessageEventParam readOneWayMessage(Channel channel, long localGuid, long remoteGuid, ByteBuf msg) {
        // 捎带确认
        long ack = msg.readLong();
        long sequence = msg.readLong();
        // 消息内容
        Object message = tryDecodeBody(msg, codec::decodeMessage);
        return new OneWayMessageEventParam(channel, localGuid, remoteGuid, ack, sequence, message);
    }

    /**
     * 尝试合并协议头和身体
     * @param allocator byteBuf分配器
     * @param head 协议头
     * @param bodyData 待编码的body
     * @param encoder 编码器
     * @return 合并后的byteBuf
     */
    @Nonnull
    private ByteBuf tryMergeBody(ByteBufAllocator allocator, ByteBuf head, Object bodyData, EncodeFunction encoder) {
        try {
            ByteBuf body = encoder.encode(allocator, bodyData);
            return Unpooled.wrappedBuffer(head, body);
        }catch (Exception e){
            // 为了不影响该连接上的其它消息，需要捕获异常
            logger.warn("deserialize body {} caught exception.", bodyData.getClass().getName(), e);
            return head;
        }
    }

    /**
     * 尝试解码消息身体
     * @param data 协议内容
     * @return 为了不引用该连接上的其它消息，如果解码失败返回null。
     */
    @Nullable
    private Object tryDecodeBody(ByteBuf data, DecodeFunction decoder) {
        Object message = null;
        try {
            message = decoder.decode(data);
        }catch (Exception e){
            // 为了不影响该连接上的其它消息，需要捕获异常
            logger.warn("deserialize body caught exception", e);
        }
        return message;
    }
    // ---------------------------------------------- 协议6/7  ---------------------------------------
    /**
     * 编码协议6/7 - ack心跳包
     */
    final void writeAckPingPongMessage(ChannelHandlerContext ctx, long ack, AckPingPongMessage sentAckPingPong, ChannelPromise promise,
                                       NetPackageType netPackageType) {
        ByteBuf byteBuf = newInitializedByteBuf(ctx, 8 + 8, netPackageType);
        byteBuf.writeLong(ack);
        byteBuf.writeLong(sentAckPingPong.getSequence());
        appendSumAndWrite(ctx, byteBuf, promise);
    }

    /**
     * 解码协议6/7 - ack心跳包
     */
    final AckPingPongEventParam readAckPingPongMessage(Channel channel, long localGuid, long remoteGuid, ByteBuf msg) {
        long ack = msg.readLong();
        long sequence = msg.readLong();
        return new AckPingPongEventParam(channel, localGuid, remoteGuid, ack, sequence);
    }
    // ------------------------------------------ 分割线 --------------------------------------------
    /**
     * 关闭channel
     * @param ctx 待关闭的context
     * @param reason 关闭context的原因
     */
    final void closeCtx(ChannelHandlerContext ctx, String reason){
        logger.warn("close channel by reason of {}", reason);
        NetUtils.closeQuietly(ctx);
    }

    /** 远程主机强制关闭了一个连接，这个异常真的有点坑爹 */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        closeCtx(ctx,"decode exceptionCaught.");
        logger.warn("", cause);
    }

    /**
     * 创建一个初始化好的byteBuf
     * 设置包总长度 和 校验和
     * @param ctx handlerContext，用于获取allocator
     * @param contentLength 有效内容的长度
     * @return 足够空间的byteBuf可以直接写入内容部分
     */
    private ByteBuf newInitializedByteBuf(ChannelHandlerContext ctx, int contentLength, NetPackageType netNetPackageType){
        return NetUtils.newInitializedByteBuf(ctx, contentLength, netNetPackageType.pkgType);
    }

    /**
     * 添加校验和并发送
     * @param ctx handlerContext，用于将数据发送出去
     * @param byteBuf 待发送的数据包
     * @param promise 操作回执
     */
    private void appendSumAndWrite(ChannelHandlerContext ctx, ByteBuf byteBuf, ChannelPromise promise) {
        NetUtils.appendLengthAndCheckSum(byteBuf);
        ctx.write(byteBuf, promise);
    }

    @FunctionalInterface
    private interface EncodeFunction {

        ByteBuf encode(ByteBufAllocator allocator, Object obj) throws IOException;
    }

    @FunctionalInterface
    private interface DecodeFunction {

        Object decode(ByteBuf data) throws IOException;
    }
}
