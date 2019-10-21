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

package com.wjybxx.fastjgame.world;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.core.onlinenode.CenterNodeData;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.mgr.*;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.JsonUtils;
import com.wjybxx.fastjgame.utils.ZKPathUtils;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


/**
 * 中心服World
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/15 22:43
 * github - https://github.com/hl845740757
 */
public class CenterWorld extends AbstractWorld {

    private final CenterDiscoverMgr centerDiscoverMgr;
    private final SceneInCenterInfoMgr sceneInCenterInfoMgr;
    private final CenterWorldInfoMgr centerWorldInfoMgr;
    private final WarzoneInCenterInfoMgr warzoneInCenterInfoMgr;
    private final CenterSendMgr sendMgr;

    @Inject
    public CenterWorld(WorldWrapper worldWrapper, CenterDiscoverMgr centerDiscoverMgr,
                       SceneInCenterInfoMgr sceneInCenterInfoMgr, WarzoneInCenterInfoMgr warzoneInCenterInfoMgr, CenterSendMgr sendMgr) {
        super(worldWrapper);
        this.centerDiscoverMgr = centerDiscoverMgr;
        this.sceneInCenterInfoMgr = sceneInCenterInfoMgr;
        centerWorldInfoMgr = (CenterWorldInfoMgr) worldWrapper.getWorldInfoMgr();
        this.warzoneInCenterInfoMgr = warzoneInCenterInfoMgr;
        this.sendMgr = sendMgr;
    }

    @Override
    protected void registerRpcService() {

    }

    @Override
    protected void registerHttpRequestHandlers() {

    }

    @Override
    protected void startHook() throws Exception {
        // 绑定端口并注册到zookeeper
        bindAndRegisterToZK();

        // 注册成功再启动服务发现
        centerDiscoverMgr.start();
    }

    private void bindAndRegisterToZK() throws Exception {
        // 它主动发起连接，不监听tcp，只监听http即可
        HostAndPort httpHostAndPort = gameAcceptorMgr.bindInnerHttpPort();

        // 注册到zk
        String parentPath = ZKPathUtils.onlineParentPath(centerWorldInfoMgr.getWarzoneId());
        String nodeName = ZKPathUtils.buildCenterNodeName(centerWorldInfoMgr.getPlatformType(), centerWorldInfoMgr.getServerId());

        CenterNodeData centerNodeData = new CenterNodeData(httpHostAndPort.toString(),
                centerWorldInfoMgr.getWorldGuid());

        final String path = ZKPaths.makePath(parentPath, nodeName);
        curatorMgr.waitForNodeDelete(path);

        final byte[] initData = JsonUtils.toJsonBytes(centerNodeData);
        ConcurrentUtils.awaitRemoteWithSleepingRetry(
                () -> curatorMgr.createNodeIfAbsent(path, CreateMode.EPHEMERAL, initData),
                3, TimeUnit.SECONDS);
    }

    @Override
    protected void tickHook() {

    }

    @Override
    protected void shutdownHook() throws IOException {
        centerDiscoverMgr.shutdown();
    }
}
