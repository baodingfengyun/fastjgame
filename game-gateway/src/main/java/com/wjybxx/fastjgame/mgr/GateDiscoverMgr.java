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
import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.node.CenterNodeData;
import com.wjybxx.fastjgame.node.CenterNodeName;
import com.wjybxx.fastjgame.node.SceneNodeData;
import com.wjybxx.fastjgame.node.SceneNodeName;
import com.wjybxx.fastjgame.misc.RoleType;
import com.wjybxx.fastjgame.utils.JsonUtils;
import com.wjybxx.fastjgame.utils.ZKPathUtils;
import com.wjybxx.fastjgame.world.GateWorldInfoMgr;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheSelector;

/**
 * 网关服节点发现管理器
 * <p>
 * 网关服需要发现<b>CenterServer</b>和<b>SceneServer</b>，并建立长链接。
 * <p>
 * 这里其实有个选择，是通过zookeeper发现scene，还是通过中心服发现scene？
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/28
 * github - https://github.com/hl845740757
 */
public class GateDiscoverMgr {

    private final CuratorMgr curatorMgr;
    private final GameEventLoopMgr gameEventLoopMgr;
    private final GateWorldInfoMgr worldInfoMgr;
    private final GateCenterSessionMgr gateCenterSessionMgr;
    private final GateSceneSessionMgr gateSceneSessionMgr;

    private TreeCache treeCache;

    @Inject
    public GateDiscoverMgr(CuratorMgr curatorMgr, GameEventLoopMgr gameEventLoopMgr,
                           GateWorldInfoMgr worldInfoMgr, GateCenterSessionMgr gateCenterSessionMgr,
                           GateSceneSessionMgr gateSceneSessionMgr) {
        this.curatorMgr = curatorMgr;
        this.gameEventLoopMgr = gameEventLoopMgr;
        this.worldInfoMgr = worldInfoMgr;
        this.gateCenterSessionMgr = gateCenterSessionMgr;
        this.gateSceneSessionMgr = gateSceneSessionMgr;
    }

    public void start() throws Exception {
        treeCache = TreeCache.newBuilder(curatorMgr.getClient(), ZKPathUtils.onlineRootPath())
                .setCreateParentNodes(true)
                .setMaxDepth(2)
                .setExecutor(new DefaultThreadFactory("GATE_DISCOVERY_THREAD"))
                .setSelector(new GATECacheSelector())
                .build();
        treeCache.getListenable().addListener((client, event) -> onEvent(event), gameEventLoopMgr.getEventLoop());
        treeCache.start();
    }

    public void shutdown() {
        if (treeCache != null) {
            treeCache.close();
        }
    }

    private void onEvent(TreeCacheEvent event) {
        // 只处理增加和删除事件
        if (event.getType() != TreeCacheEvent.Type.NODE_ADDED
                && event.getType() != TreeCacheEvent.Type.NODE_REMOVED) {
            return;
        }
        ChildData childData = event.getData();
        // 根节点
        if (childData.getPath().equals(ZKPathUtils.onlineRootPath())) {
            return;
        }
        // 战区节点
        String nodeName = ZKPathUtils.findNodeName(childData.getPath());
        if (nodeName.startsWith("warzone")) {
            return;
        }
        // 战区子节点
        RoleType serverType = ZKPathUtils.parseServerType(nodeName);
        assert serverType == RoleType.CENTER || serverType == RoleType.SCENE;

        if (serverType == RoleType.CENTER) {
            onCenterEvent(event.getType(), childData);
        } else {
            onSceneNodeEvent(event.getType(), childData);
        }
    }

    private void onCenterEvent(TreeCacheEvent.Type type, ChildData childData) {
        final CenterNodeName nodeName = ZKPathUtils.parseCenterNodeName(childData.getPath());
        if (!nodeName.getServerId().equals(worldInfoMgr.getServerId())) {
            // 不是我关心的服务器
            return;
        }

        final CenterNodeData nodeData = JsonUtils.parseJsonBytes(childData.getData(), CenterNodeData.class);
        if (type == TreeCacheEvent.Type.NODE_ADDED) {
            gateCenterSessionMgr.onDiscoverCenterNode(nodeName, nodeData);
        } else {
            gateCenterSessionMgr.onCenterNodeRemoved(nodeName, nodeData);
        }
    }

    private void onSceneNodeEvent(TreeCacheEvent.Type type, ChildData childData) {
        final SceneNodeName nodeName = ZKPathUtils.parseSceneNodeName(childData.getPath());
        final SceneNodeData nodeData = JsonUtils.parseJsonBytes(childData.getData(), SceneNodeData.class);
        if (type == TreeCacheEvent.Type.NODE_ADDED) {
            gateSceneSessionMgr.onDiscoverSceneNode(nodeName, nodeData);
        } else {
            gateSceneSessionMgr.onSceneNodeRemoved(nodeName, nodeData);
        }
    }

    /**
     * 选择需要的cache
     */
    private static class GATECacheSelector implements TreeCacheSelector {

        /**
         * 只取回warzone下的节点
         */
        @Override
        public boolean traverseChildren(String fullPath) {
            if (fullPath.equals(ZKPathUtils.onlineRootPath())) {
                return true;
            } else {
                return ZKPathUtils.findNodeName(fullPath).startsWith("warzone");
            }
        }

        /**
         * 只取回CenterServer节点
         */
        @Override
        public boolean acceptChild(String fullPath) {
            String nodeName = ZKPathUtils.findNodeName(fullPath);
            if (nodeName.startsWith("warzone")) {
                // 战区节点(容器)
                return true;
            } else {
                // 叶子节点
                final String parentNodeName = ZKPathUtils.findParentNodeName(fullPath);
                if (!parentNodeName.startsWith("warzone")) {
                    return false;
                }
                final RoleType roleType = ZKPathUtils.parseServerType(nodeName);
                // 连接scene和中心服
                return roleType == RoleType.CENTER || roleType == RoleType.SCENE;
            }
        }
    }
}
