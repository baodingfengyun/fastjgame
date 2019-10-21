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
import com.wjybxx.fastjgame.eventloop.NetEventLoop;

/**
 * 封装{@link NetEventLoop}可能使用到的所有管理器，算是一个超大黑板。
 * NetEventLoop不是依赖注入的，一个个获取实例实在有点麻烦...
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/3
 * github - https://github.com/hl845740757
 */
public class NetManagerWrapper {

    private final NetEventLoopManager netEventLoopManager;
    private final HttpSessionManager httpSessionManager;
    private final NettyThreadManager nettyThreadManager;
    private final HttpClientManager httpClientManager;
    private final NetTimeManager netTimeManager;
    private final NetTimerManager netTimerManager;
    private final AcceptorManager acceptorManager;
    private final ConnectorManager connectorManager;

    @Inject
    public NetManagerWrapper(NetEventLoopManager netEventLoopManager, HttpSessionManager httpSessionManager, NettyThreadManager nettyThreadManager,
                             HttpClientManager httpClientManager, NetTimeManager netTimeManager,
                             NetTimerManager netTimerManager, AcceptorManager acceptorManager, ConnectorManager connectorManager) {
        this.netEventLoopManager = netEventLoopManager;
        this.httpSessionManager = httpSessionManager;
        this.nettyThreadManager = nettyThreadManager;
        this.httpClientManager = httpClientManager;
        this.netTimeManager = netTimeManager;
        this.netTimerManager = netTimerManager;
        this.acceptorManager = acceptorManager;
        this.connectorManager = connectorManager;
    }

    public NetEventLoopManager getNetEventLoopManager() {
        return netEventLoopManager;
    }

    public HttpSessionManager getHttpSessionManager() {
        return httpSessionManager;
    }

    public NettyThreadManager getNettyThreadManager() {
        return nettyThreadManager;
    }

    public HttpClientManager getHttpClientManager() {
        return httpClientManager;
    }

    public NetTimeManager getNetTimeManager() {
        return netTimeManager;
    }

    public NetTimerManager getNetTimerManager() {
        return netTimerManager;
    }

    public AcceptorManager getAcceptorManager() {
        return acceptorManager;
    }

    public ConnectorManager getConnectorManager() {
        return connectorManager;
    }
}
