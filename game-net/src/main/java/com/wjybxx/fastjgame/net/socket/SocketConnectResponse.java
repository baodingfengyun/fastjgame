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

/**
 * 建立连接应答
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/30
 * github - https://github.com/hl845740757
 */
public class SocketConnectResponse {

    /**
     * 验证是否成功
     */
    private final boolean success;
    /**
     * 这是客户端第几次验证的结果 - 与连接请求匹配
     */
    private final int verifyingTimes;

    public SocketConnectResponse(boolean success, int verifyingTimes) {
        this.success = success;
        this.verifyingTimes = verifyingTimes;
    }

    public int getVerifyingTimes() {
        return verifyingTimes;
    }

    public boolean isSuccess() {
        return success;
    }

}
