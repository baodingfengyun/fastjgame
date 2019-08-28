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

    /**
     * 远程方法信息
     */
    private final RpcCall call;
    /**
     * 如果返回值类型为void/Void，那么表示不允许添加回调。
     */
    private final boolean listenable;
    /**
     * 默认为空回调，代替null判断
     */
    private RpcCallback callback = EmptyRpcCallback.INSTANCE;
    /**
     * 是否可用
     */
    private boolean available = true;

    public DefaultRpcBuilder(int methodKey, List<Object> methodParams, boolean listenable) {
        this.call = new RpcCall(methodKey, methodParams);
        this.listenable = listenable;
    }

    @Override
    public boolean isListenable() {
        return listenable;
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
        ensureListenable();
        // 多数情况下我们都只有一个回调
        if (callback == EmptyRpcCallback.INSTANCE) {
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

    /**
     * 确保允许添加rpc回调
     */
    private void ensureListenable() {
        if (!listenable) {
            throw new UnsupportedOperationException("this call " + call.getMethodKey() + " is not support callback!");
        }
    }

    /**
     * 确认状态是否正确
     */
    private void ensureState() {
        if (!available) {
            throw new IllegalStateException("this builder does not support reuse!");
        }
        // 如果是可监听的，则只能使用一次
        if (listenable) {
            available = false;
        }
    }

    @Override
    public void send(@Nullable Session session) throws IllegalStateException {
        ensureState();
        if (session != null) {
            POLICY.send(session, call);
        }
        // else do nothing
    }

    @Override
    public final void call(@Nullable Session session) {
        ensureState();
        ensureListenable();

        if (session == null) {
            // session不存在，安全的失败
            if (callback != EmptyRpcCallback.INSTANCE) {
                callback.onComplete(RpcResponse.SESSION_NULL);
            }
        } else {
            // 根据是否存在回调调用不同接口 - 可以减少不必要的传输
            if (callback == EmptyRpcCallback.INSTANCE) {
                POLICY.send(session, call);
            } else {
                POLICY.rpc(session, call, callback);
            }
        }
    }

    @Override
    public final RpcResponse sync(@Nullable Session session) {
        ensureState();
        ensureListenable();

        final RpcResponse response;
        if (session == null) {
            // session不存在，安全的失败
            response = RpcResponse.SESSION_NULL;
        } else {
            response = POLICY.sync(session, call);
        }
        // 返回之前，先执行添加的回调
        if (callback != EmptyRpcCallback.INSTANCE) {
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
        ensureState();
        ensureListenable();

        final RpcFuture future;
        if (session == null) {
            // session不存在，安全的失败
            future = new CompletedRpcFuture(ImmediateEventLoop.INSTANCE, RpcResponse.SESSION_NULL);
        } else {
            future = POLICY.rpc(session, call);
        }
        // 添加之前设置的回调
        if (callback != EmptyRpcCallback.INSTANCE) {
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
