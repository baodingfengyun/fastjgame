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
import com.wjybxx.fastjgame.misc.RpcCall;
import com.wjybxx.fastjgame.net.common.RpcResponseChannel;
import com.wjybxx.fastjgame.net.common.RpcResultCode;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.rpcservice.ICenterRouterMgr;

/**
 * 中心服路由管理器。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/4
 * github - https://github.com/hl845740757
 */
public class CenterRouterMgr implements ICenterRouterMgr {

    private final CenterWarzoneSessionMgr warzoneSessionMgr;
    private final CenterSceneSessionMgr sceneSessionMgr;

    @Inject
    public CenterRouterMgr(CenterWarzoneSessionMgr warzoneSessionMgr, CenterSceneSessionMgr sceneSessionMgr) {
        this.warzoneSessionMgr = warzoneSessionMgr;
        this.sceneSessionMgr = sceneSessionMgr;
    }

    @Override
    public void sendToWarzone(RpcCall rpcCall, RpcResponseChannel<?> rpcResponseChannel) {
        final Session warzoneSession = warzoneSessionMgr.getWarzoneSession();
        if (null == warzoneSession) {
            rpcResponseChannel.writeFailure(RpcResultCode.ROUTER_SESSION_NULL);
            return;
        }
        // 也可以使用DefaultRpcBuilder
        warzoneSession.call(rpcCall, rpcResponseChannel::write);
    }

    @Override
    public void sendToScene(long sceneWorldGuid, RpcCall rpcCall, RpcResponseChannel<?> rpcResponseChannel) {
        final Session sceneSession = sceneSessionMgr.getSceneSession(sceneWorldGuid);
        if (null == sceneSession) {
            rpcResponseChannel.writeFailure(RpcResultCode.ROUTER_SESSION_NULL);
            return;
        }
        sceneSession.call(rpcCall, rpcResponseChannel::write);
    }

    @Override
    public void sendToPlayerScene(long playerGuid, RpcCall rpcCall, RpcResponseChannel<?> rpcResponseChannel) {
        // TODO 根据玩家id查询所在scene服务器
    }
}
