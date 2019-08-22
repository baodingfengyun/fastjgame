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
import com.wjybxx.fastjgame.annotation.RpcMethod;
import com.wjybxx.fastjgame.annotation.RpcService;
import com.wjybxx.fastjgame.misc.CenterInWarzoneInfo;
import com.wjybxx.fastjgame.misc.PlatformType;
import com.wjybxx.fastjgame.misc.ServiceTable;
import com.wjybxx.fastjgame.net.Session;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/**
 * Center在Warzone中的控制器
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/17 15:43
 * github - https://github.com/hl845740757
 */
@RpcService(serviceId = ServiceTable.CENTER_IN_WARZONE_INFO_MRG)
public class CenterInWarzoneInfoMrg {

    private static final Logger logger= LoggerFactory.getLogger(CenterInWarzoneInfoMrg.class);
    /**
     * guid -> info
     */
    private final Long2ObjectMap<CenterInWarzoneInfo> guid2InfoMap=new Long2ObjectOpenHashMap<>();
    /**
     * plat -> serverId -> info
     */
    private final Map<PlatformType,Int2ObjectMap<CenterInWarzoneInfo>> platInfoMap=new EnumMap<>(PlatformType.class);

    @Inject
    public CenterInWarzoneInfoMrg() {

    }

    private void addInfo(CenterInWarzoneInfo centerInWarzoneInfo){
        guid2InfoMap.put(centerInWarzoneInfo.getGameWorldGuid(),centerInWarzoneInfo);

        Int2ObjectMap<CenterInWarzoneInfo> serverId2InfoMap = getServerId2InfoMap(centerInWarzoneInfo.getPlatformType());
        serverId2InfoMap.put(centerInWarzoneInfo.getServerId(),centerInWarzoneInfo);

        logger.info("server {}-{} register success.",centerInWarzoneInfo.getPlatformType(),centerInWarzoneInfo.getServerId());
    }

    private Int2ObjectMap<CenterInWarzoneInfo> getServerId2InfoMap(PlatformType platformType) {
        return platInfoMap.computeIfAbsent(platformType, k -> new Int2ObjectOpenHashMap<>());
    }

    private void removeInfo(CenterInWarzoneInfo centerInWarzoneInfo){
        guid2InfoMap.remove(centerInWarzoneInfo.getGameWorldGuid());
        getServerId2InfoMap(centerInWarzoneInfo.getPlatformType()).remove(centerInWarzoneInfo.getServerId());

        logger.info("server {}-{} disconnect.",centerInWarzoneInfo.getPlatformType(),centerInWarzoneInfo.getServerId());
    }

    /**
     * 中心服请求注册到战区服
     * @param session 关联的会话
     * @param platfomNumber 中心服的平台
     * @param serverId 中心服的服ID
     * @return 返回一个结果告知已完成
     */
    @RpcMethod(methodId = 1)
    public boolean connectWarzone(Session session, int platfomNumber, int serverId) {
        PlatformType platformType = PlatformType.forNumber(platfomNumber);
        assert !guid2InfoMap.containsKey(session.remoteGuid());
        assert !platInfoMap.containsKey(platformType) || !platInfoMap.get(platformType).containsKey(serverId);

        CenterInWarzoneInfo centerInWarzoneInfo = new CenterInWarzoneInfo(session.remoteGuid(), platformType, serverId, session);
        addInfo(centerInWarzoneInfo);
        return true;
    }

    public void onCenterServerDisconnect(Session session){
        CenterInWarzoneInfo centerInWarzoneInfo = guid2InfoMap.get(session.remoteGuid());
        if (null==centerInWarzoneInfo){
            return;
        }
        removeInfo(centerInWarzoneInfo);
    }

    /**
     * 获取center服信息
     * @param platformType center服所属的平台
     * @param serverId 服务器id
     * @return centerInfo
     */
    @Nullable
    public CenterInWarzoneInfo getCenterInfo(PlatformType platformType, int serverId){
        return getServerId2InfoMap(platformType).get(serverId);
    }
}
