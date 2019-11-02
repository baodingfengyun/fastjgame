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
import com.wjybxx.fastjgame.core.onlinenode.GateNodeData;
import com.wjybxx.fastjgame.mgr.GateDiscoverMgr;
import com.wjybxx.fastjgame.mgr.WorldWrapper;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.net.common.SessionLifecycleAware;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.utils.JsonUtils;
import com.wjybxx.fastjgame.utils.ZKPathUtils;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;

/**
 * 网关服world
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/28
 * github - https://github.com/hl845740757
 */
public class GateWorld extends AbstractWorld {

    private final GateWorldInfoMgr gateWorldInfoMgr;
    private final GateDiscoverMgr discoverMgr;

    @Inject
    public GateWorld(WorldWrapper worldWrapper, GateWorldInfoMgr gateWorldInfoMgr, GateDiscoverMgr discoverMgr) {
        super(worldWrapper);
        this.gateWorldInfoMgr = gateWorldInfoMgr;
        this.discoverMgr = discoverMgr;
    }

    @Override
    protected void registerRpcService() {

    }

    @Override
    protected void registerHttpRequestHandlers() {

    }

    @Override
    protected void startHook() throws Exception {
        discoverMgr.start();
        bindAndregisterToZK();
    }

    private void bindAndregisterToZK() throws Exception {
        // 绑定内网Http通信
        final HostAndPort innerHttpAddress = gameAcceptorMgr.bindInnerHttpPort();
        // TODO 绑定外网端口
        final HostAndPort outerTcpPort = gameAcceptorMgr.bindInnerTcpPort(new PlayerLifeAware());
        final HostAndPort outerWsPort = gameAcceptorMgr.bindInnerTcpPort(new PlayerLifeAware());


        final String nodeName = ZKPathUtils.buildGateNodeName(gateWorldInfoMgr.getServerId(),
                gateWorldInfoMgr.getWorldGuid());

        final GateNodeData nodeData = new GateNodeData(innerHttpAddress.toString(),
                outerTcpPort.toString(),
                outerWsPort.toString());

        final String path = ZKPaths.makePath(ZKPathUtils.onlineWarzonePath(gateWorldInfoMgr.getWarzoneId()), nodeName);
        final byte[] initData = JsonUtils.toJsonBytes(nodeData);
        curatorMgr.createNode(path, CreateMode.EPHEMERAL, initData);
    }

    @Override
    protected void tickHook() {

    }

    @Override
    protected void shutdownHook() throws Exception {
        discoverMgr.shutdown();
    }

    private class PlayerLifeAware implements SessionLifecycleAware {

        @Override
        public void onSessionConnected(Session session) {

        }

        @Override
        public void onSessionDisconnected(Session session) {

        }
    }
}
