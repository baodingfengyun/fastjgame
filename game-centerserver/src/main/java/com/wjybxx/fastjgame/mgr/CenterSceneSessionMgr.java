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

package com.wjybxx.fastjgame.mgr;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.config.SceneConfig;
import com.wjybxx.fastjgame.misc.CenterSceneSession;
import com.wjybxx.fastjgame.misc.LeastPlayerWorldChooser;
import com.wjybxx.fastjgame.misc.SceneWorldChooser;
import com.wjybxx.fastjgame.net.common.SessionLifecycleAware;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.node.SceneNodeData;
import com.wjybxx.fastjgame.node.SceneNodeName;
import com.wjybxx.fastjgame.rpcservice.ISceneCenterSessionMgrRpcProxy;
import com.wjybxx.fastjgame.rpcservice.ISceneRegionMgrRpcProxy;
import com.wjybxx.fastjgame.scene.SceneRegion;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;


/**
 * SceneServer在CenterServer中的连接等控制器。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/15 23:11
 * github - https://github.com/hl845740757
 */
public class CenterSceneSessionMgr {

    private static final Logger logger = LoggerFactory.getLogger(CenterSceneSessionMgr.class);

    // 不可以是static的，否则会导致线程安全问题
    private final List<CenterSceneSession> availableSceneProcessListCache = new ArrayList<>(8);

    private final CenterWorldInfoMgr worldInfoMgr;
    private final TemplateMgr templateMgr;
    /**
     * 负载均衡策略，当玩家请求进入某个场景的时候，由chooser负责选择。
     */
    private final SceneWorldChooser sceneWorldChooser = new LeastPlayerWorldChooser();
    /**
     * sceneGuid -> sceneInfo
     */
    private final Long2ObjectMap<CenterSceneSession> guid2InfoMap = new Long2ObjectOpenHashMap<>();

    private final GameAcceptorMgr gameAcceptorMgr;

    @Inject
    public CenterSceneSessionMgr(CenterWorldInfoMgr worldInfoMgr, TemplateMgr templateMgr, GameAcceptorMgr gameAcceptorMgr) {
        this.worldInfoMgr = worldInfoMgr;
        this.templateMgr = templateMgr;
        this.gameAcceptorMgr = gameAcceptorMgr;
    }

    private void addSceneInfo(CenterSceneSession centerSceneSession) {
        guid2InfoMap.put(centerSceneSession.getWorldGuid(), centerSceneSession);
    }

    private void removeSceneInfo(CenterSceneSession centerSceneSession) {
        guid2InfoMap.remove(centerSceneSession.getWorldGuid());
    }

    @Nullable
    public CenterSceneSession getSceneInfo(long worldGuid) {
        return guid2InfoMap.get(worldGuid);
    }

    public ObjectCollection<CenterSceneSession> getAllSceneInfo() {
        return guid2InfoMap.values();
    }

    @Nullable
    public Session getSceneSession(long worldGuid) {
        final CenterSceneSession sceneSession = guid2InfoMap.get(worldGuid);
        return null == sceneSession ? null : sceneSession.getSession();
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
     * 当与scene断开连接(异步tcp会话断掉，或zk节点消失)
     */
    private void onSceneDisconnect(final long worldGuid) {
        CenterSceneSession centerSceneSession = guid2InfoMap.get(worldGuid);
        // 可能是一个无效的会话
        if (null == centerSceneSession) {
            return;
        }

        // 关闭会话
        if (null != centerSceneSession.getSession()) {
            centerSceneSession.getSession().close();
        }

        // 断开连接日志
        logger.info("scene {} disconnect", centerSceneSession.getWorldGuid());

        // 真正删除信息
        removeSceneInfo(centerSceneSession);

        // 将在该场景服务器的玩家下线
        offlinePlayer(centerSceneSession);


        // TODO 宕机恢复
    }

    private void offlinePlayer(CenterSceneSession centerSceneSession) {
        // TODO 需要把本服在这些进程的玩家下线处理
    }

    /**
     * 场景服session生命周期通知
     */
    private class SceneLifecycleAware implements SessionLifecycleAware {

        private SceneLifecycleAware() {
        }

        @Override
        public void onSessionConnected(Session session) {
            ISceneCenterSessionMgrRpcProxy.register(worldInfoMgr.getServerId())
                    .onSuccess(result -> onRegisterSceneResult(session, result))
                    .onFailure(rpcResponse -> session.close())
                    .call(session);
        }

        @Override
        public void onSessionDisconnected(Session session) {
            onSceneDisconnect(session.remoteGuid());
        }
    }

    /**
     * 收到场景服务器的响应信息
     *
     * @param session               scene会话信息
     * @param configuredRegionsList 配置的区域
     */
    private void onRegisterSceneResult(Session session, List<SceneRegion> configuredRegionsList) {
        if (configuredRegionsList.isEmpty()) {
            session.close();
            return;
        }
        // 连接成功日志
        logger.info("connect scene {} success", session.remoteGuid());

        final CenterSceneSession centerSceneSession = new CenterSceneSession(session);
        guid2InfoMap.put(session.remoteGuid(), centerSceneSession);
        addSceneInfo(centerSceneSession);

        final Set<SceneRegion> configuredRegions = centerSceneSession.getConfiguredRegions();
        final Set<SceneRegion> activeRegions = centerSceneSession.getActiveRegions();
        for (SceneRegion sceneRegion : configuredRegionsList) {
            configuredRegions.add(sceneRegion);
            // 非互斥的区域已经启动了
            if (!sceneRegion.isMutex()) {
                activeRegions.add(sceneRegion);
            }
        }

        // TODO 检查该场景可以启动哪些互斥场景
        // TODO 这里现在是测试的
        // 这里使用同步方法调用，会大大简化逻辑
        try {
            ISceneRegionMgrRpcProxy.startMutexRegion(Collections.singletonList(SceneRegion.LOCAL_PKC.getNumber()))
                    .syncCall(session);
            activeRegions.add(SceneRegion.LOCAL_PKC);
        } catch (ExecutionException e) {
            // 遇见这个需要好好处理(适当增加超时时间)，尽量不能失败
            logger.error("active region failed, code={}", e.getCause());
        }
    }

    /**
     * 选择一个场景进程
     *
     * @param sceneId 目标场景
     * @return 如果返回-1，表示无法登录（没有可用的进程），
     */
    public long chooseSceneProcess(int sceneId) {
        SceneConfig sceneConfig = templateMgr.sceneConfigInfo.get(sceneId);
        SceneRegion sceneRegion = sceneConfig.sceneRegion;

        List<CenterSceneSession> availableSceneProcessList = availableSceneProcessListCache;
        for (CenterSceneSession centerSceneSession : guid2InfoMap.values()) {
            if (centerSceneSession.getActiveRegions().contains(sceneRegion)) {
                availableSceneProcessList.add(centerSceneSession);
            }
        }
        if (availableSceneProcessList.size() == 0) {
            return -1;
        }
        try {
            if (availableSceneProcessList.size() == 1) {
                return availableSceneProcessList.get(0).getWorldGuid();
            }
            CenterSceneSession choose = sceneWorldChooser.choose(availableSceneProcessList);
            return choose.getWorldGuid();
        } finally {
            availableSceneProcessList.clear();
        }
    }

}
