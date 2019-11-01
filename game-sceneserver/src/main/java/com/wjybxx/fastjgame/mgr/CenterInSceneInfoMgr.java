
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
import com.wjybxx.fastjgame.misc.PlatformType;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.rpcservice.ICenterInSceneInfoMgr;
import com.wjybxx.fastjgame.world.SceneWorld;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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
     * 平台 -> serverId -> 信息
     */
    private final Map<PlatformType, Int2ObjectMap<CenterInSceneInfo>> platInfoMap = new EnumMap<>(PlatformType.class);

    @Inject
    public CenterInSceneInfoMgr(SceneWorldInfoMgr sceneWorldInfoMgr) {
        this.sceneWorldInfoMgr = sceneWorldInfoMgr;
    }

    private void addInfo(CenterInSceneInfo centerInSceneInfo) {
        guid2InfoMap.put(centerInSceneInfo.getCenterWorldGuid(), centerInSceneInfo);
        Int2ObjectMap<CenterInSceneInfo> serverId2InfoMap = platInfoMap.computeIfAbsent(centerInSceneInfo.getPlatformType(),
                platformType -> new Int2ObjectOpenHashMap<>());
        serverId2InfoMap.put(centerInSceneInfo.getServerId(), centerInSceneInfo);

        logger.info("connect center server {}-{}", centerInSceneInfo.getPlatformType(), centerInSceneInfo.getServerId());
    }

    @Nonnull
    private Int2ObjectMap<CenterInSceneInfo> getServerId2InfoMap(PlatformType platformType) {
        return platInfoMap.computeIfAbsent(platformType, k -> new Int2ObjectOpenHashMap<>());
    }

    private void removeInfo(CenterInSceneInfo centerInSceneInfo) {
        guid2InfoMap.remove(centerInSceneInfo.getCenterWorldGuid());
        getServerId2InfoMap(centerInSceneInfo.getPlatformType()).remove(centerInSceneInfo.getServerId());

        logger.info("remove center server {}-{}", centerInSceneInfo.getPlatformType(), centerInSceneInfo.getServerId());
    }

    /**
     * 检测到center服进程会话断开
     *
     * @param centerWorldGuid center服务器worldGuid
     */
    public void onDisconnect(long centerWorldGuid, SceneWorld sceneWorld) {
        CenterInSceneInfo centerInSceneInfo = guid2InfoMap.get(centerWorldGuid);
        if (null == centerInSceneInfo) {
            return;
        }
        removeInfo(centerInSceneInfo);

        // 将该服的玩家下线
        offlineSpecialCenterPlayer(centerInSceneInfo.getPlatformType(), centerInSceneInfo.getServerId());

        // TODO 关闭检测
    }

    /**
     * 将特定平台特定服的玩家下线
     *
     * @param platformType 平台
     * @param serverId     区服
     */
    private void offlineSpecialCenterPlayer(PlatformType platformType, int serverId) {

    }

    @Override
    public List<SceneRegion> connectScene(Session session, PlatformType platformType, int serverId) {
        assert !guid2InfoMap.containsKey(session.remoteGuid());
        assert !platInfoMap.containsKey(platformType) || !platInfoMap.get(platformType).containsKey(serverId);

        CenterInSceneInfo centerInSceneInfo = new CenterInSceneInfo(session, platformType, serverId);
        addInfo(centerInSceneInfo);

        // 返回配置的所有区域即可，非互斥区域已启动
        // 必须返回拷贝
        return new ArrayList<>(sceneWorldInfoMgr.getConfiguredRegions());
    }

    /**
     * 获取中心服的session
     *
     * @param platformType 平台
     * @param serverId     区服
     * @return session
     */
    @Nullable
    public Session getCenterSession(PlatformType platformType, int serverId) {
        Int2ObjectMap<CenterInSceneInfo> serverId2InfoMap = getServerId2InfoMap(platformType);
        CenterInSceneInfo centerInSceneInfo = serverId2InfoMap.get(serverId);
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
        CenterInSceneInfo centerInSceneInfo = guid2InfoMap.get(worldGuid);
        return null == centerInSceneInfo ? null : centerInSceneInfo.getSession();
    }

}
