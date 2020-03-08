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
import com.wjybxx.fastjgame.net.misc.HostAndPort;
import com.wjybxx.fastjgame.net.misc.NetContext;
import com.wjybxx.fastjgame.net.rpc.DefaultRpcRequestDispatcher;
import com.wjybxx.fastjgame.net.rpc.RpcClient;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.session.SessionDisconnectAware;
import com.wjybxx.fastjgame.net.socket.SocketSessionConfig;
import com.wjybxx.fastjgame.net.utils.NetUtils;
import com.wjybxx.fastjgame.utils.TimeUtils;
import com.wjybxx.fastjgame.utils.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.utils.concurrent.RejectedExecutionHandler;
import com.wjybxx.fastjgame.utils.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.utils.concurrent.disruptor.DisruptorEventLoop;
import com.wjybxx.fastjgame.utils.concurrent.disruptor.YieldWaitStrategyFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ThreadFactory;

/**
 * rpc请求客户端示例
 *
 * @version 1.2
 * date - 2019/8/26
 */
class ExampleRpcClientLoop extends DisruptorEventLoop {

    private final LocalPort localPort;
    private final RpcClient rpcClient = new ExampleRpcClient(this);

    private Session session;
    private long startTime;
    private int index;


    public ExampleRpcClientLoop(@Nonnull ThreadFactory threadFactory,
                                @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                                @Nullable LocalPort localPort) {
        super(null, threadFactory, rejectedExecutionHandler, 1024 * 1024, 1024, new YieldWaitStrategyFactory());
        this.localPort = localPort;
    }

    @Override
    protected void init() throws Exception {
        super.init();

        final long localGuid = System.nanoTime();
        final long serverGuid = ExampleConstants.SERVER_GUID;
        final String sessionId = "client:server=" + localGuid + ":" + serverGuid;

        NetContext netContext = ExampleConstants.netEventLoop.createContext(localGuid, this);

        if (localPort != null) {
            LocalSessionConfig config = LocalSessionConfig.newBuilder()
                    .setSerializer(ExampleConstants.BINARY_SERIALIZER)
                    .setLifecycleAware(new ServerDisconnectAward())
                    .setDispatcher(new DefaultRpcRequestDispatcher())
                    .build();

            session = netContext.connectLocal(sessionId, serverGuid, localPort, config).get();
        } else {
            // 必须先启动服务器
            SocketSessionConfig config = SocketSessionConfig.newBuilder()
                    .setSerializer(ExampleConstants.BINARY_SERIALIZER)
                    .setLifecycleAware(new ServerDisconnectAward())
                    .setDispatcher(new DefaultRpcRequestDispatcher())
//                    .setAutoReconnect(true)
                    .setAsyncRpcTimeoutMs((int) (15 * TimeUtils.SEC))
                    .setMaxPendingMessages(100)
                    .setMaxCacheMessages(20000)
                    .build();

            final HostAndPort address = new HostAndPort(NetUtils.getLocalIp(), ExampleConstants.tcpPort);
            session = netContext.connectTcp(sessionId, serverGuid, address, config)
                    .get();
        }
        startTime = System.currentTimeMillis();
    }

    @Override
    protected void loopOnce() {
        if (session == null || System.currentTimeMillis() - startTime > 5 * TimeUtils.MIN) {
            shutdown();
            return;
        }
        sendRequest(index++);
    }

    private void sendRequest(final int index) {
        final long start = System.nanoTime();
        final String callResult = rpcClient.syncCall(session, ExampleRpcServiceRpcProxy.combine("wjybxx", String.valueOf(index)));
        final long costTimeMs = System.nanoTime() - start;
        System.out.println("SyncCall - " + index + " - " + callResult + " , cost time nano " + costTimeMs);

        // 方法无返回值，也可以监听，只要调用的是call, sync, syncCall都可以获知调用结果，就像future
        rpcClient.call(session, ExampleRpcServiceRpcProxy.hello("wjybxx- " + index))
                .onSuccess(result -> System.out.println("hello - " + index + " - " + result));

        rpcClient.call(session, ExampleRpcServiceRpcProxy.queryId("wjybxx-" + index))
                .onSuccess(result -> System.out.println("queryId - " + index + " - " + result));

        rpcClient.call(session, ExampleRpcServiceRpcProxy.inc(index))
                .onSuccess(result -> System.out.println("inc - " + index + " - " + result));

        rpcClient.call(session, ExampleRpcServiceRpcProxy.incWithSession(index))
                .onSuccess(result -> System.out.println("incWithSession - " + index + " - " + result));

        rpcClient.call(session, ExampleRpcServiceRpcProxy.incWithPromise(index))
                .onSuccess(result -> System.out.println("incWithPromise - " + index + " - " + result));

        rpcClient.call(session, ExampleRpcServiceRpcProxy.incWithSessionAndPromise(index))
                .onSuccess(result -> System.out.println("incWithSessionAndPromise - " + index + " - " + result));

        // 模拟场景服务器通过网关发送给玩家 - 注意：序列化方式必须一致。
        rpcClient.call(session, ExampleRpcServiceRpcProxy.sendToPlayer(12345, "这里后期替换为protoBuf消息"))
                .onSuccess(result -> System.out.println("sendToPlayer - " + index + " - invoke success"));

        rpcClient.call(session, ExampleRpcServiceRpcProxy.join("hello", "world"))
                .onSuccess(result -> System.out.println("joinResult " + result));

        // 模拟玩家通过网关发送给场景服务器 - 注意：序列化方式必须一致。
        try {
            rpcClient.call(session, ExampleRpcServiceRpcProxy.sendToScene(13245, ExampleConstants.BINARY_SERIALIZER.toBytes("这里后期替换为protoBuf消息")))
                    .onSuccess(result -> System.out.println("sendToScene - " + index + " - invoke success"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 阻塞到前面的rpc都返回，使得每次combine调用不被其它rpc调用影响
        // 因为调用的是sync(Session),对方的网络底层一定会返回一个结果，如果方法本身为void，那么返回的就是null。
        rpcClient.syncCall(session, ExampleRpcServiceRpcProxy.sync());
    }

    /**
     * @param result rpc调用结果
     * @param index  保存的索引(一个简单的上下文)
     */
    private void afterQueryId(Integer result, int index) {
        System.out.println("saferQueryId - " + index + " - " + result);
    }

    @Override
    protected void clean() throws Exception {
        super.clean();
        ExampleConstants.netEventLoop.shutdown();
    }

    private class ServerDisconnectAward implements SessionDisconnectAware {

        @Override
        public void onSessionDisconnected(Session session) {
            System.out.println(" =========== onSessionDisconnected ==============");
            ExampleRpcClientLoop.this.session = null;
            // 断开连接后关闭
            shutdown();
        }
    }

    public static void main(String[] args) {
        ExampleRpcClientLoop echoClientLoop = new ExampleRpcClientLoop(
                new DefaultThreadFactory("CLIENT"),
                RejectedExecutionHandlers.discard(),
                null);

        // 唤醒线程
        echoClientLoop.execute(() -> {
        });
    }
}
