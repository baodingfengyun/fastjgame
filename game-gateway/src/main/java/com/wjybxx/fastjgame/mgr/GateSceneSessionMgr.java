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
import com.wjybxx.fastjgame.core.onlinenode.SceneNodeData;
import com.wjybxx.fastjgame.core.onlinenode.SceneNodeName;
import com.wjybxx.fastjgame.misc.GateSceneSession;
import com.wjybxx.fastjgame.net.common.SessionLifecycleAware;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.rpcservice.ISceneGateSessionMgrRpcProxy;
import com.wjybxx.fastjgame.world.GateWorldInfoMgr;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 场景服在网关服的信息
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/1
 * github - https://github.com/hl845740757
 */
public class GateSceneSessionMgr {

    private static final Logger logger = LoggerFactory.getLogger(GateSceneSessionMgr.class);

    private final GateWorldInfoMgr worldInfoMgr;
    private final GameAcceptorMgr gameAcceptorMgr;
    /**
     * guid -> session
     */
    private final Long2ObjectMap<GateSceneSession> guid2SessionMap = new Long2ObjectOpenHashMap<>();

    @Inject
    public GateSceneSessionMgr(GateWorldInfoMgr worldInfoMgr, GameAcceptorMgr gameAcceptorMgr) {
        this.worldInfoMgr = worldInfoMgr;
        this.gameAcceptorMgr = gameAcceptorMgr;
    }

    public Session getSceneSession(long worldGuid) {
        final GateSceneSession gateSceneSession = guid2SessionMap.get(worldGuid);
        return null == gateSceneSession ? null : gateSceneSession.getSession();
    }

    /**
     * 当在zk上发现单服scene节点
     *
     * @param nodeName scene节点名字信息
     * @param nodeData scene节点其它信息
     */
    public void onDiscoverSceneNode(SceneNodeName nodeName, SceneNodeData nodeData) {
        // 建立tcp连接
        gameAcceptorMgr.connect(nodeName.getWorldGuid(),
                nodeData.getInnerTcpAddress(),
                new SceneLifecycleAware());
    }

    /**
     * 当scene节点移除
     *
     * @param sceneNodeName 节点名字
     * @param sceneNodeData 节点数据
     */
    public void onSceneNodeRemoved(SceneNodeName sceneNodeName, SceneNodeData sceneNodeData) {
        onSceneDisconnect(sceneNodeName.getWorldGuid());
    }

    /**
     * 当场景服断开连接
     *
     * @param worldGuid 场景服务器id
     */
    public void onSceneDisconnect(long worldGuid) {
        final GateSceneSession gateSceneSession = guid2SessionMap.get(worldGuid);
        if (null == gateSceneSession) {
            return;
        }
        gateSceneSession.getSession().close();
        logger.info("scene {} disconnect", worldGuid);
        // TODO 下线该场景服上的玩家
    }

    private class SceneLifecycleAware implements SessionLifecycleAware {

        @Override
        public void onSessionConnected(Session session) {
            ISceneGateSessionMgrRpcProxy.register(worldInfoMgr.getServerId())
                    .onSuccess(result -> onRegisterSceneResult(session, result))
                    .onFailure(rpcResponse -> session.close())
                    .call(session);
        }

        @Override
        public void onSessionDisconnected(Session session) {
            onSceneDisconnect(session.remoteGuid());
        }
    }

    private void onRegisterSceneResult(Session session, boolean result) {
        if (!result) {
            session.close();
            return;
        }

        guid2SessionMap.put(session.remoteGuid(), new GateSceneSession(session));
        logger.info("connect scene {} success", session.remoteGuid());
    }
}
