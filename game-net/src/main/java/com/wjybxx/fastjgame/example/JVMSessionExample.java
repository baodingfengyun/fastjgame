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
import com.wjybxx.fastjgame.concurrent.GlobalEventLoop;
import com.wjybxx.fastjgame.concurrent.Promise;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.net.local.LocalPort;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;

import java.util.concurrent.ExecutionException;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/10
 * github - https://github.com/hl845740757
 */
public class JVMSessionExample {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        final Promise<LocalPort> promise = GlobalEventLoop.INSTANCE.newPromise();
        {
            ExampleRpcServerLoop exampleRpcServerLoop = new ExampleRpcServerLoop(new DefaultThreadFactory("SERVER"),
                    RejectedExecutionHandlers.discard(),
                    promise);
            // 唤醒、启动服务器
            exampleRpcServerLoop.execute(ConcurrentUtils.NO_OP_TASK);
        }

        final LocalPort localPort = promise.get();
        {
            ExampleRpcClientLoop echoClientLoop = new ExampleRpcClientLoop(
                    new DefaultThreadFactory("CLIENT"),
                    RejectedExecutionHandlers.discard(),
                    localPort);

            // 唤醒线程
            echoClientLoop.execute(ConcurrentUtils.NO_OP_TASK);
        }
    }

}
