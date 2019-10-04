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
    private final long localGuid;
    private final long remoteGuid;
    /**
     * 我期望的下一个消息号
     */
    private final long ack;
    /**
     * 请求信息
     */
    private final SocketConnectRequest connectRequest;
    /**
     * 该端口存储的额外信息
     */
    private final SocketPortExtraInfo portExtraInfo;

    public SocketConnectRequestEvent(Channel channel, long localGuid, long remoteGuid, long ack, SocketConnectRequest connectRequest, SocketPortExtraInfo portExtraInfo) {
        this.channel = channel;
        this.localGuid = localGuid;
        this.remoteGuid = remoteGuid;
        this.ack = ack;
        this.connectRequest = connectRequest;
        this.portExtraInfo = portExtraInfo;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public long localGuid() {
        return localGuid;
    }

    @Override
    public long remoteGuid() {
        return remoteGuid;
    }

    public long getAck() {
        return ack;
    }

    public SocketConnectRequest getConnectRequest() {
        return connectRequest;
    }

    public SocketPortExtraInfo getPortExtraInfo() {
        return portExtraInfo;
    }
}
