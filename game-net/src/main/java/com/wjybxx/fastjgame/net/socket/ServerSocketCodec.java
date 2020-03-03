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

import com.wjybxx.fastjgame.net.misc.ProtocolCodec;
import com.wjybxx.fastjgame.net.rpc.NetMessageType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;

/**
 * 服务端使用的编解码器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/7 13:23
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class ServerSocketCodec extends BaseSocketCodec {

    private String sessionId;
    private final SocketPortContext portExtraInfo;

    public ServerSocketCodec(ProtocolCodec codec, SocketPortContext portExtraInfo) {
        super(codec);
        this.portExtraInfo = portExtraInfo;
    }

    /**
     * 是否已收到过客户端的连接请求,主要关系到后续需要使用的clientGuid
     *
     * @return 是否已接收到建立连接请求
     */
    private boolean isInited() {
        return sessionId != null;
    }

    /**
     * 标记为已接收过连接请求
     *
     * @param sessionId 客户端请求建立连接时的带过来的唯一标识
     */
    private void init(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        if (isInited()) {
            publish(new SocketChannelInactiveEvent(ctx.channel(), sessionId));
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msgTO, ChannelPromise promise) throws Exception {
        if (msgTO instanceof SocketMessageTO) {
            // 单个消息包
            writeSingleMsg(ctx, (SocketMessageTO) msgTO, promise);
        } else if (msgTO instanceof BatchSocketMessageTO) {
            // 批量协议包
            writeBatchMessage(ctx, (BatchSocketMessageTO) msgTO, promise);
        } else if (msgTO instanceof SocketPingPongMessageTO) {
            // 心跳包
            writeAckPingPongMessage(ctx, (SocketPingPongMessageTO) msgTO, promise);
        } else if (msgTO instanceof SocketConnectResponseTO) {
            // 建立连接验证结果
            writeConnectResponse(ctx, (SocketConnectResponseTO) msgTO, promise);
        } else {
            super.write(ctx, msgTO, promise);
        }
    }

    // region 读取消息
    @Override
    protected void readMsg(ChannelHandlerContext ctx, NetMessageType netMessageType, ByteBuf msg) throws Exception {
        switch (netMessageType) {
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
            case PING_PONG:
                tryReadAckPingMessage(ctx, msg);
                break;
            default:
                throw new IOException("unexpected netEventType " + netMessageType);
        }
    }

    /**
     * 客户端请求建立连接
     */
    private void tryReadConnectRequest(ChannelHandlerContext ctx, ByteBuf msg) {
        SocketConnectRequestEvent connectRequestEvent = readConnectRequest(ctx.channel(), msg, portExtraInfo);
        if (!isInited()) {
            init(connectRequestEvent.sessionId());
        }
        publish(connectRequestEvent);
    }

    /**
     * 尝试读取rpc请求
     */
    private void tryReadRpcRequestMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        ensureInited();
        publish(readRpcRequestMessage(ctx.channel(), sessionId, msg));
    }

    /**
     * 尝试读取rpc响应
     */
    private void tryReadRpcResponseMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        ensureInited();
        publish(readRpcResponseMessage(ctx.channel(), sessionId, msg));
    }

    /**
     * 尝试读取玩家或另一个服务器(该连接的客户端)发来的单向消息
     */
    private void tryReadOneWayMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        ensureInited();
        publish(readOneWayMessage(ctx.channel(), sessionId, msg));
    }

    /**
     * 读取客户端的ack-ping包
     */
    private void tryReadAckPingMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        ensureInited();
        publish(readAckPingPongMessage(ctx.channel(), sessionId, msg));
    }

    // endregion

    private void ensureInited() {
        if (!isInited()) {
            throw new IllegalStateException();
        }
    }

    private void publish(@Nonnull SocketEvent event) {
        portExtraInfo.netEventLoopGroup().select(sessionId).post(new GenericSocketEvent<>(event, true));
    }
}
