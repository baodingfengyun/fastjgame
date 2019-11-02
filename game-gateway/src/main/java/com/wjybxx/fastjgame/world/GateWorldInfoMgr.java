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

package com.wjybxx.fastjgame.world;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.configwrapper.ConfigWrapper;
import com.wjybxx.fastjgame.configwrapper.MapConfigWrapper;
import com.wjybxx.fastjgame.mgr.CuratorMgr;
import com.wjybxx.fastjgame.mgr.GuidMgr;
import com.wjybxx.fastjgame.mgr.WorldInfoMgr;
import com.wjybxx.fastjgame.misc.PlatformType;
import com.wjybxx.fastjgame.misc.RoleType;
import com.wjybxx.fastjgame.utils.GameUtils;
import com.wjybxx.fastjgame.utils.ZKPathUtils;

/**
 * 网关服信息
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/1
 * github - https://github.com/hl845740757
 */
public class GateWorldInfoMgr extends WorldInfoMgr {

    private final CuratorMgr curatorMgr;

    private int warzoneId;
    private PlatformType platformType;
    private int serverId;

    @Inject
    public GateWorldInfoMgr(GuidMgr guidMgr, CuratorMgr curatorMgr) {
        super(guidMgr);
        this.curatorMgr = curatorMgr;
    }

    @Override
    protected void initImp(ConfigWrapper startArgs) throws Exception {
        // 战区id由zookeeper配置指定
        this.platformType = PlatformType.valueOf(startArgs.getAsString("platform"));
        this.serverId = startArgs.getAsInt("serverId");

        // 查询zookeeper，获取该平台该服对应的战区id
        final String actualServerConfigPath = ZKPathUtils.actualServerConfigPath(platformType, this.serverId);
        ConfigWrapper serverConfig = new MapConfigWrapper(GameUtils.newJsonMap(curatorMgr.getData(actualServerConfigPath)));
        warzoneId = serverConfig.getAsInt("warzoneId");
    }

    @Override
    public RoleType getWorldType() {
        return RoleType.GATE;
    }

    public int getWarzoneId() {
        return warzoneId;
    }

    public PlatformType getPlatformType() {
        return platformType;
    }

    public int getServerId() {
        return serverId;
    }
}
