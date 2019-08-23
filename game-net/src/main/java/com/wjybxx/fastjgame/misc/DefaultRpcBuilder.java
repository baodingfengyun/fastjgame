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

    private static final int ST_INIT = 0;
    private static final int ST_PREPARED = 1;
    private static final int ST_TERMINATED = 2;

    /**
     * 远程方法信息
     */
    private final RpcCall<V> call;
    /**
     * 如果返回值类型为void/Void，那么表示不允许添加回调。
     */
    private final boolean allowCallback;
    /**
     * 发送消息的目的放
     */
    private Session session;
    /**
     * 默认为空回调，代替null判断
     */
    private RpcCallback callback = EmptyRpcCallback.INSTANCE;
    /**
     * 当前的状态
     */
    private int state = ST_INIT;

    public DefaultRpcBuilder(int methodKey, List<Object> methodParams, boolean allowCallback) {
        this.call = new RpcCall<>(methodKey, methodParams);
        this.allowCallback = allowCallback;
    }

    @Nonnull
    @Override
    public final RpcCall<V> getCall() {
        return call;
    }

    @Override
    public final RpcBuilder<V> setSession(@Nullable Session session) {
        if (state != ST_INIT) {
            throw new IllegalStateException("session is already set!");
        }
        state = ST_PREPARED;
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
        ensureAllowCallback();
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
     * 确认状态是否正确
     */
    private void ensurePrepareState() {
        if (state == ST_PREPARED) {
            state = ST_TERMINATED;
            return;
        }
        if (state == ST_INIT) {
            throw new IllegalStateException("Session is not set!");
        }
        if (state == ST_TERMINATED) {
            throw new IllegalStateException("Builder does not support reuse!");
        }
    }

    /**
     * 确保允许添加rpc回调
     */
    private void ensureAllowCallback() {
        if (!allowCallback) {
            throw new UnsupportedOperationException("this call " + call.getMethodKey() + " is not support callback!");
        }
    }

    @Override
    public final void execute() {
        ensurePrepareState();
        if (session == null) {
            // session不存在，安全的失败
            if (callback != EmptyRpcCallback.INSTANCE) {
                callback.onComplete(RpcResponse.SESSION_NULL);
            }
        } else {
            // 根据是否存在回调调用不同接口
            if (callback == EmptyRpcCallback.INSTANCE) {
                POLICY.sendMessage(session, call);
            } else {
                POLICY.execute(session, call, callback);
            }
        }
    }

    @Override
    public final RpcFuture submit() {
        ensurePrepareState();
        ensureAllowCallback();

        final RpcFuture future;
        if (session == null) {
            // session不存在，安全的失败
            future = new CompletedRpcFuture(ImmediateEventLoop.INSTANCE, RpcResponse.SESSION_NULL);
        } else {
            future = POLICY.submit(session, call);
        }
        // 添加之前设置的回调
        if (callback != EmptyRpcCallback.INSTANCE) {
            future.addCallback(callback);
        }
        return future;
    }

    @Override
    public final RpcResponse sync() {
        ensurePrepareState();
        ensureAllowCallback();

        final RpcResponse response;
        if (session == null) {
            // session不存在，安全的失败
            response = RpcResponse.SESSION_NULL;
        } else {
            response = POLICY.sync(session, call);
        }
        // 返回之前，先执行添加到回调
        if (callback != EmptyRpcCallback.INSTANCE) {
            ConcurrentUtils.safeExecute((Runnable)() -> callback.onComplete(response));
        }
        return response;
    }

    /**
     * 发送消息的策略
     */
    private interface InvokePolicy {

        /**
         * 发送一个单向通知。
         */
        void sendMessage(@Nonnull Session session, @Nonnull RpcCall<?> call);

        /**
         * 执行异步rpc调用，请确保设置了回调，没有设置回调也不会进行警告。
         */
        void execute(@Nonnull Session session, @Nonnull RpcCall<?> call, @Nonnull RpcCallback callback);

        /**
         * 执行异步调用并返回一个future
         * @return future
         */
        RpcFuture submit(@Nonnull Session session, @Nonnull RpcCall<?> call);

        /**
         * 执行同步rpc调用，并直接获得结果。
         * @return result
         */
        RpcResponse sync(@Nonnull Session session, @Nonnull RpcCall<?> call);
    }

    /**
     * 直接通过session发送消息的策略
     */
    private static class SessionPolicy implements InvokePolicy {

        @Override
        public void sendMessage(@Nonnull Session session, @Nonnull RpcCall<?> call) {
            session.sendMessage(call);
        }

        @Override
        public void execute(@Nonnull Session session, @Nonnull RpcCall<?> call, @Nonnull RpcCallback callback) {
            session.rpc(call, callback);
        }

        @Override
        public RpcFuture submit(@Nonnull Session session, @Nonnull RpcCall<?> call) {
            return session.rpc(call);
        }

        @Override
        public RpcResponse sync(@Nonnull Session session, @Nonnull RpcCall<?> call) {
            return session.syncRpcUninterruptibly(call);
        }
    }

    /**
     * 通过pipeline发送消息的策略
     */
    private static class PipelinePolicy implements InvokePolicy {

        @Override
        public void sendMessage(@Nonnull Session session, @Nonnull RpcCall<?> call) {
            session.pipeline().enqueueMessage(call);
        }

        @Override
        public void execute(@Nonnull Session session, @Nonnull RpcCall<?> call, @Nonnull RpcCallback callback) {
            session.pipeline().enqueueRpc(call, callback);
        }

        @Override
        public RpcFuture submit(@Nonnull Session session, @Nonnull RpcCall<?> call) {
            return session.pipeline().enqueueRpc(call);
        }

        @Override
        public RpcResponse sync(@Nonnull Session session, @Nonnull RpcCall<?> call) {
            return session.pipeline().syncRpcUninterruptibly(call);
        }
    }
}
