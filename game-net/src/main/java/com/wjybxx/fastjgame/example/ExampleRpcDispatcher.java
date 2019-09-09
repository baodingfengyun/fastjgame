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

import com.wjybxx.fastjgame.misc.RpcCall;
import com.wjybxx.fastjgame.misc.RpcCallDispatcher;
import com.wjybxx.fastjgame.misc.VoidRpcResponseChannel;
import com.wjybxx.fastjgame.net.ProtocolDispatcher;
import com.wjybxx.fastjgame.net.RpcResponseChannel;
import com.wjybxx.fastjgame.net.Session;

import javax.annotation.Nullable;

/**
 * rpc请求分发器示例
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/26
 * github - https://github.com/hl845740757
 */
public class ExampleRpcDispatcher implements ProtocolDispatcher {

    private final RpcCallDispatcher dispatcher;

    public ExampleRpcDispatcher(RpcCallDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void postRpcRequest(Session session, @Nullable Object request, RpcResponseChannel<?> responseChannel) {
        if (request instanceof RpcCall) {
            dispatcher.post(session, (RpcCall) request, responseChannel);
        }
    }

    @Override
    public void postOneWayMessage(Session session, @Nullable Object message) {
        if (message instanceof RpcCall) {
            dispatcher.post(session, (RpcCall) message, VoidRpcResponseChannel.INSTANCE);
        }
    }
}
