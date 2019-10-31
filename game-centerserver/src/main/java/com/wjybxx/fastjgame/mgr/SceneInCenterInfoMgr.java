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
import com.wjybxx.fastjgame.core.SceneRegion;
import com.wjybxx.fastjgame.core.onlinenode.SceneNodeData;
import com.wjybxx.fastjgame.core.onlinenode.SceneNodeName;
import com.wjybxx.fastjgame.misc.LeastPlayerWorldChooser;
import com.wjybxx.fastjgame.misc.SceneInCenterInfo;
import com.wjybxx.fastjgame.misc.SceneWorldChooser;
import com.wjybxx.fastjgame.net.common.RpcResponse;
import com.wjybxx.fastjgame.net.common.SessionLifecycleAware;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.rpcservice.ICenterInSceneInfoMgrRpcProxy;
import com.wjybxx.fastjgame.rpcservice.ISceneRegionMgrRpcProxy;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


/**
 * SceneServer在CenterServer中的连接等控制器。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/15 23:11
 * github - https://github.com/hl845740757
 */
public class SceneInCenterInfoMgr {

    private static final Logger logger = LoggerFactory.getLogger(SceneInCenterInfoMgr.class);

    // 不可以是static的，否则会导致线程安全问题
    private final List<SceneInCenterInfo> availableSceneProcessListCache = new ArrayList<>(8);

    private final CenterWorldInfoMgr worldInfoMgr;
    private final TemplateMgr templateMgr;
    /**
     * 负载均衡策略，当玩家请求进入某个场景的时候，由chooser负责选择。
     */
    private final SceneWorldChooser sceneWorldChooser = new LeastPlayerWorldChooser();
    /**
     * scene信息集合（我的私有场景和跨服场景）
     * sceneGuid->sceneInfo
     */
    private final Long2ObjectMap<SceneInCenterInfo> guid2InfoMap = new Long2ObjectOpenHashMap<>();
    /**
     * channelId->sceneInfo
     */
    private final Int2ObjectMap<SceneInCenterInfo> channelId2InfoMap = new Int2ObjectOpenHashMap<>();

    private final GameAcceptorMgr gameAcceptorMgr;

    @Inject
    public SceneInCenterInfoMgr(CenterWorldInfoMgr worldInfoMgr, TemplateMgr templateMgr, GameAcceptorMgr gameAcceptorMgr) {
        this.worldInfoMgr = worldInfoMgr;
        this.templateMgr = templateMgr;
        this.gameAcceptorMgr = gameAcceptorMgr;
    }

    private void addSceneInfo(SceneInCenterInfo sceneInCenterInfo) {
        guid2InfoMap.put(sceneInCenterInfo.getWorldGuid(), sceneInCenterInfo);
        channelId2InfoMap.put(sceneInCenterInfo.getChanelId(), sceneInCenterInfo);

        logger.info("add scene {}-{}", sceneInCenterInfo.getServerId(), sceneInCenterInfo.getChanelId());
    }

    private void removeSceneInfo(SceneInCenterInfo sceneInCenterInfo) {
        guid2InfoMap.remove(sceneInCenterInfo.getWorldGuid());
        channelId2InfoMap.remove(sceneInCenterInfo.getChanelId());

        logger.info("remove scene {}-{}", sceneInCenterInfo.getServerId(), sceneInCenterInfo.getChanelId());
    }

    public SceneInCenterInfo getSceneInfo(long worldGuid) {
        return guid2InfoMap.get(worldGuid);
    }

    public SceneInCenterInfo getSceneInfo(int channelId) {
        return channelId2InfoMap.get(channelId);
    }

    public ObjectCollection<SceneInCenterInfo> getAllSceneInfo() {
        return guid2InfoMap.values();
    }

    /**
     * 当在zk上发现单服scene节点
     *
     * @param nodeName 本服scene节点名字信息
     * @param nodeData 本服scene节点其它信息
     */
    public void onDiscoverSceneNode(SceneNodeName nodeName, SceneNodeData nodeData) {
        // 建立tcp连接
        gameAcceptorMgr.connect(nodeData.getWorldGuid(),
                nodeData.getInnerTcpAddress(),
                nodeData.getLocalAddress(),
                nodeData.getMacAddress(),
                new SingleSceneAware());

        // 保存信息
        SceneInCenterInfo sceneInCenterInfo = new SceneInCenterInfo(nodeName, nodeData.getWorldGuid());

        addSceneInfo(sceneInCenterInfo);
    }

    /**
     * 当本服scene节点移除
     *
     * @param sceneNodeName 单服节点名字
     */
    public void onSceneNodeRemoved(SceneNodeName sceneNodeName) {
        onSceneDisconnect(sceneNodeName.getChannelId());
    }

    /**
     * 当与scene断开连接(异步tcp会话断掉，或zk节点消失)
     */
    private void onSceneDisconnect(final int channelId) {
        SceneInCenterInfo sceneInCenterInfo = channelId2InfoMap.get(channelId);
        // 可能是一个无效的会话
        if (null == sceneInCenterInfo) {
            return;
        }
        // 关闭会话
        if (null != sceneInCenterInfo.getSession()) {
            sceneInCenterInfo.getSession().close();
        }
        // 真正删除信息
        removeSceneInfo(sceneInCenterInfo);

        // 将在该场景服务器的玩家下线
        offlinePlayer(sceneInCenterInfo);

        // TODO 宕机恢复？
    }

    private void offlinePlayer(SceneInCenterInfo sceneInCenterInfo) {
        // TODO 需要把本服在这些进程的玩家下线处理
    }

    /**
     * 本服scene会话信息
     */
    private class SingleSceneAware implements SessionLifecycleAware {

        @Override
        public void onSessionConnected(Session session) {
            getSceneInfo(session.remoteGuid()).setSession(session);
            ICenterInSceneInfoMgrRpcProxy.connectScene(worldInfoMgr.getPlatformType().getNumber(), worldInfoMgr.getServerId())
                    .onSuccess(result -> onConnectSceneSuccess(session, result))
                    .call(session);
        }

        @Override
        public void onSessionDisconnected(Session session) {
            final SceneInCenterInfo sceneInCenterInfo = guid2InfoMap.get(session.remoteGuid());
            if (null != sceneInCenterInfo) {
                onSceneDisconnect(sceneInCenterInfo.getChanelId());
            }
        }
    }

    /**
     * 收到场景服务器的响应信息
     *
     * @param session               scene会话信息
     * @param configuredRegionsList 配置的区域
     */
    private void onConnectSceneSuccess(Session session, List<Integer> configuredRegionsList) {
        assert guid2InfoMap.containsKey(session.remoteGuid());
        SceneInCenterInfo sceneInCenterInfo = guid2InfoMap.get(session.remoteGuid());

        Set<SceneRegion> configuredRegions = sceneInCenterInfo.getConfiguredRegions();
        Set<SceneRegion> activeRegions = sceneInCenterInfo.getActiveRegions();
        for (int regionId : configuredRegionsList) {
            SceneRegion sceneRegion = SceneRegion.forNumber(regionId);
            configuredRegions.add(sceneRegion);
            // 非互斥的区域已经启动了
            if (!sceneRegion.isMutex()) {
                activeRegions.add(sceneRegion);
            }
        }

        if (sceneInCenterInfo.getPlatformType() != worldInfoMgr.getPlatformType()
                || sceneInCenterInfo.getServerId() != worldInfoMgr.getServerId()) {
            return;
        }

        // TODO 检查该场景可以启动哪些互斥场景
        // TODO 如果目标World和当前World在一个EventLoop还可能死锁？
        // 会话id是相同的(使用同步方法调用，会大大简化逻辑)

        // TODO 这里现在是测试的
        final RpcResponse rpcResponse = ISceneRegionMgrRpcProxy.startMutexRegion(Collections.singletonList(SceneRegion.LOCAL_PKC.getNumber()))
                .sync(session);
        if (rpcResponse.isSuccess()) {
            activeRegions.add(SceneRegion.LOCAL_PKC);
        } else {
            // 遇见这个需要好好处理(适当增加超时时间)，尽量不能失败
            logger.error("active region failed, code={}", rpcResponse.getResultCode());
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

        List<SceneInCenterInfo> availableSceneProcessList = availableSceneProcessListCache;
        for (SceneInCenterInfo sceneInCenterInfo : guid2InfoMap.values()) {
            if (sceneInCenterInfo.getActiveRegions().contains(sceneRegion)) {
                availableSceneProcessList.add(sceneInCenterInfo);
            }
        }
        if (availableSceneProcessList.size() == 0) {
            return -1;
        }
        try {
            if (availableSceneProcessList.size() == 1) {
                return availableSceneProcessList.get(0).getWorldGuid();
            }
            SceneInCenterInfo choose = sceneWorldChooser.choose(availableSceneProcessList);
            return choose.getWorldGuid();
        } finally {
            availableSceneProcessList.clear();
        }
    }

}
