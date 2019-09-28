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

package com.wjybxx.fastjgame.net;

import com.wjybxx.fastjgame.misc.SessionLifecycleAware;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * 默认session配置
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/27
 * github - https://github.com/hl845740757
 */
public class DefaultSessionConfig implements SessionConfig {

    private final SessionLifecycleAware lifecycleAware;
    private final ProtocolCodec codec;
    private final ProtocolDispatcher dispatcher;
    private final int sessionTimeoutMs;
    private final int rpcCallbackTimeoutMs;
    private final int syncRpcTimeoutMs;

    private DefaultSessionConfig(SessionConfigBuilder builder) {
        this.lifecycleAware = builder.lifecycleAware;
        this.codec = builder.protocolCodec;
        this.dispatcher = builder.protocolDispatcher;
        this.sessionTimeoutMs = builder.sessionTimeoutMs;
        this.rpcCallbackTimeoutMs = builder.rpcCallbackTimeoutMs;
        this.syncRpcTimeoutMs = builder.syncRpcTimeoutMs;
    }

    @Override
    public SessionLifecycleAware lifecycleAware() {
        return lifecycleAware;
    }

    @Override
    public ProtocolCodec codec() {
        return codec;
    }

    @Override
    public ProtocolDispatcher dispatcher() {
        return dispatcher;
    }

    @Override
    public int getSessionTimeoutMs() {
        return sessionTimeoutMs;
    }

    @Override
    public int getRpcCallbackTimeoutMs() {
        return rpcCallbackTimeoutMs;
    }

    @Override
    public int getSyncRpcTimeoutMs() {
        return syncRpcTimeoutMs;
    }

    public static SessionConfigBuilder newBuilder() {
        return new SessionConfigBuilder();
    }

    public static class SessionConfigBuilder {

        private SessionLifecycleAware lifecycleAware;
        private ProtocolCodec protocolCodec;
        private ProtocolDispatcher protocolDispatcher;
        private int sessionTimeoutMs = 60 * 1000;
        private int rpcCallbackTimeoutMs = 15 * 1000;
        private int syncRpcTimeoutMs = 5 * 1000;

        public SessionConfigBuilder setLifecycleAware(@Nonnull SessionLifecycleAware lifecycleAware) {
            this.lifecycleAware = lifecycleAware;
            return this;
        }

        public SessionConfigBuilder setProtocolCodec(@Nonnull ProtocolCodec protocolCodec) {
            this.protocolCodec = protocolCodec;
            return this;
        }

        public SessionConfigBuilder setProtocolDispatcher(@Nonnull ProtocolDispatcher protocolDispatcher) {
            this.protocolDispatcher = protocolDispatcher;
            return this;
        }

        public SessionConfigBuilder setSessionTimeoutMs(int sessionTimeoutMs) {
            checkPositive(sessionTimeoutMs, "sessionTimeoutMs must greater than zero");
            this.sessionTimeoutMs = sessionTimeoutMs;
            return this;
        }

        public SessionConfigBuilder setRpcCallbackTimeoutMs(int rpcCallbackTimeoutMs) {
            checkPositive(rpcCallbackTimeoutMs, "rpcCallbackTimeoutMs must greater than zero");
            this.rpcCallbackTimeoutMs = rpcCallbackTimeoutMs;
            return this;
        }

        public SessionConfigBuilder setSyncRpcTimeoutMs(int syncRpcTimeoutMs) {
            checkPositive(rpcCallbackTimeoutMs, "syncRpcTimeoutMs must greater than zero");
            this.syncRpcTimeoutMs = syncRpcTimeoutMs;
            return this;
        }

        public SessionConfig build() {
            Objects.requireNonNull(lifecycleAware, "lifecycleAware");
            Objects.requireNonNull(protocolCodec, "protocolCodec");
            Objects.requireNonNull(protocolDispatcher, "protocolDispatcher");

            return new DefaultSessionConfig(this);
        }

        /**
         * 检查一个数是否是正数
         *
         * @param param 参数
         * @param msg   信息
         */
        public static void checkPositive(int param, String msg) {
            if (param <= 0) {
                throw new IllegalArgumentException(msg);
            }
        }
    }

}
