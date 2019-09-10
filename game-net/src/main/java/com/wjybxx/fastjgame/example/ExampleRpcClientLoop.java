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
import com.wjybxx.fastjgame.concurrent.SingleThreadEventLoop;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroup;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroupImp;
import com.wjybxx.fastjgame.misc.DefaultRpcCallDispatcher;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.RpcBuilder;
import com.wjybxx.fastjgame.net.NetContext;
import com.wjybxx.fastjgame.net.Session;
import com.wjybxx.fastjgame.net.SessionLifecycleAware;
import com.wjybxx.fastjgame.net.SessionSenderMode;
import com.wjybxx.fastjgame.net.injvm.JVMPort;
import com.wjybxx.fastjgame.utils.NetUtils;
import com.wjybxx.fastjgame.utils.TimeUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.IntStream;

/**
 * rpc请求客户端示例
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/26
 */
public class ExampleRpcClientLoop extends SingleThreadEventLoop {

    private final NetEventLoopGroup netGroup = new NetEventLoopGroupImp(1, new DefaultThreadFactory("NET-EVENT-LOOP"),
            RejectedExecutionHandlers.log());

    private NetContext netContext;

    private final JVMPort jvmPort;
    /**
     * 是否已建立tcp连接
     */
    private Session session;

    public ExampleRpcClientLoop(@Nonnull ThreadFactory threadFactory,
                                @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                                @Nullable JVMPort jvmPort) {
        super(null, threadFactory, rejectedExecutionHandler);
        this.jvmPort = jvmPort;
    }

    @Override
    protected void init() throws Exception {
        super.init();
        netContext = netGroup.createContext(ExampleConstants.clientGuid, ExampleConstants.clientRole, this).get();

        if (jvmPort != null) {
            netContext.connectInJVM(jvmPort,
                    new ServerLifeAward(),
                    new ExampleRpcDispatcher(new DefaultRpcCallDispatcher()),
                    SessionSenderMode.DIRECT);
        } else {
            // 必须先启动服务器
            final HostAndPort address = new HostAndPort(NetUtils.getLocalIp(), ExampleConstants.tcpPort);
            netContext.connectTcp(ExampleConstants.serverGuid, ExampleConstants.serverRole, address,
                    ExampleConstants.reflectBasedCodec,
                    new ServerLifeAward(),
                    new ExampleRpcDispatcher(new DefaultRpcCallDispatcher()),
                    SessionSenderMode.DIRECT);
        }
    }

    @Override
    protected void loop() {
        final long starrTime = System.currentTimeMillis();
        for (int index = 0; ; index++) {
            System.out.println("\n ------------------------------------" + index + "------------------------------------------");

            // 执行所有任务
            runAllTasks();

            if (session != null) {
                sendRequest(index);
            }

            if (confirmShutdown()) {
                break;
            }
            // 循环x分钟
            if (System.currentTimeMillis() - starrTime > TimeUtils.MIN * 3) {
                break;
            }
        }
    }

    private void sendRequest(final int index) {
        // 方法无返回值，但是还是可以监听，只要调用的是call, sync, syncCall都可以获知调用结果
        ExampleRpcServiceRpcProxy.hello("wjybxx- " + index)
                .ifSuccess(result -> System.out.println("hello - " + index + " - " + result))
                .call(session);

        ExampleRpcServiceRpcProxy.queryId("wjybxx-" + index)
                .ifSuccess(result -> System.out.println("queryId - " + index + " - " + result))
                .call(session);

        ExampleRpcServiceRpcProxy.inc(index)
                .ifSuccess(result -> System.out.println("inc - " + index + " - " + result))
                .call(session);

        ExampleRpcServiceRpcProxy.incWithSession(index)
                .ifSuccess(result -> System.out.println("incWithSession - " + index + " - " + result))
                .call(session);

        ExampleRpcServiceRpcProxy.incWithChannel(index)
                .ifSuccess(result -> System.out.println("incWithChannel - " + index + " - " + result))
                .call(session);

        ExampleRpcServiceRpcProxy.incWithSessionAndChannel(index)
                .ifSuccess(result -> System.out.println("incWithSessionAndChannel - " + index + " - " + result))
                .call(session);

        // 如果netEventLoop无缝loop (net_config.properties 配置frameInterval 为0)
        // 在我电脑上：
        // 如果是正常socket，30W - 40W 纳秒区间段最多 - (0.3 - 0.4毫秒)。
        // 如果jvmPort， 2W - 3W 纳秒区间段最多 - (0.02 - 0.03毫秒)。
        // 这还只是数据非常少的情况下，如果数据量大，这个差距更大，JVM内部通信建议使用JVMSession

        final long start = System.nanoTime();
        final String callResult = ExampleRpcServiceRpcProxy.combine("wjybxx", String.valueOf(index)).syncCall(session);
        final long costTimeMs = System.nanoTime() - start;
        System.out.println("SyncCall - " + index + " - " + callResult + " , cost time " + costTimeMs);

        // 模拟广播X次
        final RpcBuilder<?> builder = ExampleRpcServiceRpcProxy.notifySuccess(index);
        IntStream.rangeClosed(1, 3).forEach(i -> builder.send(session));
    }

    @Override
    protected void clean() throws Exception {
        super.clean();
        if (null != netContext) {
            netContext.deregister();
        }
        netGroup.shutdown();
    }

    private class ServerLifeAward implements SessionLifecycleAware {

        @Override
        public void onSessionConnected(Session session) {
            System.out.println(" ============ onSessionConnected ===============");
            ExampleRpcClientLoop.this.session = session;
        }

        @Override
        public void onSessionDisconnected(Session session) {
            System.out.println(" =========== onSessionDisconnected ==============");
            ExampleRpcClientLoop.this.session = null;
            // 断开连接后关闭
            shutdown();
        }
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExampleRpcClientLoop echoClientLoop = new ExampleRpcClientLoop(
                new DefaultThreadFactory("CLIENT"),
                RejectedExecutionHandlers.log(),
                null);

        // 唤醒线程
        echoClientLoop.execute(() -> {
        });
    }
}
