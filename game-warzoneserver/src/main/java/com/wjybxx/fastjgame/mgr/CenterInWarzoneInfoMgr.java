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

package com.wjybxx.fastjgame.mgr;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.misc.CenterInWarzoneInfo;
import com.wjybxx.fastjgame.misc.CenterServerId;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.rpcservice.ICenterInWarzoneInfoMgr;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Center在Warzone中的控制器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/17 15:43
 * github - https://github.com/hl845740757
 */
public class CenterInWarzoneInfoMgr implements ICenterInWarzoneInfoMgr {

    private static final Logger logger = LoggerFactory.getLogger(CenterInWarzoneInfoMgr.class);
    /**
     * guid -> info
     */
    private final Long2ObjectMap<CenterInWarzoneInfo> guid2InfoMap = new Long2ObjectOpenHashMap<>();
    /**
     * 服务器id -> info
     */
    private final Map<CenterServerId, CenterInWarzoneInfo> serverId2InfoMap = new HashMap<>();

    @Inject
    public CenterInWarzoneInfoMgr() {

    }

    private void addInfo(CenterInWarzoneInfo centerInWarzoneInfo) {
        guid2InfoMap.put(centerInWarzoneInfo.getCenterWorldGuid(), centerInWarzoneInfo);
        serverId2InfoMap.put(centerInWarzoneInfo.getServerId(), centerInWarzoneInfo);

        logger.info("server {} register success.", centerInWarzoneInfo.getServerId());
    }

    private void removeInfo(CenterInWarzoneInfo centerInWarzoneInfo) {
        guid2InfoMap.remove(centerInWarzoneInfo.getCenterWorldGuid());
        serverId2InfoMap.remove(centerInWarzoneInfo.getServerId());

        logger.info("server {} disconnect.", centerInWarzoneInfo);
    }

    @Override
    public boolean connectWarzone(Session session, CenterServerId serverId) {
        assert !guid2InfoMap.containsKey(session.remoteGuid());
        assert !serverId2InfoMap.containsKey(serverId);

        CenterInWarzoneInfo centerInWarzoneInfo = new CenterInWarzoneInfo(session, serverId);
        addInfo(centerInWarzoneInfo);
        return true;
    }

    public void onCenterServerDisconnect(long centerWorldGuid) {
        CenterInWarzoneInfo centerInWarzoneInfo = guid2InfoMap.get(centerWorldGuid);
        if (null == centerInWarzoneInfo) {
            return;
        }
        removeInfo(centerInWarzoneInfo);
    }

    /**
     * 获取center服的会话
     *
     * @param serverId 服务器id
     * @return session
     */
    @Nullable
    public Session getCenterSession(CenterServerId serverId) {
        final CenterInWarzoneInfo centerInWarzoneInfo = serverId2InfoMap.get(serverId);
        return null == centerInWarzoneInfo ? null : centerInWarzoneInfo.getSession();
    }

    /**
     * 获取center服的会话
     *
     * @param worldGuid 中心服对应的worldGuid
     * @return session
     */
    @Nullable
    public Session getCenterSession(long worldGuid) {
        final CenterInWarzoneInfo centerInWarzoneInfo = guid2InfoMap.get(worldGuid);
        return null == centerInWarzoneInfo ? null : centerInWarzoneInfo.getSession();
    }
}
