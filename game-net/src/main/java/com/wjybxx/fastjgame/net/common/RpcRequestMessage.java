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

package com.wjybxx.fastjgame.net.common;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Rpc请求消息
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/31
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class RpcRequestMessage extends NetLogicMessage {

    /**
     * rpc请求编号，用于返回消息
     */
    private final long requestGuid;
    /**
     * 是否是同步rpc调用
     */
    private final boolean sync;

    public RpcRequestMessage(long requestGuid, boolean sync, Object requestBody) {
        super(requestBody);
        this.requestGuid = requestGuid;
        this.sync = sync;
    }

    public long getRequestGuid() {
        return requestGuid;
    }

    public boolean isSync() {
        return sync;
    }

    @Override
    public NetMessageType type() {
        return NetMessageType.RPC_REQUEST;
    }

}
