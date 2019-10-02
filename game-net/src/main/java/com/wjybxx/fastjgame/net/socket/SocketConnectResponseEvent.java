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
 * 连接响应事件参数
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/7 13:08
 * github - https://github.com/hl845740757
 */
public class SocketConnectResponseEvent implements SocketEvent {

    /**
     * 发起请求的channel
     */
    private final Channel channel;
    /**
     * 本地会话guid
     */
    private final long localGuid;
    /**
     * 谁返回给我的结果。
     */
    private final long remoteGuid;
    /**
     * 对方期望的下一个消息号
     */
    private final long ack;
    /**
     * 建立连接结果
     */
    private final SocketConnectResponse connectResponse;

    public SocketConnectResponseEvent(Channel channel, long localGuid, long remoteGuid, long ack, SocketConnectResponse connectResponse) {
        this.channel = channel;
        this.localGuid = localGuid;
        this.remoteGuid = remoteGuid;
        this.ack = ack;
        this.connectResponse = connectResponse;
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

    public SocketConnectResponse getConnectResponse() {
        return connectResponse;
    }

    public boolean isSuccess() {
        return connectResponse.isSuccess();
    }

}
