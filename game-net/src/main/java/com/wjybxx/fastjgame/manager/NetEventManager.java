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
import com.wjybxx.fastjgame.eventloop.NetEventLoopManager;
import com.wjybxx.fastjgame.net.socket.ConnectRequestEvent;
import com.wjybxx.fastjgame.net.socket.ConnectResponseEvent;
import com.wjybxx.fastjgame.net.socket.OrderedMessageEvent;
import com.wjybxx.fastjgame.net.http.HttpRequestEvent;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(NetEventManager.class);

    private final SessionManager sessionManager;
    private final HttpSessionManager httpSessionManager;
    private final NetEventLoopManager netEventLoopManager;

    @Inject
    public NetEventManager(SessionManager sessionManager, HttpSessionManager httpSessionManager,
                           NetEventLoopManager netEventLoopManager) {
        this.sessionManager = sessionManager;
        this.httpSessionManager = httpSessionManager;
        this.netEventLoopManager = netEventLoopManager;
    }

    public void fireConnectRequest(ConnectRequestEvent eventParam) {
        ConcurrentUtils.tryCommit(netEventLoopManager.eventLoop(), () -> {
            sessionManager.onRcvConnectRequest(eventParam);
        });
    }

    public void fireConnectResponse(ConnectResponseEvent eventParam) {
        ConcurrentUtils.tryCommit(netEventLoopManager.eventLoop(), () -> {
            sessionManager.onRcvConnectResponse(eventParam);
        });
    }

    public void fireMessage(OrderedMessageEvent eventParam) {
        ConcurrentUtils.tryCommit(netEventLoopManager.eventLoop(), () -> {
            sessionManager.onRcvMessage(eventParam);
        });
    }

    public void fireHttpRequest(HttpRequestEvent eventParam) {
        ConcurrentUtils.tryCommit(netEventLoopManager.eventLoop(), () -> {
            httpSessionManager.onRcvHttpRequest(eventParam);
        });
    }

}
