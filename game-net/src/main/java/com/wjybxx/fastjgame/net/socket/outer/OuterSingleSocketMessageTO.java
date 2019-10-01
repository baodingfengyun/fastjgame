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

import com.wjybxx.fastjgame.net.socket.SingleSocketMessageTO;
import com.wjybxx.fastjgame.net.socket.SocketMessage;

/**
 * 对外的socket消息传输对象
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/1
 * github - https://github.com/hl845740757
 */
public class OuterSingleSocketMessageTO implements SingleSocketMessageTO {

    private final long ack;
    private final SocketMessage socketMessage;

    OuterSingleSocketMessageTO(long ack, SocketMessage socketMessage) {
        this.ack = ack;
        this.socketMessage = socketMessage;
    }

    @Override
    public long getAck() {
        return ack;
    }

    @Override
    public SocketMessage getSocketMessage() {
        return socketMessage;
    }
}
