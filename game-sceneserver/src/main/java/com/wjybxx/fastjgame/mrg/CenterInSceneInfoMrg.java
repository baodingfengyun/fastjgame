
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

package com.wjybxx.fastjgame.mrg;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.core.SceneRegion;
import com.wjybxx.fastjgame.core.SceneWorldType;
import com.wjybxx.fastjgame.misc.CenterInSceneInfo;
import com.wjybxx.fastjgame.misc.PlatformType;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.rpcservice.ICenterInSceneInfoMrg;
import com.wjybxx.fastjgame.serializebale.ConnectCrossSceneResult;
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
public class CenterInSceneInfoMrg implements ICenterInSceneInfoMrg {

    private static final Logger logger = LoggerFactory.getLogger(CenterInSceneInfoMrg.class);
    private final SceneWorldInfoMrg sceneWorldInfoMrg;
    private final SceneRegionMrg sceneRegionMrg;

    /**
     * wolrdguid到信息的映射
     */
    private final Long2ObjectMap<CenterInSceneInfo> guid2InfoMap = new Long2ObjectOpenHashMap<>();
    /**
     * 平台 -> serverId -> 信息
     */
    private final Map<PlatformType, Int2ObjectMap<CenterInSceneInfo>> platInfoMap = new EnumMap<>(PlatformType.class);

    @Inject
    public CenterInSceneInfoMrg(SceneWorldInfoMrg sceneWorldInfoMrg, SceneRegionMrg sceneRegionMrg) {
        this.sceneWorldInfoMrg = sceneWorldInfoMrg;
        this.sceneRegionMrg = sceneRegionMrg;
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

        // 跨服场景，收到某个center服宕机，无所谓(跨服场景会链接很多个center服)
        if (sceneWorldInfoMrg.getSceneWorldType() == SceneWorldType.CROSS) {
            // 将该服的玩家下线
            offlineSpecialCenterPlayer(centerInSceneInfo.getPlatformType(), centerInSceneInfo.getServerId());

            // TODO 如果所有服都断开了，且一段时间内没有服务器接入，需要关闭
            return;
        }

        // 单服场景(讲道理单服场景只会连接自己的服，因此这里必须成立)，自己的center服宕机，需要通知所有玩家下线，然后退出
        assert centerInSceneInfo.getPlatformType() == sceneWorldInfoMrg.getPlatformType();
        assert centerInSceneInfo.getServerId() == sceneWorldInfoMrg.getServerId();
        offlineAllOnlinePlayer();
        // 自己的中心服宕机，场景服需要自动关闭
        sceneWorld.shutdown();
    }

    /**
     * 将特定平台特定服的玩家下线
     *
     * @param platformType 平台
     * @param serverId     区服
     */
    private void offlineSpecialCenterPlayer(PlatformType platformType, int serverId) {

    }

    /**
     * 踢掉当前场景所有在线玩家
     */
    private void offlineAllOnlinePlayer() {
        // TODO 踢掉所有玩家，shutdown
    }

    @Override
    public List<Integer> connectSingleScene(Session session, int platformNumber, int serverId) {
        PlatformType platformType = PlatformType.forNumber(platformNumber);
        assert !guid2InfoMap.containsKey(session.remoteGuid());
        assert !platInfoMap.containsKey(platformType) || !platInfoMap.get(platformType).containsKey(serverId);

        CenterInSceneInfo centerInSceneInfo = new CenterInSceneInfo(session.remoteGuid(), platformType, serverId, session);
        addInfo(centerInSceneInfo);

        // 返回配置的所有区域即可，非互斥区域已启动
        IntList configuredRegions = new IntArrayList(sceneWorldInfoMrg.getConfiguredRegions().size());
        for (SceneRegion sceneRegion : sceneWorldInfoMrg.getConfiguredRegions()) {
            configuredRegions.add(sceneRegion.getNumber());
        }
        return configuredRegions;
    }

    @Override
    public ConnectCrossSceneResult connectCrossScene(Session session, int platformNumber, int serverId) {
        PlatformType platformType = PlatformType.forNumber(platformNumber);
        assert !guid2InfoMap.containsKey(session.remoteGuid());
        assert !platInfoMap.containsKey(platformType) || !platInfoMap.get(platformType).containsKey(serverId);

        CenterInSceneInfo centerInSceneInfo = new CenterInSceneInfo(session.remoteGuid(), platformType, serverId, session);
        addInfo(centerInSceneInfo);

        // 配置的区域
        IntList configuredRegions = new IntArrayList(sceneWorldInfoMrg.getConfiguredRegions().size());
        for (SceneRegion sceneRegion : sceneWorldInfoMrg.getConfiguredRegions()) {
            configuredRegions.add(sceneRegion.getNumber());
        }

        // 实际激活的区域
        IntList activeRegions = new IntArrayList(sceneRegionMrg.getActiveRegions().size());
        for (SceneRegion sceneRegion : sceneRegionMrg.getActiveRegions()) {
            activeRegions.add(sceneRegion.getNumber());
        }
        return new ConnectCrossSceneResult(configuredRegions, activeRegions);
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
