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

package com.wjybxx.fastjgame.misc;

import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * {@link RpcBuilder}的默认实现
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/23
 * github - https://github.com/hl845740757
 */
public class DefaultRpcBuilder<V> implements RpcBuilder<V>{

    /**
     * 想改变策略修改这里的实例即可
     */
    private static final InvokePolicy POLICY = new SessionPolicy<>();

    private final RpcCall<V> call;
    private Session session;
    private RpcCallback callback;

    protected DefaultRpcBuilder(@Nonnull RpcCall<V> call) {
        this.call = call;
    }

    @Nonnull
    @Override
    public final RpcCall<V> getCall() {
        return call;
    }

    @Override
    public final RpcBuilder<V> setSession(@Nonnull Session session) {
        this.session = session;
        return this;
    }

    @Override
    public final RpcBuilder<V> ifSuccess(@Nonnull SucceedRpcCallback<V> callback) {
        addCallback(callback);
        return this;
    }

    @Override
    public final RpcBuilder<V> ifFailure(@Nonnull FailedRpcCallback callback) {
        addCallback(callback);
        return this;
    }

    @Override
    public final RpcBuilder<V> any(@Nonnull RpcCallback callback) {
        addCallback(callback);
        return this;
    }

    private void addCallback(final RpcCallback newCallback) {
        if (!call.isAllowCallback()) {
            throw new IllegalArgumentException("this call " + call.getMethodKey() + " is not support callback!");
        }
        // 多数情况下我们都只有一个回调
        if (callback == null) {
            callback = newCallback;
            return;
        }
        // 添加超过两次
        if (callback instanceof CompositeRpcCallback) {
            ((CompositeRpcCallback)this.callback).any(newCallback);
        } else {
            // 添加的第二个回调
            callback = new CompositeRpcCallback<>(callback, newCallback);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void send() {
        ensureSession();
        POLICY.send(session, call);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void invoke() {
        ensureSession();
        POLICY.invoke(session, call, callback);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final RpcFuture execute() {
        ensureSession();
        return POLICY.execute(session, call, callback);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final RpcResponse sync() {
        ensureSession();
        return POLICY.sync(session, call, callback);
    }

    private void ensureSession() {
        if (session == null) {
            throw new IllegalStateException("session is null");
        }
    }

    /**
     * 发送消息的策略
     */
    private interface InvokePolicy<V> {

        /**
         * 发送一个单向通知。
         */
        void send(@Nonnull Session session, @Nonnull RpcCall<V> call);

        /**
         * 执行异步rpc调用，请确保设置了回调，没有设置回调也不会进行警告。
         */
        void invoke(@Nonnull Session session, @Nonnull RpcCall<V> call, @Nullable RpcCallback callback);

        /**
         * 执行异步调用并返回一个future
         * @return future
         */
        RpcFuture execute(@Nonnull Session session, @Nonnull RpcCall<V> call, @Nullable RpcCallback callback);

        /**
         * 执行同步rpc调用，并直接获得结果。
         * @return result
         */
        RpcResponse sync(@Nonnull Session session, @Nonnull RpcCall<V> call, @Nullable RpcCallback callback);
    }

    /**
     * 直接通过session发送消息的策略
     * @param <V>
     */
    private static class SessionPolicy<V> implements InvokePolicy<V> {

        @Override
        public void send(@Nonnull Session session, @Nonnull RpcCall<V> call) {
            session.sendMessage(call);
        }

        @Override
        public void invoke(@Nonnull Session session, @Nonnull RpcCall<V> call, @Nullable RpcCallback callback) {
            if (null == callback) {
                session.rpc(call, EmptyCallback.INSTANCE);
            } else {
                session.rpc(call, callback);
            }
        }

        @Override
        public RpcFuture execute(@Nonnull Session session, @Nonnull RpcCall<V> call, @Nullable RpcCallback callback) {
            RpcFuture rpcFuture = session.rpc(call);
            if (null != callback) {
                rpcFuture.addCallback(callback);
            }
            return rpcFuture;
        }

        @Override
        public RpcResponse sync(@Nonnull Session session, @Nonnull RpcCall<V> call, @Nullable RpcCallback callback) {
            RpcResponse rpcResponse = session.syncRpcUninterruptibly(call);
            if (null != callback) {
                ConcurrentUtils.safeExecute((Runnable) () -> callback.onComplete(rpcResponse));
            }
            return rpcResponse;
        }
    }

    /**
     * 通过pipeline发送消息的策略
     * @param <V>
     */
    private static class PipelinePolicy<V> implements InvokePolicy<V> {

        @Override
        public void send(@Nonnull Session session, @Nonnull RpcCall<V> call) {
            session.pipeline().enqueueMessage(call);
        }

        @Override
        public void invoke(@Nonnull Session session, @Nonnull RpcCall<V> call, @Nullable RpcCallback callback) {
            if (null == callback) {
                session.pipeline().enqueueRpc(call, EmptyCallback.INSTANCE);
            } else {
                session.pipeline().enqueueRpc(call, callback);
            }
        }

        @Override
        public RpcFuture execute(@Nonnull Session session, @Nonnull RpcCall<V> call, @Nullable RpcCallback callback) {
            PipelineRpcFuture rpcFuture = session.pipeline().enqueueRpc(call);
            if (null != callback) {
                rpcFuture.addCallback(callback);
            }
            return rpcFuture;
        }

        @Override
        public RpcResponse sync(@Nonnull Session session, @Nonnull RpcCall<V> call, @Nullable RpcCallback callback) {
            RpcResponse rpcResponse = session.pipeline().syncRpcUninterruptibly(call);
            if (null != callback) {
                ConcurrentUtils.safeExecute((Runnable) () -> callback.onComplete(rpcResponse));
            }
            return rpcResponse;
        }
    }

    private static class EmptyCallback implements RpcCallback {

        private static final EmptyCallback INSTANCE = new EmptyCallback();

        @Override
        public void onComplete(RpcResponse rpcResponse) {
            // ignore
        }
    }
}
