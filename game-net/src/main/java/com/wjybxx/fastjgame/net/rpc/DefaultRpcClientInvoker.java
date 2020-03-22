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

package com.wjybxx.fastjgame.net.rpc;

import com.wjybxx.fastjgame.net.exception.RpcSessionClosedException;
import com.wjybxx.fastjgame.net.exception.RpcTimeoutException;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.utils.concurrent.BlockingPromise;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/9
 */
class DefaultRpcClientInvoker implements RpcClientInvoker {

    DefaultRpcClientInvoker() {
    }

    @Override
    public void send(@Nonnull Session session, @Nonnull RpcMethodSpec<?> message, boolean flush) {
        if (session.isClosed()) {
            // 会话关闭的情况下丢弃消息
            return;
        }
        session.netEventLoop().execute(new OneWayWriteTask(session, message, flush));
    }

    @Override
    public <V> RpcFuture<V> call(@Nonnull Session session, @Nonnull RpcMethodSpec<V> request, boolean flush) {
        if (session.isClosed()) {
            // session关闭状态下直接返回
            return session.netEventLoop().newFailedRpcFuture(session.appEventLoop(), RpcSessionClosedException.INSTANCE);
        }
        // 会话活动的状态下才会发送
        final RpcPromise<V> promise = session.netEventLoop().newRpcPromise(session.appEventLoop());
        session.netEventLoop().execute(new RpcRequestWriteTask(session, request, false, session.config().getAsyncRpcTimeoutMs(), promise, flush));
        return promise;
    }

    @Nullable
    @Override
    public <V> V syncCall(@Nonnull Session session, @Nonnull RpcMethodSpec<V> request) throws CompletionException {
        if (session.isClosed()) {
            // session关闭状态下直接返回
            return session.netEventLoop().<V>newFailedRpcFuture(session.appEventLoop(), RpcSessionClosedException.INSTANCE)
                    .getNow();
        }

        final BlockingPromise<V> blockingPromise = session.netEventLoop().newBlockingPromise();
        final long syncRpcTimeoutMs = session.config().getSyncRpcTimeoutMs();

        session.netEventLoop().execute(new RpcRequestWriteTask(session, request, true, syncRpcTimeoutMs, blockingPromise, true));

        if (!blockingPromise.awaitUninterruptibly(syncRpcTimeoutMs, TimeUnit.MILLISECONDS)) {
            blockingPromise.tryFailure(RpcTimeoutException.INSTANCE);
        }
        return blockingPromise.getNow();
    }

}
