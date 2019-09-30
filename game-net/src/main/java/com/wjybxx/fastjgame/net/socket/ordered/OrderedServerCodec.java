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

import com.wjybxx.fastjgame.manager.NetEventManager;
import com.wjybxx.fastjgame.misc.SessionLifecycleAware;
import com.wjybxx.fastjgame.net.ProtocolCodec;
import com.wjybxx.fastjgame.net.socket.ConnectRequest;
import com.wjybxx.fastjgame.net.socket.ConnectRequestEvent;
import com.wjybxx.fastjgame.net.socket.ConnectResponse;
import com.wjybxx.fastjgame.net.socket.NetMessageType;
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
public class OrderedServerCodec extends OrderedBaseCodec {

    /**
     * 该channel关联哪本地哪一个用户
     */
    private final long localGuid;
    /**
     * 缓存的客户端guid，关联的远程
     */
    private long clientGuid = Long.MIN_VALUE;
    /**
     * 新建立的连接的生命周期通知器
     */
    private final SessionLifecycleAware lifecycleAware;

    private final NetEventManager netEventManager;

    public OrderedServerCodec(ProtocolCodec codec, long localGuid, SessionLifecycleAware lifecycleAware, NetEventManager netEventManager) {
        super(codec);
        this.localGuid = localGuid;
        this.lifecycleAware = lifecycleAware;
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
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof BatchOrderedMessageTO) {
            // 批量协议包
            writeBatchMessage(ctx, (BatchOrderedMessageTO) msg);
        } else if (msg instanceof SingleOrderedMessageTO) {
            // 单个协议包
            writeSingleMsg(ctx, (SingleOrderedMessageTO) msg, promise);
        } else if (msg instanceof OrderedConnectResponse) {
            // 建立连接验证结果
            writeConnectResponse(ctx, (OrderedConnectResponse) msg, promise);
        } else {
            super.write(ctx, msg, promise);
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
        OrderedConnectRequestEvent connectRequestEvent = readConnectRequest(ctx.channel(), localGuid, lifecycleAware, msg);
        netEventManager.fireConnectRequest(connectRequestEvent);

        if (!isInited()) {
            init(connectRequestEvent.getClientGuid());
        }
    }

    /**
     * 尝试读取rpc请求
     */
    private void tryReadRpcRequestMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        ensureInited();

        netEventManager.fireMessage(readRpcRequestMessage(ctx.channel(), localGuid, clientGuid, msg));
    }

    /**
     * 尝试读取rpc响应
     */
    private void tryReadRpcResponseMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        ensureInited();

        netEventManager.fireMessage(readRpcResponseMessage(ctx.channel(), localGuid, clientGuid, msg));
    }

    /**
     * 尝试读取玩家或另一个服务器(该连接的客户端)发来的单向消息
     */
    private void tryReadOneWayMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        ensureInited();

        netEventManager.fireMessage(readOneWayMessage(ctx.channel(), localGuid, clientGuid, msg));
    }

    /**
     * 读取客户端的ack-ping包
     */
    private void tryReadAckPingMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        ensureInited();

        netEventManager.fireMessage(readAckPingPongMessage(ctx.channel(), localGuid, clientGuid, msg));
    }

    // endregion

    private void ensureInited() {
        if (!isInited()) {
            throw new IllegalStateException();
        }
    }
}
