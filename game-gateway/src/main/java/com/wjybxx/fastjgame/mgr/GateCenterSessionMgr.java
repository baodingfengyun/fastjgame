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
import com.wjybxx.fastjgame.node.CenterNodeData;
import com.wjybxx.fastjgame.node.CenterNodeName;
import com.wjybxx.fastjgame.misc.GateCenterSession;
import com.wjybxx.fastjgame.net.common.SessionLifecycleAware;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.rpcservice.ICenterGateSessionMgrRpcProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * 中心服在网关服的信息管理
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/1
 * github - https://github.com/hl845740757
 */
public class GateCenterSessionMgr {

    private static final Logger logger = LoggerFactory.getLogger(GateCenterSessionMgr.class);

    private final GameAcceptorMgr gameAcceptorMgr;
    private GateCenterSession gateCenterSession;

    @Inject
    public GateCenterSessionMgr(GameAcceptorMgr gameAcceptorMgr) {
        this.gameAcceptorMgr = gameAcceptorMgr;
    }

    @Nullable
    public Session getCenterSession() {
        return null == gateCenterSession ? null : gateCenterSession.getSession();
    }

    /**
     * 当从zookeeper上监测到中心服节点
     *
     * @param nodeName 中心服节点名字
     * @param nodeData 中心服节点数据
     */
    public void onDiscoverCenterNode(CenterNodeName nodeName, CenterNodeData nodeData) {
        if (gateCenterSession != null) {
            logger.error("may loss disconnect event");
            onCenterDisconnect(gateCenterSession.worldGuid());
            return;
        }

        gameAcceptorMgr.connect(nodeData.getWorldGuid(),
                nodeData.getInnerTcpAddress(),
                new CenterLifecycleAware());
    }

    /**
     * 当从zookeeper上发现中心服节点删除
     *
     * @param nodeName 中心服节点名字
     * @param nodeData 中心服节点数据
     */
    public void onCenterNodeRemoved(CenterNodeName nodeName, CenterNodeData nodeData) {
        onCenterDisconnect(nodeData.getWorldGuid());
    }

    private class CenterLifecycleAware implements SessionLifecycleAware {

        @Override
        public void onSessionConnected(Session session) {
            ICenterGateSessionMgrRpcProxy.register()
                    .onSuccess(result -> onRegisterCenterResult(session, result))
                    .onFailure(rpcResponse -> session.close())
                    .call(session);
        }

        public void onSessionDisconnected(Session session) {
            onCenterDisconnect(session.remoteGuid());
        }
    }

    private void onRegisterCenterResult(Session session, boolean result) {
        assert result;
        gateCenterSession = new GateCenterSession(session);
        logger.info("connect center {} success", session.remoteGuid());
    }

    private void onCenterDisconnect(long worldGuid) {
        if (gateCenterSession == null) {
            return;
        }
        if (worldGuid != gateCenterSession.worldGuid()) {
            return;
        }
        gateCenterSession.getSession().close();
        logger.info("center {} disconnect", gateCenterSession.worldGuid());

        // TODO 下线所有玩家
    }
}
