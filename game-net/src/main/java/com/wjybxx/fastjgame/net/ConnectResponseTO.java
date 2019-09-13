/*
 * Copyright 2019 wjybxx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wjybxx.fastjgame.net;

/**
 * 服务器连接请求的响应的传输对象
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 11:55
 * github - https://github.com/hl845740757
 */
@TransferObject
public class ConnectResponseTO {
    /**
     * 这是客户端第几次验证的结果
     */
    private int verifyingTimes;
    /**
     * 验证是否成功
     */
    private boolean success;
    /**
     * 服务器确认收到的最大消息号，ack
     */
    private long ack;

    public ConnectResponseTO(int verifyingTimes, boolean success, long ack) {
        this.verifyingTimes = verifyingTimes;
        this.success = success;
        this.ack = ack;
    }

    public int getVerifyingTimes() {
        return verifyingTimes;
    }

    public boolean isSuccess() {
        return success;
    }

    public long getAck() {
        return ack;
    }

}
