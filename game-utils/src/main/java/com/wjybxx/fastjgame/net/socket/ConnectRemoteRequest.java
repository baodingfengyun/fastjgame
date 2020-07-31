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

import com.wjybxx.fastjgame.net.misc.HostAndPort;
import com.wjybxx.fastjgame.net.misc.NetContext;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.util.concurrent.Promise;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

/**
 * 请求与远程建立连接
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/27
 * github - https://github.com/hl845740757
 */
public class ConnectRemoteRequest {

    private final String sessionId;
    private final HostAndPort remoteAddress;
    private final SocketSessionConfig config;
    private final ChannelInitializer<SocketChannel> initializer;
    private final NetContext netContext;
    private final Promise<Session> connectPromise;

    public ConnectRemoteRequest(String sessionId,
                                HostAndPort remoteAddress,
                                SocketSessionConfig config,
                                ChannelInitializer<SocketChannel> initializer,
                                NetContext netContext,
                                Promise<Session> connectPromise) {
        this.sessionId = sessionId;
        this.remoteAddress = remoteAddress;
        this.config = config;
        this.initializer = initializer;
        this.netContext = netContext;
        this.connectPromise = connectPromise;
    }

    public String getSessionId() {
        return sessionId;
    }

    public HostAndPort getRemoteAddress() {
        return remoteAddress;
    }

    public SocketSessionConfig getConfig() {
        return config;
    }

    public ChannelInitializer<SocketChannel> getInitializer() {
        return initializer;
    }

    public NetContext getNetContext() {
        return netContext;
    }

    public Promise<Session> getConnectPromise() {
        return connectPromise;
    }
}
