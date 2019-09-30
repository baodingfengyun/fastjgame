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
import com.wjybxx.fastjgame.annotation.EventLoopSingleton;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.PortRange;
import com.wjybxx.fastjgame.misc.SessionLifecycleAware;
import com.wjybxx.fastjgame.net.NetContext;
import com.wjybxx.fastjgame.net.ProtocolCodec;
import com.wjybxx.fastjgame.net.RoleType;
import com.wjybxx.fastjgame.net.local.LocalPort;
import com.wjybxx.fastjgame.net.local.LocalSessionConfig;
import com.wjybxx.fastjgame.net.socket.SocketPort;
import com.wjybxx.fastjgame.net.socket.SocketSessionConfig;
import com.wjybxx.fastjgame.utils.GameUtils;
import com.wjybxx.fastjgame.utils.NetUtils;
import com.wjybxx.fastjgame.utils.SystemUtils;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * 内部通信建立连接的辅助类
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
@EventLoopSingleton
@NotThreadSafe
public class InnerAcceptorMrg {

    private final ProtocolCodecMrg protocolCodecMrg;
    private final ProtocolDispatcherMrg protocolDispatcherMrg;
    private final HttpDispatcherMrg httpDispatcherMrg;
    private final NetContextMrg netContextMrg;
    private final LocalPortMrg localPortMrg;
    private final WorldInfoMrg worldInfoMrg;

    @Inject
    public InnerAcceptorMrg(ProtocolCodecMrg protocolCodecMrg, ProtocolDispatcherMrg protocolDispatcherMrg, HttpDispatcherMrg httpDispatcherMrg,
                            NetContextMrg netContextMrg, LocalPortMrg localPortMrg, WorldInfoMrg worldInfoMrg) {
        this.protocolCodecMrg = protocolCodecMrg;
        this.protocolDispatcherMrg = protocolDispatcherMrg;

        this.httpDispatcherMrg = httpDispatcherMrg;
        this.netContextMrg = netContextMrg;
        this.localPortMrg = localPortMrg;
        this.worldInfoMrg = worldInfoMrg;
    }

    public void bindInnerJvmPort(SessionLifecycleAware lifecycleAware) throws ExecutionException, InterruptedException {
        LocalSessionConfig config = newJVMSessionConfig(lifecycleAware);
        final ListenableFuture<LocalPort> localPortFuture = netContextMrg.getNetContext().bindInJVM(config);
        final LocalPort localPort = localPortFuture.get();
        localPortMrg.register(worldInfoMrg.getWorldGuid(), localPort);
    }

    public HostAndPort bindInnerTcpPort(SessionLifecycleAware lifecycleAware) throws ExecutionException, InterruptedException {
        return bindTcpPort(NetUtils.getLocalIp(), GameUtils.INNER_TCP_PORT_RANGE, lifecycleAware);
    }

    private HostAndPort bindTcpPort(String host, PortRange portRange, SessionLifecycleAware lifecycleAware) throws ExecutionException, InterruptedException {
        NetContext netContext = netContextMrg.getNetContext();

        ListenableFuture<SocketPort> bindFuture = netContext.bindTcpRange(host, portRange,
                newSocketSessionConfig(lifecycleAware));

        return bindFuture.get().getHostAndPort();
    }

    public HostAndPort bindLocalTcpPort(SessionLifecycleAware lifecycleAware) throws ExecutionException, InterruptedException {
        return bindTcpPort("localhost", GameUtils.LOCAL_TCP_PORT_RANGE, lifecycleAware);
    }

    public HostAndPort bindInnerHttpPort() throws ExecutionException, InterruptedException {
        NetContext netContext = netContextMrg.getNetContext();

        ListenableFuture<HostAndPort> bindFuture = netContext.bindHttpRange(NetUtils.getLocalIp(), GameUtils.INNER_HTTP_PORT_RANGE, httpDispatcherMrg);
        return bindFuture.get();
    }

    public void connect(long remoteGuid, RoleType remoteRole, String innerTcpAddress, String localAddress, String macAddress, SessionLifecycleAware lifecycleAware) {
        final LocalPort localPort = localPortMrg.getLocalPort(remoteGuid);
        if (null != localPort) {
            // 两个world在同一个进程内
            LocalSessionConfig config = newJVMSessionConfig(lifecycleAware);
            netContextMrg.getNetContext().connectInJVM(localPort, config);
            return;
        }
        if (Objects.equals(macAddress, SystemUtils.getMAC())) {
            // 两个world在同一台机器，不走网卡
            connectTcp(remoteGuid, remoteRole, HostAndPort.parseHostAndPort(localAddress), lifecycleAware);
        } else {
            // 两个world在不同机器，走正常socket
            connectTcp(remoteGuid, remoteRole, HostAndPort.parseHostAndPort(innerTcpAddress), lifecycleAware);
        }
    }

    private void connectTcp(long remoteGuid, RoleType remoteRole, HostAndPort hostAndPort, SessionLifecycleAware lifecycleAware) {
        netContextMrg.getNetContext().connectTcp(remoteGuid, remoteRole, hostAndPort,
                newSocketSessionConfig(lifecycleAware));
    }

    public SocketSessionConfig newSocketSessionConfig(SessionLifecycleAware lifecycleAware) {
        return SocketSessionConfig.newBuilder()
                .setCodec(getInnerProtocolCodec())
                .setLifecycleAware(lifecycleAware)
                .setDispatcher(protocolDispatcherMrg)
                .build();
    }

    public LocalSessionConfig newJVMSessionConfig(SessionLifecycleAware lifecycleAware) {
        return LocalSessionConfig.newBuilder()
                .setCodec(getInnerProtocolCodec())
                .setLifecycleAware(lifecycleAware)
                .setDispatcher(protocolDispatcherMrg)
                .build();
    }

    @Nonnull
    private ProtocolCodec getInnerProtocolCodec() {
        return protocolCodecMrg.getInnerProtocolCodec();
    }
}
