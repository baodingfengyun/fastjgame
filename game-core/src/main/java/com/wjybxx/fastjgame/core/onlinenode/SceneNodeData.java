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
 * zookeeper上在线SceneServer节点信息
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/15 17:21
 * github - https://github.com/hl845740757
 */
public class SceneNodeData extends TcpServerNodeData {
    /**
     * 该场景进程的频道id
     */
    private final int channelId;
    /**
     * 对外开放的绑定的tcp端口信息(与玩家通信用)
     */
    private final String outerTcpAddress;
    /**
     * 对外开放的绑定的websocket端口信息(与玩家通信用)
     */
    private final String outerWebsocketAddress;

    @JsonCreator
    public SceneNodeData(@JsonProperty("innerTcpAddress") String innerTcpAddress,
                         @JsonProperty("innerHttpAddress") String innerHttpAddress,
                         @JsonProperty("loopbackAddress") String loopbackAddress,
                         @JsonProperty("macAddress") String macAddress,
                         @JsonProperty("channelId") int channelId,
                         @JsonProperty("outerTcpAddress") String outerTcpAddress,
                         @JsonProperty("outerWebsocketAddress") String outerWebsocketAddress) {
        super(innerTcpAddress, innerHttpAddress, loopbackAddress, macAddress);
        this.channelId = channelId;
        this.outerTcpAddress = outerTcpAddress;
        this.outerWebsocketAddress = outerWebsocketAddress;
    }

    public int getChannelId() {
        return channelId;
    }

    public String getOuterTcpAddress() {
        return outerTcpAddress;
    }

    public String getOuterWebsocketAddress() {
        return outerWebsocketAddress;
    }
}
