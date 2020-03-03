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
 * RPC调用结果
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/31
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class RpcResponseMessage extends NetLogicMessage {

    /**
     * 客户端的哪一个请求
     */
    private final long requestGuid;
    /**
     * 错误码
     */
    private final RpcErrorCode errorCode;

    public RpcResponseMessage(long requestGuid, RpcErrorCode errorCode, Object body) {
        super(body);
        this.requestGuid = requestGuid;
        this.errorCode = errorCode;
    }

    public long getRequestGuid() {
        return requestGuid;
    }

    public RpcErrorCode getErrorCode() {
        return errorCode;
    }

    @Override
    public NetMessageType type() {
        return NetMessageType.RPC_RESPONSE;
    }
}
