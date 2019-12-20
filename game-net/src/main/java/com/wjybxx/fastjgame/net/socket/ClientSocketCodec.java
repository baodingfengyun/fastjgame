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

import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.net.common.NetMessageType;
import com.wjybxx.fastjgame.net.common.ProtocolCodec;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;

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
public class ClientSocketCodec extends BaseSocketCodec {

    /**
     * channel关联的sessionId
     */
    private final String sessionId;
    private final long localGuid;
    /**
     * 是否已建立链接
     */
    private boolean connect = false;
    /**
     * 该session所属的运行环境。
     * <p>
     * 对于连接的发起方来讲，它的运行环境是确定的。
     */
    private final NetEventLoop netEventLoop;

    public ClientSocketCodec(ProtocolCodec codec, String sessionId, long localGuid, NetEventLoop netEventLoop) {
        super(codec);
        this.sessionId = sessionId;
        this.localGuid = localGuid;
        this.netEventLoop = netEventLoop;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        publish(new SocketChannelInactiveEvent(ctx.channel(), sessionId));
    }

    // region 编码消息
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
        } else if (msgTO instanceof SocketConnectRequestTO) {
            // 请求建立连接包
            writeConnectRequest(ctx, sessionId, localGuid, (SocketConnectRequestTO) msgTO, promise);
        } else {
            super.write(ctx, msgTO, promise);
        }
    }

    // endregion

    // region 读取消息
    @Override
    protected void readMsg(ChannelHandlerContext ctx, NetMessageType netMessageType, ByteBuf msg) throws IOException {
        switch (netMessageType) {
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
            case PING_PONG:
                tryReadAckPongMessage(ctx, msg);
                break;
            default:
                throw new IOException("unexpected netEventType " + netMessageType);
        }
    }

    /**
     * 服务器返回的建立连接验证结果
     */
    private void tryReadConnectResponse(ChannelHandlerContext ctx, ByteBuf msg) {
        final SocketConnectResponseEvent socketConnectResponseEvent = readConnectResponse(ctx.channel(), sessionId, msg);
        // 标记为已连接
        if (socketConnectResponseEvent.getConnectResponse().isSuccess()) {
            connect = true;
        }
        publish(socketConnectResponseEvent);
    }

    /**
     * 尝试读取远程的rpc请求消息
     */
    private void tryReadRpcRequestMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        ensureConnected();
        publish(readRpcRequestMessage(ctx.channel(), sessionId, msg));
    }

    /**
     * 读取我发起的Rpc的响应消息
     */
    private void tryReadRpcResponseMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        ensureConnected();
        publish(readRpcResponseMessage(ctx.channel(), sessionId, msg));
    }

    /**
     * 读取连接的服务器方发来的单向消息
     */
    private void tryReadOneWayMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        ensureConnected();
        publish(readOneWayMessage(ctx.channel(), sessionId, msg));
    }

    /**
     * 服务器返回的ack-pong包
     */
    private void tryReadAckPongMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        ensureConnected();
        publish(readAckPingPongMessage(ctx.channel(), sessionId, msg));
    }
    // endregion

    private void ensureConnected() {
        if (!connect) {
            throw new IllegalStateException();
        }
    }

    private void publish(@Nonnull Object event) {
        netEventLoop.post(false, event);
    }
}
