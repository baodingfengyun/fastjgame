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
import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandler;
import com.wjybxx.fastjgame.concurrent.SingleThreadEventLoop;
import com.wjybxx.fastjgame.concurrent.disruptor.DisruptorEventLoop;
import com.wjybxx.fastjgame.concurrent.event.EventDispatchTask;
import com.wjybxx.fastjgame.concurrent.event.EventLoopTerminalEvent;
import com.wjybxx.fastjgame.eventbus.Subscribe;
import com.wjybxx.fastjgame.manager.*;
import com.wjybxx.fastjgame.misc.DefaultNetContext;
import com.wjybxx.fastjgame.module.NetEventLoopModule;
import com.wjybxx.fastjgame.net.common.DefaultRpcPromise;
import com.wjybxx.fastjgame.net.common.FailedRpcFuture;
import com.wjybxx.fastjgame.net.common.RpcFuture;
import com.wjybxx.fastjgame.net.common.RpcPromise;
import com.wjybxx.fastjgame.net.http.HttpRequestEvent;
import com.wjybxx.fastjgame.net.local.ConnectLocalRequest;
import com.wjybxx.fastjgame.net.socket.*;
import com.wjybxx.fastjgame.utils.CloseableUtils;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;

import static com.wjybxx.fastjgame.utils.ThreadUtils.sleepQuietly;

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
class NetEventLoopImp extends SingleThreadEventLoop implements NetEventLoop {

    private static final Logger logger = LoggerFactory.getLogger(NetEventLoopImp.class);
    private static final int TASK_BATCH_SIZE = 8192;

    private final Set<EventLoop> appEventLoopSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final NetEventLoopManager netEventLoopManager;
    private final HttpSessionManager httpSessionManager;
    private final AcceptorManager acceptorManager;
    private final ConnectorManager connectorManager;
    private final NetTimeManager netTimeManager;
    private final NetTimerManager netTimerManager;
    private final NettyThreadManager nettyThreadManager;
    private final NetEventBusManager netEventBusManager;

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

        // EventBus
        netEventBusManager = managerWrapper.getEventBusManager();

        // 解决循环依赖
        acceptorManager.setManagerWrapper(managerWrapper);
        connectorManager.setNetManagerWrapper(managerWrapper);

        // 全局资源
        nettyThreadManager = managerWrapper.getNettyThreadManager();
    }

    @Nullable
    @Override
    public NetEventLoopGroup parent() {
        return (NetEventLoopGroup) super.parent();
    }

    @Nonnull
    @Override
    public NetEventLoop next() {
        return this;
    }

    @Nonnull
    @Override
    public NetEventLoop select(int key) {
        return this;
    }

    @Nonnull
    @Override
    public NetEventLoop select(@Nonnull String sessionId) {
        return this;
    }

    @Nonnull
    @Override
    public NetEventLoop select(@Nonnull Channel channel) {
        return this;
    }

    @Nonnull
    @Override
    public <V> RpcPromise<V> newRpcPromise(@Nonnull EventLoop appEventLoop, long timeoutMs) {
        return new DefaultRpcPromise<>(this, appEventLoop, timeoutMs);
    }

    @Nonnull
    @Override
    public <V> RpcFuture<V> newFailedRpcFuture(@Nonnull EventLoop appEventLoop, @Nonnull Throwable cause) {
        return new FailedRpcFuture<>(appEventLoop, cause);
    }

    @Override
    public final void post(@Nonnull Object event) {
        execute(new EventDispatchTask(netEventBusManager, event));
    }

    @Override
    protected void init() throws Exception {
        super.init();
        // 发布自身，使得该eventLoop的其它管理器可以方便的获取该对象，在这里才是正确的线程，构造方法里不能发布自己
        // Q: 为什么没使用threadLocal？
        // A: 本来想使用的，但是如果提供一个全局的接口的话，它也会对逻辑层开放，而逻辑层如果调用了一定会导致错误。使用threadLocal暴露了不该暴露的接口。
        netEventLoopManager.publish(this);

        // 注册事件订阅者
        registerEventSubscriber();

        // 切换到缓存策略
        netTimeManager.changeToCacheStrategy();
    }

    private void registerEventSubscriber() {
        // XXXBusRegister 是注解处理器编译时生成的
        NetEventLoopImpBusRegister.register(netEventBusManager, this);
    }

    @Override
    protected void loop() {
        while (true) {
            try {
                // 批量执行任务
                runTasksBatch(TASK_BATCH_SIZE);

                // 更新时间
                netTimeManager.update(System.currentTimeMillis());
                // 检测定时器
                netTimerManager.tick();

                if (confirmShutdown()) {
                    break;
                }

                // 等待以降低cpu利用率
                sleepQuietly(1);
            } catch (Throwable e) {
                // 避免错误的退出循环
                logger.warn("loop caught exception", e);
            }
        }
    }

    @Override
    protected void clean() throws Exception {
        appEventLoopSet.clear();
        // 清理定时器
        CloseableUtils.closeSafely(netTimerManager::close);

        // 清理资源
        CloseableUtils.closeSafely(acceptorManager::clean);
        CloseableUtils.closeSafely(connectorManager::clean);
        CloseableUtils.closeSafely(httpSessionManager::clean);
    }

    @Override
    public NetContext createContext(long localGuid, @Nonnull EventLoop appEventLoop) {
        if (appEventLoop instanceof NetEventLoop) {
            throw new IllegalArgumentException("Bad EventLoop");
        }

        if (appEventLoopSet.add(appEventLoop)) {
            // 监听用户线程关闭
            appEventLoop.terminationFuture().addListener(future -> {
                if (!isShuttingDown()) {
                    post(new EventLoopTerminalEvent(appEventLoop));
                }
            });
        }

        return new DefaultNetContext(localGuid, appEventLoop, this, nettyThreadManager);
    }

    @Subscribe
    void onAppEventLoopTerminal(EventLoopTerminalEvent event) {
        final EventLoop appEventLoop = event.getTerminatedEventLoop();
        acceptorManager.onAppEventLoopTerminal(appEventLoop);
        connectorManager.onAppEventLoopTerminal(appEventLoop);
        httpSessionManager.onAppEventLoopTerminal(appEventLoop);
    }

    // ---------------------------------------------- socket -------------------------------------------------

    @Subscribe
    void fireConnectRequest(GenericSocketEvent<SocketConnectRequestEvent> event) {
        acceptorManager.onRcvConnectRequest(event.child());
    }

    @Subscribe
    void fireConnectResponse(GenericSocketEvent<SocketConnectResponseEvent> event) {
        connectorManager.onRcvConnectResponse(event.child());
    }

    @Subscribe(onlySubEvents = true, subEvents = {
            SocketPingPongEvent.class,
            SocketMessageEvent.class,
            SocketChannelInactiveEvent.class
    })
    void fireSocketEvent(GenericSocketEvent<SocketEvent> event) {
        if (event.isForAcceptor()) {
            acceptorManager.onSessionEvent(event.child());
        } else {
            connectorManager.onSessionEvent(event.child());
        }
    }

    @Subscribe
    void fireConnectRemoteRequest(ConnectRemoteRequest request) {
        connectorManager.connect(request);
    }

    @Subscribe
    void fireConnectLocalRequest(ConnectLocalRequest request) {
        connectorManager.connectLocal(request);
    }

    @Subscribe
    void fireHttpRequest(HttpRequestEvent event) {
        httpSessionManager.onRcvHttpRequest(event);
    }

}