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

import com.wjybxx.fastjgame.net.socket.ConnectResponse;

/**
 * 带有消息确认机制的连接响应
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/30
 * github - https://github.com/hl845740757
 */
public class OrderedConnectResponse {

    private final ConnectResponse connectResponse;
    private final long ack;

    public OrderedConnectResponse(ConnectResponse connectResponse, long ack) {
        this.connectResponse = connectResponse;
        this.ack = ack;
    }

    public ConnectResponse getConnectResponse() {
        return connectResponse;
    }

    public long getAck() {
        return ack;
    }
}
