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

import com.wjybxx.fastjgame.manager.NetEventManager;
import com.wjybxx.fastjgame.net.common.NetMessageType;
import com.wjybxx.fastjgame.net.common.ProtocolCodec;
import com.wjybxx.fastjgame.utils.NetUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

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
    private final SocketPortExtraInfo portExtraInfo;
    private final NetEventManager netEventManager;

    public ServerSocketCodec(ProtocolCodec codec, SocketPortExtraInfo portExtraInfo, NetEventManager netEventManager) {
        super(codec);
        this.portExtraInfo = portExtraInfo;
        this.netEventManager = netEventManager;
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
    public void write(ChannelHandlerContext ctx, Object msgTO, ChannelPromise promise) throws Exception {
        if (msgTO instanceof SocketMessageTO) {
            // 单个协议包
            writeSingleMsg(ctx, (SocketMessageTO) msgTO, promise);
        } else if (msgTO instanceof BatchSocketMessageTO) {
            // 批量协议包
            writeBatchMessage(ctx, (BatchSocketMessageTO) msgTO);
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
        netEventManager.fireConnectRequest(connectRequestEvent);

        if (!isInited()) {
            init(connectRequestEvent.sessionId());
            // 删除读超时控制，由session负责超时控制 - 只要读取到建立session请求后，NetEventLoop就能管理channel，否则无法管理channel
            ctx.channel().pipeline().remove(NetUtils.READ_TIMEOUT_HANDLER_NAME);
        }
    }

    /**
     * 尝试读取rpc请求
     */
    private void tryReadRpcRequestMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        ensureInited();

        netEventManager.fireMessage_acceptor(readRpcRequestMessage(ctx.channel(), sessionId, msg));
    }

    /**
     * 尝试读取rpc响应
     */
    private void tryReadRpcResponseMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        ensureInited();

        netEventManager.fireMessage_acceptor(readRpcResponseMessage(ctx.channel(), sessionId, msg));
    }

    /**
     * 尝试读取玩家或另一个服务器(该连接的客户端)发来的单向消息
     */
    private void tryReadOneWayMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        ensureInited();

        netEventManager.fireMessage_acceptor(readOneWayMessage(ctx.channel(), sessionId, msg));
    }

    /**
     * 读取客户端的ack-ping包
     */
    private void tryReadAckPingMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        ensureInited();

        netEventManager.fireMessage_acceptor(readAckPingPongMessage(ctx.channel(), sessionId, msg));
    }

    // endregion

    private void ensureInited() {
        if (!isInited()) {
            throw new IllegalStateException();
        }
    }
}
