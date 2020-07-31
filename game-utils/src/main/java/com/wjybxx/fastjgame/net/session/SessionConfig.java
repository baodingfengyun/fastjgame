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

package com.wjybxx.fastjgame.net.session;

import com.wjybxx.fastjgame.net.rpc.RpcRequestDispatcher;
import com.wjybxx.fastjgame.net.serialization.Serializer;
import com.wjybxx.fastjgame.util.CheckUtils;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * session的一些配置。
 * 目前决定使用不可变对象，可以减少对象数量。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/26
 * github - https://github.com/hl845740757
 */
@Immutable
public class SessionConfig {

    private final SessionLifecycleAware lifecycleAware;
    private final Serializer serializer;
    private final RpcRequestDispatcher dispatcher;
    private final long sessionTimeoutMs;

    private final boolean rpcAvailable;
    private final long asyncRpcTimeoutMs;
    private final long syncRpcTimeoutMs;

    protected SessionConfig(SessionConfigBuilder builder) {
        this.lifecycleAware = builder.lifecycleAware;
        this.serializer = builder.serializer;
        this.dispatcher = builder.rpcRequestDispatcher;
        this.sessionTimeoutMs = builder.sessionTimeoutMs;

        this.rpcAvailable = builder.rpcAvailable;
        this.asyncRpcTimeoutMs = builder.asyncRpcTimeoutMs;
        this.syncRpcTimeoutMs = builder.syncRpcTimeoutMs;
    }

    /**
     * @return 生命周期回调
     */
    public SessionLifecycleAware lifecycleAware() {
        return lifecycleAware;
    }

    /**
     * @return 协议内容编解码器
     */
    public Serializer serializer() {
        return serializer;
    }

    /**
     * @return 请求分发器
     */
    public RpcRequestDispatcher dispatcher() {
        return dispatcher;
    }

    /**
     * @return 会话超时时间，毫秒
     */
    public long getSessionTimeoutMs() {
        return sessionTimeoutMs;
    }

    /**
     * @return rpc是否可用(是否启用rpc支持)
     */
    public boolean isRpcAvailable() {
        return rpcAvailable;
    }

    /**
     * @return 异步rpc调用超时时间，毫秒
     */
    public long getAsyncRpcTimeoutMs() {
        return asyncRpcTimeoutMs;
    }

    /**
     * @return 同步rpc调用超时时间，毫秒
     */
    public long getSyncRpcTimeoutMs() {
        return syncRpcTimeoutMs;
    }

    public static SessionConfigBuilder newBuilder() {
        return new SessionConfigBuilder();
    }

    public static class SessionConfigBuilder<T extends SessionConfigBuilder<T, U>, U extends SessionConfig> {

        private SessionLifecycleAware lifecycleAware;
        private Serializer serializer;
        private RpcRequestDispatcher rpcRequestDispatcher;
        private int sessionTimeoutMs = 60 * 1000;

        private boolean rpcAvailable = true;
        private int asyncRpcTimeoutMs = 15 * 1000;
        private int syncRpcTimeoutMs = 5 * 1000;

        public T setLifecycleAware(@Nonnull SessionLifecycleAware lifecycleAware) {
            this.lifecycleAware = lifecycleAware;
            return self();
        }

        public T setSerializer(@Nonnull Serializer serializer) {
            this.serializer = serializer;
            return self();
        }

        public T setDispatcher(@Nonnull RpcRequestDispatcher dispatcher) {
            this.rpcRequestDispatcher = dispatcher;
            return self();
        }

        public T setSessionTimeoutMs(int sessionTimeoutMs) {
            this.sessionTimeoutMs = CheckUtils.requirePositive(sessionTimeoutMs, "sessionTimeoutMs");
            return self();
        }

        public SessionConfigBuilder<T, U> setRpcAvailable(boolean rpcAvailable) {
            this.rpcAvailable = rpcAvailable;
            return this;
        }

        public T setAsyncRpcTimeoutMs(int asyncRpcTimeoutMs) {
            this.asyncRpcTimeoutMs = CheckUtils.requirePositive(asyncRpcTimeoutMs, "asyncRpcTimeoutMs");
            return self();
        }

        public T setSyncRpcTimeoutMs(int syncRpcTimeoutMs) {
            this.syncRpcTimeoutMs = CheckUtils.requirePositive(asyncRpcTimeoutMs, "syncRpcTimeoutMs");
            return self();
        }

        public final U build() {
            checkParams();
            return newInstance();
        }

        @SuppressWarnings("unchecked")
        protected U newInstance() {
            return (U) new SessionConfig(this);
        }

        protected void checkParams() {
            Objects.requireNonNull(lifecycleAware, "lifecycleAware");
            Objects.requireNonNull(serializer, "serializer");
            Objects.requireNonNull(rpcRequestDispatcher, "rpcRequestDispatcher");
        }

        @SuppressWarnings("unchecked")
        protected final T self() {
            return (T) this;
        }

    }

}
