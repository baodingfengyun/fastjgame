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

package com.wjybxx.fastjgame.mrg;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.core.onlinenode.CenterNodeData;
import com.wjybxx.fastjgame.core.onlinenode.CenterNodeName;
import com.wjybxx.fastjgame.net.RoleType;
import com.wjybxx.fastjgame.utils.JsonUtils;
import com.wjybxx.fastjgame.utils.ZKPathUtils;
import com.wjybxx.fastjgame.world.GameEventLoopMrg;
import org.apache.curator.framework.recipes.cache.*;

/**
 * 登录服，用于发现所有的CenterServer。
 * 调用{@link TreeCache#close()}的时候会关闭executor...
 * {@link PathChildrenCache}还能选是否关闭，{@link TreeCache}不能选择。
 * 不能随便关闭他人的线程池，所以如果要使用{@link TreeCache}必须新建线程池。
 *
 * 使用{@link TreeCache}逻辑处理起来会简单点，而且登录服没有太多的负载要处理，
 * 创建额外的线程池影响很小。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/17 20:29
 * github - https://github.com/hl845740757
 */
public class LoginDiscoverMrg {

    private final CuratorMrg curatorMrg;
    private final CenterInLoginInfoMrg centerInLoginInfoMrg;
    private final GameEventLoopMrg gameEventLoopMrg;

    private TreeCache treeCache;

    @Inject
    public LoginDiscoverMrg(CuratorMrg curatorMrg, CenterInLoginInfoMrg centerInLoginInfoMrg, GameEventLoopMrg gameEventLoopMrg) {
        this.curatorMrg = curatorMrg;
        this.centerInLoginInfoMrg = centerInLoginInfoMrg;
        this.gameEventLoopMrg = gameEventLoopMrg;
    }

    public void start() throws Exception {
        treeCache = TreeCache.newBuilder(curatorMrg.getClient(), ZKPathUtils.onlineRootPath())
                .setCreateParentNodes(true)
                .setMaxDepth(2)
                .setExecutor(new DefaultThreadFactory("LOGIN_DISCOVERY_THREAD"))
                .setSelector(new LoginCacheSelector())
                .build();
        treeCache.getListenable().addListener((client, event) -> onEvent(event), gameEventLoopMrg.getEventLoop());
        treeCache.start();
    }

    public void shutdown() {
        if (treeCache != null) {
            treeCache.close();
        }
    }

    private void onEvent(TreeCacheEvent event){
        if (event.getType() != TreeCacheEvent.Type.NODE_ADDED
                && event.getType() != TreeCacheEvent.Type.NODE_REMOVED){
            return;
        }
        ChildData childData = event.getData();
        // 根节点
        if (childData.getPath().equals(ZKPathUtils.onlineRootPath())){
            return;
        }
        // 战区节点
        String nodeName = ZKPathUtils.findNodeName(childData.getPath());
        if (nodeName.startsWith("warzone")){
            return;
        }
        // 战区子节点
        RoleType serverType = ZKPathUtils.parseServerType(nodeName);
        assert serverType == RoleType.CENTER;

        CenterNodeName centerNodeName = ZKPathUtils.parseCenterNodeName(childData.getPath());
        CenterNodeData centerNode= JsonUtils.parseJsonBytes(childData.getData(), CenterNodeData.class);
        if (event.getType() == TreeCacheEvent.Type.NODE_ADDED){
            centerInLoginInfoMrg.onDiscoverCenterServer(centerNodeName,centerNode);
        } else if (event.getType() == TreeCacheEvent.Type.NODE_REMOVED){
            centerInLoginInfoMrg.onCenterServerNodeRemove(centerNodeName,centerNode);
        }
    }

    /**
     * 选择需要的cache
     */
    private static class LoginCacheSelector implements TreeCacheSelector{

        /**
         * 只取回warzone下的节点
         */
        @Override
        public boolean traverseChildren(String fullPath) {
            if (fullPath.equals(ZKPathUtils.onlineRootPath())){
                return true;
            }else {
                return ZKPathUtils.findNodeName(fullPath).startsWith("warzone");
            }
        }

        /**
         * 只取回CenterServer节点
         */
        @Override
        public boolean acceptChild(String fullPath) {
            String nodeName = ZKPathUtils.findNodeName(fullPath);
            // 战区节点(容器)
            if (nodeName.startsWith("warzone")){
                return true;
            }else {
                // 叶子节点
                RoleType serverType = ZKPathUtils.parseServerType(nodeName);
                return serverType == RoleType.CENTER;
            }
        }
    }
}
