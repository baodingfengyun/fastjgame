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
public class RpcRequestMessage extends NetMessage {

    /**
     * rpc请求编号，用于返回消息
     */
    private long requestGuid;
    /**
     * 是否加急
     */
    private boolean immediate;
    /**
     * rpc请求内容
     */
    private Object request;

    public RpcRequestMessage(long requestGuid, boolean immediate, Object request) {
        super();
        this.requestGuid = requestGuid;
        this.immediate = immediate;
        this.request = request;
    }

    public long getRequestGuid() {
        return requestGuid;
    }

    public boolean isImmediate() {
        return immediate;
    }

    public Object getRequest() {
        return request;
    }
}
