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

package com.wjybxx.fastjgame.net.socket.ordered;

import com.wjybxx.fastjgame.net.socket.MessageEvent;
import com.wjybxx.fastjgame.net.socket.SocketEvent;
import io.netty.channel.Channel;

/**
 * 消息事件参数。
 * 它对应{@link OrderedMessage}，对方发送一个{@link OrderedMessage}，则产生一个{@link OrderedMessageEvent}事件。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/9 9:09
 * github - https://github.com/hl845740757
 */
public final class OrderedMessageEvent implements SocketEvent {
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
    private final MessageEvent wrappedMessageEvent;

    public OrderedMessageEvent(long ack, long sequence, MessageEvent wrappedMessageEvent) {
        this.wrappedMessageEvent = wrappedMessageEvent;
        this.ack = ack;
        this.sequence = sequence;
    }

    @Override
    public long remoteGuid() {
        return wrappedMessageEvent.remoteGuid();
    }

    @Override
    public Channel channel() {
        return wrappedMessageEvent.channel();
    }

    @Override
    public long localGuid() {
        return wrappedMessageEvent.localGuid();
    }

    public long getAck() {
        return ack;
    }

    public long getSequence() {
        return sequence;
    }

    public MessageEvent getWrappedMessageEvent() {
        return wrappedMessageEvent;
    }
}
