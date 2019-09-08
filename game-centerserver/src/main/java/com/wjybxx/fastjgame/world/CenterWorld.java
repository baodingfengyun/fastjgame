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
import com.wjybxx.fastjgame.mrg.*;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.JsonUtils;
import com.wjybxx.fastjgame.utils.ZKPathUtils;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


/**
 * 中心服World
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/15 22:43
 * github - https://github.com/hl845740757
 */
public class CenterWorld extends AbstractWorld {

    private final CenterDiscoverMrg centerDiscoverMrg;
    private final SceneInCenterInfoMrg sceneInCenterInfoMrg;
    private final CenterWorldInfoMrg centerWorldInfoMrg;
    private final WarzoneInCenterInfoMrg warzoneInCenterInfoMrg;
    private final CenterSendMrg sendMrg;

    @Inject
    public CenterWorld(WorldWrapper worldWrapper, CenterDiscoverMrg centerDiscoverMrg,
                       SceneInCenterInfoMrg sceneInCenterInfoMrg, WarzoneInCenterInfoMrg warzoneInCenterInfoMrg, CenterSendMrg sendMrg) {
        super(worldWrapper);
        this.centerDiscoverMrg = centerDiscoverMrg;
        this.sceneInCenterInfoMrg = sceneInCenterInfoMrg;
        centerWorldInfoMrg = (CenterWorldInfoMrg) worldWrapper.getWorldInfoMrg();
        this.warzoneInCenterInfoMrg = warzoneInCenterInfoMrg;
        this.sendMrg = sendMrg;
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
        centerDiscoverMrg.start();
    }

    private void bindAndRegisterToZK() throws Exception {
        // 它主动发起连接，不监听tcp，只监听http即可
        HostAndPort httpHostAndPort = innerAcceptorMrg.bindInnerHttpPort();

        // 注册到zk
        String parentPath= ZKPathUtils.onlineParentPath(centerWorldInfoMrg.getWarzoneId());
        String nodeName= ZKPathUtils.buildCenterNodeName(centerWorldInfoMrg.getPlatformType(), centerWorldInfoMrg.getServerId());

        CenterNodeData centerNodeData =new CenterNodeData(httpHostAndPort.toString(),
                centerWorldInfoMrg.getWorldGuid());

        final String path = ZKPaths.makePath(parentPath, nodeName);
        curatorMrg.waitForNodeDelete(path);

        final byte[] initData = JsonUtils.toJsonBytes(centerNodeData);
        ConcurrentUtils.awaitRemoteWithSleepingRetry(
                ()-> curatorMrg.createNodeIfAbsent(path,CreateMode.EPHEMERAL,initData),
                3, TimeUnit.SECONDS);
    }

    @Override
    protected void tickHook() {

    }

    @Override
    protected void shutdownHook() throws IOException {
        centerDiscoverMrg.shutdown();
    }
}
