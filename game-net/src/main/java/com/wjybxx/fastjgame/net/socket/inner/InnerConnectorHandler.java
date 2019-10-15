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

package com.wjybxx.fastjgame.net.socket.inner;

import com.wjybxx.fastjgame.concurrent.adapter.NettyFutureAdapter;
import com.wjybxx.fastjgame.net.common.OneWaySupportHandler;
import com.wjybxx.fastjgame.net.common.RpcSupportHandler;
import com.wjybxx.fastjgame.net.session.SessionDuplexHandlerAdapter;
import com.wjybxx.fastjgame.net.session.SessionHandlerContext;
import com.wjybxx.fastjgame.net.socket.*;
import com.wjybxx.fastjgame.utils.NetUtils;
import io.netty.channel.ChannelFuture;

/**
 * 内网建立连接的处理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/15
 * github - https://github.com/hl845740757
 */
public class InnerConnectorHandler extends SessionDuplexHandlerAdapter {

    public static final int INNER_VERIFY_TIMES = 1;

    private final ChannelFuture channelFuture;

    public InnerConnectorHandler(ChannelFuture channelFuture) {
        this.channelFuture = channelFuture;
    }

    @Override
    public void handlerAdded(SessionHandlerContext ctx) throws Exception {
        // 监听操作完成
        new NettyFutureAdapter<>(ctx.netEventLoop(), channelFuture)
                .addListener(future -> onConnectDone(ctx));
    }

    private void onConnectDone(SessionHandlerContext ctx) {
        assert ctx.netEventLoop().inEventLoop();
        final SocketSessionImp session = (SocketSessionImp) ctx.session();
        // 在等待的过程中，session被关闭了
        if (session.isClosed()) {
            NetUtils.closeQuietly(channelFuture);
            return;
        }
        // socket连接成功，则发送建立session请求，否则关闭session
        if (channelFuture.isSuccess()) {
            SocketConnectRequest connectRequest = new SocketConnectRequest(INNER_VERIFY_TIMES);
            InnerSocketConnectRequestTO connectRequestTO = new InnerSocketConnectRequestTO(connectRequest);
            channelFuture.channel().writeAndFlush(connectRequestTO);
        } else {
            ctx.session().close();
        }
    }

    @Override
    public void read(SessionHandlerContext ctx, Object msg) {
        SocketEvent socketEvent = (SocketEvent) msg;
        if (socketEvent.channel() == channelFuture.channel()) {
            final SocketSessionImp session = (SocketSessionImp) ctx.session();
            if (socketEvent instanceof SocketConnectResponseEvent) {
                // 建立连接响应
                onRcvConnectResponse(session, (SocketConnectResponseEvent) msg);
                return;
            }
            // socket断开事件，无法建立连接
            if (socketEvent instanceof SocketDisconnectEvent) {
                session.close();
                return;
            }
            // 错误的消息
            NetUtils.closeQuietly(socketEvent.channel());
        } else {
            // 错误的channel
            NetUtils.closeQuietly(socketEvent.channel());
        }
    }

    /**
     * 接收到建立连接响应
     */
    private void onRcvConnectResponse(SocketSessionImp session, SocketConnectResponseEvent connectResponseEvent) {
        // 验证次数不对 - 先核对次数
        SocketConnectResponse connectResponse = connectResponseEvent.getConnectResponse();
        if (connectResponse.getVerifyingTimes() != INNER_VERIFY_TIMES) {
            session.close();
            return;
        }
        // 初始化ack错误
        if (connectResponseEvent.getAck() != MessageQueue.INIT_ACK) {
            session.close();
            return;
        }
        // 不允许建立连接
        if (!connectResponse.isSuccess()) {
            session.close();
            return;
        }
        // 建立连接成功，删除自己，添加真正的handler逻辑
        session.pipeline()
                .remove(this)
                .addLast(new InnerSocketTransferHandler(channelFuture.channel()))
                .addLast(new InnerPingSupportHandler())
                .addLast(new OneWaySupportHandler())
                .addLast(new RpcSupportHandler());

        // 尝试激活session
        if (session.tryActive()) {
            session.pipeline().fireSessionActive();
        } else {
            session.closeForcibly();
        }
    }

    @Override
    public void close(SessionHandlerContext ctx) throws Exception {
        NetUtils.closeQuietly(channelFuture);
    }
}
