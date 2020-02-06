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

package com.wjybxx.fastjgame.mgr;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.misc.RoleType;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.rpcservice.ICenterRouterMgrRpcProxy;
import com.wjybxx.fastjgame.rpcservice.ISceneTestMgrRpcProxy;
import com.wjybxx.fastjgame.rpcservice.IWarzoneTestMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 战区服测试管理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/4
 * github - https://github.com/hl845740757
 */
public class WarzoneTestMgr implements IWarzoneTestMgr {

    private static final Logger logger = LoggerFactory.getLogger(WarzoneTestMgr.class);

    private final WorldInfoMgr worldInfoMgr;
    private final WorldTimerMgr worldTimerMgr;

    @Inject
    public WarzoneTestMgr(WorldInfoMgr worldInfoMgr, WorldTimerMgr worldTimerMgr) {
        this.worldInfoMgr = worldInfoMgr;
        this.worldTimerMgr = worldTimerMgr;
    }

    @Override
    public String hello(Session centerSession, RoleType caller, long worldGuid) {
        if (caller == RoleType.SCENE) {
            worldTimerMgr.nextTick(handle -> callScene(centerSession, worldGuid));
        }
        return "Return by warzone, caller: " + caller + ", worldGuid:" + worldGuid;
    }

    private void callScene(Session centerSession, long sceneWorldGuid) {
        ISceneTestMgrRpcProxy.hello(RoleType.WARZONE, worldInfoMgr.getWorldGuid())
                .router(rpcCall -> ICenterRouterMgrRpcProxy.routeToScene(sceneWorldGuid, rpcCall))
                .call(centerSession)
                .onSuccess(result -> logger.info("Rcv scene response: {}", result))
                .onFailure(failureResult -> logger.info("Failure response: {}", failureResult));
    }
}
