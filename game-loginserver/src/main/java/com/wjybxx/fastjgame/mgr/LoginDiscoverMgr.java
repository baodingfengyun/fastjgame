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
import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.core.onlinenode.CenterNodeData;
import com.wjybxx.fastjgame.core.onlinenode.CenterNodeName;
import com.wjybxx.fastjgame.misc.RoleType;
import com.wjybxx.fastjgame.utils.JsonUtils;
import com.wjybxx.fastjgame.utils.ZKPathUtils;
import org.apache.curator.framework.recipes.cache.*;

/**
 * 登录服，用于发现所有的CenterServer。
 * <p>
 * 调用{@link TreeCache#close()}的时候会关闭executor...
 * {@link PathChildrenCache}还能选是否关闭，{@link TreeCache}不能选择。
 * 不能随便关闭他人的线程池，所以如果要使用{@link TreeCache}必须新建线程池。
 * <p>
 * 使用{@link TreeCache}逻辑处理起来会简单点，而且登录服没有太多的负载要处理，
 * 创建额外的线程池影响很小。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/17 20:29
 * github - https://github.com/hl845740757
 */
public class LoginDiscoverMgr {

    private final CuratorMgr curatorMgr;
    private final CenterInLoginInfoMgr centerInLoginInfoMgr;
    private final GameEventLoopMgr gameEventLoopMgr;

    private TreeCache treeCache;

    @Inject
    public LoginDiscoverMgr(CuratorMgr curatorMgr, CenterInLoginInfoMgr centerInLoginInfoMgr, GameEventLoopMgr gameEventLoopMgr) {
        this.curatorMgr = curatorMgr;
        this.centerInLoginInfoMgr = centerInLoginInfoMgr;
        this.gameEventLoopMgr = gameEventLoopMgr;
    }

    public void start() throws Exception {
        treeCache = TreeCache.newBuilder(curatorMgr.getClient(), ZKPathUtils.onlineRootPath())
                .setCreateParentNodes(true)
                .setMaxDepth(2)
                .setExecutor(new DefaultThreadFactory("LOGIN_DISCOVERY_THREAD"))
                .setSelector(new LoginCacheSelector())
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
        assert serverType == RoleType.CENTER;

        CenterNodeName centerNodeName = ZKPathUtils.parseCenterNodeName(childData.getPath());
        CenterNodeData centerNode = JsonUtils.parseJsonBytes(childData.getData(), CenterNodeData.class);
        if (event.getType() == TreeCacheEvent.Type.NODE_ADDED) {
            centerInLoginInfoMgr.onDiscoverCenterServer(centerNodeName, centerNode);
        } else if (event.getType() == TreeCacheEvent.Type.NODE_REMOVED) {
            centerInLoginInfoMgr.onCenterServerNodeRemove(centerNodeName, centerNode);
        }
    }

    /**
     * 选择需要的cache
     */
    private static class LoginCacheSelector implements TreeCacheSelector {

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
                return parentNodeName.startsWith("warzone") && ZKPathUtils.parseServerType(nodeName) == RoleType.CENTER;
            }
        }
    }
}
