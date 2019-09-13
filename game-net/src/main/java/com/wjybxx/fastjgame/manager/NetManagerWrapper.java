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
import com.wjybxx.fastjgame.eventloop.NetEventLoopManager;

/**
 * 封装{@link NetEventLoop}可能使用到的所有管理器。
 * NetEventLoop不是依赖注入的，一个个获取实例实在有点麻烦...
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/3
 * github - https://github.com/hl845740757
 */
public class NetManagerWrapper {

    private final NetEventLoopManager netEventLoopManager;
    private final SocketS2CSessionManager socketS2CSessionManager;
    private final SocketC2SSessionManager socketC2SSessionManager;
    private final HttpSessionManager httpSessionManager;
    private final NetEventManager netEventManager;
    private final NettyThreadManager nettyThreadManager;
    private final NetConfigManager netConfigManager;
    private final AcceptorManager acceptorManager;
    private final HttpClientManager httpClientManager;
    private final NetTimeManager netTimeManager;
    private final NetTimerManager netTimerManager;
    private final JVMC2SSessionManager jvmc2SSessionManager;
    private final JVMS2CSessionManager jvms2CSessionManager;

    @Inject
    public NetManagerWrapper(NetEventLoopManager netEventLoopManager,
                             SocketS2CSessionManager socketS2CSessionManager, SocketC2SSessionManager socketC2SSessionManager,
                             HttpSessionManager httpSessionManager, NetEventManager netEventManager,
                             NettyThreadManager nettyThreadManager,
                             NetConfigManager netConfigManager, AcceptorManager acceptorManager,
                             HttpClientManager httpClientManager, NetTimeManager netTimeManager,
                             NetTimerManager netTimerManager, JVMC2SSessionManager jvmc2SSessionManager, JVMS2CSessionManager jvms2CSessionManager) {
        this.netEventLoopManager = netEventLoopManager;
        this.socketS2CSessionManager = socketS2CSessionManager;
        this.socketC2SSessionManager = socketC2SSessionManager;
        this.httpSessionManager = httpSessionManager;
        this.netEventManager = netEventManager;
        this.nettyThreadManager = nettyThreadManager;
        this.netConfigManager = netConfigManager;
        this.acceptorManager = acceptorManager;
        this.httpClientManager = httpClientManager;
        this.netTimeManager = netTimeManager;
        this.netTimerManager = netTimerManager;
        this.jvmc2SSessionManager = jvmc2SSessionManager;
        this.jvms2CSessionManager = jvms2CSessionManager;
    }

    public NetEventLoopManager getNetEventLoopManager() {
        return netEventLoopManager;
    }

    public SocketS2CSessionManager getSocketS2CSessionManager() {
        return socketS2CSessionManager;
    }

    public SocketC2SSessionManager getSocketC2SSessionManager() {
        return socketC2SSessionManager;
    }

    public HttpSessionManager getHttpSessionManager() {
        return httpSessionManager;
    }

    public NetEventManager getNetEventManager() {
        return netEventManager;
    }

    public NettyThreadManager getNettyThreadManager() {
        return nettyThreadManager;
    }

    public AcceptorManager getAcceptorManager() {
        return acceptorManager;
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

    public NetConfigManager getNetConfigManager() {
        return netConfigManager;
    }

    public JVMC2SSessionManager getJvmc2SSessionManager() {
        return jvmc2SSessionManager;
    }

    public JVMS2CSessionManager getJvms2CSessionManager() {
        return jvms2CSessionManager;
    }
}
