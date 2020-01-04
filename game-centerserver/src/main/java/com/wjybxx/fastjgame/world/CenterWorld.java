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
import com.wjybxx.fastjgame.mgr.*;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.net.common.SessionLifecycleAware;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.node.CenterNodeData;
import com.wjybxx.fastjgame.rpcservice.ICenterGateSessionMgrRpcRegister;
import com.wjybxx.fastjgame.rpcservice.ICenterRouterMgrRpcRegister;
import com.wjybxx.fastjgame.rpcservice.IPlayerMessageDispatcherMgrRpcRegister;
import com.wjybxx.fastjgame.utils.JsonUtils;
import com.wjybxx.fastjgame.utils.ZKPathUtils;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;

import java.io.IOException;


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
    private final CenterSceneSessionMgr centerSceneSessionMgr;
    private final CenterGateSessionMgr centerGateSessionMgr;
    private final CenterWorldInfoMgr centerWorldInfoMgr;
    private final CenterWarzoneSessionMgr centerWarzoneSessionMgr;
    private final CenterPlayerMessageDispatcherMgr playerMessageDispatcherMgr;
    private final CenterMongoDBMgr mongoDBMgr;
    private final CenterRouterMgr routerMgr;

    @Inject
    public CenterWorld(WorldWrapper worldWrapper, CenterDiscoverMgr centerDiscoverMgr,
                       CenterSceneSessionMgr centerSceneSessionMgr, CenterGateSessionMgr centerGateSessionMgr,
                       CenterWarzoneSessionMgr centerWarzoneSessionMgr, CenterPlayerMessageDispatcherMgr playerMessageDispatcherMgr,
                       CenterMongoDBMgr mongoDBMgr, CenterRouterMgr routerMgr) {
        super(worldWrapper);
        this.centerDiscoverMgr = centerDiscoverMgr;
        this.centerSceneSessionMgr = centerSceneSessionMgr;
        centerWorldInfoMgr = (CenterWorldInfoMgr) worldWrapper.getWorldInfoMgr();
        this.centerGateSessionMgr = centerGateSessionMgr;
        this.centerWarzoneSessionMgr = centerWarzoneSessionMgr;
        this.playerMessageDispatcherMgr = playerMessageDispatcherMgr;
        this.mongoDBMgr = mongoDBMgr;
        this.routerMgr = routerMgr;
    }

    @Override
    protected void registerRpcService() {
        ICenterGateSessionMgrRpcRegister.register(protocolDispatcherMgr, centerGateSessionMgr);
        IPlayerMessageDispatcherMgrRpcRegister.register(protocolDispatcherMgr, playerMessageDispatcherMgr);
        ICenterRouterMgrRpcRegister.register(protocolDispatcherMgr, routerMgr);
    }

    @Override
    protected void registerHttpRequestHandlers() {

    }

    @Override
    protected void registerEventHandlers() {

    }

    @Override
    protected void startHook() throws Exception {
        // 绑定端口并注册到zookeeper
        bindAndRegisterToZK();

        // 注册成功再启动服务发现
        centerDiscoverMgr.start();
    }

    private void bindAndRegisterToZK() throws Exception {
        final GateLifeAware gateLifeAware = new GateLifeAware();
        // 绑定JVM内部端口
        gameAcceptorMgr.bindLocalPort(gateLifeAware);
        // 绑定socket端口
        HostAndPort tcpHostAndPort = gameAcceptorMgr.bindInnerTcpPort(gateLifeAware);
        HostAndPort httpHostAndPort = gameAcceptorMgr.bindInnerHttpPort();

        // 注册到zk
        String parentPath = ZKPathUtils.onlineWarzonePath(centerWorldInfoMgr.getWarzoneId());
        String nodeName = ZKPathUtils.buildCenterNodeName(centerWorldInfoMgr.getServerId());

        final CenterNodeData centerNodeData = new CenterNodeData(httpHostAndPort.toString(),
                tcpHostAndPort.toString(),
                centerWorldInfoMgr.getWorldGuid());

        final String path = ZKPaths.makePath(parentPath, nodeName);
        final byte[] initData = JsonUtils.toJsonBytes(centerNodeData);

        // 创建失败则退出
        curatorMgr.waitForNodeDelete(path);
        curatorMgr.createNode(path, CreateMode.EPHEMERAL, initData);
    }

    @Override
    protected void tickHook() {

    }

    @Override
    protected void shutdownHook() throws IOException {
        centerDiscoverMgr.shutdown();
        mongoDBMgr.close();
    }

    private class GateLifeAware implements SessionLifecycleAware {

        @Override
        public void onSessionConnected(Session session) {

        }

        @Override
        public void onSessionDisconnected(Session session) {

        }
    }
}
