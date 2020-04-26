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

import com.wjybxx.fastjgame.net.rpc.AbstractRpcClient;
import com.wjybxx.fastjgame.net.rpc.RpcMethodSpec;
import com.wjybxx.fastjgame.net.rpc.RpcServerSpec;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.concurrent.FluentFuture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletionException;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/8
 * github - https://github.com/hl845740757
 */
public class ExampleRpcClient extends AbstractRpcClient {

    ExampleRpcClient(EventLoop defaultExecutor) {
        super(defaultExecutor);
    }

    @Override
    public void send(@Nonnull RpcServerSpec serverSpec, @Nonnull RpcMethodSpec<?> message) {
        if (serverSpec instanceof Session) {
            invoker.send((Session) serverSpec, message, false);
        }
    }

    @Override
    public void sendAndFlush(@Nonnull RpcServerSpec serverSpec, @Nonnull RpcMethodSpec<?> message) {
        if (serverSpec instanceof Session) {
            invoker.send((Session) serverSpec, message, true);
        }
    }

    @Override
    public <V> FluentFuture<V> call(@Nonnull RpcServerSpec serverSpec, @Nonnull RpcMethodSpec<V> request) {
        if (serverSpec instanceof Session) {
            return invoker.call((Session) serverSpec, request, false);
        }
        return newSessionNotFoundFuture(serverSpec);
    }

    @Override
    public <V> FluentFuture<V> callAndFlush(@Nonnull RpcServerSpec serverSpec, @Nonnull RpcMethodSpec<V> request) {
        if (serverSpec instanceof Session) {
            return invoker.call((Session) serverSpec, request, true);
        }
        return newSessionNotFoundFuture(serverSpec);
    }

    @Nullable
    @Override
    public <V> V syncCall(@Nonnull RpcServerSpec serverSpec, @Nonnull RpcMethodSpec<V> request) throws CompletionException {
        if (serverSpec instanceof Session) {
            return invoker.syncCall((Session) serverSpec, request);
        }
        final FluentFuture<V> failedRpcFuture = newSessionNotFoundFuture(serverSpec);
        return failedRpcFuture.join();
    }
}
