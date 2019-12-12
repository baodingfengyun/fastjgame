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
 * zookeeper在线WarzoneServer节点信息
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/15 17:22
 * github - https://github.com/hl845740757
 */
public class WarzoneNodeData extends TcpServerNodeData {

    /**
     * 战区节点必须互斥，因此guid在data里面，而不在名字里。
     */
    private final long worldGuid;

    @JsonCreator
    public WarzoneNodeData(@JsonProperty("innerHttpAddres") String innerHttpAddress,
                           @JsonProperty("innerTcpAddress") String innerTcpAddress,
                           @JsonProperty("worldGuid") long worldGuid) {
        super(innerHttpAddress, innerTcpAddress);
        this.worldGuid = worldGuid;
    }

    public long getWorldGuid() {
        return worldGuid;
    }
}
