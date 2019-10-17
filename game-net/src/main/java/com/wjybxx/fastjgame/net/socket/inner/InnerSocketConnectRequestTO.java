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

import com.wjybxx.fastjgame.net.socket.SocketConnectRequest;
import com.wjybxx.fastjgame.net.socket.SocketConnectRequestTO;

/**
 * 内网建立连接请求传输对象 - 没有真正的ack
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/2
 * github - https://github.com/hl845740757
 */
public class InnerSocketConnectRequestTO implements SocketConnectRequestTO {

    private final SocketConnectRequest connectRequest;

    public InnerSocketConnectRequestTO(SocketConnectRequest connectRequest) {
        this.connectRequest = connectRequest;
    }

    @Override
    public long getInitSequence() {
        return InnerUtils.INNER_SEQUENCE;
    }

    @Override
    public long getAck() {
        return InnerUtils.INNER_ACK;
    }

    @Override
    public SocketConnectRequest getConnectRequest() {
        return connectRequest;
    }
}
