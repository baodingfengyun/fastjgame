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
import com.wjybxx.fastjgame.annotation.WorldSingleton;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.net.NetContext;
import com.wjybxx.fastjgame.misc.PortRange;
import com.wjybxx.fastjgame.net.RoleType;
import com.wjybxx.fastjgame.net.SessionLifecycleAware;
import com.wjybxx.fastjgame.net.initializer.HttpServerInitializer;
import com.wjybxx.fastjgame.net.initializer.TCPClientChannelInitializer;
import com.wjybxx.fastjgame.net.initializer.TCPServerChannelInitializer;
import com.wjybxx.fastjgame.utils.GameUtils;
import com.wjybxx.fastjgame.utils.NetUtils;
import com.wjybxx.fastjgame.utils.SystemUtils;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Objects;

/**
 * 内部通信建立连接的辅助类
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
@WorldSingleton
@NotThreadSafe
public class InnerAcceptorMrg {

    private final ProtocolCodecMrg protocolCodecMrg;
    private final MessageDispatcherMrg messageDispatcherMrg;
    private final HttpDispatcherMrg httpDispatcherMrg;
    private final NetContextMrg netContextMrg;

    @Inject
    public InnerAcceptorMrg(ProtocolCodecMrg protocolCodecMrg, MessageDispatcherMrg messageDispatcherMrg, HttpDispatcherMrg httpDispatcherMrg, NetContextMrg netContextMrg) {
        this.protocolCodecMrg = protocolCodecMrg;
        this.messageDispatcherMrg = messageDispatcherMrg;

        this.httpDispatcherMrg = httpDispatcherMrg;
        this.netContextMrg = netContextMrg;
    }

    public HostAndPort bindInnerTcpPort(SessionLifecycleAware lifecycleAware) {
        return bindTcpPort(NetUtils.getLocalIp(), GameUtils.INNER_TCP_PORT_RANGE, lifecycleAware);
    }

    private HostAndPort bindTcpPort(String host, PortRange portRange, SessionLifecycleAware lifecycleAware) {
        NetContext netContext = netContextMrg.getNetContext();
        TCPServerChannelInitializer serverChannelInitializer = netContext.newTcpServerInitializer(protocolCodecMrg.getInnerProtocolCodec());

        ListenableFuture<HostAndPort> bindFuture = netContext.bindRange(host, portRange,
                serverChannelInitializer, lifecycleAware, messageDispatcherMrg);

        bindFuture.awaitUninterruptibly();
        return bindFuture.getNow();
    }

    public HostAndPort bindLocalTcpPort(SessionLifecycleAware lifecycleAware) {
        return bindTcpPort("localhost", GameUtils.LOCAL_TCP_PORT_RANGE, lifecycleAware);
    }

    public HostAndPort bindInnerHttpPort() {
        NetContext netContext = netContextMrg.getNetContext();
        HttpServerInitializer httpServerInitializer = netContext.newHttpServerInitializer();

        ListenableFuture<HostAndPort> bindFuture = netContext.bindRange(NetUtils.getLocalIp(), GameUtils.INNER_HTTP_PORT_RANGE, httpServerInitializer, httpDispatcherMrg);
        bindFuture.awaitUninterruptibly();
        return bindFuture.getNow();
    }

    public ListenableFuture<?> connect(long remoteGuid, RoleType remoteRole, String innerTcpAddress, String localAddress, String macAddress, SessionLifecycleAware lifecycleAware) {
        if (Objects.equals(macAddress, SystemUtils.getMAC())) {
            // 两个world在同一台机器，不走网卡
            return connect(remoteGuid, remoteRole, HostAndPort.parseHostAndPort(localAddress), lifecycleAware);
        } else {
            // 两个world在不同机器，走正常socket
            return connect(remoteGuid, remoteRole, HostAndPort.parseHostAndPort(innerTcpAddress), lifecycleAware);
        }
    }

    private ListenableFuture<?> connect(long remoteGuid, RoleType remoteRole, HostAndPort hostAndPort, SessionLifecycleAware lifecycleAware) {
        NetContext netContext = netContextMrg.getNetContext();
        TCPClientChannelInitializer clientChannelInitializer = netContext.newTcpClientInitializer(remoteGuid, protocolCodecMrg.getInnerProtocolCodec());

        return netContext.connect(remoteGuid, remoteRole, hostAndPort,
                () -> clientChannelInitializer,
                lifecycleAware, messageDispatcherMrg);
    }
}
