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
import com.wjybxx.fastjgame.misc.RoleType;
import com.wjybxx.fastjgame.scene.SceneRegion;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * 场景服信息管理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/16 10:47
 * github - https://github.com/hl845740757
 */
public class SceneWorldInfoMgr extends WorldInfoMgr {

    /**
     * 所属的战区
     */
    private int warzoneId;
    /**
     * 当前进程配合的所有支持的区域
     */
    private Set<SceneRegion> configuredRegions;

    @Inject
    public SceneWorldInfoMgr(GuidMgr guidMgr) {
        super(guidMgr);
    }

    @Override
    protected void initImp(ConfigWrapper startArgs) throws Exception {
        warzoneId = startArgs.getAsInt("warzoneId");
        // 配置的要启动的区域
        // TODO 这种配置方式不方便配置
        String[] configuredRegionArray = startArgs.getAsStringArray("configuredRegions");
        Set<SceneRegion> modifiableRegionSet = EnumSet.noneOf(SceneRegion.class);
        for (String regionName : configuredRegionArray) {
            final SceneRegion sceneRegion = SceneRegion.valueOf(regionName);
            modifiableRegionSet.add(sceneRegion);
        }
        this.configuredRegions = Collections.unmodifiableSet(modifiableRegionSet);
    }

    @Override
    public RoleType getWorldType() {
        return RoleType.SCENE;
    }

    public int getWarzoneId() {
        return warzoneId;
    }

    public Set<SceneRegion> getConfiguredRegions() {
        return configuredRegions;
    }

}
