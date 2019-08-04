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
import com.wjybxx.fastjgame.core.CenterInLoginInfo;
import com.wjybxx.fastjgame.core.onlinenode.CenterNodeData;
import com.wjybxx.fastjgame.core.onlinenode.CenterNodeName;
import com.wjybxx.fastjgame.misc.PlatformType;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;

/**
 * CenterServer在LoginServer端的信息
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/17 21:23
 * github - https://github.com/hl845740757
 */
public class CenterInLoginInfoMrg {

    private static final Logger logger= LoggerFactory.getLogger(CenterInLoginInfoMrg.class);
    /**
     * 平台 -> 服id-> 服信息的映射，暂时未涉及跨平台
     */
    private final Map<PlatformType,Int2ObjectMap<CenterInLoginInfo>> platInfoMap = new EnumMap<>(PlatformType.class);

    @Inject
    public CenterInLoginInfoMrg() {

    }

    private Int2ObjectMap<CenterInLoginInfo> getServerId2InfoMap(PlatformType platformType){
        return platInfoMap.computeIfAbsent(platformType, type -> new Int2ObjectOpenHashMap<>());
    }

    public void onDiscoverCenterServer(CenterNodeName nodeName, CenterNodeData nodeData){
        Int2ObjectMap<CenterInLoginInfo> serverId2InfoMap = getServerId2InfoMap(nodeName.getPlatformType());
        assert !serverId2InfoMap.containsKey(nodeName.getServerId());

        CenterInLoginInfo centerInLoginInfo=new CenterInLoginInfo(nodeData.getWorldGuid(),nodeName.getPlatformType(),
                nodeName.getServerId(),nodeData.getInnerHttpAddress());
        serverId2InfoMap.put(nodeName.getServerId(),centerInLoginInfo);
        logger.info("{}-{} nodeData added.",nodeName.getPlatformType(),nodeName.getServerId());
    }

    public void onCenterServerNodeRemove(CenterNodeName nodeName, CenterNodeData nodeData){
        Int2ObjectMap<CenterInLoginInfo> serverId2InfoMap = getServerId2InfoMap(nodeName.getPlatformType());
        assert serverId2InfoMap.containsKey(nodeName.getServerId());

        serverId2InfoMap.remove(nodeName.getServerId());
        logger.info("{}-{} nodeData removed.",nodeName.getPlatformType(),nodeName.getServerId());
    }

}
