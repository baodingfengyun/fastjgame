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

import com.wjybxx.fastjgame.net.common.NetMessage;
import io.netty.channel.Channel;

/**
 * 逻辑消息包事件
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/28
 * github - https://github.com/hl845740757
 */
public final class SocketMessageEvent implements SocketEvent {

    private final Channel channel;
    private final String sessionId;
    /**
     * 捎带确认的ack
     */
    private final long ack;
    /**
     * 当前包id
     */
    private final long sequence;
    /**
     * 被包装的消息
     */
    private final NetMessage wrappedMessage;

    public SocketMessageEvent(Channel channel, String sessionId, long ack, long sequence, NetMessage wrappedMessage) {
        this.channel = channel;
        this.sessionId = sessionId;
        this.ack = ack;
        this.sequence = sequence;
        this.wrappedMessage = wrappedMessage;
    }

    @Override
    public final Channel channel() {
        return channel;
    }

    @Override
    public String sessionId() {
        return sessionId;
    }

    public long getAck() {
        return ack;
    }

    public long getSequence() {
        return sequence;
    }

    public NetMessage getWrappedMessage() {
        return wrappedMessage;
    }
}
