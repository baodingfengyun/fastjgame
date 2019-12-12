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

import com.wjybxx.fastjgame.misc.PlatformType;

/**
 * 网关节点名字(网关节点不互斥)
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/11
 * github - https://github.com/hl845740757
 */
public class GateNodeName {

    /**
     * 战区id来自父节点
     */
    private final int warzoneId;
    /**
     * 所属的平台
     */
    private final PlatformType platformType;
    /**
     * 所属的服
     */
    private final int serverId;
    /**
     * guid
     */
    private final long worldGuid;

    public GateNodeName(int warzoneId, PlatformType platformType, int serverId, long worldGuid) {
        this.warzoneId = warzoneId;
        this.platformType = platformType;
        this.serverId = serverId;
        this.worldGuid = worldGuid;
    }

    public int getWarzoneId() {
        return warzoneId;
    }

    public PlatformType getPlatformType() {
        return platformType;
    }

    public int getServerId() {
        return serverId;
    }

    public long getWorldGuid() {
        return worldGuid;
    }

}
