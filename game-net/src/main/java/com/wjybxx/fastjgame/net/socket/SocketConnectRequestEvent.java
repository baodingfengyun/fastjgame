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

import io.netty.channel.Channel;

/**
 * 连接请求事件参数
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/2
 * github - https://github.com/hl845740757
 */
public class SocketConnectRequestEvent implements SocketEvent {

    private final Channel channel;
    private final String sessionId;
    /**
     * 我的初始sequence
     */
    private final long initSequence;
    /**
     * 我期望的下一个消息号
     */
    private final long ack;
    /**
     * 是否是关闭连接请求
     */
    private final boolean close;
    /**
     * 请求信息
     */
    private final SocketConnectRequest connectRequest;
    /**
     * 该端口存储的额外信息
     */
    private final SocketPortContext portExtraInfo;

    public SocketConnectRequestEvent(Channel channel, String sessionId, long initSequence, long ack, boolean close, SocketConnectRequest connectRequest, SocketPortContext portExtraInfo) {
        this.channel = channel;
        this.sessionId = sessionId;
        this.initSequence = initSequence;
        this.ack = ack;
        this.close = close;
        this.connectRequest = connectRequest;
        this.portExtraInfo = portExtraInfo;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public String sessionId() {
        return sessionId;
    }

    public long getInitSequence() {
        return initSequence;
    }

    public long getAck() {
        return ack;
    }

    public boolean isClose() {
        return close;
    }

    public SocketConnectRequest getConnectRequest() {
        return connectRequest;
    }

    public SocketPortContext getPortExtraInfo() {
        return portExtraInfo;
    }

}
