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
import com.wjybxx.fastjgame.rpcservice.ICenterRouterMgrRpcProxy;
import com.wjybxx.fastjgame.rpcservice.ISceneTestMgr;
import com.wjybxx.fastjgame.rpcservice.IWarzoneTestMgrRpcProxy;
import com.wjybxx.fastjgame.timer.TimerHandle;
import com.wjybxx.fastjgame.utils.DebugUtils;
import com.wjybxx.fastjgame.utils.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 场景服测试用的管理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/4
 * github - https://github.com/hl845740757
 */
public class SceneTestMgr implements ISceneTestMgr {

    private static final Logger logger = LoggerFactory.getLogger(SceneTestMgr.class);

    private final WorldInfoMgr worldInfoMgr;
    private final SceneCenterSessionMgr centerSessionMgr;

    @Inject
    public SceneTestMgr(WorldInfoMgr worldInfoMgr, WorldTimerMgr timerMgr, SceneCenterSessionMgr centerSessionMgr) {
        this.worldInfoMgr = worldInfoMgr;
        this.centerSessionMgr = centerSessionMgr;

        if (DebugUtils.isDebugOpen()) {
            timerMgr.newFixedDelay(TimeUtils.SEC, this::callWarzone);
        }
    }

    private void callWarzone(TimerHandle timerHandle) {
        // 获取要调用的战区方法信息
        IWarzoneTestMgrRpcProxy.hello(RoleType.SCENE, worldInfoMgr.getWorldGuid())
                .onSuccess(result -> timerHandle.close())
                .onSuccess(result -> logger.info("Rcv warzone response: {}", result))
                .onFailure(rpcResponse -> logger.info("Failure Response: {}", rpcResponse))
                .router(ICenterRouterMgrRpcProxy::routeToWarzone)
                .call(centerSessionMgr.getFirstCenterSession());
    }

    @Override
    public String hello(RoleType caller, long worldGuid) {
        return "Return by scene, caller: " + caller + ", worldGuid: " + worldGuid;
    }
}
