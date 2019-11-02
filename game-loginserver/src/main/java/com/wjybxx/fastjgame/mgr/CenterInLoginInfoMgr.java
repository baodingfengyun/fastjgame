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
import com.wjybxx.fastjgame.core.onlinenode.CenterNodeData;
import com.wjybxx.fastjgame.core.onlinenode.CenterNodeName;
import com.wjybxx.fastjgame.misc.CenterInLoginInfo;
import com.wjybxx.fastjgame.misc.CenterServerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * CenterServer在LoginServer端的信息
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/17 21:23
 * github - https://github.com/hl845740757
 */
public class CenterInLoginInfoMgr {

    private static final Logger logger = LoggerFactory.getLogger(CenterInLoginInfoMgr.class);
    /**
     * 中心服id -> 中心服信息
     */
    private final Map<CenterServerId, CenterInLoginInfo> serverId2InfoMap = new HashMap<>();

    @Inject
    public CenterInLoginInfoMgr() {

    }

    public void onDiscoverCenterServer(CenterNodeName nodeName, CenterNodeData nodeData) {
        assert !serverId2InfoMap.containsKey(nodeName.getServerId());

        final CenterInLoginInfo centerInLoginInfo = new CenterInLoginInfo(nodeName, nodeData);
        serverId2InfoMap.put(nodeName.getServerId(), centerInLoginInfo);

        logger.info("{} nodeData added.", nodeName.getServerId());
    }

    public void onCenterServerNodeRemove(CenterNodeName nodeName, CenterNodeData nodeData) {
        assert serverId2InfoMap.containsKey(nodeName.getServerId());

        serverId2InfoMap.remove(nodeName.getServerId());
        logger.info("{} nodeData removed.", nodeName.getServerId());
    }

}
