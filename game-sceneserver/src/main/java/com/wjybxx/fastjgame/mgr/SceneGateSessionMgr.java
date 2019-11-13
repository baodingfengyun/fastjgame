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

package com.wjybxx.fastjgame.mgr;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.misc.CenterServerId;
import com.wjybxx.fastjgame.misc.SceneGateSession;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.rpcservice.ISceneGateSessionMgr;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 网关服在场景服下的连接管理
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/3
 * github - https://github.com/hl845740757
 */
public class SceneGateSessionMgr implements ISceneGateSessionMgr {

    private static final Logger logger = LoggerFactory.getLogger(SceneGateSessionMgr.class);

    /**
     * worldGuid -> info
     * 一个scene可能连接同一个中心服的多个网关
     */
    private final Long2ObjectMap<SceneGateSession> guid2InfoMap = new Long2ObjectOpenHashMap<>();

    @Inject
    public SceneGateSessionMgr() {

    }

    @Override
    public boolean register(Session session, CenterServerId serverId) {
        final SceneGateSession sceneGateSession = new SceneGateSession(session, serverId);
        guid2InfoMap.put(session.remoteGuid(), sceneGateSession);

        logger.info("gate server {} register success", session.sessionId());
        return true;
    }

    public Session getGateSession(long worldGuid) {
        final SceneGateSession sceneGateSession = guid2InfoMap.get(worldGuid);
        return null == sceneGateSession ? null : sceneGateSession.getSession();
    }

    public void onSessionDisconnect(long worldGuid) {
        final SceneGateSession sceneGateSession = guid2InfoMap.remove(worldGuid);
        if (null == sceneGateSession) {
            // 无效guid
            return;
        }
        sceneGateSession.getSession().close();
        logger.info("gate server {} disconnect", sceneGateSession.getSession().sessionId());

        // TODO 下线该网关上的玩家
    }
}
