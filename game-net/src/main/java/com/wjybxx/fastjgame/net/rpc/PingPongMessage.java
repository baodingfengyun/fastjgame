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

package com.wjybxx.fastjgame.net.rpc;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * 心跳包。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 11:51
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class PingPongMessage implements NetMessage {

    public static final PingPongMessage PING = new PingPongMessage();
    public static final PingPongMessage PONG = new PingPongMessage();

    private PingPongMessage() {
    }

    @Override
    public NetMessageType type() {
        return NetMessageType.PING_PONG;
    }
}
