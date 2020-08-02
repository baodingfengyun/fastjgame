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
import com.wjybxx.fastjgame.net.exception.RpcSessionNotFoundException;
import com.wjybxx.fastjgame.net.exception.RpcTimeoutException;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.util.concurrent.FluentFuture;
import com.wjybxx.fastjgame.util.concurrent.FutureUtils;
import com.wjybxx.fastjgame.util.concurrent.Promise;
import com.wjybxx.fastjgame.util.function.FunctionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import static com.wjybxx.fastjgame.util.concurrent.FutureUtils.newFailedFuture;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/9
 */
public final class DefaultRpcClientInvoker implements RpcClientInvoker {

    public DefaultRpcClientInvoker() {
    }

    @Override
    public void send(@Nullable Session session, @Nonnull RpcMethodSpec<?> message, boolean flush) {
        if (session == null || session.isClosed()) {
            // session不存在或关闭的情况下丢弃消息
            return;
        }
        session.netEventLoop().execute(new OneWayWriteTask(session, message, flush));
    }

    @Override
    public <V> FluentFuture<V> call(@Nullable Session session, @Nonnull RpcMethodSpec<V> request, boolean flush) {
        if (session == null) {
            // session不存在
            return newSessionNotFoundFuture();
        }

        if (session.isClosed()) {
            // session关闭状态下直接返回
            return newFailedFuture(RpcSessionClosedException.INSTANCE);
        }

        // 会话活动的状态下才会发送
        final Promise<V> promise = FutureUtils.newPromise();
        session.netEventLoop()
                .execute(new RpcRequestWriteTask(session, request, false, session.config().getAsyncRpcTimeoutMs(), promise, flush));

        // 回调到用户线程
        return promise.whenCompleteAsync(FunctionUtils.emptyBiConsumer(), session.appEventLoop());
    }

    @Nullable
    @Override
    public <V> V syncCall(@Nullable Session session, @Nonnull RpcMethodSpec<V> request) throws CompletionException {
        if (session == null) {
            // session不存在
            final FluentFuture<V> future = newSessionNotFoundFuture();
            return future.getNow();
        }

        if (session.isClosed()) {
            // session关闭状态下直接返回
            final FluentFuture<V> future = newFailedFuture(RpcSessionClosedException.INSTANCE);
            return future.getNow();
        }

        final Promise<V> promise = FutureUtils.newPromise();
        final long syncRpcTimeoutMs = session.config().getSyncRpcTimeoutMs();

        session.netEventLoop()
                .execute(new RpcRequestWriteTask(session, request, true, syncRpcTimeoutMs, promise, true));

        if (!promise.awaitUninterruptibly(syncRpcTimeoutMs, TimeUnit.MILLISECONDS)) {
            promise.tryFailure(RpcTimeoutException.INSTANCE);
        }

        return promise.getNow();
    }

    private static <V> FluentFuture<V> newSessionNotFoundFuture() {
        return newFailedFuture(new RpcSessionNotFoundException());
    }
}
