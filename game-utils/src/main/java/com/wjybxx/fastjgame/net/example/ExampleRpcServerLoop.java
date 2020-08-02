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

package com.wjybxx.fastjgame.net.example;

import com.wjybxx.fastjgame.net.local.LocalPort;
import com.wjybxx.fastjgame.net.local.LocalSessionConfig;
import com.wjybxx.fastjgame.net.misc.NetContext;
import com.wjybxx.fastjgame.net.rpc.DefaultRpcRequestProcessor;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.session.SessionLifecycleAware;
import com.wjybxx.fastjgame.net.socket.SocketSessionConfig;
import com.wjybxx.fastjgame.net.utils.NetUtils;
import com.wjybxx.fastjgame.util.concurrent.*;
import com.wjybxx.fastjgame.util.concurrent.disruptor.DisruptorEventLoop;
import com.wjybxx.fastjgame.util.concurrent.disruptor.YieldWaitStrategyFactory;
import com.wjybxx.fastjgame.util.time.TimeUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ThreadFactory;

/**
 * 示例rpc服务器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/10
 * github - https://github.com/hl845740757
 */
class ExampleRpcServerLoop extends DisruptorEventLoop {

    private final DefaultRpcRequestProcessor rpcRequestProcessor = new DefaultRpcRequestProcessor();

    private final Promise<LocalPort> localPortPromise;

    private long startTime;

    public ExampleRpcServerLoop(@Nonnull ThreadFactory threadFactory,
                                @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                                @Nullable Promise<LocalPort> localPortPromise) {
        super(null, threadFactory, rejectedExecutionHandler, new YieldWaitStrategyFactory(), 64 * 1024, 8192);
        this.localPortPromise = localPortPromise;
    }

    @Override
    protected void init() throws Exception {
        super.init();
        // 创建网络环境
        NetContext netContext = ExampleConstants.netEventLoop.createContext(this);
        // 注册rpc服务
        ExampleRpcServiceRpcRegister.register(rpcRequestProcessor, new ExampleRpcService());

        if (localPortPromise != null) {
            // 绑定jvm内部端口
            try {
                LocalSessionConfig config = LocalSessionConfig.newBuilder()
                        .setSerializer(ExampleConstants.BINARY_SERIALIZER)
                        .setLifecycleAware(new ClientLifeAware())
                        .setRpcRequestProcessor(rpcRequestProcessor)
                        .build();

                final LocalPort localPort = netContext.bindLocal(config);
                localPortPromise.trySuccess(localPort);
            } catch (Throwable e) {
                localPortPromise.tryFailure(e);
            }
        } else {
            // 监听tcp端口
            SocketSessionConfig config = SocketSessionConfig.newBuilder()
                    .setSerializer(ExampleConstants.BINARY_SERIALIZER)
                    .setLifecycleAware(new ClientLifeAware())
                    .setRpcRequestProcessor(rpcRequestProcessor)
//                    .setAutoReconnect(true)
                    .setAsyncRpcTimeoutMs((int) TimeUtils.MIN)
                    .setMaxPendingMessages(100)
                    .setMaxCacheMessages(10000)
                    .build();

            netContext.bindTcp(NetUtils.getLocalIp(), ExampleConstants.tcpPort, config);
        }
        startTime = System.currentTimeMillis();
    }

    @Override
    protected void loopOnce() {
        if (System.currentTimeMillis() - startTime > TimeUtils.MIN) {
            shutdown();
        }
    }

    @Override
    protected void clean() throws Exception {
        super.clean();
        ExampleConstants.netEventLoop.shutdown();
    }

    private static class ClientLifeAware implements SessionLifecycleAware {

        @Override
        public void onSessionConnected(Session session) {
            System.out.println("session : " + session.sessionId() + " connect");
        }

        @Override
        public void onSessionDisconnected(Session session) {
            System.out.println("session : " + session.sessionId() + " disconnect");
        }
    }

    public static void main(String[] args) {
        final ExampleRpcServerLoop serviceLoop = new ExampleRpcServerLoop(new DefaultThreadFactory("SERVICE"),
                RejectedExecutionHandlers.discard(),
                null);
        // 唤醒线程
        serviceLoop.execute(ConcurrentUtils.NO_OP_TASK);
    }
}
