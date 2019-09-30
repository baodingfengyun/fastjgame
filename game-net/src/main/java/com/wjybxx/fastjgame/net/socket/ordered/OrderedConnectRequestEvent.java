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

import com.wjybxx.fastjgame.net.socket.ConnectRequestEvent;

/**
 * 带有消息确认机制的连接请求事件
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/30
 * github - https://github.com/hl845740757
 */
public class OrderedConnectRequestEvent {

    private final ConnectRequestEvent connectRequestEvent;
    private final long ack;

    public OrderedConnectRequestEvent(ConnectRequestEvent connectRequestEvent, long ack) {
        this.connectRequestEvent = connectRequestEvent;
        this.ack = ack;
    }

    public ConnectRequestEvent getConnectRequestEvent() {
        return connectRequestEvent;
    }

    public long getClientGuid() {
        return connectRequestEvent.getClientGuid();
    }

    public long getAck() {
        return ack;
    }
}
