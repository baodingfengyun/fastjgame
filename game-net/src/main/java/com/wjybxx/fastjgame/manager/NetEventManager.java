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

package com.wjybxx.fastjgame.manager;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.net.http.HttpRequestEvent;
import com.wjybxx.fastjgame.net.socket.SocketConnectRequestEvent;
import com.wjybxx.fastjgame.net.socket.SocketConnectResponseEvent;
import com.wjybxx.fastjgame.net.socket.SocketMessageEvent;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;

import javax.annotation.concurrent.ThreadSafe;

/**
 * 网络事件管理器。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/29
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class NetEventManager {

    private final AcceptorManager acceptorManager;
    private final ConnectorManager connectorManager;
    private final HttpSessionManager httpSessionManager;
    private final NetEventLoopManager netEventLoopManager;

    @Inject
    public NetEventManager(AcceptorManager acceptorManager, ConnectorManager connectorManager, HttpSessionManager httpSessionManager,
                           NetEventLoopManager netEventLoopManager) {
        this.acceptorManager = acceptorManager;
        this.connectorManager = connectorManager;
        this.httpSessionManager = httpSessionManager;
        this.netEventLoopManager = netEventLoopManager;
    }

    public void fireConnectRequest(SocketConnectRequestEvent eventParam) {
        ConcurrentUtils.tryCommit(netEventLoopManager.eventLoop(), () -> {
            acceptorManager.onRcvConnectRequest(eventParam);
        });
    }

    public void fireConnectResponse(SocketConnectResponseEvent eventParam) {
        ConcurrentUtils.tryCommit(netEventLoopManager.eventLoop(), () -> {
            connectorManager.onRcvConnectResponse(eventParam);
        });
    }

    public void fireMessage_acceptor(SocketMessageEvent eventParam) {
        ConcurrentUtils.tryCommit(netEventLoopManager.eventLoop(), () -> {
            acceptorManager.onRcvMessage(eventParam);
        });
    }

    public void fireMessage_connector(SocketMessageEvent eventParam) {
        ConcurrentUtils.tryCommit(netEventLoopManager.eventLoop(), () -> {
            connectorManager.onRcvMessage(eventParam);
        });
    }

    public void fireHttpRequest(HttpRequestEvent eventParam) {
        ConcurrentUtils.tryCommit(netEventLoopManager.eventLoop(), () -> {
            httpSessionManager.onRcvHttpRequest(eventParam);
        });
    }

}
