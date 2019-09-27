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

package com.wjybxx.fastjgame.net.pipeline;

import com.wjybxx.fastjgame.net.ProtocolCodec;
import com.wjybxx.fastjgame.net.ProtocolDispatcher;
import com.wjybxx.fastjgame.net.SessionLifecycleAware;

import javax.annotation.Nonnull;

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

    private DefaultSessionConfig(Builder builder) {
        this.lifecycleAware = builder.lifecycleAware;
        this.codec = builder.codec;
        this.dispatcher = builder.dispatcher;
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

    public static class Builder {

        private SessionLifecycleAware lifecycleAware;
        private ProtocolCodec codec;
        private ProtocolDispatcher dispatcher;
        private int sessionTimeoutMs;
        private int rpcCallbackTimeoutMs;
        private int syncRpcTimeoutMs;

        public Builder setLifecycleAware(@Nonnull SessionLifecycleAware lifecycleAware) {
            this.lifecycleAware = lifecycleAware;
            return this;
        }

        public Builder setCodec(@Nonnull ProtocolCodec codec) {
            this.codec = codec;
            return this;
        }

        public Builder setDispatcher(@Nonnull ProtocolDispatcher dispatcher) {
            this.dispatcher = dispatcher;
            return this;
        }

        public Builder setSessionTimeoutMs(int sessionTimeoutMs) {
            checkPositive(sessionTimeoutMs, "sessionTimeoutMs must greater than zero");
            this.sessionTimeoutMs = sessionTimeoutMs;
            return this;
        }

        public Builder setRpcCallbackTimeoutMs(int rpcCallbackTimeoutMs) {
            checkPositive(rpcCallbackTimeoutMs, "rpcCallbackTimeoutMs must greater than zero");
            this.rpcCallbackTimeoutMs = rpcCallbackTimeoutMs;
            return this;
        }

        public Builder setSyncRpcTimeoutMs(int syncRpcTimeoutMs) {
            checkPositive(rpcCallbackTimeoutMs, "syncRpcTimeoutMs must greater than zero");
            this.syncRpcTimeoutMs = syncRpcTimeoutMs;
            return this;
        }

        public DefaultSessionConfig build() {
            return new DefaultSessionConfig(this);
        }

        /**
         * 检查一个数是否是正数
         *
         * @param param 参数
         * @param msg   信息
         */
        private static void checkPositive(int param, String msg) {
            if (param <= 0) {
                throw new IllegalArgumentException(msg);
            }
        }
    }

}
