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

package com.wjybxx.fastjgame.core.onlinenode;

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
public abstract class TcpServerNodeData {

    /**
     * 服务器之间通信用的tcp端口信息，格式  host:port
     * (可以和对外开放的端口是同一个，如果与前端通信也用protoBuffer)
     */
    private final String innerTcpAddress;

    /**
     * 用于GM等工具而绑定的http端口信息
     */
    private final String innerHttpAddress;
    /**
     * 127.0.0.1:x类型地址
     */
    private final String loopbackAddress;
    /**
     * 机器mac地址
     */
    private final String macAddress;

    @JsonCreator
    public TcpServerNodeData(@JsonProperty("innerTcpAddress") String innerTcpAddress,
                             @JsonProperty("innerHttpAddress") String innerHttpAddress,
                             @JsonProperty("loopbackAddress") String loopbackAddress,
                             @JsonProperty("macAddress") String macAddress) {
        this.innerTcpAddress = innerTcpAddress;
        this.innerHttpAddress = innerHttpAddress;
        this.loopbackAddress = loopbackAddress;
        this.macAddress = macAddress;
    }

    public String getInnerTcpAddress() {
        return innerTcpAddress;
    }

    public String getInnerHttpAddress() {
        return innerHttpAddress;
    }

    public String getLoopbackAddress() {
        return loopbackAddress;
    }

    public String getMacAddress() {
        return macAddress;
    }
}
