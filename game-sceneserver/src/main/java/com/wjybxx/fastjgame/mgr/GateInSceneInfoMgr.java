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
import com.wjybxx.fastjgame.misc.GateInSceneInfo;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.rpcservice.IGateInSceneInfoMgr;
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
public class GateInSceneInfoMgr implements IGateInSceneInfoMgr {

    private static final Logger logger = LoggerFactory.getLogger(GateInSceneInfoMgr.class);

    /**
     * worldGuid -> info
     * 一个scene可能连接同一个中心服的多个网关
     */
    private final Long2ObjectMap<GateInSceneInfo> guid2InfoMap = new Long2ObjectOpenHashMap<>();

    @Inject
    public GateInSceneInfoMgr() {

    }

    @Override
    public boolean register(Session session, CenterServerId serverId) {
        final GateInSceneInfo gateInSceneInfo = new GateInSceneInfo(session, serverId);
        guid2InfoMap.put(session.remoteGuid(), gateInSceneInfo);

        logger.info("gate server {} register success", session.sessionId());
        return true;
    }

    public Session getGateSession(long worldGuid) {
        final GateInSceneInfo gateInSceneInfo = guid2InfoMap.get(worldGuid);
        return null == gateInSceneInfo ? null : gateInSceneInfo.getSession();
    }

    public void onSessionDisconnect(long worldGuid) {
        final GateInSceneInfo gateInSceneInfo = guid2InfoMap.remove(worldGuid);
        if (null == gateInSceneInfo) {
            // 无效guid
            return;
        }
        gateInSceneInfo.getSession().close();
        logger.info("gate server {} disconnect", gateInSceneInfo.getSession().sessionId());

        // TODO 下线该网关上的玩家
    }
}
