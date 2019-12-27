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
import com.wjybxx.fastjgame.mgr.*;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.net.common.SessionLifecycleAware;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.socket.SocketSessionConfig;
import com.wjybxx.fastjgame.node.GateNodeData;
import com.wjybxx.fastjgame.utils.GameUtils;
import com.wjybxx.fastjgame.utils.JsonUtils;
import com.wjybxx.fastjgame.utils.NetUtils;
import com.wjybxx.fastjgame.utils.ZKPathUtils;
import org.apache.zookeeper.CreateMode;

import java.net.BindException;

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
    private final GatePlayerSessionMgr playerSessionMgr;
    private final NetContextMgr netContextMgr;
    private final ProtocolCodecMgr protocolCodecMgr;

    @Inject
    public GateWorld(WorldWrapper worldWrapper, GateWorldInfoMgr gateWorldInfoMgr, GateDiscoverMgr discoverMgr,
                     GatePlayerSessionMgr playerSessionMgr, NetContextMgr netContextMgr,
                     ProtocolCodecMgr protocolCodecMgr) {
        super(worldWrapper);
        this.gateWorldInfoMgr = gateWorldInfoMgr;
        this.discoverMgr = discoverMgr;
        this.playerSessionMgr = playerSessionMgr;
        this.netContextMgr = netContextMgr;
        this.protocolCodecMgr = protocolCodecMgr;
    }

    @Override
    protected void registerRpcService() {

    }

    @Override
    protected void registerHttpRequestHandlers() {

    }

    @Override
    protected void registerEventHandlers() {

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
        final HostAndPort outerTcpPort = bindOuterTcpPort();

        final String nodeName = ZKPathUtils.buildGateNodeName(gateWorldInfoMgr.getServerId(),
                gateWorldInfoMgr.getWorldGuid());

        final GateNodeData nodeData = new GateNodeData(innerHttpAddress.toString(),
                outerTcpPort.toString(),
                outerTcpPort.toString());

        final String path = ZKPathUtils.makePath(ZKPathUtils.onlineWarzonePath(gateWorldInfoMgr.getWarzoneId()), nodeName);
        final byte[] initData = JsonUtils.toJsonBytes(nodeData);
        curatorMgr.createNode(path, CreateMode.EPHEMERAL, initData);
    }

    private HostAndPort bindOuterTcpPort() throws BindException {
        return netContextMgr.getNetContext().bindTcpRange(NetUtils.getOuterIp(),
                GameUtils.OUTER_TCP_PORT_RANGE, newPlayerAcceptorConfig()).getHostAndPort();
    }

    private SocketSessionConfig newPlayerAcceptorConfig() {
        return SocketSessionConfig.newBuilder()
                .setAutoReconnect(true)
                .setMaxPendingMessages(50)
                .setMaxCacheMessages(500)
                .setCodec(protocolCodecMgr.getInnerProtocolCodec())
                .setLifecycleAware(new PlayerLifeAware())
                .setDispatcher(playerSessionMgr)
                .build();
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
            playerSessionMgr.onSessionConnected(session);
        }

        @Override
        public void onSessionDisconnected(Session session) {
            playerSessionMgr.onSessionDisconnected(session);
        }
    }
}
