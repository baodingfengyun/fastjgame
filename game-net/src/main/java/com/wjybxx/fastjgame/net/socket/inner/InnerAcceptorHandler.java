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

import com.wjybxx.fastjgame.manager.AcceptorManager;
import com.wjybxx.fastjgame.manager.NetManagerWrapper;
import com.wjybxx.fastjgame.net.common.OneWaySupportHandler;
import com.wjybxx.fastjgame.net.common.RpcSupportHandler;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.socket.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;

/**
 * 它并不是一个真正的handler - 只是用于封装代码
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/16
 * github - https://github.com/hl845740757
 */
public class InnerAcceptorHandler {

    /**
     * 接收到一个内网连接请求 - 不开启消息确认机制的请求
     */
    public static void onRcvInnerConnectRequest(SocketConnectRequestEvent event, NetManagerWrapper netManagerWrapper, AcceptorManager acceptorManager) {
        final Channel channel = event.channel();
        final SocketConnectRequest connectRequest = event.getConnectRequest();
        final Session existSession = acceptorManager.getSession(event.sessionId());
        if (existSession == null) {
            // 尝试建立新的session

            if (event.getInitSequence() != InnerUtils.INNER_SEQUENCE
                    || event.getAck() != InnerUtils.INNER_ACK) {
                // 不匹配内网ack和sequence参数
                onInnerConnectFail(channel, connectRequest);
                return;
            }

            if (connectRequest.getVerifyingTimes() != InnerUtils.INNER_VERIFY_TIMES
                    || connectRequest.getVerifiedTimes() != InnerUtils.INNER_VERIFIED_TIMES) {
                // 不匹配内网请求参数
                onInnerConnectFail(channel, connectRequest);
                return;
            }

            if (event.isClose()) {
                // 内网不应该出现close为true
                onInnerConnectFail(channel, connectRequest);
                return;
            }

            // 建立连接成功
            final SocketPortContext portExtraInfo = event.getPortExtraInfo();
            final SocketSessionImp session = new SocketSessionImp(portExtraInfo.getNetContext(),
                    event.sessionId(),
                    event.remoteGuid(),
                    portExtraInfo.getSessionConfig(),
                    netManagerWrapper,
                    acceptorManager);

            // 初始化管道
            session.pipeline()
                    .addLast(new InnerSocketTransferHandler(channel))
                    .addLast(new PingPingSupportHandler())
                    .addLast(new OneWaySupportHandler());

            // 判断是否支持rpc
            if (session.config().isRpcAvailable()) {
                session.pipeline().addLast(new RpcSupportHandler());
            }

            // 激活session并传递激活事件
            session.tryActive();
            session.pipeline().fireSessionActive();

            // 建立连接成功，告知对方
            onInnerConnectSuccess(channel, connectRequest);
        } else {
            // 内网无消息确认机制，不支持重连
            onInnerConnectFail(channel, connectRequest);
        }
    }

    /**
     * 建立连接成功 - 告知对方成功
     */
    private static void onInnerConnectSuccess(final Channel channel, final SocketConnectRequest connectRequest) {
        final SocketConnectResponse connectResponse = new SocketConnectResponse(true, connectRequest);
        final InnerSocketConnectResponseTO connectResponseTO = new InnerSocketConnectResponseTO(connectResponse);
        channel.writeAndFlush(connectResponseTO);
    }

    /**
     * 建立连接失败 - 告知对方关闭
     */
    private static void onInnerConnectFail(final Channel channel, final SocketConnectRequest connectRequest) {
        final SocketConnectResponse connectResponse = new SocketConnectResponse(false, connectRequest);
        final InnerSocketConnectResponseTO connectResponseTO = new InnerSocketConnectResponseTO(connectResponse);
        // 告知验证失败，且发送之后关闭channel
        channel.writeAndFlush(connectResponseTO)
                .addListener(ChannelFutureListener.CLOSE);
    }
}
