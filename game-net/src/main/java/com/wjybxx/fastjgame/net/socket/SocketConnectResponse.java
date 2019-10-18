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
import com.wjybxx.fastjgame.net.common.NetMessageType;

/**
 * 建立连接应答
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/30
 * github - https://github.com/hl845740757
 */
public class SocketConnectResponse implements NetMessage {

    /**
     * 验证是否成功
     */
    private final boolean success;
    // 请求参数
    private final int verifyingTimes;
    private final int verifiedTimes;

    public SocketConnectResponse(boolean success, SocketConnectRequest connectRequest) {
        this(success, connectRequest.getVerifyingTimes(), connectRequest.getVerifiedTimes());
    }

    public SocketConnectResponse(boolean success, int verifyingTimes, int verifiedTimes) {
        this.success = success;
        this.verifyingTimes = verifyingTimes;
        this.verifiedTimes = verifiedTimes;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getVerifyingTimes() {
        return verifyingTimes;
    }

    public int getVerifiedTimes() {
        return verifiedTimes;
    }

    @Override
    public NetMessageType type() {
        return NetMessageType.CONNECT_RESPONSE;
    }
}
