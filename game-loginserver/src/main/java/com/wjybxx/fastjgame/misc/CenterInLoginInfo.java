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

package com.wjybxx.fastjgame.misc;

import com.wjybxx.fastjgame.core.onlinenode.CenterNodeData;
import com.wjybxx.fastjgame.core.onlinenode.CenterNodeName;

/**
 * 中心服在登录服的信息
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/17 21:24
 * github - https://github.com/hl845740757
 */
public class CenterInLoginInfo {

    private final CenterNodeName nodeName;
    private final CenterNodeData nodeData;

    public CenterInLoginInfo(CenterNodeName nodeName, CenterNodeData nodeData) {
        this.nodeName = nodeName;
        this.nodeData = nodeData;
    }

    public CenterServerId getServerID() {
        return nodeName.getServerId();
    }

    public long getWorldGuid() {
        return nodeData.getWorldGuid();
    }

    public String getInnerTcpAddress() {
        return nodeData.getInnerTcpAddress();
    }

    public String getInnerHttpAddress() {
        return nodeData.getInnerHttpAddress();
    }

    public String getLocalAddress() {
        return nodeData.getLocalAddress();
    }

    public String getMacAddress() {
        return nodeData.getMacAddress();
    }
}
