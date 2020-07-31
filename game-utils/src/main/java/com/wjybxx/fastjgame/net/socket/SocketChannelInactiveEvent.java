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
import io.netty.channel.ChannelHandlerContext;

/**
 * {@link io.netty.channel.ChannelInboundHandler#channelInactive(ChannelHandlerContext)}事件
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/14
 * github - https://github.com/hl845740757
 */
public class SocketChannelInactiveEvent implements SocketEvent {

    private final Channel channel;
    private final String sessionId;

    public SocketChannelInactiveEvent(Channel channel, String sessionId) {
        this.channel = channel;
        this.sessionId = sessionId;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public String sessionId() {
        return sessionId;
    }

}
