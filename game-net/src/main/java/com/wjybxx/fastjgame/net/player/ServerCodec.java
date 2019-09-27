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

package com.wjybxx.fastjgame.net.player;

import com.wjybxx.fastjgame.manager.NetEventManager;
import com.wjybxx.fastjgame.net.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * 服务端使用的编解码器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/7 13:23
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class ServerCodec extends BaseCodec {

    /**
     * 该channel关联哪本地哪一个用户
     */
    private final long localGuid;
    /**
     * 缓存的客户端guid，关联的远程
     */
    private long clientGuid = Long.MIN_VALUE;

    private final PortContext portContext;
    private final NetEventManager netEventManager;

    public ServerCodec(ProtocolCodec codec, long localGuid, PortContext portContext, NetEventManager netEventManager) {
        super(codec);
        this.localGuid = localGuid;
        this.portContext = portContext;
        this.netEventManager = netEventManager;
    }

    /**
     * 是否已收到过客户端的连接请求,主要关系到后续需要使用的clientGuid
     *
     * @return 是否已接收到建立连接请求
     */
    private boolean isInited() {
        return clientGuid != Long.MIN_VALUE;
    }

    /**
     * 标记为已接收过连接请求
     *
     * @param clientGuid 客户端请求建立连接时的guid
     */
    private void init(long clientGuid) {
        this.clientGuid = clientGuid;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msgTO, ChannelPromise promise) throws Exception {
        if (msgTO instanceof BatchMessageTO) {
            // 批量协议包
            BatchMessageTO batchMessageTO = (BatchMessageTO) msgTO;
            long ack = batchMessageTO.getAck();
            for (OrderedMessage message : batchMessageTO.getOrderedMessageList()) {
                writeSingleMsg(ctx, ack, message, ctx.voidPromise());
            }
        } else if (msgTO instanceof SingleMessageTO) {
            // 单个协议包
            SingleMessageTO singleMessageTO = (SingleMessageTO) msgTO;
            writeSingleMsg(ctx, singleMessageTO.getAck(), singleMessageTO.getOrderedMessage(), promise);
        } else if (msgTO instanceof ConnectResponseTO) {
            // 建立连接验证结果
            writeConnectResponse(ctx, (ConnectResponseTO) msgTO, promise);
        } else {
            super.write(ctx, msgTO, promise);
        }
    }

    /**
     * 发送单个消息
     */
    private void writeSingleMsg(ChannelHandlerContext ctx, long ack, OrderedMessage orderedMessage, ChannelPromise promise) throws Exception {
        // 按出现的几率判断
        if (orderedMessage.getWrappedMessage() instanceof OneWayMessage) {
            // 单向消息
            writeOneWayMessage(ctx, ack, orderedMessage, promise);
        } else if (orderedMessage.getWrappedMessage() instanceof RpcResponseMessage) {
            // RPC响应
            writeRpcResponseMessage(ctx, ack, orderedMessage, promise);
        } else if (orderedMessage.getWrappedMessage() instanceof RpcRequestMessage) {
            // 向另一个服务器发起rpc请求
            writeRpcRequestMessage(ctx, ack, orderedMessage, promise);
        } else if (orderedMessage.getWrappedMessage() instanceof AckPingPongMessage) {
            // 服务器ack心跳返回消息
            writeAckPingPongMessage(ctx, ack, orderedMessage, promise, NetPackageType.ACK_PONG);
        } else {
            super.write(ctx, orderedMessage, promise);
        }
    }

    // region 读取消息
    @Override
    protected void readMsg(ChannelHandlerContext ctx, NetPackageType netPackageType, ByteBuf msg) throws Exception {
        switch (netPackageType) {
            case CONNECT_REQUEST:
                tryReadConnectRequest(ctx, msg);
                break;
            case RPC_REQUEST:
                tryReadRpcRequestMessage(ctx, msg);
                break;
            case RPC_RESPONSE:
                tryReadRpcResponseMessage(ctx, msg);
                break;
            case ONE_WAY_MESSAGE:
                tryReadOneWayMessage(ctx, msg);
                break;
            case ACK_PING:
                tryReadAckPingMessage(ctx, msg);
                break;
            default:
                closeCtx(ctx, "unexpected netEventType " + netPackageType);
                break;
        }
    }

    /**
     * 客户端请求建立连接
     */
    private void tryReadConnectRequest(ChannelHandlerContext ctx, ByteBuf msg) {
        ConnectRequestTO connectRequestTO = readConnectRequest(msg);
        ConnectRequestEventParam connectRequestEventParam = new ConnectRequestEventParam(ctx.channel(), localGuid, portContext, connectRequestTO);
        netEventManager.publishEvent(NetEventType.CONNECT_REQUEST, connectRequestEventParam);
        if (!isInited()) {
            init(connectRequestTO.getClientGuid());
        }
    }

    /**
     * 读取客户端的ack-ping包
     */
    private void tryReadAckPingMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        ensureInited();

        AckPingPongEventParam ackPingEventParam = readAckPingPongMessage(ctx.channel(), localGuid, clientGuid, msg);
        netEventManager.publishEvent(NetEventType.ACK_PING, ackPingEventParam);
    }

    /**
     * 尝试读取rpc请求
     */
    private void tryReadRpcRequestMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        ensureInited();

        RpcRequestEventParam rpcRequestEventParam = readRpcRequestMessage(ctx.channel(), localGuid, clientGuid, msg);
        netEventManager.publishEvent(NetEventType.RPC_REQUEST, rpcRequestEventParam);
    }

    /**
     * 尝试读取rpc响应
     */
    private void tryReadRpcResponseMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        ensureInited();

        RpcResponseEventParam rpcResponseEventParam = readRpcResponseMessage(ctx.channel(), localGuid, clientGuid, msg);
        netEventManager.publishEvent(NetEventType.RPC_RESPONSE, rpcResponseEventParam);
    }

    /**
     * 尝试读取玩家或另一个服务器(该连接的客户端)发来的单向消息
     */
    private void tryReadOneWayMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        ensureInited();

        OneWayMessageEventParam oneWayMessageEventParam = readOneWayMessage(ctx.channel(), localGuid, clientGuid, msg);
        netEventManager.publishEvent(NetEventType.ONE_WAY_MESSAGE, oneWayMessageEventParam);
    }
    // endregion

    private void ensureInited() {
        if (!isInited()) {
            throw new IllegalStateException();
        }
    }
}
