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
import com.wjybxx.fastjgame.misc.CenterGateSession;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.rpcservice.ICenterGateSessionMgr;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 网关服session管理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/3
 * github - https://github.com/hl845740757
 */
public class CenterGateSessionMgr implements ICenterGateSessionMgr {

    private static final Logger logger = LoggerFactory.getLogger(CenterGateSessionMgr.class);

    /**
     * worldGuid -> info
     * 网关只有本服的网关，因此只需要建立一个映射即可
     */
    private final Long2ObjectMap<CenterGateSession> guid2InfoMap = new Long2ObjectOpenHashMap<>();

    @Inject
    public CenterGateSessionMgr() {
    }

    @Override
    public boolean register(Session session) {
        guid2InfoMap.put(session.remoteGuid(), new CenterGateSession(session));
        logger.info("gate {} registered", session.sessionId());
        return true;
    }

    @Override
    public void syncOnlinePlayerNum(Session session, int onlinePlayerNum) {
        final CenterGateSession centerGateSession = guid2InfoMap.get(session.remoteGuid());
        if (null != centerGateSession) {
            centerGateSession.getOnlinePlayerSequencer().set(onlinePlayerNum);
        }
    }

    public Session getGateSession(long worldGuid) {
        final CenterGateSession centerGateSession = guid2InfoMap.get(worldGuid);
        return null == centerGateSession ? null : centerGateSession.getSession();
    }

    public void onSessionDisconnect(Session session) {

    }
}
