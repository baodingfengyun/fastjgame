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

package com.wjybxx.fastjgame.example;

import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandler;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.concurrent.disruptor.DisruptorEventLoop;
import com.wjybxx.fastjgame.concurrent.disruptor.DisruptorWaitStrategyType;
import com.wjybxx.fastjgame.eventloop.NetContext;
import com.wjybxx.fastjgame.misc.DefaultProtocolDispatcher;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.RpcBuilder;
import com.wjybxx.fastjgame.net.common.SessionDisconnectAware;
import com.wjybxx.fastjgame.net.local.LocalPort;
import com.wjybxx.fastjgame.net.local.LocalSessionConfig;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.socket.SocketSessionConfig;
import com.wjybxx.fastjgame.utils.NetUtils;
import com.wjybxx.fastjgame.utils.TimeUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.concurrent.ThreadFactory;
import java.util.stream.IntStream;

/**
 * rpc请求客户端示例
 *
 * @version 1.2
 * date - 2019/8/26
 */
public class ExampleRpcClientLoop extends DisruptorEventLoop {

    private NetContext netContext;

    private final LocalPort localPort;
    /**
     * 是否已建立tcp连接
     */
    private Session session;
    private long startTime;
    private int index;

    public ExampleRpcClientLoop(@Nonnull ThreadFactory threadFactory,
                                @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                                @Nullable LocalPort localPort) {
        super(null, threadFactory, rejectedExecutionHandler, DisruptorWaitStrategyType.YIELD);
        this.localPort = localPort;
    }

    @Override
    protected void init() throws Exception {
        super.init();
        netContext = ExampleConstants.netEventLoop.createContext(ExampleConstants.CLIENT_GUID, this);

        if (localPort != null) {
            LocalSessionConfig config = LocalSessionConfig.newBuilder()
                    .setCodec(ExampleConstants.reflectBasedCodec)
                    .setLifecycleAware(new ServerDisconnectAward())
                    .setDispatcher(new DefaultProtocolDispatcher())
                    .build();

            session = netContext.connectLocal(ExampleConstants.SESSION_ID, ExampleConstants.SERVER_GUID, localPort, config).get();
        } else {
            // 必须先启动服务器
            SocketSessionConfig config = SocketSessionConfig.newBuilder()
                    .setCodec(ExampleConstants.reflectBasedCodec)
                    .setLifecycleAware(new ServerDisconnectAward())
                    .setDispatcher(new DefaultProtocolDispatcher())
                    .setAutoReconnect(true)
                    .build();

            final HostAndPort address = new HostAndPort(NetUtils.getLocalIp(), ExampleConstants.tcpPort);
            session = netContext.connectTcp(ExampleConstants.SESSION_ID, ExampleConstants.SERVER_GUID, address, config)
                    .get();
        }
        startTime = System.currentTimeMillis();
    }

    @Override
    protected void loopOnce() {
        if (session == null || System.currentTimeMillis() - startTime > TimeUtils.MIN) {
            shutdown();
            return;
        }
        sendRequest(index++);
    }

    private void sendRequest(final int index) {
        final long start = System.nanoTime();
        final String callResult = ExampleRpcServiceRpcProxy.combine("wjybxx", String.valueOf(index)).syncCall(session);
        final long costTimeMs = System.nanoTime() - start;
        System.out.println("SyncCall - " + index + " - " + callResult + " , cost time nano " + costTimeMs);

        // 方法无返回值，也可以监听，只要调用的是call, sync, syncCall都可以获知调用结果，就像future
        ExampleRpcServiceRpcProxy.hello("wjybxx- " + index)
                .onSuccess(result -> System.out.println("hello - " + index + " - " + result))
                .call(session);

        ExampleRpcServiceRpcProxy.queryId("wjybxx-" + index)
                .onSuccess(result -> System.out.println("queryId - " + index + " - " + result))
                .call(session);

        // - SaferRpcCallback
        ExampleRpcServiceRpcProxy.queryId("wjybxx-" + index)
                .onSuccess(this::afterQueryId, index)
                .call(session);

        ExampleRpcServiceRpcProxy.inc(index)
                .onSuccess(result -> System.out.println("inc - " + index + " - " + result))
                .call(session);

        ExampleRpcServiceRpcProxy.incWithSession(index)
                .onSuccess(result -> System.out.println("incWithSession - " + index + " - " + result))
                .call(session);

        ExampleRpcServiceRpcProxy.incWithChannel(index)
                .onSuccess(result -> System.out.println("incWithChannel - " + index + " - " + result))
                .call(session);

        ExampleRpcServiceRpcProxy.incWithSessionAndChannel(index)
                .onSuccess(result -> System.out.println("incWithSessionAndChannel - " + index + " - " + result))
                .call(session);

        // 模拟场景服务器通过网关发送给玩家 - 注意：序列化方式必须一致。
        ExampleRpcServiceRpcProxy.sendToPlayer(12345, "这里后期替换为protoBuf消息")
                .onSuccess(result -> System.out.println("sendToPlayer - " + index + " - invoke success"))
                .call(session);

        // 模拟玩家通过网关发送给场景服务器 - 注意：序列化方式必须一致。
        try {
            ExampleRpcServiceRpcProxy.sendToScene(13245, ExampleConstants.reflectBasedCodec.serializeToBytes("这里后期替换为protoBuf消息"))
                    .onSuccess(result -> System.out.println("sendToScene - " + index + " - invoke success"))
                    .call(session);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 模拟广播X次
        final RpcBuilder<?> builder = ExampleRpcServiceRpcProxy.notifySuccess(index);
        IntStream.rangeClosed(1, 3).forEach(i -> builder.send(session));
        // 上面等同于下面
        builder.broadcast(Arrays.asList(session, session, session));

        // 阻塞到前面的rpc都返回，使得每次combine调用不被其它rpc调用影响
        // 因为调用的是sync(Session),对方的网络底层一定会返回一个结果，如果方法本身为void，那么返回的就是null。
        ExampleRpcServiceRpcProxy.sync().sync(session);
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
