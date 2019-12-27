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
import com.wjybxx.fastjgame.node.LoginNodeData;
import com.wjybxx.fastjgame.utils.JsonUtils;
import com.wjybxx.fastjgame.utils.ZKPathUtils;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;

/**
 * 登录服World
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/17 20:11
 * github - https://github.com/hl845740757
 */
public class LoginWorld extends AbstractWorld {

    private final LoginDiscoverMgr loginDiscoverMgr;
    private final LoginWorldInfoMgr loginWorldInfoMgr;
    private final LoginCenterSessionMgr loginCenterSessionMgr;
    private final LoginMongoDBMgr mongoDBMgr;

    @Inject
    public LoginWorld(WorldWrapper worldWrapper, LoginDiscoverMgr loginDiscoverMgr,
                      LoginCenterSessionMgr loginCenterSessionMgr, LoginMongoDBMgr mongoDBMgr) {
        super(worldWrapper);
        this.loginDiscoverMgr = loginDiscoverMgr;
        this.loginWorldInfoMgr = (LoginWorldInfoMgr) worldWrapper.getWorldInfoMgr();
        this.loginCenterSessionMgr = loginCenterSessionMgr;
        this.mongoDBMgr = mongoDBMgr;
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
        loginDiscoverMgr.start();
        bindAndregisterToZK();
    }

    private void bindAndregisterToZK() throws Exception {
        HostAndPort innerHttpAddress = gameAcceptorMgr.bindInnerHttpPort();
        HostAndPort outerHttpAddress = gameAcceptorMgr.bindOuterHttpPort(loginWorldInfoMgr.getPort());

        String nodeName = ZKPathUtils.buildLoginNodeName(loginWorldInfoMgr.getWorldGuid());

        LoginNodeData loginNodeData = new LoginNodeData(innerHttpAddress.toString(),
                outerHttpAddress.toString());

        final String path = ZKPaths.makePath(ZKPathUtils.onlineLoginRootPath(), nodeName);
        final byte[] initData = JsonUtils.toJsonBytes(loginNodeData);
        curatorMgr.createNode(path, CreateMode.EPHEMERAL, initData);
    }

    @Override
    protected void tickHook() {

    }

    @Override
    protected void shutdownHook() {
        loginDiscoverMgr.shutdown();
        mongoDBMgr.close();
    }
}
