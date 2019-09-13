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

import com.wjybxx.fastjgame.manager.NetEventManager;
import com.wjybxx.fastjgame.net.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * 客户端用编解码器。
 * 并非真正的非线程安全，而是唯一关联一个会话，没法共享。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/7 12:10
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class ClientCodec extends BaseCodec {

    /**
     * 该channel关联的本地对象标识
     */
    private final long localGuid;
    /**
     * 该channel关联的远程对象标识
     */
    private final long serverGuid;
    /**
     * 是否已建立链接
     */
    private boolean connect = false;
    /**
     * 用于发布网络事件
     */
    private final NetEventManager netEventManager;

    public ClientCodec(ProtocolCodec codec, long localGuid, long serverGuid, NetEventManager netEventManager) {
        super(codec);
        this.localGuid = localGuid;
        this.serverGuid = serverGuid;
        this.netEventManager = netEventManager;
    }

    // region 编码消息
    @Override
    public void write(ChannelHandlerContext ctx, Object msgTO, ChannelPromise promise) throws Exception {
        if (msgTO instanceof BatchMessageTO) {
            // 批量协议包
            BatchMessageTO batchMessageTO = (BatchMessageTO) msgTO;
            long ack = batchMessageTO.getAck();
            for (NetMessage message : batchMessageTO.getNetMessages()) {
                writeSingleMsg(ctx, ack, message, ctx.voidPromise());
            }
        } else if (msgTO instanceof SingleMessageTO) {
            // 单个协议包
            SingleMessageTO singleMessageTO = (SingleMessageTO) msgTO;
            writeSingleMsg(ctx, singleMessageTO.getAck(), singleMessageTO.getNetMessage(), promise);
        } else if (msgTO instanceof ConnectRequestTO) {
            // 请求建立连接包
            writeConnectRequest(ctx, (ConnectRequestTO) msgTO, promise);
        } else {
            super.write(ctx, msgTO, promise);
        }
    }

    /**
     * 发送单个消息
     */
    private void writeSingleMsg(ChannelHandlerContext ctx, long ack, NetMessage netMessage, ChannelPromise promise) throws Exception {
        // 按出现的几率判断
        if (netMessage instanceof RpcRequestMessage) {
            // rpc请，向另一个服务器发起rpc请求
            writeRpcRequestMessage(ctx, ack, (RpcRequestMessage) netMessage, promise);
        } else if (netMessage instanceof OneWayMessage) {
            // 单向消息，向另一个服务器发送单向消息
            writeOneWayMessage(ctx, ack, (OneWayMessage) netMessage, promise);
        } else if (netMessage instanceof RpcResponseMessage) {
            // rpc返回结果
            writeRpcResponseMessage(ctx, ack, (RpcResponseMessage) netMessage, promise);
        } else if (netMessage instanceof AckPingPongMessage) {
            // 客户端ack-ping包
            writeAckPingPongMessage(ctx, ack, (AckPingPongMessage) netMessage, promise, NetPackageType.ACK_PING);
        } else {
            super.write(ctx, netMessage, promise);
        }
    }
    // endregion

    // region 读取消息
    @Override
    protected void readMsg(ChannelHandlerContext ctx, NetPackageType netPackageType, ByteBuf msg) {
        switch (netPackageType) {
            case CONNECT_RESPONSE:
                tryReadConnectResponse(ctx, msg);
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
            case ACK_PONG:
                tryReadAckPongMessage(ctx, msg);
                break;
            default:
                closeCtx(ctx, "unexpected netEventType " + netPackageType);
                break;
        }
    }

    /**
     * 服务器返回的建立连接验证结果
     */
    private void tryReadConnectResponse(ChannelHandlerContext ctx, ByteBuf msg) {
        ConnectResponseTO responseTO = readConnectResponse(msg);
        ConnectResponseEventParam connectResponseParam = new ConnectResponseEventParam(ctx.channel(), localGuid, serverGuid, responseTO);
        netEventManager.publishEvent(NetEventType.CONNECT_RESPONSE, connectResponseParam);

        // 标记为已连接
        if (connectResponseParam.isSuccess()) {
            connect = true;
        }
    }

    /**
     * 服务器返回的ack-pong包
     */
    private void tryReadAckPongMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        ensureConnected();

        AckPingPongEventParam ackPongParam = readAckPingPongMessage(ctx.channel(), localGuid, serverGuid, msg);
        netEventManager.publishEvent(NetEventType.ACK_PONG, ackPongParam);
    }

    /**
     * 尝试读取远程的rpc请求消息
     */
    private void tryReadRpcRequestMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        ensureConnected();

        RpcRequestEventParam rpcRequestEventParam = readRpcRequestMessage(ctx.channel(), localGuid, serverGuid, msg);
        netEventManager.publishEvent(NetEventType.S2C_RPC_REQUEST, rpcRequestEventParam);
    }

    /**
     * 读取我发起的Rpc的响应消息
     */
    private void tryReadRpcResponseMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        ensureConnected();

        RpcResponseEventParam rpcResponseEventParam = readRpcResponseMessage(ctx.channel(), localGuid, serverGuid, msg);
        netEventManager.publishEvent(NetEventType.C2S_RPC_RESPONSE, rpcResponseEventParam);
    }

    /**
     * 读取连接的服务器方发来的单向消息
     */
    private void tryReadOneWayMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        ensureConnected();

        OneWayMessageEventParam oneWayMessageEventParam = readOneWayMessage(ctx.channel(), localGuid, serverGuid, msg);
        netEventManager.publishEvent(NetEventType.S2C_ONE_WAY_MESSAGE, oneWayMessageEventParam);
    }
    // endregion

    private void ensureConnected() {
        if (!connect) {
            throw new IllegalStateException();
        }
    }
}
