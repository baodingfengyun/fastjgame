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

import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandler;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.concurrent.SingleThreadEventLoop;
import com.wjybxx.fastjgame.eventloop.NetContext;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.eventloop.NetEventLoopImp;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.net.common.SessionDisconnectAware;
import com.wjybxx.fastjgame.net.common.ProtocolDispatcher;
import com.wjybxx.fastjgame.net.common.RpcCallback;
import com.wjybxx.fastjgame.net.common.RpcResponse;
import com.wjybxx.fastjgame.net.common.RpcResponseChannel;
import com.wjybxx.fastjgame.net.http.OkHttpCallback;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.socket.SocketSessionConfig;
import com.wjybxx.fastjgame.utils.JsonUtils;
import com.wjybxx.fastjgame.utils.NetUtils;
import com.wjybxx.fastjgame.utils.TimeUtils;
import okhttp3.Call;
import okhttp3.Response;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.LockSupport;

import static com.wjybxx.fastjgame.example.ExampleConstants.EMPTY_TOKEN;

/**
 * 测试客户端
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/6
 * github - https://github.com/hl845740757
 */
public class EchoClientLoop extends SingleThreadEventLoop {

    private final NetEventLoop netGroup = new NetEventLoopImp(new DefaultThreadFactory("NET-EVENT-LOOP"),
            RejectedExecutionHandlers.log());

    private NetContext netContext;

    /**
     * 是否已建立tcp连接
     */
    private Session session;

    public EchoClientLoop(@Nonnull ThreadFactory threadFactory,
                          @Nonnull RejectedExecutionHandler rejectedExecutionHandler) {
        super(null, threadFactory, rejectedExecutionHandler);
    }

    @Override
    protected void init() throws Exception {
        super.init();
        netContext = netGroup.createContext(ExampleConstants.clientGuid, this).get();
        // 必须先启动服务器
        final HostAndPort address = new HostAndPort(NetUtils.getLocalIp(), ExampleConstants.tcpPort);
        SocketSessionConfig config = SocketSessionConfig.newBuilder()
                .setCodec(ExampleConstants.jsonBasedCodec)
                .setLifecycleAware(new ServerDisconnectAware())
                .setDispatcher(new EchoProtocolDispatcher())
                .build();

        session = netContext.connectTcp(ExampleConstants.serverGuid, address, EMPTY_TOKEN, config)
                .get();
    }

    public void loop() {
        for (int index = 0; index < 1000; index++) {
            System.out.println("\n ------------------------------------" + index + "------------------------------------------");

            // 执行所有任务
            runAllTasks();

            if (confirmShutdown()) {
                break;
            }

            // 某一个task处理了断开连接事件
            if (null == session) {
                break;
            }

            trySendMessage(index);

            // X秒一个循环
            LockSupport.parkNanos(TimeUtils.NANO_PER_MILLISECOND * TimeUtils.SEC);
        }
    }

    private void trySendMessage(final int index) {
        // 发送单向消息
        {
            ExampleMessages.Hello hello = new ExampleMessages.Hello();
            hello.setId(index);
            hello.setMessage("OneWayMessage - " + System.currentTimeMillis());
            session.send(hello);
        }
        // 发送异步rpc请求
        {
            ExampleMessages.Hello hello = new ExampleMessages.Hello();
            hello.setId(index);
            hello.setMessage("asyncRpcRequest without future - " + System.currentTimeMillis());
            session.call(hello, rpcResponse -> {
                System.out.println("\nasyncRpcResponse without future - " + JsonUtils.toJson(rpcResponse));
            });
        }
        // 发送同步rpc请求
        {
            ExampleMessages.Hello hello = new ExampleMessages.Hello();
            hello.setId(index);
            hello.setMessage("syncRpcRequest - " + System.currentTimeMillis());
            RpcResponse rpcResponse = session.sync(hello);
            System.out.println("\nsyncRpcResponse - " + JsonUtils.toJson(rpcResponse));
        }

        // 发起http请求
        String url = NetUtils.getLocalIp() + ":" + ExampleConstants.httpPort;
        // 异步get
        {
            HashMap<String, String> params = new HashMap<>();
            params.put("asyncGetIndex", String.valueOf(index));
            netContext.asyncGet(url, params, new OkHttpCallback() {
                @Override
                public void onFailure(@Nonnull Call call, @Nonnull IOException cause) {
                    System.out.println("asycnGet  failure.");
                }

                @Override
                public void onResponse(@Nonnull Call call, @Nonnull Response response) throws IOException {
                    System.out.println("\nasyncGet  response - " + response.body().string());
                }
            });
        }
        // 同步get
        {
            HashMap<String, String> params = new HashMap<>();
            params.put("syncGetIndex", String.valueOf(index));
            try {
                Response response = netContext.syncGet(url, params);
                System.out.println("\nsyncGet response - " + response.body().string());
            } catch (IOException e) {
                System.out.println("syncGet caught exception.");
                e.printStackTrace();
            }
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

    private class ServerDisconnectAware implements SessionDisconnectAware {

        @Override
        public void onSessionDisconnected(Session session) {
            System.out.println(" =========== onSessionDisconnected ==============");
            EchoClientLoop.this.session = null;
            // 断开连接后关闭
            shutdown();
        }
    }

    private class EchoProtocolDispatcher implements ProtocolDispatcher {

        @Override
        public void postOneWayMessage(Session session, @Nullable Object message) {
            System.out.println("\nonMessage - " + JsonUtils.toJson(message));
        }

        @Override
        public void postRpcRequest(Session session, @Nullable Object request, @Nonnull RpcResponseChannel<?> responseChannel) {
            // unreachable
        }

        @Override
        public void postRpcCallback(Session session, RpcCallback rpcCallback, RpcResponse rpcResponse) {
            rpcCallback.onComplete(rpcResponse);
        }
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        EchoClientLoop echoClientLoop = new EchoClientLoop(new DefaultThreadFactory("CLIENT"),
                RejectedExecutionHandlers.log());

        // 唤醒线程
        echoClientLoop.execute(() -> {
        });
    }
}
