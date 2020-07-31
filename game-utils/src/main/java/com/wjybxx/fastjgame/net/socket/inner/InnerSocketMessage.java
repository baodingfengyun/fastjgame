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

import com.wjybxx.fastjgame.net.rpc.NetMessage;
import com.wjybxx.fastjgame.net.socket.SocketMessage;
import com.wjybxx.fastjgame.net.socket.SocketMessageTO;

/**
 * 内网单个socket消息 - 自己负责传输
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/1
 * github - https://github.com/hl845740757
 */
public class InnerSocketMessage implements SocketMessage, SocketMessageTO {

    /**
     * 被包装的消息
     */
    private final NetMessage wrappedMessage;

    InnerSocketMessage(NetMessage wrappedMessage) {
        this.wrappedMessage = wrappedMessage;
    }

    @Override
    public long getSequence() {
        return InnerUtils.INNER_SEQUENCE;
    }

    @Override
    public NetMessage getWrappedMessage() {
        return wrappedMessage;
    }

    @Override
    public long getAck() {
        return InnerUtils.INNER_ACK;
    }

    @Override
    public SocketMessage getSocketMessage() {
        return this;
    }
}
