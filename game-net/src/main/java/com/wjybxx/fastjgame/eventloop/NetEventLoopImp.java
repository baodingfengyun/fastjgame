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

package com.wjybxx.fastjgame.eventloop;

import com.google.inject.Injector;
import com.wjybxx.fastjgame.concurrent.*;
import com.wjybxx.fastjgame.concurrent.disruptor.DisruptorEventLoop;
import com.wjybxx.fastjgame.manager.*;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.HttpPortContext;
import com.wjybxx.fastjgame.misc.PortRange;
import com.wjybxx.fastjgame.module.NetEventLoopModule;
import com.wjybxx.fastjgame.net.common.*;
import com.wjybxx.fastjgame.net.http.HttpRequestDispatcher;
import com.wjybxx.fastjgame.net.http.HttpRequestEvent;
import com.wjybxx.fastjgame.net.http.HttpServerInitializer;
import com.wjybxx.fastjgame.net.http.OkHttpCallback;
import com.wjybxx.fastjgame.net.local.DefaultLocalPort;
import com.wjybxx.fastjgame.net.local.LocalPort;
import com.wjybxx.fastjgame.net.local.LocalSessionConfig;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.socket.*;
import com.wjybxx.fastjgame.net.ws.WsClientChannelInitializer;
import com.wjybxx.fastjgame.net.ws.WsServerChannelInitializer;
import com.wjybxx.fastjgame.timer.FixedDelayHandle;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.LockSupport;

/**
 * 网络事件循环。
 * <p>
 * 其实网络层使用{@link DisruptorEventLoop}实现可以大幅降低延迟和提高吞吐量，但是有风险，因为网络层服务的用户太多，一旦网络层阻塞，会引发死锁风险。
 * 如果能预估最大的消息数量，那么可以考虑使用{@link DisruptorEventLoop}实现，改动很小。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/3
 * github - https://github.com/hl845740757
 */
public class NetEventLoopImp extends SingleThreadEventLoop implements NetEventLoop {

    private static final Logger logger = LoggerFactory.getLogger(NetEventLoopImp.class);
    private static final int MAX_BATCH_SIZE = 32 * 1024;

    private final NetEventLoopManager netEventLoopManager;
    private final HttpClientManager httpClientManager;
    private final HttpSessionManager httpSessionManager;
    private final AcceptorManager acceptorManager;
    private final ConnectorManager connectorManager;
    private final NetTimeManager netTimeManager;
    private final NetTimerManager netTimerManager;
    private final NettyThreadManager nettyThreadManager;

    NetEventLoopImp(@Nonnull NetEventLoopGroup parent,
                    @Nonnull ThreadFactory threadFactory,
                    @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                    @Nonnull Injector parentInjector) {
        super(parent, threadFactory, rejectedExecutionHandler);

        Injector injector = parentInjector.createChildInjector(new NetEventLoopModule());
        NetManagerWrapper managerWrapper = injector.getInstance(NetManagerWrapper.class);
        // 用于发布自己
        netEventLoopManager = managerWrapper.getNetEventLoopManager();
        // session管理
        acceptorManager = managerWrapper.getAcceptorManager();
        connectorManager = managerWrapper.getConnectorManager();
        httpSessionManager = managerWrapper.getHttpSessionManager();

        // 时间管理器和timer管理器
        netTimeManager = managerWrapper.getNetTimeManager();
        netTimerManager = managerWrapper.getNetTimerManager();

        // 解决循环依赖
        acceptorManager.setManagerWrapper(managerWrapper);
        connectorManager.setNetManagerWrapper(managerWrapper);
        httpSessionManager.setManagerWrapper(managerWrapper);

        // 全局httpClient资源
        httpClientManager = managerWrapper.getHttpClientManager();
        nettyThreadManager = managerWrapper.getNettyThreadManager();
    }

    @Nonnull
    @Override
    public NetEventLoop next() {
        return (NetEventLoop) super.next();
    }

    @Nonnull
    @Override
    public NetEventLoop select(int key) {
        return (NetEventLoop) super.select(key);
    }

    @Nonnull
    @Override
    public RpcPromise newRpcPromise(@Nonnull EventLoop userEventLoop, long timeoutMs) {
        return new DefaultRpcPromise(this, userEventLoop, timeoutMs);
    }

    @Nonnull
    @Override
    public RpcFuture newCompletedRpcFuture(@Nonnull EventLoop userEventLoop, @Nonnull RpcResponse rpcResponse) {
        return new CompletedRpcFuture(userEventLoop, rpcResponse);
    }

    @Override
    protected void init() throws Exception {
        super.init();
        // 发布自身，使得该eventLoop的其它管理器可以方便的获取该对象，在这里才是正确的线程，构造方法里不能发布自己
        // Q: 为什么没使用threadLocal？
        // A: 本来想使用的，但是如果提供一个全局的接口的话，它也会对逻辑层开放，而逻辑层如果调用了一定会导致错误。使用threadLocal暴露了不该暴露的接口。
        netEventLoopManager.publish(this);
        // 10ms一次tick足够了
        netTimerManager.newFixedDelay(10, this::tick);
        // 切换到缓存策略
        netTimeManager.changeToCacheStrategy();
    }

    @Override
    protected void loop() {
        while (true) {
            try {
                runTasksBatch(MAX_BATCH_SIZE);

                // 更新时间
                netTimeManager.update(System.currentTimeMillis());
                // 检测定时器
                netTimerManager.tick();

                if (confirmShutdown()) {
                    break;
                }
                // 降低cpu利用率
                LockSupport.parkNanos(100);
            } catch (Throwable e) {
                // 避免错误的退出循环
                logger.warn("loop caught exception", e);
            }
        }
    }

    /**
     * 定时刷帧，不必频繁刷帧
     */
    private void tick(FixedDelayHandle handle) {
        // 刷帧
        acceptorManager.tick();
        connectorManager.tick();
    }

    private void onUserEventLoopTerminal(EventLoop userEventLoop) {
        acceptorManager.onUserEventLoopTerminal(userEventLoop);
        connectorManager.onUserEventLoopTerminal(userEventLoop);
        httpSessionManager.onUserEventLoopTerminal(userEventLoop);
    }

    @Override
    protected void clean() throws Exception {
        super.clean();
        // 清理定时器
        netTimerManager.close();

        // 清理资源
        ConcurrentUtils.safeExecute((Runnable) acceptorManager::clean);
        ConcurrentUtils.safeExecute((Runnable) connectorManager::clean);
        ConcurrentUtils.safeExecute((Runnable) httpSessionManager::clean);
    }

    @Override
    public NetContext createContext(@Nonnull EventLoop localEventLoop) {
        if (localEventLoop instanceof NetEventLoop) {
            throw new IllegalArgumentException("Bad EventLoop");
        }
        // 监听用户线程关闭事件
        localEventLoop.terminationFuture().addListener(future -> {
            onUserEventLoopTerminal(localEventLoop);
        }, this);
        return new NetContextImp(this, localEventLoop);
    }

    // ------------------------------------------------------------- socket -------------------------------------------------

    @Override
    public void fireConnectRequest(SocketConnectRequestEvent event) {
        execute(() -> {
            acceptorManager.onRcvConnectRequest(event);
        });
    }

    @Override
    public void fireMessage_acceptor(SocketMessageEvent event) {
        execute(() -> {
            acceptorManager.onRcvMessage(event);
        });
    }

    @Override
    public void fireHttpRequest(HttpRequestEvent event) {
        execute(() -> {
            httpSessionManager.onRcvHttpRequest(event);
        });
    }

    @Override
    public void fireConnectResponse(SocketConnectResponseEvent event) {
        execute(() -> {
            connectorManager.onRcvConnectResponse(event);
        });
    }

    @Override
    public void fireMessage_connector(SocketMessageEvent event) {
        execute(() -> {
            connectorManager.onRcvMessage(event);
        });
    }

    @Override
    public ListenableFuture<SocketPort> bindTcpRange(String host, PortRange portRange, SocketSessionConfig config, NetContext netContext) {
        SocketPortContext portExtraInfo = new SocketPortContext(netContext, config);
        TCPServerChannelInitializer initializer = new TCPServerChannelInitializer(portExtraInfo);
        return submit(() -> {
            return nettyThreadManager.bindRange(host, portRange, config.sndBuffer(), config.rcvBuffer(), initializer);
        });
    }

    @Override
    public ListenableFuture<Session> connectTcp(String sessionId, HostAndPort remoteAddress, byte[] token, SocketSessionConfig config, NetContext netContext) {
        final TCPClientChannelInitializer initializer = new TCPClientChannelInitializer(sessionId, config, this);
        return submit(() -> {
            return connectorManager.connect(netContext.localEventLoop(), sessionId, remoteAddress, token, config, initializer);
        });
    }

    @Override
    public ListenableFuture<SocketPort> bindWSRange(String host, PortRange portRange, String websocketPath, SocketSessionConfig config, NetContext netContext) {
        SocketPortContext portExtraInfo = new SocketPortContext(netContext, config);
        WsServerChannelInitializer initializer = new WsServerChannelInitializer(websocketPath, portExtraInfo);
        return submit(() -> {
            return nettyThreadManager.bindRange(host, portRange, config.sndBuffer(), config.rcvBuffer(), initializer);
        });
    }

    @Override
    public ListenableFuture<Session> connectWS(String sessionId, HostAndPort remoteAddress, String websocketUrl, byte[] token, SocketSessionConfig config, NetContext netContext) {
        final WsClientChannelInitializer initializer = new WsClientChannelInitializer(sessionId, websocketUrl, config, this);
        return submit(() -> {
            return connectorManager.connect(netContext.localEventLoop(), sessionId, remoteAddress, token, config, initializer);
        });
    }

    // --------------------------------------------------------- localSession ----------------------------------------------------

    @Override
    public ListenableFuture<LocalPort> bindLocal(NetContext netContext, LocalSessionConfig config) {
        return submit(() -> {
            return acceptorManager.bindLocal(netContext, config);
        });
    }

    @Override
    public ListenableFuture<Session> connectLocal(LocalPort localPort, String sessionId, byte[] token, LocalSessionConfig config, NetContext netContext) {
        if (!(localPort instanceof DefaultLocalPort)) {
            return new FailedFuture<>(this, new UnsupportedOperationException());
        }
        return submit(() -> {
            return connectorManager.connectLocal((DefaultLocalPort) localPort, netContext.localEventLoop(), sessionId, token, config);
        });
    }
    // -------------------------------------------------------------- http --------------------------------------------------------

    @Override
    public ListenableFuture<SocketPort> bindHttpRange(String host, PortRange portRange, @Nonnull HttpRequestDispatcher httpRequestDispatcher, @Nonnull NetContext netContext) {
        final HttpPortContext httpPortContext = new HttpPortContext(netContext, httpRequestDispatcher);
        final HttpServerInitializer initializer = new HttpServerInitializer(httpPortContext);
        return submit(() -> {
            return nettyThreadManager.bindRange(host, portRange, 8192, 8192, initializer);
        });
    }

    @Override
    public Response syncGet(String url, @Nonnull Map<String, String> params) throws IOException {
        return httpClientManager.syncGet(url, params);
    }

    @Override
    public void asyncGet(String url, @Nonnull Map<String, String> params, @Nonnull OkHttpCallback okHttpCallback, @Nonnull EventLoop localEventLoop) {
        httpClientManager.asyncGet(url, params, localEventLoop, okHttpCallback);
    }

    @Override
    public Response syncPost(String url, @Nonnull Map<String, String> params) throws IOException {
        return httpClientManager.syncPost(url, params);
    }

    @Override
    public void asyncPost(String url, @Nonnull Map<String, String> params, @Nonnull OkHttpCallback okHttpCallback, EventLoop localEventLoop) {
        httpClientManager.asyncPost(url, params, localEventLoop, okHttpCallback);
    }

}