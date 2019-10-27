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

package com.wjybxx.fastjgame.net.local;

import com.wjybxx.fastjgame.concurrent.Promise;
import com.wjybxx.fastjgame.eventloop.NetContext;
import com.wjybxx.fastjgame.net.session.Session;

/**
 * 请求与JVM内的另一个线程建立连接
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/27
 * github - https://github.com/hl845740757
 */
public class ConnectLocalRequest {

    private final String sessionId;
    private final long remoteGuid;
    private final DefaultLocalPort localPort;
    private final LocalSessionConfig config;
    private final NetContext netContext;
    private final Promise<Session> connectPromise;

    public ConnectLocalRequest(String sessionId, long remoteGuid,
                               DefaultLocalPort localPort,
                               LocalSessionConfig config,
                               NetContext netContext,
                               Promise<Session> connectPromise) {
        this.sessionId = sessionId;
        this.remoteGuid = remoteGuid;
        this.localPort = localPort;
        this.config = config;
        this.netContext = netContext;
        this.connectPromise = connectPromise;
    }

    public String getSessionId() {
        return sessionId;
    }

    public long getRemoteGuid() {
        return remoteGuid;
    }

    public DefaultLocalPort getLocalPort() {
        return localPort;
    }

    public LocalSessionConfig getConfig() {
        return config;
    }

    public NetContext getNetContext() {
        return netContext;
    }

    public Promise<Session> getConnectPromise() {
        return connectPromise;
    }
}
