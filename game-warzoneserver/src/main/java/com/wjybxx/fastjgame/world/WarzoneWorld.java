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
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.mrg.*;
import com.wjybxx.fastjgame.net.Session;
import com.wjybxx.fastjgame.net.SessionLifecycleAware;
import com.wjybxx.fastjgame.rpcservice.ICenterInWarzoneInfoMrgRpcRegister;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.JsonUtils;
import com.wjybxx.fastjgame.utils.SystemUtils;
import com.wjybxx.fastjgame.utils.ZKPathUtils;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;

import java.util.concurrent.TimeUnit;


/**
 * WarzoneServer
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/15 18:31
 * github - https://github.com/hl845740757
 */
public class WarzoneWorld extends AbstractWorld {

    private final WarzoneWorldInfoMrg warzoneWorldInfoMrg;
    private final CenterInWarzoneInfoMrg centerInWarzoneInfoMrg;
    private final WarzoneSendMrg sendMrg;

    @Inject
    public WarzoneWorld(WorldWrapper worldWrapper, WarzoneWorldInfoMrg warzoneWorldInfoMrg,
                        CenterInWarzoneInfoMrg centerInWarzoneInfoMrg, WarzoneSendMrg sendMrg) {
        super(worldWrapper);
        this.warzoneWorldInfoMrg = warzoneWorldInfoMrg;
        this.centerInWarzoneInfoMrg = centerInWarzoneInfoMrg;
        this.sendMrg = sendMrg;
    }

    @Override
    protected void registerMessageHandlers() {

    }

    @Override
    protected void registerRpcService() {
        ICenterInWarzoneInfoMrgRpcRegister.register(protocolDispatcherMrg, centerInWarzoneInfoMrg);
    }

    @Override
    protected void registerHttpRequestHandlers() {

    }

    @Override
    protected void startHook() throws Exception {
        bindAndRegisterToZK();
    }

    private void bindAndRegisterToZK() throws Exception {
        // 绑定3个内部交互的端口
        HostAndPort tcpHostAndPort = innerAcceptorMrg.bindInnerTcpPort(new CenterLifeAware());
        HostAndPort httpHostAndPort = innerAcceptorMrg.bindInnerHttpPort();
        HostAndPort localAddress = innerAcceptorMrg.bindLocalTcpPort(new CenterLifeAware());

        // 注册到zk
        String parentPath= ZKPathUtils.onlineParentPath(warzoneWorldInfoMrg.getWarzoneId());
        String nodeName= ZKPathUtils.buildWarzoneNodeName(warzoneWorldInfoMrg.getWarzoneId());

        WarzoneNodeData centerNodeData =new WarzoneNodeData(tcpHostAndPort.toString(), httpHostAndPort.toString(), localAddress.toString(), SystemUtils.getMAC(),
                warzoneWorldInfoMrg.getWorldGuid());

        final String path = ZKPaths.makePath(parentPath, nodeName);
        curatorMrg.waitForNodeDelete(path);

        final byte[] initData = JsonUtils.toJsonBytes(centerNodeData);
        ConcurrentUtils.awaitRemoteWithSleepingRetry(
                () -> curatorMrg.createNodeIfAbsent(path, CreateMode.EPHEMERAL,initData),
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
            centerInWarzoneInfoMrg.onCenterServerDisconnect(session);
        }
    }
}
