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

import com.wjybxx.fastjgame.concurrent.*;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroup;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroupImp;
import com.wjybxx.fastjgame.misc.DefaultRpcCallDispatcher;
import com.wjybxx.fastjgame.misc.RpcCallDispatcher;
import com.wjybxx.fastjgame.net.NetContext;
import com.wjybxx.fastjgame.net.Session;
import com.wjybxx.fastjgame.net.SessionLifecycleAware;
import com.wjybxx.fastjgame.net.SessionSenderMode;
import com.wjybxx.fastjgame.net.injvm.JVMPort;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.NetUtils;
import com.wjybxx.fastjgame.utils.TimeUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.LockSupport;

/**
 * 示例rpc服务器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/10
 * github - https://github.com/hl845740757
 */
class ExampleRpcServerLoop extends SingleThreadEventLoop {

    private final NetEventLoopGroup netGroup = new NetEventLoopGroupImp(1, new DefaultThreadFactory("NET-EVENT-LOOP"),
            RejectedExecutionHandlers.log());
    private final RpcCallDispatcher dispatcher;

    private NetContext netContext;
    private final Promise<JVMPort> jvmPortPromise;

    public ExampleRpcServerLoop(@Nonnull ThreadFactory threadFactory,
                                @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                                @Nonnull RpcCallDispatcher dispatcher,
                                @Nullable Promise<JVMPort> jvmPortPromise) {
        super(null, threadFactory, rejectedExecutionHandler);
        this.dispatcher = dispatcher;
        this.jvmPortPromise = jvmPortPromise;
    }

    @Override
    protected void init() throws Exception {
        super.init();
        // 创建网络环境
        netContext = netGroup.createContext(ExampleConstants.serverGuid, ExampleConstants.serverRole, this).get();

        if (jvmPortPromise != null) {
            // 绑定jvm端口
            try {
                final JVMPort jvmPort = netContext.bindInJVM(ExampleConstants.reflectBasedCodec,
                        new ClientLifeAware(),
                        new ExampleRpcDispatcher(dispatcher),
                        SessionSenderMode.DIRECT).get();
                jvmPortPromise.trySuccess(jvmPort);
            } catch (Exception e) {
                jvmPortPromise.tryFailure(e);
            }
        } else {
            // 监听tcp端口
            netContext.bindTcp(NetUtils.getLocalIp(),
                    ExampleConstants.tcpPort,
                    ExampleConstants.reflectBasedCodec,
                    new ClientLifeAware(),
                    new ExampleRpcDispatcher(dispatcher),
                    SessionSenderMode.DIRECT);
        }
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

    public static void main(String[] args) {
        final DefaultRpcCallDispatcher dispatcher = new DefaultRpcCallDispatcher();
        ExampleRpcServiceRpcRegister.register(dispatcher, new ExampleRpcService());
        final ExampleRpcServerLoop serviceLoop = new ExampleRpcServerLoop(new DefaultThreadFactory("SERVICE"),
                RejectedExecutionHandlers.log(),
                dispatcher,
                null);
        // 唤醒线程
        serviceLoop.execute(ConcurrentUtils.NO_OP_TASK);
    }
}
