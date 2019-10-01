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
    private final SocketConnectRequest connectRequest;
    // TODO 该端口上需要的监听者信息 config

    public SocketConnectRequestEvent(Channel channel, long localGuid, SocketConnectRequest connectRequest) {
        this.localGuid = localGuid;
        this.channel = channel;
        this.connectRequest = connectRequest;
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
        return connectRequest.getClientGuid();
    }

    public SocketConnectRequest getConnectRequest() {
        return connectRequest;
    }
}
