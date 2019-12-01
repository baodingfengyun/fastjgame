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
import com.wjybxx.fastjgame.core.onlinenode.WarzoneNodeData;
import com.wjybxx.fastjgame.mgr.WarzoneCenterSessionMgr;
import com.wjybxx.fastjgame.mgr.WarzoneWorldInfoMgr;
import com.wjybxx.fastjgame.mgr.WorldWrapper;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.net.common.SessionLifecycleAware;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.rpcservice.IWarzoneCenterSessionMgrRpcRegister;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.JsonUtils;
import com.wjybxx.fastjgame.utils.ZKPathUtils;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;

import java.util.concurrent.TimeUnit;


/**
 * WarzoneServer
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/15 18:31
 * github - https://github.com/hl845740757
 */
public class WarzoneWorld extends AbstractWorld {

    private final WarzoneWorldInfoMgr warzoneWorldInfoMgr;
    private final WarzoneCenterSessionMgr warzoneCenterSessionMgr;

    @Inject
    public WarzoneWorld(WorldWrapper worldWrapper, WarzoneWorldInfoMgr warzoneWorldInfoMgr,
                        WarzoneCenterSessionMgr warzoneCenterSessionMgr) {
        super(worldWrapper);
        this.warzoneWorldInfoMgr = warzoneWorldInfoMgr;
        this.warzoneCenterSessionMgr = warzoneCenterSessionMgr;
    }

    @Override
    protected void registerRpcService() {
        IWarzoneCenterSessionMgrRpcRegister.register(protocolDispatcherMgr, warzoneCenterSessionMgr);
    }

    @Override
    protected void registerHttpRequestHandlers() {

    }

    @Override
    protected void startHook() throws Exception {
        bindAndRegisterToZK();
    }

    private void bindAndRegisterToZK() throws Exception {
        final CenterLifeAware centerLifeAware = new CenterLifeAware();
        // 绑定JVM内部端口
        gameAcceptorMgr.bindLocalPort(centerLifeAware);
        // 绑定socket端口
        HostAndPort tcpHostAndPort = gameAcceptorMgr.bindInnerTcpPort(centerLifeAware);
        HostAndPort httpHostAndPort = gameAcceptorMgr.bindInnerHttpPort();

        // 注册到zk
        String parentPath = ZKPathUtils.onlineWarzonePath(warzoneWorldInfoMgr.getWarzoneId());
        String nodeName = ZKPathUtils.buildWarzoneNodeName(warzoneWorldInfoMgr.getWarzoneId());

        WarzoneNodeData centerNodeData = new WarzoneNodeData(httpHostAndPort.toString(),
                tcpHostAndPort.toString(),
                warzoneWorldInfoMgr.getWorldGuid());

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
    protected void shutdownHook() {

    }

    private class CenterLifeAware implements SessionLifecycleAware {
        @Override
        public void onSessionConnected(Session session) {

        }

        @Override
        public void onSessionDisconnected(Session session) {
            warzoneCenterSessionMgr.onCenterServerDisconnect(session.remoteGuid());
        }
    }
}
