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
import com.wjybxx.fastjgame.core.SceneRegion;
import com.wjybxx.fastjgame.misc.PlatformType;
import com.wjybxx.fastjgame.misc.RoleType;
import com.wjybxx.fastjgame.utils.GameUtils;
import com.wjybxx.fastjgame.utils.ZKPathUtils;
import org.apache.zookeeper.CreateMode;

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

    private static final int SINGLE_SCENE_MIN_CHANNEL_ID = 1;
    private static final int CROSS_SCENE_MIN_CHANNEL_ID = 10001;

    /**
     * 所属的战区
     */
    private int warzoneId;

    /**
     * 所属的平台
     * 1. 如果是单服进程，表示它所属的平台
     * 2. 如果是跨服进程，则固定为{@link PlatformType#CROSS}
     */
    private PlatformType platformType;
    /**
     * 1. 如果的单服进程，那么表示它所属的服务器
     * 2. 如果是跨服进程，则固定为0；
     */
    private int serverId;
    /**
     * 频道id，自动生成会比较好
     */
    private int channelId;
    /**
     * 当前进程配合的所有支持的区域
     */
    private Set<SceneRegion> configuredRegions;

    private final CuratorMgr curatorMgr;

    @Inject
    public SceneWorldInfoMgr(GuidMgr guidMgr, CuratorMgr curatorMgr) {
        super(guidMgr);
        this.curatorMgr = curatorMgr;
    }

    @Override
    protected void initImp(ConfigWrapper startArgs) throws Exception {
        warzoneId = startArgs.getAsInt("warzoneId", 0);
        if (warzoneId == 0) {
            // 本服场景配置平台和服id，战区id由zookeeper配置指定
            this.platformType = PlatformType.valueOf(startArgs.getAsString("platform"));
            this.serverId = startArgs.getAsInt("serverId");

            // 查询zookeeper，获取该平台该服对应的战区id
            final String actualServerConfigPath = ZKPathUtils.actualServerConfigPath(platformType, this.serverId);
            ConfigWrapper serverConfig = new MapConfigWrapper(GameUtils.newJsonMap(curatorMgr.getData(actualServerConfigPath)));
            warzoneId = serverConfig.getAsInt("warzoneId");

            final String originPath = ZKPathUtils.singleChannelPath(warzoneId, this.serverId);
            this.initChannelId(originPath, SINGLE_SCENE_MIN_CHANNEL_ID);
        } else {
            // 跨服场景的 战区id由启动参数决定
            this.platformType = PlatformType.CROSS;
            this.serverId = 0;

            final String originPath = ZKPathUtils.crossChannelPath(warzoneId);
            this.initChannelId(originPath, CROSS_SCENE_MIN_CHANNEL_ID);
        }

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

    public PlatformType getPlatformType() {
        return platformType;
    }

    public int getServerId() {
        return serverId;
    }

    public Set<SceneRegion> getConfiguredRegions() {
        return configuredRegions;
    }

    public int getChannelId() {
        return channelId;
    }

    /**
     * 初始化channelId
     *
     * @param originPath     创建的节点原始路径，因为临时节点，实际路径名会不一样
     * @param startChannelId 其实channelId
     * @throws Exception zk errors
     */
    private void initChannelId(String originPath, int startChannelId) throws Exception {
        final String parent = ZKPathUtils.findParentPath(originPath);
        final String lockPath = ZKPathUtils.findAppropriateLockPath(parent);
        // 如果父节点存在，且没有子节点，则先删除，让序号初始化为0,再创建节点
        // 整个是一个先检查后执行的逻辑，因此需要加锁，保证整个操作的原子性
        curatorMgr.actionWhitLock(lockPath, () -> {
            curatorMgr.deleteNodeIfNoChild(parent);
            String realPath = curatorMgr.createNode(originPath, CreateMode.EPHEMERAL_SEQUENTIAL);
            this.channelId = startChannelId + ZKPathUtils.parseSequentialId(realPath);
        });

    }
}
