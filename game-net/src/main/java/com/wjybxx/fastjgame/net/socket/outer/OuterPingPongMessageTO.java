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

package com.wjybxx.fastjgame.net.socket.outer;

import com.wjybxx.fastjgame.net.rpc.PingPongMessage;
import com.wjybxx.fastjgame.net.socket.SocketPingPongMessageTO;

/**
 * 带有消息确认机制的心跳消息传输对象
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/18
 * github - https://github.com/hl845740757
 */
public class OuterPingPongMessageTO implements SocketPingPongMessageTO {

    private final long ack;
    private PingPongMessage message;

    public OuterPingPongMessageTO(long ack, PingPongMessage message) {
        this.ack = ack;
        this.message = message;
    }

    @Override
    public long getAck() {
        return ack;
    }

    @Override
    public PingPongMessage getPingOrPong() {
        return message;
    }
}
