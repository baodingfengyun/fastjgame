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

package com.wjybxx.fastjgame.misc;

import com.wjybxx.fastjgame.net.session.Session;

/**
 * CenterServer在SceneServer中的信息
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/15 12:30
 * github - https://github.com/hl845740757
 */
public class CenterInSceneInfo {

    /**
     * CenterServer的worldguid
     */
    private final long centerWorldGuid;
    /**
     * 归属的平台
     */
    private final PlatformType platformType;
    /**
     * 连接的是哪个服
     */
    private final int serverId;

    private final Session session;

    public CenterInSceneInfo(long centerWorldGuid, PlatformType platformType, int serverId, Session session) {
        this.centerWorldGuid = centerWorldGuid;
        this.platformType = platformType;
        this.serverId = serverId;
        this.session = session;
    }

    public long getCenterWorldGuid() {
        return centerWorldGuid;
    }

    public PlatformType getPlatformType() {
        return platformType;
    }

    public int getServerId() {
        return serverId;
    }

    public Session getSession() {
        return session;
    }
}

