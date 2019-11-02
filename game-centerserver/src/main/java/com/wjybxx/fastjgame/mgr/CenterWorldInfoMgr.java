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
import com.wjybxx.fastjgame.configwrapper.ConfigWrapper;
import com.wjybxx.fastjgame.configwrapper.MapConfigWrapper;
import com.wjybxx.fastjgame.misc.CenterServerId;
import com.wjybxx.fastjgame.misc.PlatformType;
import com.wjybxx.fastjgame.misc.RoleType;
import com.wjybxx.fastjgame.utils.GameUtils;
import com.wjybxx.fastjgame.utils.ZKPathUtils;

/**
 * 中心服信息管理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/15 23:30
 * github - https://github.com/hl845740757
 */
public class CenterWorldInfoMgr extends WorldInfoMgr {

    private final CuratorMgr curatorMgr;
    /**
     * 真实服信息配置
     */
    private ConfigWrapper actualServerConfig;
    /**
     * 逻辑服信息配置
     */
    private ConfigWrapper originalServerConfig;
    /**
     * 从属的战区
     */
    private int warzoneId;
    /**
     * 服务器唯一标识
     */
    private CenterServerId serverId;

    @Inject
    public CenterWorldInfoMgr(GuidMgr guidMgr, CuratorMgr curatorMgr) {
        super(guidMgr);
        this.curatorMgr = curatorMgr;
    }

    @Override
    protected void initImp(ConfigWrapper startArgs) throws Exception {
        final PlatformType platformType = PlatformType.valueOf(startArgs.getAsString("platform"));
        final int serverId = startArgs.getAsInt("serverId");
        this.serverId = new CenterServerId(platformType, serverId);

        // 合服前配置
        String actualServerPath = ZKPathUtils.actualServerConfigPath(this.serverId);
        this.actualServerConfig = new MapConfigWrapper(GameUtils.newJsonMap(curatorMgr.getData(actualServerPath)));

        // 合服后配置
        String originalServerPath = ZKPathUtils.originalServerConfigPath(this.serverId);
        this.originalServerConfig = new MapConfigWrapper(GameUtils.newJsonMap(curatorMgr.getData(originalServerPath)));

        // 战区通过zookeeper节点获取
        warzoneId = actualServerConfig.getAsInt("warzoneId");
    }

    @Override
    public RoleType getWorldType() {
        return RoleType.CENTER;
    }

    public int getWarzoneId() {
        return warzoneId;
    }

    public CenterServerId getServerId() {
        return serverId;
    }

    public ConfigWrapper getActualServerConfig() {
        return actualServerConfig;
    }

    public ConfigWrapper getOriginalServerConfig() {
        return originalServerConfig;
    }
}
