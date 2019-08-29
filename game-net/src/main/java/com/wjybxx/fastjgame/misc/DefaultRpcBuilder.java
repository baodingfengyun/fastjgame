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

import com.wjybxx.fastjgame.concurrent.ImmediateEventLoop;
import com.wjybxx.fastjgame.configwrapper.ConfigWrapper;
import com.wjybxx.fastjgame.configwrapper.EmptyConfigWrapper;
import com.wjybxx.fastjgame.manager.NetConfigManager;
import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.ConfigLoader;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * {@link RpcBuilder}的默认实现
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/23
 * github - https://github.com/hl845740757
 */
public class DefaultRpcBuilder<V> implements RpcBuilder<V>{

    private static final InvokePolicy POLICY;

    static {
        ConfigWrapper configWrapper;
        try {
            configWrapper = ConfigLoader.loadConfig(ClassLoader.getSystemClassLoader(), NetConfigManager.NET_CONFIG_NAME);
        } catch (Exception ignore) {
            configWrapper = EmptyConfigWrapper.INSTANCE;
        }
        if (configWrapper.getAsBool("DefaultRpcBuilder.POLICY.PIPELINE", false)){
            POLICY = new PipelinePolicy();
        } else {
            POLICY = new SessionPolicy();
        }
    }

    private static final int SHARE_MODE_ANY = 0;
    private static final int SHARE_MODE_SEND = 1;
    private static final int SHARE_MODE_NONE = 2;

    /**
     * 远程方法信息
     */
    private final RpcCall call;
    /**
     * 添加的回调
     */
    private RpcCallback callback = null;
    /**
     * 共享模式
     */
    private int shareMode = SHARE_MODE_ANY;

    public DefaultRpcBuilder(int methodKey, List<Object> methodParams) {
        this.call = new RpcCall(methodKey, methodParams);
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

    @Override
    public void send(@Nullable Session session) throws IllegalStateException {
        ensureSendAvailable();
        if (session != null) {
            POLICY.send(session, call);
        }
        // else do nothing
    }

    /**
     * 确保可发送send请求
     */
    private void ensureSendAvailable() {
        if (shareMode > SHARE_MODE_SEND) {
            throw new IllegalStateException("this builder does not support reuse!");
        }
        shareMode = SHARE_MODE_SEND;
    }

    /**
     * 确保可发送rpc请求
     */
    private void ensureRpcAvailable() {
        if (shareMode > SHARE_MODE_ANY) {
            throw new IllegalStateException("this builder does not support reuse!");
        }
        shareMode = SHARE_MODE_NONE;
    }

    @Override
    public final void call(@Nullable Session session) {
        // 可监听，并且设置了回调
        ensureRpcAvailable();
        if (session == null) {
            if (callback != null) {
                // session不存在，安全的失败
                callback.onComplete(RpcResponse.SESSION_NULL);
            }
        } else {
            if (callback == null) {
                // 没有设置回调，使用通知代替rpc调用，对方不会返回结果
                POLICY.send(session, call);
            } else {
                // 设置了回调，走rpc，对方一定会返回一个值
                POLICY.rpc(session, call, callback);
            }
        }
    }

    @Override
    public final RpcResponse sync(@Nullable Session session) throws IllegalStateException {
        ensureRpcAvailable();
        final RpcResponse response;
        if (session == null) {
            // session不存在，安全的失败
            response = RpcResponse.SESSION_NULL;
        } else {
            response = POLICY.sync(session, call);
        }
        // 返回之前，先执行添加的回调
        if (callback != null) {
            ConcurrentUtils.safeExecute((Runnable)() -> callback.onComplete(response));
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public V syncCall(@Nullable Session session) {
        final RpcResponse rpcResponse = sync(session);
        if (rpcResponse.isSuccess()) {
            return (V) rpcResponse.getBody();
        } else {
            return null;
        }
    }

    @Override
    public final RpcFuture submit(@Nullable Session session) {
        ensureRpcAvailable();
        final RpcFuture future;
        if (session == null) {
            // session不存在，安全的失败
            future = new CompletedRpcFuture(ImmediateEventLoop.INSTANCE, RpcResponse.SESSION_NULL);
        } else {
            future = POLICY.rpc(session, call);
        }
        // 添加之前设置的回调
        if (callback != null) {
            future.addCallback(callback);
        }
        return future;
    }

    /**
     * 发送消息的策略
     */
    private interface InvokePolicy {

        /**
         * 发送一个单向通知。
         */
        void send(@Nonnull Session session, @Nonnull RpcCall call);

        /**
         * 执行异步rpc调用
         */
        void rpc(@Nonnull Session session, @Nonnull RpcCall call, @Nonnull RpcCallback callback);

        /**
         * 执行异步调用并返回一个future
         * @return future
         */
        RpcFuture rpc(@Nonnull Session session, @Nonnull RpcCall call);

        /**
         * 执行同步rpc调用，并直接获得结果。
         * @return result
         */
        RpcResponse sync(@Nonnull Session session, @Nonnull RpcCall call);
    }

    /**
     * 直接通过session发送消息的策略
     */
    private static class SessionPolicy implements InvokePolicy {

        @Override
        public void send(@Nonnull Session session, @Nonnull RpcCall call) {
            session.sendMessage(call);
        }

        @Override
        public void rpc(@Nonnull Session session, @Nonnull RpcCall call, @Nonnull RpcCallback callback) {
            session.rpc(call, callback);
        }

        @Override
        public RpcFuture rpc(@Nonnull Session session, @Nonnull RpcCall call) {
            return session.rpc(call);
        }

        @Override
        public RpcResponse sync(@Nonnull Session session, @Nonnull RpcCall call) {
            return session.syncRpcUninterruptibly(call);
        }
    }

    /**
     * 通过pipeline发送消息的策略
     */
    private static class PipelinePolicy implements InvokePolicy {

        @Override
        public void send(@Nonnull Session session, @Nonnull RpcCall call) {
            session.pipeline().enqueueMessage(call);
        }

        @Override
        public void rpc(@Nonnull Session session, @Nonnull RpcCall call, @Nonnull RpcCallback callback) {
            session.pipeline().enqueueRpc(call, callback);
        }

        @Override
        public RpcFuture rpc(@Nonnull Session session, @Nonnull RpcCall call) {
            return session.pipeline().enqueueRpc(call);
        }

        @Override
        public RpcResponse sync(@Nonnull Session session, @Nonnull RpcCall call) {
            return session.pipeline().syncRpcUninterruptibly(call);
        }
    }
}
