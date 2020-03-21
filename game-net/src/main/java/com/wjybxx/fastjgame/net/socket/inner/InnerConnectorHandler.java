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

import com.wjybxx.fastjgame.net.rpc.OneWaySupportHandler;
import com.wjybxx.fastjgame.net.rpc.RpcSupportHandler;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.session.SessionDuplexHandlerAdapter;
import com.wjybxx.fastjgame.net.session.SessionHandlerContext;
import com.wjybxx.fastjgame.net.socket.*;
import com.wjybxx.fastjgame.net.utils.NetUtils;
import com.wjybxx.fastjgame.net.utils.NettyAdapters;
import com.wjybxx.fastjgame.utils.concurrent.Promise;
import io.netty.channel.ChannelFuture;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 内网建立连接的处理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/15
 * github - https://github.com/hl845740757
 */
public class InnerConnectorHandler extends SessionDuplexHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(InnerConnectorHandler.class);

    private final ChannelFuture channelFuture;
    private final Promise<Session> connectPromise;
    /**
     * 建立连接的超时时间
     */
    private long deadline;

    public InnerConnectorHandler(ChannelFuture channelFuture, Promise<Session> connectPromise) {
        this.channelFuture = channelFuture;
        this.connectPromise = connectPromise;
    }

    @Override
    public void handlerAdded(SessionHandlerContext ctx) throws Exception {
        SocketSessionConfig config = (SocketSessionConfig) ctx.session().config();
        deadline = ctx.timerSystem().curTimeMillis()
                + config.connectTimeoutMs() + config.verifyTimeoutMs();

        // 监听操作完成
        NettyAdapters.delegateFuture(ctx.netEventLoop(), channelFuture)
                .addListener(future -> onConnectDone(ctx));
    }

    private void onConnectDone(SessionHandlerContext ctx) {
        assert ctx.netEventLoop().inEventLoop();
        final SocketSessionImp session = (SocketSessionImp) ctx.session();
        if (session.isClosed()) {
            // 在等待的过程中，session被关闭了
            NetUtils.closeQuietly(channelFuture);
            return;
        }

        if (channelFuture.isSuccess()) {
            // socket连接成功，则发送建立session请求
            channelFuture.channel().writeAndFlush(InnerUtils.INNER_CONNECT_REQUEST_TO);
        } else {
            // socket建立失败，关闭session
            ctx.session().close();
        }
    }

    @Override
    public void tick(SessionHandlerContext ctx) throws Exception {
        if (ctx.timerSystem().curTimeMillis() > deadline) {
            // 指定时间内未连接到对方，关闭session
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

            if (socketEvent instanceof SocketChannelInactiveEvent) {
                // socket断开事件，无法建立连接
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
    private void onRcvConnectResponse(SocketSessionImp session, SocketConnectResponseEvent event) {
        SocketConnectResponse connectResponse = event.getConnectResponse();

        if (event.isClose()) {
            // 内网不应该出现close为true
            session.close();
            return;
        }

        if (event.getInitSequence() != InnerUtils.INNER_SEQUENCE
                || event.getAck() != InnerUtils.INNER_ACK) {
            // 不匹配内网ack和sequence参数
            session.close();
            return;
        }

        if (connectResponse.getVerifyingTimes() != InnerUtils.INNER_VERIFY_TIMES
                || connectResponse.getVerifiedTimes() != InnerUtils.INNER_VERIFIED_TIMES) {
            // 不匹配内网请求参数
            session.close();
            return;
        }

        if (!connectResponse.isSuccess()) {
            // 不允许建立连接
            session.close();
            return;
        }

        // 验证成功 - 可以建立连接
        if (connectPromise.trySuccess(session)) {
            // 激活session成功并初始化通道 - 删除自己，添加真正的handler逻辑
            session.tryActive();
            session.pipeline()
                    .remove(this)
                    .addLast(new InnerSocketTransferHandler(channelFuture.channel()))
                    .addLast(new PingPingSupportHandler())
                    .addLast(new OneWaySupportHandler());

            // 判断是否支持rpc
            if (session.config().isRpcAvailable()) {
                session.pipeline().addLast(new RpcSupportHandler());
            }

            // 传递session激活事件
            session.pipeline().fireSessionActive();
        } else {
            // 用户取消了连接
            session.closeForcibly();
        }
    }

    @Override
    public void close(SessionHandlerContext ctx) throws Exception {
        NetUtils.closeQuietly(channelFuture);
        // 无法建立连接
        connectPromise.tryFailure(new IOException("connect failure"));

        if (logger.isDebugEnabled()) {
            // 打印关闭原因
            logger.debug("close stacktrace {} ", ExceptionUtils.getStackTrace(new RuntimeException()));
        }
    }
}
