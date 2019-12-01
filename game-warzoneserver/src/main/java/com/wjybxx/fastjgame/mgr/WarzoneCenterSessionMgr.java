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
import com.wjybxx.fastjgame.misc.CenterServerId;
import com.wjybxx.fastjgame.misc.WarzoneCenterSession;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.rpcservice.IWarzoneCenterSessionMgr;
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
public class WarzoneCenterSessionMgr implements IWarzoneCenterSessionMgr {

    private static final Logger logger = LoggerFactory.getLogger(WarzoneCenterSessionMgr.class);
    /**
     * guid -> info
     */
    private final Long2ObjectMap<WarzoneCenterSession> guid2InfoMap = new Long2ObjectOpenHashMap<>();
    /**
     * 服务器id -> info
     */
    private final Map<CenterServerId, WarzoneCenterSession> serverId2InfoMap = new HashMap<>();

    @Inject
    public WarzoneCenterSessionMgr() {

    }

    private void addInfo(WarzoneCenterSession warzoneCenterSession) {
        guid2InfoMap.put(warzoneCenterSession.getCenterWorldGuid(), warzoneCenterSession);
        serverId2InfoMap.put(warzoneCenterSession.getServerId(), warzoneCenterSession);
    }

    private void removeInfo(WarzoneCenterSession warzoneCenterSession) {
        guid2InfoMap.remove(warzoneCenterSession.getCenterWorldGuid());
        serverId2InfoMap.remove(warzoneCenterSession.getServerId());
    }

    @Override
    public boolean register(Session session, CenterServerId serverId) {
        if (serverId2InfoMap.containsKey(serverId)) {
            return false;
        }

        WarzoneCenterSession warzoneCenterSession = new WarzoneCenterSession(session, serverId);
        addInfo(warzoneCenterSession);
        logger.info("center server {} register success.", warzoneCenterSession.getServerId());
        return true;
    }

    public void onCenterServerDisconnect(long centerWorldGuid) {
        WarzoneCenterSession warzoneCenterSession = guid2InfoMap.get(centerWorldGuid);
        if (null == warzoneCenterSession) {
            return;
        }
        removeInfo(warzoneCenterSession);
        logger.info("center server {} disconnect.", warzoneCenterSession);
    }

    /**
     * 获取center服的会话
     *
     * @param serverId 服务器id
     * @return session
     */
    @Nullable
    public Session getCenterSession(CenterServerId serverId) {
        final WarzoneCenterSession warzoneCenterSession = serverId2InfoMap.get(serverId);
        return null == warzoneCenterSession ? null : warzoneCenterSession.getSession();
    }

    /**
     * 获取center服的会话
     *
     * @param worldGuid 中心服对应的worldGuid
     * @return session
     */
    @Nullable
    public Session getCenterSession(long worldGuid) {
        final WarzoneCenterSession warzoneCenterSession = guid2InfoMap.get(worldGuid);
        return null == warzoneCenterSession ? null : warzoneCenterSession.getSession();
    }
}
