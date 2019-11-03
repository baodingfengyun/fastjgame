
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
import com.wjybxx.fastjgame.core.SceneRegion;
import com.wjybxx.fastjgame.misc.CenterInSceneInfo;
import com.wjybxx.fastjgame.misc.CenterServerId;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.rpcservice.ICenterInSceneInfoMgr;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;

/**
 * CenterServer在SceneServer中的连接管理等。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/15 22:08
 * github - https://github.com/hl845740757
 */
public class CenterInSceneInfoMgr implements ICenterInSceneInfoMgr {

    private static final Logger logger = LoggerFactory.getLogger(CenterInSceneInfoMgr.class);
    private final SceneWorldInfoMgr sceneWorldInfoMgr;

    /**
     * wolrdguid到信息的映射
     */
    private final Long2ObjectMap<CenterInSceneInfo> guid2InfoMap = new Long2ObjectOpenHashMap<>();
    /**
     * serverId到信息的映射
     */
    private final Map<CenterServerId, CenterInSceneInfo> serverId2InfoMap = new HashMap<>();

    @Inject
    public CenterInSceneInfoMgr(SceneWorldInfoMgr sceneWorldInfoMgr) {
        this.sceneWorldInfoMgr = sceneWorldInfoMgr;
    }

    private void addInfo(CenterInSceneInfo centerInSceneInfo) {
        guid2InfoMap.put(centerInSceneInfo.getCenterWorldGuid(), centerInSceneInfo);
        serverId2InfoMap.put(centerInSceneInfo.getServerId(), centerInSceneInfo);

        logger.info("center server {} register success", centerInSceneInfo.getServerId());
    }

    private void removeInfo(CenterInSceneInfo centerInSceneInfo) {
        guid2InfoMap.remove(centerInSceneInfo.getCenterWorldGuid());
        serverId2InfoMap.remove(centerInSceneInfo.getServerId());

        logger.info("center server {} disconnect", centerInSceneInfo.getServerId());
    }

    /**
     * 检测到center服进程会话断开
     *
     * @param centerWorldGuid center服务器worldGuid
     */
    public void onCenterDisconnect(long centerWorldGuid) {
        CenterInSceneInfo centerInSceneInfo = guid2InfoMap.get(centerWorldGuid);
        if (null == centerInSceneInfo) {
            return;
        }
        removeInfo(centerInSceneInfo);

        // 将该服的玩家下线
        offlineSpecialCenterPlayer(centerInSceneInfo.getServerId());

        // TODO 关闭检测
    }

    /**
     * 将特定平台特定服的玩家下线
     *
     * @param serverId 区服
     */
    private void offlineSpecialCenterPlayer(CenterServerId serverId) {

    }

    @Override
    public List<SceneRegion> register(Session session, CenterServerId serverId) {
        if (serverId2InfoMap.containsKey(serverId)) {
            return Collections.emptyList();
        }

        CenterInSceneInfo centerInSceneInfo = new CenterInSceneInfo(session, serverId);
        addInfo(centerInSceneInfo);

        // 返回配置的所有区域即可，非互斥区域已启动
        // 必须返回拷贝
        return new ArrayList<>(sceneWorldInfoMgr.getConfiguredRegions());
    }

    /**
     * 获取中心服的session
     *
     * @param serverId 区服
     * @return session
     */
    @Nullable
    public Session getCenterSession(CenterServerId serverId) {
        final CenterInSceneInfo centerInSceneInfo = serverId2InfoMap.get(serverId);
        return null == centerInSceneInfo ? null : centerInSceneInfo.getSession();
    }

    /**
     * 获取中心服的session
     *
     * @param worldGuid 中心服的worldGuid
     * @return session
     */
    @Nullable
    public Session getCenterSession(long worldGuid) {
        final CenterInSceneInfo centerInSceneInfo = guid2InfoMap.get(worldGuid);
        return null == centerInSceneInfo ? null : centerInSceneInfo.getSession();
    }

}
