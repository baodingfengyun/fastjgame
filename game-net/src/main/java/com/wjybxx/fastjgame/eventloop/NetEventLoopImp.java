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
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandler;
import com.wjybxx.fastjgame.concurrent.SingleThreadEventLoop;
import com.wjybxx.fastjgame.concurrent.disruptor.DisruptorEventLoop;
import com.wjybxx.fastjgame.manager.*;
import com.wjybxx.fastjgame.module.NetEventLoopModule;
import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.timer.FixedDelayHandle;
import com.wjybxx.fastjgame.utils.CollectionUtils;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.FunctionUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
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

    private static final int MAX_BATCH_SIZE = 32 * 1024;

    private final NetManagerWrapper managerWrapper;
    private final NetEventLoopManager netEventLoopManager;
    private final NetConfigManager netConfigManager;
    private final NettyThreadManager nettyThreadManager;
    private final SocketS2CSessionManager socketS2CSessionManager;
    private final SocketC2SSessionManager socketC2SSessionManager;
    private final HttpSessionManager httpSessionManager;
    private final JVMS2CSessionManager jvms2CSessionManager;
    private final JVMC2SSessionManager jvmc2SSessionManager;
    private final NetTimeManager netTimeManager;
    private final NetTimerManager netTimerManager;

    /**
     * 已注册的用户的EventLoop集合，它是一个安全措施，如果用户在退出时如果没有执行取消操作，
     * 那么当监听到所在的EventLoop进入终止状态时，取消该EventLoop上注册的用户。
     */
    private final Set<EventLoop> registeredUserEventLoopSet = new HashSet<>();
    /**
     * 已注册的用户集合
     */
    private final Long2ObjectMap<NetContextImp> registeredUserMap = new Long2ObjectOpenHashMap<>();

    NetEventLoopImp(@Nonnull NetEventLoopGroup parent,
                    @Nonnull ThreadFactory threadFactory,
                    @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                    @Nonnull Injector parentInjector) {
        super(parent, threadFactory, rejectedExecutionHandler);

        // 使得新创建的injector可以直接使用全局单例
        Injector injector = parentInjector.createChildInjector(new NetEventLoopModule());
        managerWrapper = injector.getInstance(NetManagerWrapper.class);
        netConfigManager = managerWrapper.getNetConfigManager();
        // 用于发布自己
        netEventLoopManager = managerWrapper.getNetEventLoopManager();
        // session管理
        socketS2CSessionManager = managerWrapper.getSocketS2CSessionManager();
        socketC2SSessionManager = managerWrapper.getSocketC2SSessionManager();
        httpSessionManager = managerWrapper.getHttpSessionManager();

        jvms2CSessionManager = managerWrapper.getJvms2CSessionManager();
        jvmc2SSessionManager = managerWrapper.getJvmc2SSessionManager();

        // NetEventLoop私有的资源
        nettyThreadManager = managerWrapper.getNettyThreadManager();
        // 时间管理器和timer管理器
        netTimeManager = managerWrapper.getNetTimeManager();
        netTimerManager = managerWrapper.getNetTimerManager();
        // 解决循环依赖
        socketS2CSessionManager.setManagerWrapper(managerWrapper);
        socketC2SSessionManager.setManagerWrapper(managerWrapper);
        httpSessionManager.setManagerWrapper(managerWrapper);
        jvms2CSessionManager.setNetManagerWrapper(managerWrapper);
        jvmc2SSessionManager.setNetManagerWrapper(managerWrapper);
    }


    @Nullable
    @Override
    public NetEventLoopGroup parent() {
        return (NetEventLoopGroup) super.parent();
    }

    @Nonnull
    @Override
    public NetEventLoop next() {
        return (NetEventLoop) super.next();
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
    public ListenableFuture<NetContext> createContext(long localGuid, RoleType localRole, @Nonnull EventLoop localEventLoop) {
        if (localEventLoop instanceof NetEventLoop) {
            throw new IllegalArgumentException("Unexpected invoke.");
        }
        // 这里一定是逻辑层，不同线程
        return submit(() -> {
            if (registeredUserMap.containsKey(localGuid)) {
                throw new IllegalArgumentException("user " + localGuid + " is already registered!");
            }
            // 创建context
            NetContextImp netContext = new NetContextImp(localGuid, localRole, localEventLoop, this, managerWrapper);
            registeredUserMap.put(localGuid, netContext);
            // 监听用户线程关闭
            if (registeredUserEventLoopSet.add(localEventLoop)) {
                localEventLoop.terminationFuture().addListener(future -> onUserEventLoopTerminal(localEventLoop), this);
            }
            return netContext;
        });
    }

    @Override
    protected void init() throws Exception {
        super.init();
        // Q:为什么没使用threadLocal？
        // A:本来想使用的，但是如果提供一个全局的接口的话，它也会对逻辑层开放，而逻辑层如果调用了一定会导致错误。使用threadLocal暴露了不该暴露的接口。
        // 发布自身，使得该eventLoop的其它管理器可以方便的获取该对象
        netEventLoopManager.publish(this);
        // 10ms一次tick足够了
        netTimerManager.newFixedDelay(10, this::tick);
        // 切换到缓存策略
        netTimeManager.changeToCacheStrategy();
    }

    @Override
    protected void loop() {
        while (true) {
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
        }
    }

    /**
     * 定时刷帧，不必频繁刷帧
     */
    private void tick(FixedDelayHandle handle) {
        // 刷帧
        socketS2CSessionManager.tick();
        socketC2SSessionManager.tick();
        jvms2CSessionManager.tick();
        jvmc2SSessionManager.tick();
    }

    @Override
    protected void clean() throws Exception {
        super.clean();
        // 清理定时器
        netTimerManager.close();
        // 删除所有的用户信息
        CollectionUtils.removeIfAndThen(registeredUserMap.values(),
                FunctionUtils::TRUE,
                NetContextImp::afterRemoved);
        // 关闭netty线程
        ConcurrentUtils.safeExecute((Runnable) nettyThreadManager::shutdown);
    }

    @Nonnull
    @Override
    public ListenableFuture<?> deregisterContext(long localGuid) {
        // 逻辑层调用
        return submit(() -> {
            NetContextImp netContext = registeredUserMap.remove(localGuid);
            if (null == netContext) {
                // 早已取消
                return;
            }
            netContext.afterRemoved();
        });
    }

    private void onUserEventLoopTerminal(EventLoop userEventLoop) {
        // 删除该EventLoop相关的所有context
        CollectionUtils.removeIfAndThen(registeredUserMap.values(),
                netContext -> netContext.localEventLoop() == userEventLoop,
                NetContextImp::afterRemoved);

        // 更彻底的清理
        managerWrapper.getSocketS2CSessionManager().onUserEventLoopTerminal(userEventLoop);
        managerWrapper.getSocketC2SSessionManager().onUserEventLoopTerminal(userEventLoop);
        managerWrapper.getHttpSessionManager().onUserEventLoopTerminal(userEventLoop);

        managerWrapper.getJvmc2SSessionManager().onUserEventLoopTerminal(userEventLoop);
        managerWrapper.getJvms2CSessionManager().onUserEventLoopTerminal(userEventLoop);
    }
}