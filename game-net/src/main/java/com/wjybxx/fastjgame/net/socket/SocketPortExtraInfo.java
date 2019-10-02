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

package com.wjybxx.fastjgame.net.socket;

import com.wjybxx.fastjgame.eventloop.NetContext;

/**
 * socket端口监听信息 - 存储监听者的一些信息
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/2
 * github - https://github.com/hl845740757
 */
public class SocketPortExtraInfo {

    /**
     * 监听者的信息
     */
    private final NetContext netContext;
    /**
     * session配置信息
     */
    private final SocketSessionConfig sessionConfig;

    public SocketPortExtraInfo(NetContext netContext, SocketSessionConfig sessionConfig) {
        this.netContext = netContext;
        this.sessionConfig = sessionConfig;
    }

    public NetContext getNetContext() {
        return netContext;
    }

    public SocketSessionConfig getSessionConfig() {
        return sessionConfig;
    }
}
