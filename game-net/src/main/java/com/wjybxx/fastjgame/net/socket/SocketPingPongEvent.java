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

import com.wjybxx.fastjgame.net.rpc.PingPongMessage;
import io.netty.channel.Channel;

/**
 * 心跳事件
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/18
 * github - https://github.com/hl845740757
 */
public class SocketPingPongEvent implements SocketEvent {

    private final Channel channel;
    private final String sessionId;
    /**
     * 捎带确认的ack
     */
    private final long ack;

    /**
     * 被包装的消息
     */
    private final PingPongMessage pingOrPong;

    public SocketPingPongEvent(Channel channel, String sessionId, long ack, PingPongMessage pingOrPong) {
        this.channel = channel;
        this.sessionId = sessionId;
        this.ack = ack;
        this.pingOrPong = pingOrPong;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public String sessionId() {
        return sessionId;
    }

    public long getAck() {
        return ack;
    }

    public PingPongMessage getPingOrPong() {
        return pingOrPong;
    }

}
