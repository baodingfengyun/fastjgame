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

package com.wjybxx.fastjgame.node;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 监听了内网tcp端口的节点。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/15 17:23
 * github - https://github.com/hl845740757
 */
public class TcpServerNodeData {

    /**
     * 用于GM等工具而绑定的http端口信息
     */
    private final String innerHttpAddress;
    /**
     * 服务器之间通信用的tcp端口信息，格式  host:port
     * (可以和对外开放的端口是同一个，如果与前端通信也用protoBuffer)
     */
    private final String innerTcpAddress;

    @JsonCreator
    public TcpServerNodeData(@JsonProperty("innerHttpAddress") String innerHttpAddress,
                             @JsonProperty("innerTcpAddress") String innerTcpAddress) {
        this.innerTcpAddress = innerTcpAddress;
        this.innerHttpAddress = innerHttpAddress;
    }

    public String getInnerTcpAddress() {
        return innerTcpAddress;
    }

    public String getInnerHttpAddress() {
        return innerHttpAddress;
    }

}
