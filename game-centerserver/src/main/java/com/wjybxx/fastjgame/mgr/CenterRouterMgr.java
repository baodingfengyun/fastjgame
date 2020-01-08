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
import com.wjybxx.fastjgame.misc.DefaultRpcBuilder;
import com.wjybxx.fastjgame.net.common.RpcCall;
import com.wjybxx.fastjgame.net.common.RpcErrorCode;
import com.wjybxx.fastjgame.net.common.RpcResponseChannel;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.rpcservice.ICenterRouterMgr;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
    public <V> void routeToWarzone(RpcCall<V> rpcCall, RpcResponseChannel<V> rpcResponseChannel) {
        routeImp(warzoneSessionMgr.getWarzoneSession(), rpcCall, rpcResponseChannel);
    }

    @Override
    public <V> void routeToScene(long sceneWorldGuid, RpcCall<V> rpcCall, RpcResponseChannel<V> rpcResponseChannel) {
        routeImp(sceneSessionMgr.getSceneSession(sceneWorldGuid), rpcCall, rpcResponseChannel);
    }

    @Override
    public <V> void routeToPlayerScene(long playerGuid, RpcCall<V> rpcCall, RpcResponseChannel<V> rpcResponseChannel) {
        // TODO 根据玩家id查询所在scene服务器
    }

    private static <V> void routeImp(@Nullable Session targetSession, @Nonnull RpcCall<V> rpcCall, @Nonnull RpcResponseChannel<V> rpcResponseChannel) {
        if (targetSession == null) {
            rpcResponseChannel.writeFailure(RpcErrorCode.SERVER_ROUTER_SESSION_NULL, "can't find target session");
            return;
        }

        if (rpcResponseChannel.isVoid()) {
            targetSession.send(rpcCall);
            return;
        }

        new DefaultRpcBuilder<>(rpcCall)
                .onFailure(rpcResponseChannel::writeFailure)
                .onSuccess(rpcResponseChannel::writeSuccess)
                .call(targetSession);
    }
}
