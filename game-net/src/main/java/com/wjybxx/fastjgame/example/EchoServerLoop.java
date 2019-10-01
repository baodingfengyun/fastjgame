/*
 *    Copyright 2019 wjybxx
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.wjybxx.fastjgame.example;

import com.wjybxx.fastjgame.concurrent.*;
import com.wjybxx.fastjgame.eventloop.NetContext;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.eventloop.NetEventLoopImp;
import com.wjybxx.fastjgame.misc.SessionLifecycleAware;
import com.wjybxx.fastjgame.net.common.ProtocolDispatcher;
import com.wjybxx.fastjgame.net.common.RpcCallback;
import com.wjybxx.fastjgame.net.common.RpcResponse;
import com.wjybxx.fastjgame.net.common.RpcResponseChannel;
import com.wjybxx.fastjgame.net.http.HttpRequestDispatcher;
import com.wjybxx.fastjgame.net.http.HttpRequestParam;
import com.wjybxx.fastjgame.net.http.HttpResponseHelper;
import com.wjybxx.fastjgame.net.http.HttpSession;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.socket.SocketSessionConfig;
import com.wjybxx.fastjgame.utils.NetUtils;
import com.wjybxx.fastjgame.utils.TimeUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.LockSupport;

/**
 * 简单的服务器，每次将客户的请求直接返回。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/6
 * github - https://github.com/hl845740757
 */
public class EchoServerLoop extends SingleThreadEventLoop {

    private final NetEventLoop netGroup = new NetEventLoopImp(new DefaultThreadFactory("NET-EVENT-LOOP"),
            RejectedExecutionHandlers.log());

    private NetContext netContext;

    public EchoServerLoop(@Nullable EventLoopGroup parent, @Nonnull ThreadFactory threadFactory, @Nonnull RejectedExecutionHandler rejectedExecutionHandler) {
        super(parent, threadFactory, rejectedExecutionHandler);
    }

    @Override
    protected void init() throws Exception {
        super.init();
        // 创建网络环境
        netContext = netGroup.createContext(ExampleConstants.serverGuid, ExampleConstants.serverRole, this).get();

        // 监听tcp端口
        SocketSessionConfig config = SocketSessionConfig.newBuilder().setCodec(ExampleConstants.jsonBasedCodec)
                .setLifecycleAware(new ClientLifeAware())
                .setDispatcher(new EchoProtocolDispatcher())
                .build();
        netContext.bindTcp(NetUtils.getLocalIp(), ExampleConstants.tcpPort,
                config);

        // 监听http端口
        netContext.bindHttp(NetUtils.getLocalIp(), ExampleConstants.httpPort, new EchoHttpRequestDispatcher());
    }

    @Override
    protected void loop() {
        final long starrTime = System.currentTimeMillis();
        for (; ; ) {
            // 执行所有任务
            runAllTasks();
            // 循环x分钟
            if (System.currentTimeMillis() - starrTime > TimeUtils.MIN * 3) {
                break;
            }
            // 确认是否退出
            if (confirmShutdown()) {
                break;
            }
            // 睡10毫秒
            LockSupport.parkNanos(TimeUtils.NANO_PER_MILLISECOND * 10);
        }
    }

    @Override
    protected void clean() throws Exception {
        super.clean();
        if (null != netContext) {
            netContext.deregister();
        }
        netGroup.shutdown();
    }

    private static class ClientLifeAware implements SessionLifecycleAware {

        @Override
        public void onSessionConnected(Session session) {
            System.out.println("-----------------onSessionConnected----------------------");
        }

        @Override
        public void onSessionDisconnected(Session session) {
            System.out.println("----------------onSessionDisconnected---------------------");
        }
    }

    private static class EchoProtocolDispatcher implements ProtocolDispatcher {

        @Override
        public void postOneWayMessage(Session session, @Nullable Object message) {
            assert null != message;
            session.send(message);
        }

        @Override
        public void postRpcRequest(Session session, @Nullable Object request, @Nonnull RpcResponseChannel<?> responseChannel) {
            assert null != request;
            @SuppressWarnings("unchecked")
            RpcResponseChannel<Object> channel = (RpcResponseChannel<Object>) responseChannel;
            channel.writeSuccess(request);
        }

        @Override
        public void postRpcCallback(Session session, RpcCallback rpcCallback, RpcResponse rpcResponse) {
            rpcCallback.onComplete(rpcResponse);
        }
    }

    private static class EchoHttpRequestDispatcher implements HttpRequestDispatcher {

        @Override
        public void post(HttpSession httpSession, String path, HttpRequestParam params) {
            httpSession.writeAndFlush(HttpResponseHelper.newStringResponse("path - " + path + ", params - " + params.toString()));
        }

    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        EchoServerLoop echoServerLoop = new EchoServerLoop(null, new DefaultThreadFactory("SERVER"), RejectedExecutionHandlers.log());
        // 唤醒线程
        echoServerLoop.execute(() -> {
        });
    }
}
