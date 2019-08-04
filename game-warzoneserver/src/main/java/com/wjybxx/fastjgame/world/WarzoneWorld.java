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
import com.wjybxx.fastjgame.mrg.CenterInWarzoneInfoMrg;
import com.wjybxx.fastjgame.mrg.WarzoneSendMrg;
import com.wjybxx.fastjgame.mrg.WarzoneWorldInfoMrg;
import com.wjybxx.fastjgame.mrg.WorldWrapper;
import com.wjybxx.fastjgame.net.S2CSession;
import com.wjybxx.fastjgame.net.SessionLifecycleAware;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.JsonUtils;
import com.wjybxx.fastjgame.utils.ZKPathUtils;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;

import java.util.concurrent.TimeUnit;

import static com.wjybxx.fastjgame.protobuffer.p_center_warzone.p_center_warzone_hello;

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
        registerMessageHandler(p_center_warzone_hello.class,centerInWarzoneInfoMrg::p_center_warzone_hello_handler);
    }

    @Override
    protected void registerRpcRequestHandlers() {

    }

    @Override
    protected void registerHttpRequestHandlers() {

    }


    @Override
    protected void startHook() throws Exception {
        bindAndRegisterToZK();
    }

    private void bindAndRegisterToZK() throws Exception {
        // 绑定3个内部交互的端口(还有一个其实是local类型，现在还没支持)
        HostAndPort tcpHostAndPort = innerAcceptorMrg.bindInnerTcpPort(true, new CenterLifeAware());
        HostAndPort httpHostAndPort = innerAcceptorMrg.bindInnerHttpPort();

        // 注册到zk
        String parentPath= ZKPathUtils.onlineParentPath(warzoneWorldInfoMrg.getWarzoneId());
        String nodeName= ZKPathUtils.buildWarzoneNodeName(warzoneWorldInfoMrg.getWarzoneId());

        WarzoneNodeData centerNodeData =new WarzoneNodeData(tcpHostAndPort.toString(),
                httpHostAndPort.toString(),
                warzoneWorldInfoMrg.getWorldGuid());

        final String path = ZKPaths.makePath(parentPath, nodeName);
        curatorMrg.waitForNodeDelete(path);

        final byte[] initData = JsonUtils.toJsonBytes(centerNodeData);
        ConcurrentUtils.awaitRemoteWithSleepingRetry(path, resource -> {
            return curatorMrg.createNodeIfAbsent(path, CreateMode.EPHEMERAL,initData);
        },3, TimeUnit.SECONDS);
    }

    @Override
    protected void tickHook() {

    }

    @Override
    protected void shutdownHook() {

    }

    private class CenterLifeAware implements SessionLifecycleAware<S2CSession> {
        @Override
        public void onSessionConnected(S2CSession session) {

        }

        @Override
        public void onSessionDisconnected(S2CSession session) {
            centerInWarzoneInfoMrg.onCenterServerDisconnect(session);
        }
    }
}
