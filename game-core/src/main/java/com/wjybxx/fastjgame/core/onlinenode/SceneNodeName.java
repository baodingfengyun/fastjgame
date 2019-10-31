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

import com.wjybxx.fastjgame.misc.PlatformType;

/**
 * 场景节点名字。
 * 场景服需要不同的名字，场景进程之间没有直接的互斥关系，后启动的可以和先启动的同时存在。
 * platformType-serverId-channelId达成互斥
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/16 0:15
 * github - https://github.com/hl845740757
 */
public class SceneNodeName {
    /**
     * 战区id来自父节点
     */
    private final int warzoneId;
    /**
     * 所属的平台
     */
    private final PlatformType platformType;
    /**
     * 所属的服 - 如果为0，表示不指定中心服，只可以使用该场景服提供的跨服区域。
     */
    private final int serverId;
    /**
     * 频道id
     */
    private final int channelId;

    public SceneNodeName(int warzoneId, PlatformType platformType, int serverId, int channelId) {
        this.warzoneId = warzoneId;
        this.platformType = platformType;
        this.serverId = serverId;
        this.channelId = channelId;
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

    public int getChannelId() {
        return channelId;
    }

    @Override
    public String toString() {
        return "SceneNodeName{" +
                "warzoneId=" + warzoneId +
                ", platformType=" + platformType +
                ", serverId=" + serverId +
                ", channelId=" + channelId +
                '}';
    }
}
